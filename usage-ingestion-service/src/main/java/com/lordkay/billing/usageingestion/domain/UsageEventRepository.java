package com.lordkay.billing.usageingestion.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsageEventRepository extends JpaRepository<UsageEvent, String> {

	Optional<UsageEvent> findByTenantIdAndIdempotencyKey(String tenantId, String idempotencyKey);
}
