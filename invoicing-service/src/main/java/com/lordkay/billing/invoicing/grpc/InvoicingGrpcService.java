package com.lordkay.billing.invoicing.grpc;

import com.lordkay.billing.proto.v1.CreateInvoiceRequest;
import com.lordkay.billing.proto.v1.CreateInvoiceResponse;
import com.lordkay.billing.proto.v1.InvoicingServiceGrpc;
import io.grpc.stub.StreamObserver;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class InvoicingGrpcService extends InvoicingServiceGrpc.InvoicingServiceImplBase {

	@Override
	public void createInvoice(CreateInvoiceRequest request, StreamObserver<CreateInvoiceResponse> responseObserver) {
		CreateInvoiceResponse response = CreateInvoiceResponse.newBuilder()
			.setInvoiceId("inv-" + UUID.randomUUID())
			.setTenantId(request.getTenantId())
			.setTotalMinor(request.getAmountMinor())
			.setCurrencyCode(request.getCurrencyCode())
			.setStatus("DRAFT")
			.build();

		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}
}
