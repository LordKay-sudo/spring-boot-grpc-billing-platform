package com.lordkay.billing.usageingestion.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BillingPeriodKeyTest {

	@Test
	void derivesUtcMonthFromEpochMillis() {
		assertThat(BillingPeriodKey.fromEpochMs(1715068800000L)).isEqualTo("2024-05");
	}
}
