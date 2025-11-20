package edu.westminsteru.cmpt355.level3;

/**
 * Exception type representing duplicate definition of a symbol (e.g. declaring
 * two variables with the same name in the same method, or declaring two
 * functions with the same name)
 */
public class DuplicateDefinitionException extends SemanticAnalysisException {

    public DuplicateDefinitionException() {
        super();
    }

    public DuplicateDefinitionException(String message) {
        super(message);
    }
    public DuplicateDefinitionException(String message, Throwable cause) {
        super(message, cause);
    }

    public DuplicateDefinitionException(Throwable cause) {
        super(cause);
    }

    protected DuplicateDefinitionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
