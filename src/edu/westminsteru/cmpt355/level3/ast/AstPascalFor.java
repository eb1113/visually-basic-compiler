package edu.westminsteru.cmpt355.level3.ast;

import edu.westminsteru.cmpt355.level3.lang.Type;

/**
 * Represents a Pascal-style for loop in the AST.
 * @param type The type of the variable.
 * @param varName The name of the variable.
 * @param startVal The start value expression.
 * @param endVal The end value expression.
 * @param stepVal The step value expression.
 * @param body The body of the loop.
 */
public record AstPascalFor(Type type, String varName, AstExpression startVal, AstExpression endVal, AstExpression stepVal, AstBlock body)
        implements AstStatement {
    @Override
    public Iterable<? extends AstNode> children() {
        return java.util.List.of(startVal, endVal, stepVal, body);
    }
}

