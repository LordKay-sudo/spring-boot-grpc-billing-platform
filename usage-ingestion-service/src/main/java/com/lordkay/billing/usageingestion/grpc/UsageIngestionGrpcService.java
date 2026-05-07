package com.lordkay.billing.usageingestion.grpc;

import com.lordkay.billing.proto.v1.UsageEventRequest;
import com.lordkay.billing.proto.v1.UsageEventResponse;
import com.lordkay.billing.proto.v1.UsageIngestionServiceGrpc;
import io.grpc.stub.StreamObserver;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class UsageIngestionGrpcService extends UsageIngestionServiceGrpc.UsageIngestionServiceImplBase {

	@Override
	public void ingestUsage(UsageEventRequest request, StreamObserver<UsageEventResponse> responseObserver) {
		UsageEventResponse response = UsageEventResponse.newBuilder()
			.setUsageEventId(UUID.randomUUID().toString())
			.setStatus("ACCEPTED")
			.setMessage("Usage event accepted for tenant " + request.getTenantId())
			.build();

		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}
}
