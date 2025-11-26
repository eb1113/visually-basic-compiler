package edu.westminsteru.cmpt355.level3.ir;

import edu.westminsteru.cmpt355.level3.lang.Type;

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
