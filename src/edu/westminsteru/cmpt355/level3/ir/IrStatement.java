package edu.westminsteru.cmpt355.level3.ir;


public sealed interface IrStatement extends IrNode
        permits IrAssembly, IrBlock, IrBranch, IrBranchTarget, 
        IrExpressionStatement, IrFunctionCall, IrFunctionDeclaration, IrGoto,
        IrMethodCall, IrPrint, IrRecordConstruction, IrReturn, 
        IrVariableAssignment, IrVariableDeclaration {
}
