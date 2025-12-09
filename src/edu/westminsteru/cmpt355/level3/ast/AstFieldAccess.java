package edu.westminsteru.cmpt355.level3.ast;

/**
 * Field access expression: record.field
 * Can be used as both lvalue and rvalue
 * @param record The record expression.
 * @param fieldName The name of the field.
 */
public record AstFieldAccess(AstExpression record, String fieldName)
        implements AstExpression, AstLvalue {
    @Override
    public Iterable<? extends AstExpression> children() {
        return java.util.List.of(record);
    }
}

