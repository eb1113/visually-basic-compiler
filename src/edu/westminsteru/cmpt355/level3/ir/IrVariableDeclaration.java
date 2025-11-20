package edu.westminsteru.cmpt355.level3.ir;

import edu.westminsteru.cmpt355.level3.lang.Variable;

import java.util.List;
import java.util.Optional;

public record IrVariableDeclaration(Variable variable, Optional<IrExpression> initializer) implements IrStatement {
    @Override
    public Iterable<? extends IrNode> children() {
        return initializer.map(List::of).orElse(List.of());
    }
}
