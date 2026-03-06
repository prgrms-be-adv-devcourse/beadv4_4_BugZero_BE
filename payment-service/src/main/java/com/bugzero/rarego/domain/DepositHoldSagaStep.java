package com.bugzero.rarego.domain;

public enum DepositHoldSagaStep {
	INITIATED,
	HOLD_LOCAL_DONE,
	WAITING_BID_RESULT,
	CONFIRMED,
	COMPENSATION_RELEASE_DONE,
	COMPLETED
}
