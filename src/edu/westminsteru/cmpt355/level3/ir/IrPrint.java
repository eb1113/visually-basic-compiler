package edu.westminsteru.cmpt355.level3.ir;

import java.util.List;

public record IrPrint(List<IrExpression> args) implements IrStatement {
    @Override
    public Iterable<? extends IrNode> children() { return args; }
}
