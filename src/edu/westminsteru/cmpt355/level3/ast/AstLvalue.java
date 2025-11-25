package edu.westminsteru.cmpt355.level3.ast;

/**
 * Base interface for all lvalues (things that can appear on left side of assignment).
 * All lvalues are also expressions (rvalues).
 * Note: This is a marker interface - the actual lvalue types are AstVariable, AstArraySubscript, AstFieldAccess
 * which directly implement AstExpression.
 */
public non-sealed interface AstLvalue extends AstExpression {
}

