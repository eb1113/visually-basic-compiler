package edu.westminsteru.cmpt355.level3.ir;

import edu.westminsteru.cmpt355.level3.lang.Type;

/**
 * Cast operation: (type) expr
 * @param targetType The target type of the cast.
 * @param expr The expression to be cast.
 */
public record IrCast(Type targetType, IrExpression expr) implements IrExpression {
    @Override
    public Type type() {
        return targetType;
    }

    @Override
    public Iterable<? extends IrNode> children() {
        return java.util.List.of(expr);
    }
}

