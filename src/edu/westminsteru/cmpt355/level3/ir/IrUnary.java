package edu.westminsteru.cmpt355.level3.ir;

import edu.westminsteru.cmpt355.level3.InternalCompilerException;
import edu.westminsteru.cmpt355.level3.ast.UnaryOp;
import edu.westminsteru.cmpt355.level3.lang.Type;

public record IrUnary(UnaryOp op, IrExpression expr) implements IrExpression {
    @Override
    public Type type() {
        switch (op) {
            case NEGATE -> {
                // Negate must be INT or DOUBLE
                if (expr.type() != Type.INT && expr.type() != Type.DOUBLE) {
                    throw new InternalCompilerException("Type mismatch in negate: " + expr.type());
                }
                return expr.type();
            }
            case NOT -> {
                // NOT must be BOOLEAN
                if (expr.type() != Type.BOOLEAN) {
                    throw new InternalCompilerException("Type mismatch in NOT: " + expr.type() + " must be BOOLEAN");
                }
                return Type.BOOLEAN;
            }
            default -> throw new InternalCompilerException("Unknown unary operation: " + op);
        }
    }

    @Override
    public Iterable<? extends IrNode> children() {
        return java.util.List.of(expr);
    }
}

