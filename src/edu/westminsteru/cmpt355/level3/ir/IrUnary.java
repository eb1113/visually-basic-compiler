package edu.westminsteru.cmpt355.level3.ir;

import edu.westminsteru.cmpt355.level3.InternalCompilerException;
import edu.westminsteru.cmpt355.level3.ast.UnaryOp;
import edu.westminsteru.cmpt355.level3.lang.BuiltInType;
import edu.westminsteru.cmpt355.level3.lang.Type;

public record IrUnary(UnaryOp op, IrExpression expr) implements IrExpression {
    @Override
    public Type type() {
        switch (op) {
            case NEGATE -> {
                // Negate must be INT or DOUBLE
                Type exprType = expr.type();
                if (exprType != BuiltInType.INT && exprType != BuiltInType.DOUBLE) {
                    throw new InternalCompilerException("Type mismatch in negate: " + exprType);
                }
                return exprType;
            }
            case NOT -> {
                // NOT must be BOOLEAN
                if (expr.type() != BuiltInType.BOOLEAN) {
                    throw new InternalCompilerException("Type mismatch in NOT: " + expr.type() + " must be BOOLEAN");
                }
                return BuiltInType.BOOLEAN;
            }
            default -> throw new InternalCompilerException("Unknown unary operation: " + op);
        }
    }

    @Override
    public Iterable<? extends IrNode> children() {
        return java.util.List.of(expr);
    }
}

