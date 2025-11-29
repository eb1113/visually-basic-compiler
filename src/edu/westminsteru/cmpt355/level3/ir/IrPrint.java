package edu.westminsteru.cmpt355.level3.ir;

import java.util.List;

/**
 * Print statement: print(args)
 * @param args The arguments to the print statement (args).
 */
public record IrPrint(List<IrExpression> args) implements IrStatement {
    @Override
    public Iterable<? extends IrNode> children() { return args; }
}
