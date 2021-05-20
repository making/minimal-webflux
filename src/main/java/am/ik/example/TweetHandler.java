package am.ik.example;

import am.ik.yavi.core.ConstraintViolations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.UnicastProcessor;

class TweetHandler {
	private static final Logger log = LoggerFactory.getLogger(TweetHandler.class);
	private final UnicastProcessor<Tweet> tweets = UnicastProcessor.create();
	private final Flux<Tweet> timeline = tweets.publish().autoConnect().share();
	private final TweetMapper tweetMapper;

	TweetHandler(TweetMapper tweetMapper) {
		this.tweetMapper = tweetMapper;
	}

	RouterFunction<ServerResponse> routes() {
		return route() //
				.GET("/tweets", this::getTweets) //
				.GET("/timeline", this::getTimeline) //
				.POST("/tweets", this::createTweet) //
				.build();
	}

	Mono<ServerResponse> getTweets(ServerRequest req) {
		return ServerResponse.ok() //
				.contentType(MediaType.APPLICATION_JSON) //
				.body(this.tweetMapper.findLatest(), Tweet.class);
	}

	Mono<ServerResponse> getTimeline(ServerRequest req) {
		return ServerResponse.ok() //
				.contentType(MediaType.TEXT_EVENT_STREAM) //
				.body(this.timeline, Tweet.class);
	}

	Mono<ServerResponse> createTweet(ServerRequest req) {
		return req.bodyToMono(Tweet.class) //
				.flatMap(body -> body.validate().fold(
						v -> ServerResponse.badRequest().bodyValue(ConstraintViolations.of(v).details()), //
						tweet -> ServerResponse.status(CREATED).body(this.tweetMapper
								.insert(tweet).doOnSuccess(tweets::onNext),
								Tweet.class)));
	}
}
