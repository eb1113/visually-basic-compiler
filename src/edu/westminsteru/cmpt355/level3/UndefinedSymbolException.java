package edu.westminsteru.cmpt355.level3;

/**
 * Exception representing the use of a symbol (variable, function, ...) that hasn't
 * yet been declared/defined
 */
public class UndefinedSymbolException extends SemanticAnalysisException {

    public UndefinedSymbolException() {
        super();
    }

    public UndefinedSymbolException(String message) {
        super(message);
    }

    public UndefinedSymbolException(String message, Throwable cause) {
        super(message, cause);
    }

    public UndefinedSymbolException(Throwable cause) {
        super(cause);
    }

    protected UndefinedSymbolException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
