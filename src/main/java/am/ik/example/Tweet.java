package am.ik.example;

import java.time.Instant;
import java.util.UUID;

import am.ik.yavi.builder.ValidatorBuilder;
import am.ik.yavi.core.Validated;
import am.ik.yavi.core.Validator;
import am.ik.yavi.meta.ConstraintTarget;

import static am.ik.yavi.constraint.charsequence.codepoints.AsciiCodePoints.ASCII_PRINTABLE_CHARS;

public class Tweet {
	private static final Validator<Tweet> validator = ValidatorBuilder.of(Tweet.class)
			.constraint(_TweetMeta.USERNAME,
					c -> c.notBlank().lessThanOrEqual(128)
							.codePoints(ASCII_PRINTABLE_CHARS).asWhiteList())
			.constraint(_TweetMeta.TEXT, c -> c.notBlank().emoji().lessThanOrEqual(64))
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

	@ConstraintTarget
	public String getUsername() {
		return username;
	}

	@ConstraintTarget
	public String getText() {
		return text;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Validated<Tweet> validate() {
		return validator.applicative().validate(this);
	}
}
