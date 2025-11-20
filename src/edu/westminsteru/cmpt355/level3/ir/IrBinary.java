package edu.westminsteru.cmpt355.level3.ir;

import edu.westminsteru.cmpt355.level3.InternalCompilerException;
import edu.westminsteru.cmpt355.level3.ast.BinaryOp;
import edu.westminsteru.cmpt355.level3.lang.Type;

public record IrBinary(BinaryOp op, edu.westminsteru.cmpt355.level3.ir.IrExpression left, IrExpression right) implements IrExpression {
    @Override
    public Type type() {
        switch (op) {
            case ADD, SUB, MUL, DIV, MOD -> {
                // Both sides must be same type (INT or DOUBLE)
                if (!left.type().equals(right.type())) {
                    throw new InternalCompilerException("Type mismatch in " + op + ": " + left.type() + " and " + right.type());
                }
                if (left.type() != Type.INT && left.type() != Type.DOUBLE) {
                    throw new InternalCompilerException("Type mismatch in " + op + ": " + left.type() + " is not numeric");
                }
                return left.type();
            }
            case CONCAT -> {
                // Both sides must be STRING
                if (left.type() != Type.STRING || right.type() != Type.STRING) {
                    throw new InternalCompilerException("Type mismatch in concat: " + left.type() + " and " + right.type());
                }
                return Type.STRING;
            }
            case EQ, NE, LT, LE, GT, GE -> {
                // Comparison operators: both sides must be same type (INT, DOUBLE, or STRING)
                if (!left.type().equals(right.type())) {
                    throw new InternalCompilerException("Type mismatch in " + op + ": " + left.type() + " and " + right.type());
                }
                if (left.type() != Type.INT && left.type() != Type.DOUBLE && left.type() != Type.STRING) {
                    throw new InternalCompilerException("Type mismatch in " + op + ": " + left.type() + " is not comparable");
                }
                return Type.BOOLEAN;
            }
            case AND, OR -> {
                // Logical operators: both sides must be BOOLEAN
                if (left.type() != Type.BOOLEAN || right.type() != Type.BOOLEAN) {
                    throw new InternalCompilerException("Type mismatch in " + op + ": " + left.type() + " and " + right.type() + " must be BOOLEAN");
                }
                return Type.BOOLEAN;
            }
            default -> throw new InternalCompilerException("Unknown binary operation: " + op);
        }
    }

    @Override
    public Iterable<? extends IrNode> children() {
        return java.util.List.of(left, right);
    }
}

