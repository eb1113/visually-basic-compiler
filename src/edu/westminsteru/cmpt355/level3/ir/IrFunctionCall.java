package edu.westminsteru.cmpt355.level3.ir;

import edu.westminsteru.cmpt355.level3.lang.Type;

import java.util.List;

public record IrFunctionCall(Type type, String name, List<IrExpression> args) implements IrStatement, IrExpression {
    @Override
    public Type type() { return type; }

    @Override
    public Iterable<? extends IrNode> children() { return args; }
}
