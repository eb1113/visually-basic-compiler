package edu.westminsteru.cmpt355.level3.ir;

import edu.westminsteru.cmpt355.level3.InternalCompilerException;
import edu.westminsteru.cmpt355.level3.lang.Type;

public record IrSqrt(IrExpression expr) implements IrExpression {
    @Override
    public Type type() { // Should be int or double
        if(!expr.type().equals(Type.INT) && !expr.type().equals(Type.DOUBLE)) {
            throw new InternalCompilerException("Type mismatch in negate: " + expr.type());
        }
        return expr.type();
    }

    @Override
    public Iterable<? extends IrNode> children() {
        return java.util.List.of(expr);
    }
}
