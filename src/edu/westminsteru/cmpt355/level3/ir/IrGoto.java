package edu.westminsteru.cmpt355.level3.ir;

/**
 * Goto statement: goto targetLabel
 * @param targetLabel The target label.
 */
public record IrGoto(String targetLabel) implements IrStatement {
    @Override
    public Iterable<? extends IrNode> children() {
        return java.util.List.of();
    }
}

