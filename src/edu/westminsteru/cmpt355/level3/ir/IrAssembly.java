package edu.westminsteru.cmpt355.level3.ir;

/**
 * Assembly comment: anything that starts with ///
 * @param comment The comment.
 */
public record IrAssembly(String comment) implements IrStatement {
    @Override
    public Iterable<? extends IrNode> children() {
        return java.util.List.of();
    }
}
