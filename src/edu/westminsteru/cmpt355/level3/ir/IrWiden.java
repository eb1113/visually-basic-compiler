package edu.westminsteru.cmpt355.level3.ir;

import edu.westminsteru.cmpt355.level3.lang.BuiltInType;

import java.util.List;

public record IrWiden(IrExpression expr) implements IrExpression {
    @Override
    public BuiltInType type() {
        return BuiltInType.DOUBLE;
    }
    
    @Override
    public Iterable<? extends IrNode> children() {
        return List.of(expr);
    }
}
