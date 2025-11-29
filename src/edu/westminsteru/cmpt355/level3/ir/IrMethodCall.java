package edu.westminsteru.cmpt355.level3.ir;

import edu.westminsteru.cmpt355.level3.lang.Type;

import java.util.List;

public record IrMethodCall(IrExpression record, String methodName, List<IrExpression> args, Type returnType)
        implements IrStatement, IrExpression {
    @Override
    public Type type() {
        return returnType;
    }

    @Override
    public Iterable<? extends IrNode> children() {
        java.util.List<IrNode> children = new java.util.ArrayList<>();
        children.add(record);
        children.addAll(args);
        return children;
    }
}
