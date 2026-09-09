package com.lordkay.billing.invoicing.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceLineItemRepository extends JpaRepository<InvoiceLineItem, Long> {

	Optional<InvoiceLineItem> findByUsageEventId(String usageEventId);
}
