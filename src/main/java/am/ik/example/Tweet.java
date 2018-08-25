package am.ik.example;

import java.time.Instant;
import java.util.UUID;

import am.ik.yavi.core.ConstraintViolations;
import am.ik.yavi.core.Validator;
import am.ik.yavi.fn.Either;

public class Tweet {
	private static final Validator<Tweet> validator = Validator.builder(Tweet.class)
			.constraint(Tweet::getUsername, "username",
					c -> c.notBlank().lessThanOrEqual(128).pattern("[a-zA-Z0-9]+"))
			.constraint(Tweet::getText, "text", c -> c.notBlank().lessThanOrEqual(64))
			.build();
	private UUID uuid;
	private String username;
	private String text;
	private Instant createdAt;

	private Tweet() {
		// for Jackson deserialization
		this.uuid = UUID.randomUUID();
		this.createdAt = Instant.now();
	}

	public Tweet(String username, String text) {
		this();
		this.username = username;
		this.text = text;
	}

	public Tweet(UUID uuid, String username, String text, Instant createdAt) {
		this.uuid = uuid;
		this.username = username;
		this.text = text;
		this.createdAt = createdAt;
	}

	public UUID getUuid() {
		return uuid;
	}

	public String getUsername() {
		return username;
	}

	public String getText() {
		return text;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Either<ConstraintViolations, Tweet> validate() {
		return validator.validateToEither(this);
	}
}
