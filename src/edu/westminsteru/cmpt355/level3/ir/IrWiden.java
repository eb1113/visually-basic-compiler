package edu.westminsteru.cmpt355.level3.ir;

import edu.westminsteru.cmpt355.level3.lang.Type;

import java.util.List;

public record IrWiden(IrExpression expr) implements IrExpression {
    @Override
    public Type type() {return Type.DOUBLE;}
    @Override
    public Iterable<? extends IrNode> children() { return List.of(expr); }

}
