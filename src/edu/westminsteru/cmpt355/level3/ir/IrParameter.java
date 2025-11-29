package edu.westminsteru.cmpt355.level3.ir;

import edu.westminsteru.cmpt355.level3.lang.Variable;

/**
 * Parameter: type name
 * @param variable The variable representing the parameter.
 */
public record IrParameter(Variable variable) implements IrNode {

    @Override
    public Iterable<? extends IrNode> children() {
        return java.util.List.of();
    }
}
