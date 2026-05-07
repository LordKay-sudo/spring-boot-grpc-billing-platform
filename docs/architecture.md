# Architecture Notes

This document will capture architecture decisions as the platform evolves.

Milestone 1 baseline:
- contract-first API with protobuf
- one gRPC service (`usage-ingestion-service`)
- synchronous unary call for usage ingestion
