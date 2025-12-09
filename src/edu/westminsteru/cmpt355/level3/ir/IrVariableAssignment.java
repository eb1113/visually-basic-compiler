package edu.westminsteru.cmpt355.level3.ir;

/**
 * Represents a variable assignment in the IR. lvalue = expr
 * @param lvalue The left-hand side value (lvalue).
 * @param expr The right-hand side expression (expr).
 */
public record IrVariableAssignment(IrExpression lvalue, IrExpression value) implements IrStatement {
    @Override
    public Iterable<? extends IrNode> children() {
        return java.util.List.of(lvalue, value);
    }
}