package am.ik.example;

import java.sql.Timestamp;
import java.util.UUID;

import io.r2dbc.client.R2dbc;
import io.r2dbc.spi.ConnectionFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class TweetMapper {
	private final R2dbc r2dbc;

	public TweetMapper(ConnectionFactory connectionFactory) {
		this.r2dbc = new R2dbc(connectionFactory);
	}

	public Flux<Tweet> findLatest() {
		return this.r2dbc.withHandle(handle -> handle.createQuery(
				"SELECT uuid, text, username, created_at FROM tweets ORDER BY created_at DESC LIMIT 30")
				.mapRow(row -> new Tweet(UUID.fromString(row.get("uuid", String.class)),
						row.get("username", String.class), row.get("text", String.class),
						row.get("created_at", Timestamp.class).toInstant())));
	}

	public Mono<Tweet> insert(Tweet tweet) {
		return this.r2dbc.inTransaction(handle -> handle.createUpdate(
				"INSERT INTO tweets(uuid, text, username, created_at) VALUES($1,$2,$3,$4)")
				.bind("$1", tweet.getUuid().toString()) //
				.bind("$2", tweet.getText()) //
				.bind("$3", tweet.getUsername()) //
				.bind("$4", Timestamp.from(tweet.getCreatedAt())) //
				.execute()) //
				.then(Mono.just(tweet));
	}

	Mono<Integer> truncate() {
		return this.r2dbc
				.withHandle(handle -> handle.execute("TRUNCATE TABLE tweets"))
				.single();
	}
}
