package edu.westminsteru.cmpt355.level3.ir;

import edu.westminsteru.cmpt355.level3.InternalCompilerException;
import edu.westminsteru.cmpt355.level3.ast.IncrementOp;
import edu.westminsteru.cmpt355.level3.lang.BuiltInType;
import edu.westminsteru.cmpt355.level3.lang.Type;

public record IrIncrement(IncrementOp op, IrVariable variable, boolean isPrefix) implements IrExpression {
    @Override
    public Type type() {
        // Increment/decrement only works on int and double variables
        Type varType = variable.type();
        if (varType != BuiltInType.INT && varType != BuiltInType.DOUBLE) {
            throw new InternalCompilerException("Increment/decrement only works on int and double variables, got: " + varType);
        }
        return varType;
    }

    @Override
    public Iterable<? extends IrNode> children() {
        return java.util.List.of(variable);
    }
}

