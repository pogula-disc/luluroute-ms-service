package com.luluroute.ms.service.exception;
/**
 * 
 * @author MANDALAKARTHIK1
 *
 */
public class RetryableException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public RetryableException() {
	        super();
	    }

	public RetryableException(String message) {
	        super(message);
	    }

	public RetryableException(Throwable cause) {
	        super(cause);
	    }

	public RetryableException(String message, Throwable cause) {
	        super(message, cause);
	    }

}
