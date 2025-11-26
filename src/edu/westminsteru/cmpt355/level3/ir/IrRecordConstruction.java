package edu.westminsteru.cmpt355.level3.ir;

import edu.westminsteru.cmpt355.level3.lang.RecordType;

import java.util.List;

public record IrRecordConstruction(String name, List<IrExpression> args, RecordType recordType)
    implements IrStatement{
    @Override
    public Iterable<? extends IrNode> children() {
        return args;
    }
}
