package edu.westminsteru.cmpt355.level3;

/** Exception representing an error in typechecking */
public class TypecheckException extends SemanticAnalysisException {

    public TypecheckException() {
        super();
    }

    public TypecheckException(String message) {
        super(message);
    }

    public TypecheckException(String message, Throwable cause) {
        super(message, cause);
    }

    public TypecheckException(Throwable cause) {
        super(cause);
    }

    protected TypecheckException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
