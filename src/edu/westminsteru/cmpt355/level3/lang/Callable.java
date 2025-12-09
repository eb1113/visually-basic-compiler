package edu.westminsteru.cmpt355.level3.lang;

import java.util.List;

/**
 * Common interface for functions and methods that can be called.
 * Used in semantic analysis to handle both functions and record methods uniformly.
 */
public interface Callable {
    String name();
    ReturnType returnType();
    List<Type> parameterTypes();
}

