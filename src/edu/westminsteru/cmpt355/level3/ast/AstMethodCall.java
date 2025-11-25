package edu.westminsteru.cmpt355.level3.ast;

import java.util.List;

/**
 * Method call on a record: record.method(args)
 * @param record The record expression.
 * @param methodName The name of the method.
 * @param arguments The arguments to the method.
 */
public record AstMethodCall(AstExpression record, String methodName, List<AstExpression> arguments)
        implements AstExpression {
    @Override
    public Iterable<? extends AstExpression> children() {
        java.util.List<AstExpression> children = new java.util.ArrayList<>();
        children.add(record);
        children.addAll(arguments);
        return children;
    }
}

