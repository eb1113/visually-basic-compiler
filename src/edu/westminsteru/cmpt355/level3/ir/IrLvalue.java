package edu.westminsteru.cmpt355.level3.ir;

/**
 * Base interface for all IR nodes representing lvalues, i.e., things that can appear on the left-hand side of an assignment.
 */
public sealed interface IrLvalue extends IrExpression
    permits IrArraySubscript, IrFieldAccess, IrVariable {
}
