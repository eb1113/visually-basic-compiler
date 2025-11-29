package edu.westminsteru.cmpt355.level3.lang;

import java.util.List;

/**
 * Record representing a method in the language.
 * <br>
 * N.B.: this is not an IR node and hence should not contain IR nodes: this
 * record should contain just the needed information to access (call) the
 * method, but not the method's code.
 * <br>
 * Methods are non-static and belong to records (unlike functions which are static).
 */
public record Method(
    String name,
    ReturnType returnType,
    List<Type> parameterTypes
) {

    public String getDescriptor() {
        StringBuilder sb = new StringBuilder();
        sb.append('(');
        for (Type paramType : parameterTypes) {
            sb.append(paramType.getDescriptor());
        }
        sb.append(')');
        sb.append(returnType.getDescriptor());
        return sb.toString();
    }
}

