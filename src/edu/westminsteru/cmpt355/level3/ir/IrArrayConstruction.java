package edu.westminsteru.cmpt355.level3.ir;

import edu.westminsteru.cmpt355.level3.lang.ArrayType;
import edu.westminsteru.cmpt355.level3.lang.Type;
/**
 * Array construction: new int[100]
 * @param elementType The type of the elements in the array.
 * @param dimension The dimension of the array.
 */
public record IrArrayConstruction(Type elementType, IrExpression dimension)
    implements IrExpression{
    @Override
    public Type type() {
        return ArrayType.of(elementType);
    }

    @Override
    public Iterable<? extends IrNode> children() {
        return java.util.List.of(dimension);
    }
}
