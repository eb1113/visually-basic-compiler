package edu.westminsteru.cmpt355.level3.ir;

/** IR node type representing the program as a whole: the root IR node */
public record IrProgram(/* fill in needed fields */) implements IrNode {

    @Override
    public Iterable<? extends IrNode> children() {
        throw new RuntimeException("Not implemented");
    }
}
