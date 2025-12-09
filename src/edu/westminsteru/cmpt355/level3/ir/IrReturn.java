package edu.westminsteru.cmpt355.level3.ir;

import java.util.Optional;

/**
 * Return statement: return value
 * @param value The value to return (optional).
 */
public record IrReturn(Optional<IrExpression> value) implements IrStatement {
    @Override
    public Iterable<? extends IrNode> children() {
        return value.map(java.util.List::of).orElse(java.util.List.of());
    }
}
