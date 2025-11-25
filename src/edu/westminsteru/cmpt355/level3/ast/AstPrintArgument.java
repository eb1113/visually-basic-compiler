package edu.westminsteru.cmpt355.level3.ast;

/**
 * Represents a print argument in the AST.
 */
public sealed interface AstPrintArgument extends AstNode
permits AstExpression, AstString{
}
