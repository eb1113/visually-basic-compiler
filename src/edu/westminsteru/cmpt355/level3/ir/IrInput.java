package edu.westminsteru.cmpt355.level3.ir;

import edu.westminsteru.cmpt355.level3.lang.Type;

/**
 * Input statement: input type prompt
 * @param type The type of the input.
 * @param prompt The prompt expression.
 */
public record IrInput(Type type, IrExpression prompt) implements IrExpression {
    @Override
    public Iterable<? extends IrNode> children() {
        return java.util.List.of(prompt);
    }

    @Override
    public Type type() {
        return type;
    }
}