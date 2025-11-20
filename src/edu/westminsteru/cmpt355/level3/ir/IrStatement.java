package edu.westminsteru.cmpt355.level3.ir;


public sealed interface IrStatement extends IrNode
        permits IrAssembly, IrBlock, 
        IrPrint, IrReturn,
        IrVariableAssignment, IrVariableDeclaration, 
        IrFunctionDeclaration, IrFunctionCall,
        IrBranch, IrBranchTarget, IrGoto, IrExpressionStatement {
}
