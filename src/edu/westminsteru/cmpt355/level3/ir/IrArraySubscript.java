package edu.westminsteru.cmpt355.level3.ir;

import edu.westminsteru.cmpt355.level3.lang.Type;

public record IrArraySubscript(IrExpression array, IrExpression index, Type elementType)
    implements IrExpression, IrLvalue{
    @Override
    public Type type() {
        return elementType;
    }

    @Override
    public Iterable<? extends IrNode> children() {
        return java.util.List.of(array);
    }
}
