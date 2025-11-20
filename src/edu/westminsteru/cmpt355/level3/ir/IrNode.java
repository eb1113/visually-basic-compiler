package edu.westminsteru.cmpt355.level3.ir;

import edu.westminsteru.cmpt355.level3.util.TreeNode;

/** Base interface for all IR nodes */
public sealed interface IrNode extends TreeNode<IrNode>
        permits IrExpression, IrParameter, IrProgram, IrStatement {

    /** Returns the child nodes of this IR node */
    @Override
    Iterable<? extends IrNode> children();

    /** Returns a short description of this IR node */
    @Override
    String toString();

}
