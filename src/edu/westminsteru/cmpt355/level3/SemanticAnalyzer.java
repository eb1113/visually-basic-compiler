package edu.westminsteru.cmpt355.level3;

import edu.westminsteru.cmpt355.level3.ast.*;
import edu.westminsteru.cmpt355.level3.ir.*;
import edu.westminsteru.cmpt355.level3.lang.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Class that performs semantic analysis, deriving an IR from an AST.
 * The semantic analyzer ensures that the semantics of the AST are consistent, for
 * example confirming that the types make sense (typechecking) and that variables are
 * declared before their use. (See the subclasses of SemanticAnalysisException
 * for some examples of things the semantic analyzer should be checking for.)
 */
public class SemanticAnalyzer {

    private int labelCounter = 0;

    private String newLabel(String base) {
        return base + (labelCounter++);
    }

    private SymbolTable enterScope(SymbolTable table) {
        if (table instanceof LocalSymbolTable local) {
            return local.createChildScope();
        } else if (table instanceof GlobalSymbolTable global) {
            // Create a LocalSymbolTable with the global table as parent
            // This allows blocks at global scope to have their own local variables
            // while still being able to access global variables
            return new LocalSymbolTable(global);
        }
        return table;
    }

    public IrProgram analyzeProgram(AstProgram astProgram) throws SemanticAnalysisException {
        GlobalSymbolTable globalTable = new GlobalSymbolTable();
        List<IrFunctionDeclaration> irFunctions = new ArrayList<>();
        List<IrStatement> irStatements = new ArrayList<>();

        // Define all functions (without analyzing bodies)
        for (AstFunctionDeclaration funcDecl : astProgram.funcs()) {
            try {
                globalTable.defineFunction(funcDecl.type(), funcDecl.name(), 
                    funcDecl.parameters().stream().map(p -> p.type()).toList());
            } catch (DuplicateDefinitionException e) {
                throw new SemanticAnalysisException("Function " + funcDecl.name() + " is already defined.");
            }
        }

        // Define all global variables (without analyzing initializers)
        for (AstStatement stmt : astProgram.stmts()) {
            if (stmt instanceof AstVariableDeclaration decl) {
                try {
                    globalTable.defineVariable(decl.type(), decl.name());
                } catch (DuplicateDefinitionException e) {
                    throw new SemanticAnalysisException("Variable " + decl.name() + " is already defined.");
                }
            }
        }

        // Analyze all function bodies
        for (AstFunctionDeclaration funcDecl : astProgram.funcs()) {
            IrFunctionDeclaration irFunc = analyzeFunction(funcDecl, globalTable);
            irFunctions.add(irFunc);
        }

        // Analyze the main program statements
        for (AstStatement stmt : astProgram.stmts()) {
            List<IrStatement> analyzed = analyzeStatement(stmt, globalTable, null);
            if (analyzed != null) {
                irStatements.addAll(analyzed);
            }
        }

        return new IrProgram(irFunctions, irStatements);
    }

    public IrFunctionDeclaration analyzeFunction(AstFunctionDeclaration astFunction, GlobalSymbolTable table) throws SemanticAnalysisException {
        // Create a local symbol table
        LocalSymbolTable localTable = new LocalSymbolTable(table);

        // Build IR parameters by defining with local variables
        List<IrParameter> irParameters = new ArrayList<>();
        for (AstParameter param : astFunction.parameters()) {
            LocalVariable localVar = localTable.defineVariable(param.type(), param.name());
            IrParameter irParam = new IrParameter(localVar);
            irParameters.add(irParam);
        }
        // Get the already-defined function from the global table
        Function currentFunction = table.resolveFunction(astFunction.name())
            .orElseThrow(() -> new SemanticAnalysisException("Function " + astFunction.name() + " not found in symbol table."));

        // Analyze the function body
        IrBlock irBlock = analyzeBlock(astFunction.body(), localTable, currentFunction);

        // Create and return the IR function declaration
        return new IrFunctionDeclaration(astFunction.type(), astFunction.name(), irParameters, irBlock);

    }

    public IrBlock analyzeBlock(AstBlock body, SymbolTable table, Function currentFunction) throws SemanticAnalysisException{
        SymbolTable blockTable = enterScope(table);
        List<IrStatement> irStatements = new ArrayList<>();
        for (AstStatement stmt : body.statements()) {
            List<IrStatement> analyzed = analyzeStatement(stmt, blockTable, currentFunction);
            if (analyzed != null) {
                irStatements.addAll(analyzed);
            }
        }
        return new IrBlock(irStatements);
    }

    public List<IrStatement> analyzeStatement(AstStatement astStatement, SymbolTable table, Function currrentFunction) throws SemanticAnalysisException {
        // astStatement could be at either the local or global level
        // symbols variable or functions deal with optional case
        // currentFunction can be null if at global level
        if (astStatement == null) {
            // Skip null statements (can happen with empty for loop parts)
            return null;
        }
        switch(astStatement){
            case AstBlock block -> {
                return List.of(analyzeBlock(block, table, currrentFunction));
            }
            case AstAssembly assembly -> {
                IrAssembly irAssembly = new IrAssembly(assembly.comment());
                return List.of(irAssembly);
            }
            case AstFunctionCall call -> {
                // Check return oldType of function call - if void then return IrStatement
                Function func = table.resolveFunction(call.name()).orElseThrow(() -> new SemanticAnalysisException("Function not defined: " + call.name()));
                List<IrExpression> irArgs = new ArrayList<>();
                
                // Analyze arguments and widen them to match function signature
                List<Type> paramTypes = func.parameterTypes();
                
                // Validate argument count
                if (call.args().size() != paramTypes.size()) {
                    throw new SemanticAnalysisException(
                        "Function " + func.name() + " expects " + paramTypes.size() +
                        " arguments but got " + call.args().size());
                }
                
                for (int i = 0; i < call.args().size(); i++) {
                    IrExpression arg = analyzeExpression(call.args().get(i), table, currrentFunction);
                    Type argType = arg.type();
                    Type paramType = paramTypes.get(i);
                    
                    // Widen int to double if needed
                    if (argType == BuiltInType.INT && paramType == BuiltInType.DOUBLE) {
                        arg = new IrWiden(arg);
                    }
                    // Convert to string if needed
                    else if (paramType == BuiltInType.STRING && argType != BuiltInType.STRING) {
                        arg = new IrToString(arg);
                    }
                    // Error if types don't match and can't be widened/converted
                    else if (argType != paramType) {
                        throw new SemanticAnalysisException("Type mismatch in function call: argument " + (i+1) + " has oldType " + argType + " but function expects " + paramType);
                    }
                    
                    irArgs.add(arg);
                }
                Type irType = switch (func.returnType()) {
                    case Type t -> t;
                    case ReturnType.Void _ -> throw new SemanticAnalysisException("Cannot use void function as expression");
                };
                IrFunctionCall irCall = new IrFunctionCall(irType, func.name(), irArgs);
                // Always return as statement; CodeGenerator will pop non-void results
                return List.of(irCall);
            }
            case AstFunctionDeclaration funcDecl -> {
                IrFunctionDeclaration irFuncDecl = analyzeFunction(funcDecl, (GlobalSymbolTable) table);
                return List.of(irFuncDecl);
            }
            case AstPrint print -> {
                List<IrExpression> irArgs = new ArrayList<>();
                for (AstPrintArgument arg : print.args()) {
                    if (arg == null) {
                        throw new SemanticAnalysisException("Print argument cannot be null");
                    }
                    if (arg instanceof AstExpression exprArg) {
                        irArgs.add(analyzeExpression(exprArg, table, currrentFunction));
                    } else if (arg instanceof AstString strArg) {
                        irArgs.add(new IrString(strArg.text()));
                    } else {
                        throw new SemanticAnalysisException("Unknown print argument oldType: " + arg.getClass().getSimpleName());
                    }
                }
                return List.of(new IrPrint(irArgs));
            }
            case AstReturn ret -> {
                // Validate that return is inside a function
                if (currrentFunction == null) {
                    throw new SemanticAnalysisException("Return statement not inside a function");
                }
                
                ReturnType retType = currrentFunction.returnType();
                
                // Handle void function returns
                if (ret.expr().isEmpty()) {
                    if (!(retType instanceof ReturnType.Void)) {
                        throw new SemanticAnalysisException("Missing return value in non-void function " + currrentFunction.name());
                    }
                    return List.of(new IrReturn(Optional.empty()));
                }
                
                // Handle non-void function returns
                IrExpression value = analyzeExpression(ret.expr().get(), table, currrentFunction);
                Type valueType = value.type();
                
                if (retType instanceof ReturnType.Void) {
                    throw new SemanticAnalysisException("Cannot return a value from void function " + currrentFunction.name());
                }
                
                Type targetType = ((Type) retType);
                
                // Apply same widening/toString rules as assignment
                if (valueType == BuiltInType.INT && targetType == BuiltInType.DOUBLE) {
                    value = new IrWiden(value);
                } else if (targetType == BuiltInType.STRING && valueType != BuiltInType.STRING) {
                    value = new IrToString(value);
                } else if (valueType != targetType) {
                    throw new SemanticAnalysisException("Type mismatch in return: cannot return " + valueType + " from function " + currrentFunction.name() + " expecting " + targetType);
                }
                
                return List.of(new IrReturn(Optional.of(value)));
            }
            case AstVariableAssignment assign -> {
                // For now, only handle simple variable lvalues
                // TODO: Support array subscript and field access lvalues
                if (!(assign.lvalue() instanceof AstVariable varAst)) {
                    throw new SemanticAnalysisException("Complex lvalues (array subscripts, field access) not yet supported in assignments");
                }
                
                String varName = varAst.variable();
                Variable var = table.resolveVariable(varName).orElseThrow(() -> new SemanticAnalysisException("Variable not defined: " + varName));
                Type targetType = var.type();
                IrExpression valueExpr;

                // Special handling: input() as RHS should adopt the target variable's type
                if (assign.expr() instanceof AstInput inputExpr) {
                    // Analyze prompt and coerce to string if needed
                    IrExpression promptExpr = analyzeExpression(inputExpr.prompt(), table, currrentFunction);
                    if (promptExpr.type() != BuiltInType.STRING) {
                        promptExpr = new IrToString(promptExpr);
                    }
                    valueExpr = new IrInput(targetType, promptExpr);
                } else {
                    valueExpr = analyzeExpression(assign.expr(), table, currrentFunction);
                }

                Type valueType = valueExpr.type();

                // Widen int to double if needed
                if (valueType == BuiltInType.INT && targetType == BuiltInType.DOUBLE) {
                    valueExpr = new IrWiden(valueExpr);
                }
                // Convert to string if needed
                else if (targetType == BuiltInType.STRING && valueType != BuiltInType.STRING) {
                    valueExpr = new IrToString(valueExpr);
                }
                // Error if types don't match and can't be widened/converted
                else if (valueType != targetType) {
                    throw new SemanticAnalysisException("Type mismatch: cannot assign " + valueType + " to " + targetType);
                }

                IrVariable irVariable = new IrVariable(var);
                IrVariableAssignment varAssign = new IrVariableAssignment(irVariable, valueExpr);
                return List.of(varAssign);
            }
            case AstVariableDeclaration decl -> {
                // Check if we're in a local scope (function) or global scope
                Variable var;
                if (table instanceof LocalSymbolTable) {
                    // Inside a function or block scope, define a new local variable
                    try {
                        var = table.defineVariable(decl.type(), decl.name());
                    } catch (DuplicateDefinitionException e) {
                        throw new SemanticAnalysisException("Variable " + decl.name() + " is already defined in this scope.");
                    }
                } else {
                    // Global scope - check if variable was already defined at top level (not in nested scopes)
                    // Only check the global table itself, not parent scopes (to allow shadowing)
                    Optional<? extends Variable> existingVar = table.resolveVariable(decl.name());
                    if (existingVar.isPresent() && existingVar.get() instanceof GlobalVariable) {
                        // Variable was defined at top level in global scope, use that
                        var = existingVar.get();
                    } else {
                        // Variable declaration at global scope - define it in global scope
                        // This allows shadowing of variables from nested scopes (like functions)
                        try {
                            var = table.defineVariable(decl.type(), decl.name());
                        } catch (DuplicateDefinitionException e) {
                            throw new SemanticAnalysisException("Variable " + decl.name() + " is already defined in this scope.");
                        }
                    }
                }
                
                Optional<IrExpression> irInit;
                if (decl.expr().isPresent()) {
                    Type targetType = var.type();
                    IrExpression valueExpr;
                    // Special handling: input() as initializer should adopt the declared oldType
                    if (decl.expr().get() instanceof AstInput inputExpr) {
                        IrExpression promptExpr = analyzeExpression(inputExpr.prompt(), table, currrentFunction);
                        if (promptExpr.type() != BuiltInType.STRING) {
                            promptExpr = new IrToString(promptExpr);
                        }
                        valueExpr = new IrInput(targetType, promptExpr);
                    } else {
                        valueExpr = analyzeExpression(decl.expr().get(), table, currrentFunction);
                    }
                    Type valueType = valueExpr.type();
                    
                    // Widen int to double if needed
                    if (valueType == BuiltInType.INT && targetType == BuiltInType.DOUBLE) {
                        valueExpr = new IrWiden(valueExpr);
                    }
                    // Convert to string if needed
                    else if (targetType == BuiltInType.STRING && valueType != BuiltInType.STRING) {
                        valueExpr = new IrToString(valueExpr);
                    }
                    // Error if types don't match and can't be widened/converted
                    else if (valueType != targetType) {
                        throw new SemanticAnalysisException("Type mismatch: cannot assign " + valueType + " to " + targetType);
                    }
                    
                    irInit = Optional.of(valueExpr);
                } else {
                    irInit = Optional.empty();
                }
                return List.of(new IrVariableDeclaration(var, irInit));
            }
            
            case AstIf astIf -> {
                IrExpression condition = analyzeExpression(astIf.condition(), table, currrentFunction);
                if (condition.type() != BuiltInType.BOOLEAN) {
                    throw new SemanticAnalysisException("Condition of if statement must be BOOLEAN, found " + condition.type());
                }
                condition = invertCondition(condition);

                List<IrStatement> result = new ArrayList<>();
                String elseLabel = astIf.elseBody().isPresent() ? newLabel("else") : newLabel("endif");
                String endLabel = astIf.elseBody().isPresent() ? newLabel("endif") : elseLabel;

                result.add(new IrBranch(condition, elseLabel));

                // Create a parent scope for if/else blocks. Each block (then/else) will get its own
                // nested scope as a child of ifElseScope, so variables defined in one block are not
                // accessible in the other, but both can access variables from the parent scope.
                SymbolTable ifElseScope = enterScope(table);
                result.addAll(analyzeBlock(astIf.thenBody(), ifElseScope, currrentFunction).statements());

                if (astIf.elseBody().isPresent()) {
                    result.add(new IrGoto(endLabel));
                    result.add(new IrBranchTarget(elseLabel));

                    // Use the same parent scope for else block
                    result.addAll(analyzeBlock(astIf.elseBody().get(), ifElseScope, currrentFunction).statements());
                    result.add(new IrBranchTarget(endLabel));
                } else {
                    result.add(new IrBranchTarget(endLabel));
                }

                return result;
            }
            case AstWhile astWhile -> {
                // Create a scope for the while loop so variables defined in the body
                // are accessible throughout the loop (including in nested statements)
                SymbolTable whileScope = enterScope(table);
                List<IrStatement> result = new ArrayList<>();
                String startLabel = newLabel("whileStart");
                String endLabel = newLabel("whileEnd");

                result.add(new IrBranchTarget(startLabel));

                IrExpression condition = analyzeExpression(astWhile.condition(), whileScope, currrentFunction);
                if (condition.type() != BuiltInType.BOOLEAN) {
                    throw new SemanticAnalysisException("Condition of while loop must be BOOLEAN, found " + condition.type());
                }
                condition = invertCondition(condition);

                result.add(new IrBranch(condition, endLabel));

                result.addAll(analyzeBlock(astWhile.body(), whileScope, currrentFunction).statements());

                result.add(new IrGoto(startLabel));
                result.add(new IrBranchTarget(endLabel));
                return result;
            }
            case AstPascalFor pascalFor -> {
                // Desugar Pascal-style for loop to standard AstFor
                // Init: TYPE var = startVal
                AstVariableDeclaration initDecl = new AstVariableDeclaration(
                    pascalFor.type(), pascalFor.varName(), Optional.of(pascalFor.startVal()));
                Optional<AstStatement> init = Optional.of(initDecl);
                
                // Update: var = var + stepVal
                AstVariable varRef = new AstVariable(pascalFor.varName());
                AstBinary addExpr = new AstBinary(BinaryOp.ADD, varRef, pascalFor.stepVal());
                AstVariableAssignment updateAssign = new AstVariableAssignment(varRef, addExpr);
                Optional<AstStatement> update = Optional.of(updateAssign);
                
                // Condition: (stepVal >= 0 && var <= endVal) || (stepVal < 0 && var >= endVal)
                // This handles both positive and negative steps
                AstVariable varRefForCond = new AstVariable(pascalFor.varName());
                AstInt zero = new AstInt(0);
                AstBinary stepGeZero = new AstBinary(BinaryOp.GE, pascalFor.stepVal(), zero);
                AstBinary varLeEnd = new AstBinary(BinaryOp.LE, varRefForCond, pascalFor.endVal());
                AstBinary positiveCase = new AstBinary(BinaryOp.AND, stepGeZero, varLeEnd);
                
                AstBinary stepLtZero = new AstBinary(BinaryOp.LT, pascalFor.stepVal(), zero);
                AstBinary varGeEnd = new AstBinary(BinaryOp.GE, varRefForCond, pascalFor.endVal());
                AstBinary negativeCase = new AstBinary(BinaryOp.AND, stepLtZero, varGeEnd);
                
                AstBinary condition = new AstBinary(BinaryOp.OR, positiveCase, negativeCase);
                Optional<AstExpression> cond = Optional.of(condition);
                
                // Create desugared AstFor and analyze it
                AstFor desugaredFor = new AstFor(init, cond, update, pascalFor.body());
                return analyzeStatement(desugaredFor, table, currrentFunction);
            }
            case AstFor astFor -> {
                SymbolTable loopScope = enterScope(table);
                List<IrStatement> result = new ArrayList<>();

                if (astFor.init().isPresent()) {
                    List<IrStatement> initIr = analyzeStatement(astFor.init().get(), loopScope, currrentFunction);
                    if (initIr != null) {
                        result.addAll(initIr);
                    }
                }

                String startLabel = newLabel("forStart");
                String endLabel = newLabel("forEnd");

                result.add(new IrBranchTarget(startLabel));

                if (astFor.condition().isPresent()) {
                    IrExpression condition = analyzeExpression(astFor.condition().get(), loopScope, currrentFunction);
                    if (condition.type() != BuiltInType.BOOLEAN) {
                        throw new SemanticAnalysisException("Condition of for loop must be BOOLEAN, found " + condition.type());
                    }
                    condition = invertCondition(condition);
                    result.add(new IrBranch(condition, endLabel));
                }

                result.addAll(analyzeBlock(astFor.body(), loopScope, currrentFunction).statements());

                if (astFor.update().isPresent()) {
                    List<IrStatement> updateIr = analyzeStatement(astFor.update().get(), loopScope, currrentFunction);
                    if (updateIr != null) {
                        result.addAll(updateIr);
                    }
                }

                result.add(new IrGoto(startLabel));
                result.add(new IrBranchTarget(endLabel));

                return result;
            }
            case AstIncrement increment -> {
                // Analyze increment as an expression, then wrap it as a statement
                IrExpression irExpr = analyzeExpression(increment, table, currrentFunction);
                return List.of(new IrExpressionStatement(irExpr));
            }
            default -> throw new SemanticAnalysisException("Unknown statement oldType: " + astStatement.getClass().getSimpleName());
        }
    }

    public IrExpression analyzeExpression(AstExpression expr, SymbolTable table, Function currentFunction) throws SemanticAnalysisException{
        if (expr == null) {
            throw new SemanticAnalysisException("Cannot analyze null expression");
        }
        switch(expr){
            case AstBinary binary -> {
                IrExpression left = analyzeExpression(binary.left(), table, currentFunction);
                IrExpression right = analyzeExpression(binary.right(), table, currentFunction);
                
                return switch (binary.op()) {
                    case ADD -> {
                        // ADD with strings should use CONCAT
                        if (left.type() == BuiltInType.STRING || right.type() == BuiltInType.STRING) {
                            // Convert both sides to string if needed
                            if (left.type() != BuiltInType.STRING) {
                                left = new IrToString(left);
                            }
                            if (right.type() != BuiltInType.STRING) {
                                right = new IrToString(right);
                            }
                            yield new IrBinary(BinaryOp.CONCAT, left, right);
                        }
                        // Numeric ADD: widen int to double if needed
                        if (left.type() == BuiltInType.INT && right.type() == BuiltInType.DOUBLE) {
                            left = new IrWiden(left);
                        } else if (left.type() == BuiltInType.DOUBLE && right.type() == BuiltInType.INT) {
                            right = new IrWiden(right);
                        } else if (left.type() != right.type()) {
                            throw new SemanticAnalysisException("Type mismatch in " + binary.op() + ": " + left.type() + " and " + right.type());
                        }
                        yield new IrBinary(binary.op(), left, right);
                    }
                    case SUB, MUL, DIV, MOD -> {
                        // Widen int to double if needed
                        if (left.type() == BuiltInType.INT && right.type() == BuiltInType.DOUBLE) {
                            left = new IrWiden(left);
                        } else if (left.type() == BuiltInType.DOUBLE && right.type() == BuiltInType.INT) {
                            right = new IrWiden(right);
                        } else if (left.type() != right.type()) {
                            throw new SemanticAnalysisException("Type mismatch in " + binary.op() + ": " + left.type() + " and " + right.type());
                        }
                        yield new IrBinary(binary.op(), left, right);
                    }
                    case CONCAT -> {
                        // Convert both sides to string if needed
                        if (left.type() != BuiltInType.STRING) {
                            left = new IrToString(left);
                        }
                        if (right.type() != BuiltInType.STRING) {
                            right = new IrToString(right);
                        }
                        yield new IrBinary(binary.op(), left, right);
                    }
                    case EQ, NE, LT, LE, GT, GE -> {
                        // Both sides must be same oldType (INT, DOUBLE, or STRING)
                        // Widen int to double if needed for numeric comparisons
                        if (left.type() == BuiltInType.INT && right.type() == BuiltInType.DOUBLE) {
                            left = new IrWiden(left);
                        } else if (left.type() == BuiltInType.DOUBLE && right.type() == BuiltInType.INT) {
                            right = new IrWiden(right);
                        } else if (left.type() != right.type()) {
                            throw new SemanticAnalysisException("Type mismatch in " + binary.op() + ": " + left.type() + " and " + right.type());
                        }
                        // Verify oldType is comparable (INT, DOUBLE, or STRING)
                        if (left.type() != BuiltInType.INT && left.type() != BuiltInType.DOUBLE && left.type() != BuiltInType.STRING) {
                            throw new SemanticAnalysisException("Type mismatch in " + binary.op() + ": " + left.type() + " is not comparable");
                        }
                        yield new IrBinary(binary.op(), left, right);
                    }
                    case AND, OR -> {
                        // Logical operators: both sides must be BOOLEAN
                        if (left.type() != BuiltInType.BOOLEAN || right.type() != BuiltInType.BOOLEAN) {
                            throw new SemanticAnalysisException("Type mismatch in " + binary.op() + ": " + left.type() + " and " + right.type() + " must be BOOLEAN");
                        }
                        yield new IrBinary(binary.op(), left, right);
                    }
                };
            }
            case AstUnary unary -> {
                IrExpression inner = analyzeExpression(unary.expr(), table, currentFunction);
                return switch (unary.op()) {
                    case NEGATE -> {
                        // Negate must be INT or DOUBLE
                        if (inner.type() != BuiltInType.INT && inner.type() != BuiltInType.DOUBLE) {
                            throw new SemanticAnalysisException("Type mismatch in negate: " + inner.type() + " must be INT or DOUBLE");
                        }
                        yield new IrUnary(unary.op(), inner);
                    }
                    case NOT -> {
                        // NOT must be BOOLEAN
                        if (inner.type() != BuiltInType.BOOLEAN) {
                            throw new SemanticAnalysisException("Type mismatch in NOT: " + inner.type() + " must be BOOLEAN");
                        }
                        yield new IrUnary(unary.op(), inner);
                    }
                };
            }
            case AstInt intLiteral -> {
                return new IrInt(intLiteral.value());
            }
            case AstDouble doubleLiteral -> {
                return new IrDouble(doubleLiteral.value());
            }
            case AstString stringLiteral -> {
                return new IrString(stringLiteral.text());
            }
            case AstBoolean booleanLiteral -> {
                return new IrBoolean(booleanLiteral.value());
            }
            case AstFunctionCall call -> {
                Function func = table.resolveFunction(call.name())
                        .orElseThrow(() -> new SemanticAnalysisException("Function not defined: " + call.name()));
                // Check if function returns a non-void oldType
                if (func.returnType() instanceof ReturnType.Void) {
                    throw new SemanticAnalysisException("Cannot use void function call as an expression: " + call.name());
                } else if (func.returnType() instanceof Type t && t != BuiltInType.VOID) {
                    List<IrExpression> irArgs = new ArrayList<>();
                    List<Type> paramTypes = func.parameterTypes();
                    
                    // Validate argument count
                    if (call.args().size() != paramTypes.size()) {
                        throw new SemanticAnalysisException(
                            "Function " + func.name() + " expects " + paramTypes.size() +
                            " arguments but got " + call.args().size());
                    }
                    
                    // Analyze arguments and widen/convert them to match function signature
                    for (int i = 0; i < call.args().size(); i++) {
                        IrExpression arg = analyzeExpression(call.args().get(i), table, currentFunction);
                        Type argType = arg.type();
                        Type paramType = paramTypes.get(i);
                        
                        // Widen int to double if needed
                        if (argType == BuiltInType.INT && paramType == BuiltInType.DOUBLE) {
                            arg = new IrWiden(arg);
                        }
                        // Convert to string if needed
                        else if (paramType == BuiltInType.STRING && argType != BuiltInType.STRING) {
                            arg = new IrToString(arg);
                        }
                        // Error if types don't match and can't be widened/converted
                        else if (argType != paramType) {
                            throw new SemanticAnalysisException("Type mismatch in function call: argument " + (i+1) + " has oldType " + argType + " but function expects " + paramType);
                        }
                        
                        irArgs.add(arg);
                    }
                    return new IrFunctionCall(t, func.name(), irArgs);
                } else {
                    throw new SemanticAnalysisException("Cannot use void function call as an expression: " + call.name());
                }
            }
            case AstCast cast -> {
                IrExpression inner = analyzeExpression(cast.expr(), table, currentFunction);
                Type sourceType = inner.type();
                Type targetType = cast.targetType();
                
                // If types already match, no conversion needed
                if (sourceType == targetType) {
                    return inner;
                }
                
                // Handle various cast conversions
                if (targetType instanceof BuiltInType builtIn) {
                    return switch (builtIn) {
                        case INT -> {
                            // Convert to int
                            if (sourceType == BuiltInType.DOUBLE) {
                                // Double to int: truncate
                                yield new IrCast(targetType, inner);
                            } else if (sourceType == BuiltInType.STRING) {
                                // String to int: parse
                                yield new IrCast(targetType, inner);
                            } else if (sourceType == BuiltInType.BOOLEAN) {
                                // Boolean to int: true -> 1, false -> 0
                                yield new IrCast(targetType, inner);
                            } else {
                                throw new SemanticAnalysisException("Cannot cast " + sourceType + " to " + targetType);
                            }
                        }
                        case DOUBLE -> {
                            // Convert to double
                            if (sourceType == BuiltInType.INT) {
                                // Int to double: widen
                                yield new IrWiden(inner);
                            } else if (sourceType == BuiltInType.STRING) {
                                // String to double: parse
                                yield new IrCast(targetType, inner);
                            } else if (sourceType == BuiltInType.BOOLEAN) {
                                // Boolean to double: true -> 1.0, false -> 0.0
                                yield new IrCast(targetType, inner);
                            } else {
                                throw new SemanticAnalysisException("Cannot cast " + sourceType + " to " + targetType);
                            }
                        }
                        case STRING -> {
                            // Convert to string
                            if (sourceType == BuiltInType.STRING) {
                                yield inner; // Already a string
                            } else {
                                // Any type to string: use toString
                                yield new IrToString(inner);
                            }
                        }
                        case BOOLEAN -> {
                            // Convert to boolean
                            if (sourceType == BuiltInType.BOOLEAN) {
                                yield inner; // Already a boolean
                            } else if (sourceType == BuiltInType.INT || sourceType == BuiltInType.DOUBLE) {
                                // Numeric to boolean: non-zero -> true, zero -> false
                                yield new IrCast(targetType, inner);
                            } else if (sourceType == BuiltInType.STRING) {
                                // String to boolean: parse "true"/"false" or check if non-empty
                                yield new IrCast(targetType, inner);
                            } else {
                                throw new SemanticAnalysisException("Cannot cast " + sourceType + " to " + targetType);
                            }
                        }
                        default -> throw new SemanticAnalysisException("Cannot cast to type: " + targetType);
                    };
                } else {
                    // ArrayType or RecordType - for now, just pass through
                    // TODO: Implement proper casting for arrays and records
                    throw new SemanticAnalysisException("Cannot cast to type: " + targetType);
                }
            }
            case AstVariable variable -> {
                // Resolve variable from symbol table
                Optional<? extends Variable> resolved = table.resolveVariable(variable.variable());
                if (resolved.isEmpty()) {
                    throw new SemanticAnalysisException("Variable not defined: " + variable.variable());
                }
                Variable var = resolved.get();
                return new IrVariable(var);
            }
            case AstIncrement increment -> {
                // For now, only handle simple variable lvalues
                // TODO: Support array subscript and field access lvalues
                if (!(increment.lvalue() instanceof AstVariable varAst)) {
                    throw new SemanticAnalysisException("Complex lvalues (array subscripts, field access) not yet supported in increments");
                }
                
                // Resolve variable from symbol table
                String varName = varAst.variable();
                Variable var = table.resolveVariable(varName)
                    .orElseThrow(() -> new SemanticAnalysisException("Variable not defined: " + varName));
                
                // Variable must be INT or DOUBLE
                if (var.type() != BuiltInType.INT && var.type() != BuiltInType.DOUBLE) {
                    throw new SemanticAnalysisException("Increment/decrement only works on int and double variables, got: " + var.type());
                }
                
                IrVariable irVariable = new IrVariable(var);
                return new IrIncrement(increment.op(), irVariable, increment.isPrefix());
            }
            case AstInput input -> {
                // Analyze prompt and coerce to string if needed
                IrExpression promptExpr = analyzeExpression(input.prompt(), table, currentFunction);
                if (promptExpr.type() != BuiltInType.STRING) {
                    promptExpr = new IrToString(promptExpr);
                }
                // Default to STRING oldType for standalone input() expressions
                return new IrInput(BuiltInType.STRING, promptExpr);
            }
            default -> throw new SemanticAnalysisException("Unknown expression oldType: " + expr.getClass().getSimpleName());
        }
    }

    private IrExpression invertCondition(IrExpression condition) throws SemanticAnalysisException {
        return switch (condition) {
            case IrBinary binary -> {
                IrExpression result = switch (binary.op()) {
                    // Invert comparison operators
                    case EQ -> new IrBinary(BinaryOp.NE, binary.left(), binary.right());
                    case NE -> new IrBinary(BinaryOp.EQ, binary.left(), binary.right());
                    case LT -> new IrBinary(BinaryOp.GE, binary.left(), binary.right());
                    case LE -> new IrBinary(BinaryOp.GT, binary.left(), binary.right());
                    case GT -> new IrBinary(BinaryOp.LE, binary.left(), binary.right());
                    case GE -> new IrBinary(BinaryOp.LT, binary.left(), binary.right());
                    // De Morgan's laws for logical operators
                    case AND -> new IrBinary(BinaryOp.OR, 
                        invertCondition(binary.left()), 
                        invertCondition(binary.right()));
                    case OR -> new IrBinary(BinaryOp.AND, 
                        invertCondition(binary.left()), 
                        invertCondition(binary.right()));
                    default -> throw new SemanticAnalysisException("Cannot invert binary operator: " + binary.op());
                };
                yield result;
            }
            case IrUnary unary -> {
                if (unary.op() == UnaryOp.NOT) {
                    // Double negation: !(!x) = x
                    yield unary.expr();
                } else {
                    // Regular unary operators: wrap in NOT
                    yield new IrUnary(UnaryOp.NOT, unary);
                }
            }
            case IrBoolean bool -> {
                // Invert boolean literal
                yield new IrBoolean(!bool.value());
            }
            default -> {
                // Other expressions (variables, function calls, etc.), wrap in NOT
                yield new IrUnary(UnaryOp.NOT, condition);
            }
        };
    }

}
