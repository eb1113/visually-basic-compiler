package edu.westminsteru.cmpt355.level3.ir;

import edu.westminsteru.cmpt355.level3.lang.ArrayType;
import edu.westminsteru.cmpt355.level3.lang.ScalarType;
import edu.westminsteru.cmpt355.level3.lang.Type;

import java.util.List;

/**
 * Array construction: new int[100] or new int[10][20]...
 * @param baseType The base type of the array (ScalarType before adding dimensions).
 * @param dimensions List of dimension size expressions.
 */
public record IrArrayConstruction(ScalarType baseType, List<IrExpression> dimensions)
    implements IrExpression{
    @Override
    public Type type() {
        // Build ArrayType with the correct number of dimensions
        Type result = baseType;
        for (int i = 0; i < dimensions.size(); i++) {
            result = ArrayType.of(result);
        }
        return result;
    }

    @Override
    public Iterable<? extends IrNode> children() {
        return dimensions;
    }
}
