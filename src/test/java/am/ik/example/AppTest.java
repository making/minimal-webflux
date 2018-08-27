package am.ik.example;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcConnectionPool;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;

import com.fasterxml.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;

import reactor.core.publisher.Flux;

public class AppTest {
	private WebTestClient testClient;
	private TweetMapper tweetMapper;
	private JdbcConnectionPool dataSource;
	private Instant now = Instant.now();

	@Before
	public void setUp() throws Exception {
		this.dataSource = this.dataSource();
		this.tweetMapper = new TweetMapper(this.dataSource);
		RouterFunction<?> routes = new TweetHandler(tweetMapper).routes();
		this.testClient = WebTestClient.bindToRouterFunction(routes).build();
		this.tweetMapper.truncate()
				.thenMany(
						Flux.concat(
								this.tweetMapper.insert(new Tweet(
										UUID.fromString(
												"81e7c7df-17f2-4527-bc53-927e11956671"),
										"foo", "Hello1", Instant.EPOCH.plusSeconds(1))),
								this.tweetMapper.insert(new Tweet(
										UUID.fromString(
												"81e7c7df-17f2-4527-bc53-927e11956672"),
										"foo", "Hello2", Instant.EPOCH.plusSeconds(2)))))
				.blockLast();
	}

	@After
	public void tearDown() throws Exception {
		this.dataSource.dispose();
	}

	@Test
	public void testGetTweets() throws Exception {
		this.testClient.get().uri("/tweets") //
				.exchange() //
				.expectStatus().isOk() //
				.expectBodyList(JsonNode.class).hasSize(2) //
				.value(tweets -> {
					assertThat(tweets.get(0).get("uuid").asText()).hasSize(36);
					assertThat(tweets.get(0).get("username").asText()).isEqualTo("foo");
					assertThat(tweets.get(0).get("text").asText()).isEqualTo("Hello2");
					assertThat(tweets.get(0).get("createdAt").asLong()).isEqualTo(2L);
					assertThat(tweets.get(1).get("uuid").asText()).hasSize(36);
					assertThat(tweets.get(1).get("username").asText()).isEqualTo("foo");
					assertThat(tweets.get(1).get("text").asText()).isEqualTo("Hello1");
					assertThat(tweets.get(1).get("createdAt").asLong()).isEqualTo(1L);
				});
	}

	@Test
	public void testPostTweet201() throws Exception {
		this.testClient.post().uri("/tweets") //
				.syncBody(new Tweet("demo", "Demo")).exchange() //
				.expectStatus().isCreated();

		this.testClient.get().uri("/tweets") //
				.exchange() //
				.expectStatus().isOk() //
				.expectBodyList(JsonNode.class).hasSize(3) //
				.value(tweets -> {
					JsonNode created = tweets.get(0);
					assertThat(created.get("uuid").asText()).hasSize(36);
					assertThat(created.get("username").asText()).isEqualTo("demo");
					assertThat(created.get("text").asText()).isEqualTo("Demo");
					assertThat(created.get("createdAt").asLong())
							.isGreaterThanOrEqualTo(now.getEpochSecond());
				});
	}

	@Test
	public void testCountEmojiProperly() throws Exception {
		this.testClient.post().uri("/tweets") //
				.syncBody(new Tweet("demo",
						IntStream.range(0, 64).mapToObj(x -> "❤️")
								.collect(Collectors.joining())))
				.exchange() //
				.expectStatus().isCreated();
	}

	@Test
	public void testPostTweet400() throws Exception {
		this.testClient.post().uri("/tweets") //
				.syncBody(new Tweet("demoです",
						IntStream.rangeClosed(0, 64).mapToObj(x -> "a")
								.collect(Collectors.joining())))
				.exchange() //
				.expectStatus().isBadRequest() //
				.expectBodyList(JsonNode.class).hasSize(2) //
				.value(errors -> {
					assertThat(errors.get(0).get("defaultMessage").asText())
							.isEqualTo("\"[で, す]\" is/are not allowed for \"username\"");
					assertThat(errors.get(1).get("defaultMessage").asText()).isEqualTo(
							"The size of \"text\" must be less than or equal to 64. The given size is 65");
				});
	}

	private JdbcConnectionPool dataSource() {
		JdbcConnectionPool connectionPool = JdbcConnectionPool
				.create("jdbc:h2:mem:tweet;DB_CLOSE_DELAY=-1;", "sa", "sa");
		Flyway flyway = new Flyway();
		flyway.setDataSource(connectionPool);
		flyway.setLocations("classpath:db/migration");
		flyway.migrate();
		return connectionPool;
	}
}
