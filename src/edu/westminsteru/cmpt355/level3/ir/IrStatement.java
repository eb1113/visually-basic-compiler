package edu.westminsteru.cmpt355.level3.ir;

/**
 * Base interface for all IR nodes representing statements, i.e., things that
 * function as a sentence/line of code
 */
public sealed interface IrStatement extends IrNode
        permits IrAssembly, IrBlock, IrBranch, IrBranchTarget, 
        IrExpressionStatement, IrFunctionCall, IrFunctionDeclaration, IrGoto,
        IrMethodCall, IrPrint, IrRecordConstruction, IrReturn, 
        IrVariableAssignment, IrVariableDeclaration {
}
