package com.bugzero.rarego.domain;

import java.util.Arrays;

public enum AuctionTickPolicy {
	UNDER_10K(10_000, 500),
	UNDER_50K(50_000, 1_000),
	UNDER_100K(100_000, 2_000),
	UNDER_300K(300_000, 5_000),
	UNDER_1M(1_000_000, 10_000),
	OVER_OR_EQUAL_1M(Integer.MAX_VALUE, 30_000);

	private final int upperBoundExclusive;
	private final int tickSize;

	AuctionTickPolicy(int upperBoundExclusive, int tickSize) {
		this.upperBoundExclusive = upperBoundExclusive;
		this.tickSize = tickSize;
	}

	public int tickSize() {
		return tickSize;
	}

	public static int resolveTickSize(int startPrice) {
		return Arrays.stream(values())
			.filter(policy -> startPrice < policy.upperBoundExclusive)
			.findFirst()
			.orElse(OVER_OR_EQUAL_1M)
			.tickSize();
	}
}
