package edu.westminsteru.cmpt355.level3.ast;

import edu.westminsteru.cmpt355.level3.lang.Type;

import java.util.List;
import java.util.Optional;

/**
 * Represents a variable declaration in the AST.
 * @param type The type of the variable.
 * @param name The name of the variable.
 * @param expr The expression to initialize the variable.
 */
public record AstVariableDeclaration(Type type, String name, Optional<AstExpression> expr)
        implements AstStatement{

    @Override
    public Iterable<? extends AstExpression> children() {
        return expr.map(List::of).orElse(List.of());
    }
}
