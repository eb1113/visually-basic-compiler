package edu.westminsteru.cmpt355.level3;

import edu.westminsteru.cmpt355.level3.ast.AstProgram;
import edu.westminsteru.cmpt355.level3.ir.IrProgram;

/**
 * Class that performs semantic analysis, deriving an IR from an AST.
 * The semantic analyzer ensures that the semantics of the AST are consistent, for
 * example confirming that the types make sense (typechecking) and that variables are
 * declared before their use. (See the subclasses of SemanticAnalysisException
 * for some examples of things the semantic analyzer should be checking for.)
 */
public class SemanticAnalyzer {

    public IrProgram analyzeProgram(AstProgram astProgram) throws SemanticAnalysisException {
        throw new RuntimeException("Not implemented");
    }

    // Suggestion: write methods like the following....
    /*
    public IrFunction analyzeFunction(AstFunction astFunction, GlobalSymbolTable table) throws SemanticAnalysisException {

    }

    public IrStatement analyzeStatement(AstStatement astStatement, SymbolTable table) throws SemanticAnalysisException {
        // astStatement could be at either the local or global level
    }
     */
}
