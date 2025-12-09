package edu.westminsteru.cmpt355.level3.ir;

import edu.westminsteru.cmpt355.level3.InternalCompilerException;
import edu.westminsteru.cmpt355.level3.ast.IncrementOp;
import edu.westminsteru.cmpt355.level3.lang.BuiltInType;
import edu.westminsteru.cmpt355.level3.lang.Type;

/**
 * Increment/decrement operation: lvalue op
 * @param op The operation (INCREMENT or DECREMENT).
 * @param lvalue The left-hand side value (lvalue).
 * @param isPrefix Whether the operation is prefix or postfix.
 */
public record IrIncrement(IncrementOp op, IrExpression lvalue, boolean isPrefix) implements IrExpression {
    @Override
    public Type type() {
        // Increment/decrement only works on int and double lvalues
        Type lvalueType = lvalue.type();
        if (lvalueType != BuiltInType.INT && lvalueType != BuiltInType.DOUBLE) {
            throw new InternalCompilerException("Increment/decrement only works on int and double lvalues, got: " + lvalueType);
        }
        return lvalueType;
    }

    @Override
    public Iterable<? extends IrNode> children() {
        return java.util.List.of(lvalue);
    }
}

