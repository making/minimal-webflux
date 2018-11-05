package am.ik.example;

import java.util.LinkedList;

import org.reactivestreams.Publisher;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryMetadata;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class MockTweetMapper extends TweetMapper {
	public MockTweetMapper() {
		super(new ConnectionFactory() {
			@Override
			public Publisher<? extends Connection> create() {
				return null;
			}

			@Override
			public ConnectionFactoryMetadata getMetadata() {
				return null;
			}
		});
	}

	private final LinkedList<Tweet> tweets = new LinkedList<>();

	@Override
	public Flux<Tweet> findLatest() {
		return Flux.fromIterable(this.tweets);
	}

	@Override
	public Mono<Tweet> insert(Tweet tweet) {
		this.tweets.addFirst(tweet);
		return Mono.just(tweet);
	}

	@Override
	Mono<Integer> truncate() {
		this.tweets.clear();
		return Mono.just(1);
	}
}
