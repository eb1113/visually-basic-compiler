package edu.westminsteru.cmpt355.level3.ir;

/**
 * Wraps an expression to be used as a statement (e.g., increment statements like i++;)
 * @param expression The expression to be wrapped.
 */
public record IrExpressionStatement(IrExpression expression) implements IrStatement {
    @Override
    public Iterable<? extends IrNode> children() {
        return java.util.List.of(expression);
    }
}

