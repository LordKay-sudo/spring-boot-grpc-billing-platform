package com.lordkay.billing.usageingestion.grpc;

import com.lordkay.billing.proto.v1.UsageEventRequest;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.ArrayList;
import java.util.List;

final class UsageEventValidator {

	private UsageEventValidator() {
	}

	static void validate(UsageEventRequest request) {
		List<String> violations = new ArrayList<>();
		if (request.getTenantId().isBlank()) {
			violations.add("tenant_id is required");
		}
		if (request.getMeterId().isBlank()) {
			violations.add("meter_id is required");
		}
		if (request.getIdempotencyKey().isBlank()) {
			violations.add("idempotency_key is required");
		}
		if (request.getQuantity() <= 0) {
			violations.add("quantity must be greater than zero");
		}
		if (request.getOccurredAtEpochMs() <= 0) {
			violations.add("occurred_at_epoch_ms must be positive");
		}
		if (!violations.isEmpty()) {
			throw Status.INVALID_ARGUMENT.withDescription(String.join("; ", violations)).asRuntimeException();
		}
	}
}
