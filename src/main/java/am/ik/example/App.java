package am.ik.example;

import java.time.Duration;
import java.util.Optional;

import org.flywaydb.core.Flyway;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.RouterFunctions.toHttpHandler;

import io.r2dbc.postgresql.PostgresqlConnectionConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
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

	static ConnectionFactory connectionFactory(String host, int port, String username,
			String password, String database) {
		migrate(host, port, username, password, database);
		return new PostgresqlConnectionFactory(PostgresqlConnectionConfiguration.builder() //
				.host(host) //
				.port(port) //
				.username(username) //
				.password(password) //
				.database(database) //
				.build());
	}

	private static void migrate(String host, int port, String username, String password,
			String database) {
		PGSimpleDataSource dataSource = new PGSimpleDataSource();
		dataSource.setUser(username);
		dataSource.setPassword(password);
		dataSource.setServerName(host);
		dataSource.setPortNumber(port);
		dataSource.setDatabaseName(database);
		Mono.empty() //
				.doFinally(x -> Flyway.configure().dataSource(dataSource)
						.locations("classpath:db/migration") //
						.load() //
						.migrate()) //
				.subscribeOn(Schedulers.single()) // run on background
				.subscribe();
	}

	public static void main(String[] args) throws Exception {
		System.setProperty("reactor.netty.http.server.accessLogEnabled", "true");
		long begin = System.currentTimeMillis();
		int port = Optional.ofNullable(System.getenv("PORT")) //
				.map(Integer::parseInt) //
				.orElse(8080);
		HttpServer httpServer = HttpServer.create().host("0.0.0.0").port(port);

		String dbHost = Optional.ofNullable(System.getenv("DB_HOST")) //
				.orElse("localhost");
		int dbPort = Optional.ofNullable(System.getenv("DB_PORT")) //
				.map(Integer::parseInt) //
				.orElse(5432);
		String dbUsername = Optional.ofNullable(System.getenv("DB_USERNAME")) //
				.orElse("root");
		String dbPassword = Optional.ofNullable(System.getenv("DB_PASSWORD")) //
				.orElse("");
		String dbDatabase = Optional.ofNullable(System.getenv("DB_DATABASE")) //
				.orElse("tweets");

		httpServer.route(routes -> {
			HttpHandler httpHandler = toHttpHandler(App.routes(connectionFactory(dbHost,
					dbPort, dbUsername, dbPassword, dbDatabase)),
					HandlerStrategies.builder().build());
			routes.route(x -> true, new ReactorHttpHandlerAdapter(httpHandler));
		}).bindUntilJavaShutdown(Duration.ofSeconds(3), disposableServer -> {
			long elapsed = System.currentTimeMillis() - begin;
			LoggerFactory.getLogger(App.class).info("Started in {} seconds",
					elapsed / 1000.0);
		});
	}
}
