package edu.westminsteru.cmpt355.level3.ir;

import edu.westminsteru.cmpt355.level3.InternalCompilerException;
import edu.westminsteru.cmpt355.level3.ast.BinaryOp;
import edu.westminsteru.cmpt355.level3.lang.BuiltInType;
import edu.westminsteru.cmpt355.level3.lang.Type;

/**
 * Binary operation: left op right
 * @param op The binary operation.
 * @param left The left operand.
 * @param right The right operand.
 */
public record IrBinary(BinaryOp op, edu.westminsteru.cmpt355.level3.ir.IrExpression left, IrExpression right) implements IrExpression {
    @Override
    public Type type() {
        switch (op) {
            case ADD, SUB, MUL, DIV, MOD -> {
                // Both sides must be same type (INT or DOUBLE)
                Type leftType = left.type();
                Type rightType = right.type();
                if (!leftType.equals(rightType)) {
                    throw new InternalCompilerException("Type mismatch in " + op + ": " + leftType + " and " + rightType);
                }
                if (leftType != BuiltInType.INT && leftType != BuiltInType.DOUBLE) {
                    throw new InternalCompilerException("Type mismatch in " + op + ": " + leftType + " is not numeric");
                }
                return leftType;
            }
            case CONCAT -> {
                // Both sides must be STRING
                if (left.type() != BuiltInType.STRING || right.type() != BuiltInType.STRING) {
                    throw new InternalCompilerException("Type mismatch in concat: " + left.type() + " and " + right.type());
                }
                return BuiltInType.STRING;
            }
            case EQ, NE, LT, LE, GT, GE -> {
                // Comparison operators: both sides must be same type (INT, DOUBLE, or STRING)
                Type leftType = left.type();
                Type rightType = right.type();
                if (!leftType.equals(rightType)) {
                    throw new InternalCompilerException("Type mismatch in " + op + ": " + leftType + " and " + rightType);
                }
                if (leftType != BuiltInType.INT && leftType != BuiltInType.DOUBLE && leftType != BuiltInType.STRING) {
                    throw new InternalCompilerException("Type mismatch in " + op + ": " + leftType + " is not comparable");
                }
                return BuiltInType.BOOLEAN;
            }
            case AND, OR -> {
                // Logical operators: both sides must be BOOLEAN
                if (left.type() != BuiltInType.BOOLEAN || right.type() != BuiltInType.BOOLEAN) {
                    throw new InternalCompilerException("Type mismatch in " + op + ": " + left.type() + " and " + right.type() + " must be BOOLEAN");
                }
                return BuiltInType.BOOLEAN;
            }
            default -> throw new InternalCompilerException("Unknown binary operation: " + op);
        }
    }

    @Override
    public Iterable<? extends IrNode> children() {
        return java.util.List.of(left, right);
    }
}

