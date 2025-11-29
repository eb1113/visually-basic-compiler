package edu.westminsteru.cmpt355.level3.ir;

import edu.westminsteru.cmpt355.level3.lang.Type;
import edu.westminsteru.cmpt355.level3.lang.Variable;

/**
 * Represents a variable in the IR.
 * @param variable The variable.
 */
public record IrVariable(Variable variable) implements IrExpression, IrLvalue {
    @Override
    public Type type() {
        return variable.type();
    }
    
    @Override
    public Iterable<? extends IrNode> children() {
        return java.util.List.of();
    }
}
