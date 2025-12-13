package edu.westminsteru.cmpt355.level3.ir;

import edu.westminsteru.cmpt355.level3.lang.BuiltInType;
/**
 * Boolean literal: 'literally' or '2 legit' (true), 'nah' or 'no way' (false)
 * @param value The boolean value.
 */
public record IrBoolean(boolean value) implements IrExpression {
    @Override
    public BuiltInType type() {
        return BuiltInType.BOOLEAN;
    }

    @Override
    public Iterable<? extends IrNode> children() {
        return java.util.List.of();
    }
}

