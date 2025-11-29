package edu.westminsteru.cmpt355.level3.ir;

public record IrVariableAssignment(IrExpression lvalue, IrExpression value) implements IrStatement {
    @Override
    public Iterable<? extends IrNode> children() {
        return java.util.List.of(lvalue, value);
    }
}