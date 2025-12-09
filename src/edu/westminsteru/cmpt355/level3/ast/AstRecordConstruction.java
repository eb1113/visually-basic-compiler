package edu.westminsteru.cmpt355.level3.ast;

import java.util.List;

/**
 * Record construction: new Point(1.2, 3.4)
 * @param recordName The name of the record.
 * @param arguments The arguments to the record construction.
 */
public record AstRecordConstruction(String recordName, List<AstExpression> arguments)
        implements AstExpression {
    @Override
    public Iterable<? extends AstExpression> children() {
        return arguments;
    }
}

