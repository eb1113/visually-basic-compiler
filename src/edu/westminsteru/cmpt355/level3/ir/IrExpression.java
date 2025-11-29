package edu.westminsteru.cmpt355.level3.ir;

import edu.westminsteru.cmpt355.level3.lang.Type;

/**
 * Base interface for all IR nodes representing expressions, i.e., things that
 * evaluate to a value
 */
public sealed interface IrExpression extends IrNode
        permits IrArrayConstruction, IrArraySubscript, IrBinary, IrBoolean, 
        IrCast, IrDouble, IrFieldAccess, IrFunctionCall, IrIncrement, 
        IrInput, IrInt, IrLvalue, IrMethodCall, IrRecordConstruction, IrString, IrToString, 
        IrUnary, IrVariable, IrWiden {

    /** Returns the data type of this expression */
    Type type();
}
