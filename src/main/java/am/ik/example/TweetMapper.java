package am.ik.example;

import java.sql.Timestamp;
import java.util.UUID;

import javax.sql.DataSource;

import org.davidmoten.rx.jdbc.Database;
import org.davidmoten.rx.jdbc.pool.NonBlockingConnectionPool;
import org.davidmoten.rx.jdbc.pool.Pools;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class TweetMapper {
	private Database db;

	public TweetMapper(DataSource dataSource) {
		NonBlockingConnectionPool pool = Pools.nonBlocking()
				.maxPoolSize(Runtime.getRuntime().availableProcessors() * 5)
				.connectionProvider(dataSource).build();
		this.db = Database.from(pool);
	}

	public Flux<Tweet> findLatest() {
		return Flux.from(this.db.select(
				"SELECT uuid, text, username, created_at FROM tweets ORDER BY created_at DESC LIMIT 30")
				.get(rs -> new Tweet(UUID.fromString(rs.getString("uuid")),
						rs.getString("username"), rs.getString("text"),
						rs.getTimestamp("created_at").toInstant())));
	}

	public Mono<Tweet> insert(Tweet tweet) {
		return Flux.from(this.db.update(
				"INSERT INTO tweets(uuid, text, username, created_at) VALUES(?,?,?,?)")
				.parameters(tweet.getUuid(), tweet.getText(), tweet.getUsername(),
						Timestamp.from(tweet.getCreatedAt()))
				.counts()) //
				.map(i -> tweet) //
				.next();
	}

	Mono<Void> truncate() {
		return Flux.from(this.db.update("TRUNCATE TABLE tweets").counts()).then();
	}
}
