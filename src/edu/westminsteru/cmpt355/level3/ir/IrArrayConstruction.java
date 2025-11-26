package edu.westminsteru.cmpt355.level3.ir;

import edu.westminsteru.cmpt355.level3.lang.Type;

public record IrArrayConstruction(Type elementType, IrExpression dimension)
    implements IrExpression{
    @Override
    public Type type() {
        return elementType;
    }

    @Override
    public Iterable<? extends IrNode> children() {
        return java.util.List.of();
    }
}
