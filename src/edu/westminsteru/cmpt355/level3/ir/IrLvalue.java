package edu.westminsteru.cmpt355.level3.ir;

public sealed interface IrLvalue extends IrExpression
    permits IrFieldAccess, IrArraySubscript, IrVariable {
}
