package edu.westminsteru.cmpt355.level3.ir;

public record IrAssembly(String comment) implements IrStatement {
    @Override
    public Iterable<? extends IrNode> children() {
        return java.util.List.of();
    }
}
