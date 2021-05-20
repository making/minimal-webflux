package am.ik.example;

import java.time.Instant;
import java.util.UUID;

import io.r2dbc.spi.ConnectionFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.transaction.reactive.TransactionalOperator;

public class TweetMapper {
	private final DatabaseClient databaseClient;

	private final TransactionalOperator transactionalOperator;

	public TweetMapper(ConnectionFactory connectionFactory) {
		this.databaseClient = DatabaseClient.create(connectionFactory);
		this.transactionalOperator = TransactionalOperator.create(new R2dbcTransactionManager(connectionFactory));
	}

	public Flux<Tweet> findLatest() {
		return this.databaseClient.sql("SELECT uuid, text, username, created_at FROM tweets ORDER BY created_at DESC LIMIT 30")
				.map(row -> new Tweet(UUID.fromString(row.get("uuid", String.class)),
						row.get("username", String.class), row.get("text", String.class),
						row.get("created_at", Instant.class)))
				.all();
	}

	public Mono<Tweet> insert(Tweet tweet) {
		return this.transactionalOperator.transactional(
				this.databaseClient.sql("INSERT INTO tweets(uuid, text, username, created_at) VALUES(:uuid, :text, :username, :created_at)")
						.bind("uuid", tweet.getUuid().toString()) //
						.bind("text", tweet.getText()) //
						.bind("username", tweet.getUsername()) //
						.bind("created_at", tweet.getCreatedAt())
						.fetch()
						.rowsUpdated())
				.thenReturn(tweet);
	}

	Mono<Integer> truncate() {
		return this.databaseClient
				.sql("TRUNCATE TABLE tweets")
				.fetch()
				.rowsUpdated();
	}
}
