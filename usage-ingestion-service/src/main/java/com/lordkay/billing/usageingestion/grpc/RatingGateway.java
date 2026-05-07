package com.lordkay.billing.usageingestion.grpc;

import com.lordkay.billing.proto.v1.RateUsageResponse;

public interface RatingGateway {

	RateUsageResponse rateUsage(String usageEventId, String tenantId, String meterId, long quantity);
}
