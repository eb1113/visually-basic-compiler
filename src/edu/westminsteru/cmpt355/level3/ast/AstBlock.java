package edu.westminsteru.cmpt355.level3.ast;

import java.util.List;
/**
 * Represents a block of statements in the AST.
 * @param statements The list of statements in the block.
 */
public record AstBlock(List<AstStatement> statements)
        implements AstStatement {
    @Override
    public Iterable<? extends AstNode> children() {
        return statements;
    }
}
