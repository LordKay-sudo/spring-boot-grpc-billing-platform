package com.lordkay.billing.invoicing.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, String> {

	Optional<Invoice> findByTenantIdAndBillingPeriodKeyAndCurrencyCode(
		String tenantId,
		String billingPeriodKey,
		String currencyCode
	);
}
