package com.luluroute.ms.service.exception;
/**
 * 
 * @author MANDALAKARTHIK1
 *
 */
public class RestFailureException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public RestFailureException() {
	        super();
	    }

	public RestFailureException(String message) {
	        super(message);
	    }

	public RestFailureException(Throwable cause) {
	        super(cause);
	    }

	public RestFailureException(String message, Throwable cause) {
	        super(message, cause);
	    }

}
