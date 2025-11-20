package edu.westminsteru.cmpt355.level3.ast;

import edu.westminsteru.cmpt355.level3.util.TreeNode;
import java.lang.Iterable;

/** Base interface for all AST nodes */
public sealed interface AstNode extends TreeNode<AstNode>
        permits AstExpression, AstParameter, AstPrintArgument, AstProgram, AstStatement
{
    /** Returns the child nodes of this AST node */
    @Override
    Iterable<? extends AstNode> children();

    /** Returns a short description of this AST node */
    @Override
    String toString();
}
