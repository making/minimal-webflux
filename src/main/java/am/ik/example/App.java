package am.ik.example;

import java.time.Duration;
import java.util.Optional;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcConnectionPool;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.resources;

import reactor.netty.http.server.HttpServer;

public class App {

	static RouterFunction<ServerResponse> routes(DataSource dataSource) {
		TweetMapper tweetMapper = new TweetMapper(dataSource);
		TweetHandler tweetHandler = new TweetHandler(tweetMapper);
		return tweetHandler.routes()
				.andRoute(GET("/"), req -> ServerResponse
						.seeOther(req.uriBuilder().path("/index.html").build()).build())
				.and(resources("/**", new ClassPathResource("static/")));
	}

	static DataSource dataSource() {
		JdbcConnectionPool connectionPool = JdbcConnectionPool
				.create("jdbc:h2:./target/tweet", "sa", "sa");
		Flyway flyway = new Flyway();
		flyway.setDataSource(connectionPool);
		flyway.setLocations("classpath:db/migration");
		flyway.migrate();
		return connectionPool;
	}

	public static void main(String[] args) throws Exception {
		long begin = System.currentTimeMillis();
		int port = Optional.ofNullable(System.getenv("PORT")) //
				.map(Integer::parseInt) //
				.orElse(8080);
		HttpServer httpServer = HttpServer.create().host("0.0.0.0").port(port);
		httpServer.route(routes -> {
			HttpHandler httpHandler = RouterFunctions.toHttpHandler(
					App.routes(dataSource()), HandlerStrategies.builder().build());
			routes.route(x -> true, new ReactorHttpHandlerAdapter(httpHandler));
		}).bindUntilJavaShutdown(Duration.ofSeconds(3), disposableServer -> {
			long elapsed = System.currentTimeMillis() - begin;
			LoggerFactory.getLogger(App.class).info("Started in {} seconds",
					elapsed / 1000.0);
		});
	}
}
