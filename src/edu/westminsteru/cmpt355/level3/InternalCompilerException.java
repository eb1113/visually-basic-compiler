package edu.westminsteru.cmpt355.level3;

/**
 * Exception type representing an error in the compiler itself: throw this
 * if something happens in the compiler that should not be possible
 */
public class InternalCompilerException extends RuntimeException {

    public InternalCompilerException(String message) {
        super(message);
    }

    public InternalCompilerException() {
        super();
    }

    public InternalCompilerException(String message, Throwable cause) {
        super(message, cause);
    }

    public InternalCompilerException(Throwable cause) {
        super(cause);
    }

    protected InternalCompilerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
