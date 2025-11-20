package edu.westminsteru.cmpt355.level3.ir;

import java.util.List;

public record IrBlock(List<IrStatement> statements) implements IrStatement {
    @Override
    public Iterable<? extends IrNode> children() {
        return statements;
    }
}