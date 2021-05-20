package am.ik.example;

import java.time.Duration;
import java.util.Optional;

import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcConnectionPool;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.RouterFunctions.toHttpHandler;

import io.r2dbc.h2.H2ConnectionConfiguration;
import io.r2dbc.h2.H2ConnectionFactory;
import io.r2dbc.spi.ConnectionFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.server.HttpServer;

public class App {

	static RouterFunction<ServerResponse> routes(ConnectionFactory connectionFactory) {
		TweetMapper tweetMapper = new TweetMapper(connectionFactory);
		return route()
				.GET("/", req -> ServerResponse
						.seeOther(req.uriBuilder().path("/index.html").build()).build())
				.add(new TweetHandler(tweetMapper).routes())
				.resources("/**", new ClassPathResource("static/")).build();
	}

	static ConnectionFactory connectionFactory(String url, String username,
			String password, String database) {
		migrate(url, username, password, database);
		return new H2ConnectionFactory(H2ConnectionConfiguration.builder() //
				.url(url + ":" + database) //
				.username(username) //
				.password(password) //
				.build());
	}

	private static void migrate(String url, String username, String password,
			String database) {
		Flyway.configure()
						.dataSource(JdbcConnectionPool.create(
								"jdbc:h2:" + url + ":" + database, username, password))
						.locations("classpath:db/migration") //
						.load() //
						.migrate();
	}

	public static void main(String[] args) throws Exception {
		System.setProperty("reactor.netty.http.server.accessLogEnabled", "true");
		long begin = System.currentTimeMillis();
		int port = Optional.ofNullable(System.getenv("PORT")) //
				.map(Integer::parseInt) //
				.orElse(8080);
		HttpServer httpServer = HttpServer.create().host("0.0.0.0").port(port);
		httpServer.route(routes -> {
			HttpHandler httpHandler = toHttpHandler(
					App.routes(connectionFactory("./target/tweet", "sa", "sa", "test")),
					HandlerStrategies.builder().build());
			routes.route(x -> true, new ReactorHttpHandlerAdapter(httpHandler));
		}).bindUntilJavaShutdown(Duration.ofSeconds(3), disposableServer -> {
			long elapsed = System.currentTimeMillis() - begin;
			LoggerFactory.getLogger(App.class).info("Started in {} seconds",
					elapsed / 1000.0);
		});
	}
}
