package edu.westminsteru.cmpt355.level3.ir;

import edu.westminsteru.cmpt355.level3.lang.RecordType;
import edu.westminsteru.cmpt355.level3.lang.Type;

import java.util.List;

public record IrRecordConstruction(String name, List<IrExpression> args, RecordType recordType)
    implements IrExpression, IrStatement {
    @Override
    public Type type() {
        return recordType;
    }

    @Override
    public Iterable<? extends IrNode> children() {
        return args;
    }
}
