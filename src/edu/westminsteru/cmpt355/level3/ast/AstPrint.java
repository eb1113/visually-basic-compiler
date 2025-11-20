package edu.westminsteru.cmpt355.level3.ast;

import java.util.List;

public record AstPrint(List<AstPrintArgument> args)
    implements AstStatement{

    @Override
    public Iterable<? extends AstPrintArgument> children() {
        return args;
    }
}
