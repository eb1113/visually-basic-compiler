package edu.westminsteru.cmpt355.level3.ast;

/**
 * Base interface for all AST nodes representing statements, i.e., things that
 * function as a sentence/line of code
 */
public sealed interface AstStatement extends AstNode
    permits /* AST statement types go here */
{ }
