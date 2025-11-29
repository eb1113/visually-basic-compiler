package edu.westminsteru.cmpt355.level3.ir;

/**
 * Branch statement: if (condition) goto target
 * @param condition The condition expression.
 * @param targetLabel The target label.
 */
public record IrBranch(IrExpression condition, String targetLabel) implements IrStatement {
    @Override
    public Iterable<? extends IrNode> children() {
        return java.util.List.of(condition);
    }
}

