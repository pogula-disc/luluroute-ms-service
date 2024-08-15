package com.luluroute.ms.service.util;

import java.util.UUID;

import org.slf4j.MDC;


public class EntityConstants {

	public static final String X_CORRELATION_ID = "X-Correlation-Id";
	public static final String X_TRANSACTION_REFERENCE = "X-Transaction-Reference";
	
	public static String getCorrelationId() {
		return UUID.randomUUID().toString();
	}

	public static void clearMDC() {
		MDC.remove(EntityConstants.X_CORRELATION_ID);
		MDC.remove(EntityConstants.X_TRANSACTION_REFERENCE);
	}
}
