package edu.westminsteru.cmpt355.level3.ir;

import edu.westminsteru.cmpt355.level3.lang.Type;

import java.util.List;

/**
 * Function call: functionName(args)
 * @param type The type of the function (return type).
 * @param name The name of the function (functionName).
 * @param args The arguments to the function (args).
 */
public record IrFunctionCall(Type type, String name, List<IrExpression> args) implements IrStatement, IrExpression {
    @Override
    public Type type() {
        return type;
    }

    @Override
    public Iterable<? extends IrNode> children() {
        return args;
    }
}
