package edu.westminsteru.cmpt355.level3.ast;

public sealed interface AstPrintArgument extends AstNode
permits AstExpression, AstString{
}
