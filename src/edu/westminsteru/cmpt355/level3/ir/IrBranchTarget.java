package edu.westminsteru.cmpt355.level3.ir;

/**
 * Branch target: a label to jump to
 * @param label The label.
 */
public record IrBranchTarget(String label) implements IrStatement {
    @Override
    public Iterable<? extends IrNode> children() {
        return java.util.List.of();
    }
}

