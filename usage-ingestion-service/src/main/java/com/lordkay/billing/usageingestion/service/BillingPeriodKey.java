package com.lordkay.billing.usageingestion.service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

final class BillingPeriodKey {

	private static final DateTimeFormatter PERIOD_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

	private BillingPeriodKey() {
	}

	static String fromEpochMs(long occurredAtEpochMs) {
		return Instant.ofEpochMilli(occurredAtEpochMs).atZone(ZoneOffset.UTC).format(PERIOD_FORMAT);
	}
}
