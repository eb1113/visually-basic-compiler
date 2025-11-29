package edu.westminsteru.cmpt355.level3.ir;

import edu.westminsteru.cmpt355.level3.lang.Type;

/**
 * Field access expression: record.fieldName
 * @param record The record expression (record).
 * @param fieldName The name of the field (fieldName).
 * @param fieldType The type of the field (fieldType).
 */
public record IrFieldAccess(IrExpression record, String fieldName, Type fieldType)
    implements IrExpression, IrLvalue {
    @Override
    public Type type() {
        return fieldType;
    }

    @Override
    public Iterable<? extends IrNode> children() {
        return java.util.List.of(record);
    }
}
