package edu.westminsteru.cmpt355.level3.ast;

/**
 * Base interface for all AST nodes representing expressions, i.e., things that
 * evaluate to a value
 */
public sealed interface AstExpression extends AstNode
    permits /* AST expression types go here */
{ }
