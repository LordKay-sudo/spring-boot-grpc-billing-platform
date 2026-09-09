package com.lordkay.billing.usageingestion.grpc;

import com.lordkay.billing.proto.v1.UsageEventRequest;
import com.lordkay.billing.proto.v1.UsageEventResponse;
import com.lordkay.billing.proto.v1.UsageIngestionServiceGrpc;
import com.lordkay.billing.usageingestion.service.UsageIngestionOrchestrator;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;

@Service
public class UsageIngestionGrpcService extends UsageIngestionServiceGrpc.UsageIngestionServiceImplBase {

	private final UsageIngestionOrchestrator orchestrator;

	public UsageIngestionGrpcService(UsageIngestionOrchestrator orchestrator) {
		this.orchestrator = orchestrator;
	}

	@Override
	public void ingestUsage(UsageEventRequest request, StreamObserver<UsageEventResponse> responseObserver) {
		try {
			UsageEventValidator.validate(request);
			UsageEventResponse response = orchestrator.ingest(request);
			responseObserver.onNext(response);
			responseObserver.onCompleted();
		}
		catch (RuntimeException ex) {
			responseObserver.onError(ex);
		}
	}
}
