package edu.westminsteru.cmpt355.level3.ir;

import edu.westminsteru.cmpt355.level3.lang.RecordType;
import edu.westminsteru.cmpt355.level3.lang.Type;

import java.util.List;

/**
 * Record construction: new RecordName(args)
 * @param name The name of the record (RecordName).
 * @param args The arguments to the record construction (args).
 * @param recordType The type of the record (recordType).
 */
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
