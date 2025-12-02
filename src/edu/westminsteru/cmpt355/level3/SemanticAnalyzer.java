package edu.westminsteru.cmpt355.level3;

import edu.westminsteru.cmpt355.level3.ast.*;
import edu.westminsteru.cmpt355.level3.ir.*;
import edu.westminsteru.cmpt355.level3.lang.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private GlobalSymbolTable globalTable;

    private String newLabel(String base) {
        return base + (labelCounter++);
    }

    /**
     * Returns the global symbol table used during analysis.
     * @return The global symbol table.
     */
    public GlobalSymbolTable getGlobalSymbolTable() {
        return globalTable;
    }

    /**
     * Helper method to enter a new scope.
     * @param table The symbol table to enter.
     * @return The new symbol table.
     */
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

    /**
     * Helper method to extract the GlobalSymbolTable from any SymbolTable.
     * Returns the GlobalSymbolTable directly, or extracts it from LocalSymbolTable.
     */
    private GlobalSymbolTable extractGlobalSymbolTable(SymbolTable table) throws SemanticAnalysisException {
        if (table instanceof GlobalSymbolTable global) {
            return global;
        } else if (table instanceof LocalSymbolTable local) {
            return local.getGlobalTable();
        }
        throw new SemanticAnalysisException("Unknown symbol table type: " + table.getClass());
    }

    /**
     * Analyzes an AST program and generates an IR program.
     * @param astProgram The AST program to analyze.
     * @return The generated IR program.
     * @throws SemanticAnalysisException If there is a semantic analysis error.
     */
    public IrProgram analyzeProgram(AstProgram astProgram) throws SemanticAnalysisException {
        this.globalTable = new GlobalSymbolTable();
        List<IrFunctionDeclaration> irFunctions = new ArrayList<>();
        List<IrStatement> irStatements = new ArrayList<>();
        Map<String, List<IrFunctionDeclaration>> recordMethods = new HashMap<>();

        // Process record declarations first (define signatures only)
        for (AstRecordDeclaration recordDecl : astProgram.records()) {
            analyzeRecord(recordDecl, globalTable);
        }
        
        // Analyze record method bodies (after all records are defined)
        for (AstRecordDeclaration recordDecl : astProgram.records()) {
            List<IrFunctionDeclaration> methodBodies = analyzeRecordMethodBodies(recordDecl, globalTable);
            recordMethods.put(recordDecl.name(), methodBodies);
        }

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

        return new IrProgram(irFunctions, irStatements, recordMethods);
    }

    /**
     * Analyzes an AST record declaration and registers it in the global symbol table.
     * @param astRecord The AST record declaration to analyze.
     * @param table The global symbol table.
     * @throws SemanticAnalysisException If there is a semantic analysis error.
     */
    private void analyzeRecord(AstRecordDeclaration astRecord, GlobalSymbolTable table) throws SemanticAnalysisException {
        try {
            // Create a new record symbol table for the record declaration
            RecordSymbolTable recordTable = new RecordSymbolTable();
            
            // Define all fields (types can be used as-is since RecordType equality is name-based)
            for (Map.Entry<String, Type> field : astRecord.fields().entrySet()) {
                Type fieldType = field.getValue();
                recordTable.defineField(fieldType, field.getKey());
            }
            
            // Define all methods (signatures only, bodies analyzed later)
            for (AstFunctionDeclaration methodDecl : astRecord.methods()) {
                List<Type> paramTypes = methodDecl.parameters().stream()
                    .map(p -> p.type())
                    .toList();
                ReturnType returnType = methodDecl.type();
                recordTable.defineMethod(returnType, methodDecl.name(), paramTypes);
            }
            
            // Register record in global symbol table
            table.defineRecord(astRecord.name(), recordTable);
        } catch (DuplicateDefinitionException e) {
            throw new SemanticAnalysisException("Record " + astRecord.name() + " is already defined: " + e.getMessage());
        }
    }

    /**
     * Analyzes record method bodies and returns them as IR declarations.
     * Similar to analyzeFunction, but for record methods (with 'this' as local 0).
     * @param astRecord The AST record declaration containing method bodies.
     * @param table The global symbol table.
     * @return List of IR function declarations for the record's methods.
     * @throws SemanticAnalysisException If there is a semantic analysis error.
     */
    private List<IrFunctionDeclaration> analyzeRecordMethodBodies(AstRecordDeclaration astRecord, GlobalSymbolTable table) throws SemanticAnalysisException {
        // Get the record symbol table
        RecordSymbolTable recordTable = table.resolveRecord(astRecord.name())
            .orElseThrow(() -> new SemanticAnalysisException("Record " + astRecord.name() + " not found in symbol table."));
        
        // Get RecordType for this record
        RecordType recordType = table.resolveRecordType(astRecord.name())
            .orElseThrow(() -> new SemanticAnalysisException("Record type " + astRecord.name() + " not found in symbol table."));
        
        List<IrFunctionDeclaration> methodBodies = new ArrayList<>();
        
        // Analyze each method body (similar to analyzeFunction)
        for (AstFunctionDeclaration methodDecl : astRecord.methods()) {
            // Get the method signature
            Method method = recordTable.resolveMethod(methodDecl.name())
                .orElseThrow(() -> new SemanticAnalysisException("Method " + methodDecl.name() + " not found in record " + astRecord.name()));
            
            // Create a local symbol table for the method
            LocalSymbolTable localTable = new LocalSymbolTable(table);
            
            // Define 'this' as local variable 0
            LocalVariable thisVar = localTable.defineVariable(recordType, "this");
            if (thisVar.index() != 0) {
                throw new SemanticAnalysisException("Internal error: 'this' should be at index 0");
            }
            
            // Build IR parameters by defining with local variables (starting at index 1)
            List<IrParameter> irParameters = new ArrayList<>();
            for (AstParameter param : methodDecl.parameters()) {
                Type paramType = param.type();
                LocalVariable localVar = localTable.defineVariable(paramType, param.name());
                IrParameter irParam = new IrParameter(localVar);
                irParameters.add(irParam);
            }
            
            // Analyze the method body (with Method context, not Function)
            IrBlock irBlock = analyzeBlock(methodDecl.body(), localTable, method);
            
            // Create the IR function declaration
            IrFunctionDeclaration irMethod = new IrFunctionDeclaration(
                methodDecl.type(),
                methodDecl.name(),
                irParameters,
                irBlock
            );
            
            methodBodies.add(irMethod);
        }
        
        return methodBodies;
    }

    /**
     * Analyzes an AST function declaration and generates an IR function declaration.
     * @param astFunction The AST function declaration to analyze.
     * @param table The global symbol table.
     * @return The generated IR function declaration.
     * @throws SemanticAnalysisException If there is a semantic analysis error.
     */
    public IrFunctionDeclaration analyzeFunction(AstFunctionDeclaration astFunction, GlobalSymbolTable table) throws SemanticAnalysisException {
        // Create a local symbol table
        LocalSymbolTable localTable = new LocalSymbolTable(table);

        // Build IR parameters by defining with local variables
        List<IrParameter> irParameters = new ArrayList<>();
        for (AstParameter param : astFunction.parameters()) {
            Type paramType = param.type();
            LocalVariable localVar = localTable.defineVariable(paramType, param.name());
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

    public IrBlock analyzeBlock(AstBlock body, SymbolTable table, Callable currentCallable) throws SemanticAnalysisException{
        SymbolTable blockTable = enterScope(table);
        List<IrStatement> irStatements = new ArrayList<>();
        for (AstStatement stmt : body.statements()) {
            List<IrStatement> analyzed = analyzeStatement(stmt, blockTable, currentCallable);
            irStatements.addAll(analyzed);
        }
        return new IrBlock(irStatements);
    }

    /**
     * Analyzes an AST statement and generates an IR statement.
     * @param astStatement The AST statement to analyze.
     * @param table The symbol table.
     * @param currentCallable The current function or method.
     * @return The generated IR statement (never null, may be empty list).
     * @throws SemanticAnalysisException If there is a semantic analysis error.
     */
    public List<IrStatement> analyzeStatement(AstStatement astStatement, SymbolTable table, Callable currentCallable) throws SemanticAnalysisException {
        // astStatement could be at either the local or global level
        // symbols variable or functions deal with optional case
        // currentCallable can be null if at global level
        if (astStatement == null) {
            // Skip null statements (can happen with empty for loop parts)
            return List.of();
        }
        switch(astStatement){
            case AstBlock block -> {
                return List.of(analyzeBlock(block, table, currentCallable));
            }
            case AstAssembly assembly -> {
                IrAssembly irAssembly = new IrAssembly(assembly.comment());
                return List.of(irAssembly);
            }
            case AstFunctionCall call -> {
                // In statement context, void functions are allowed
                Function func = table.resolveFunction(call.name()).orElseThrow(() -> new SemanticAnalysisException("Function not defined: " + call.name()));
                List<IrExpression> irArgs = analyzeArguments(call.args(), func.parameterTypes(), table, currentCallable, 
                    "Function " + func.name());
                
                Type irType = switch (func.returnType()) {
                    case Type t -> t;
                    case ReturnType.Void _ -> BuiltInType.VOID;  // Allow void functions as statements
                };
                IrFunctionCall irCall = new IrFunctionCall(irType, func.name(), irArgs);
                // Always return as statement; CodeGenerator will pop non-void results
                return List.of(irCall);
            }
            case AstMethodCall methodCall -> {
                // Method calls can be used as statements
                IrExpression irMethodCall = analyzeExpression(methodCall, table, currentCallable);
                // IrMethodCall implements both IrStatement and IrExpression
                return List.of((IrStatement) irMethodCall);
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
                        irArgs.add(analyzeExpression(exprArg, table, currentCallable));
                    } else if (arg instanceof AstString strArg) {
                        irArgs.add(new IrString(strArg.text()));
                    } else {
                        throw new SemanticAnalysisException("Unknown print argument oldType: " + arg.getClass().getSimpleName());
                    }
                }
                return List.of(new IrPrint(irArgs));
            }
            case AstReturn ret -> {
                // Validate that return is inside a function or method
                if (currentCallable == null) {
                    throw new SemanticAnalysisException("Return statement not inside a function or method");
                }
                
                ReturnType retType = currentCallable.returnType();
                
                // Handle void function/method returns
                if (ret.expr().isEmpty()) {
                    if (!(retType instanceof ReturnType.Void)) {
                        throw new SemanticAnalysisException("Missing return value in non-void " + currentCallable.name());
                    }
                    return List.of(new IrReturn(Optional.empty()));
                }
                
                // Handle non-void function/method returns
                IrExpression value = analyzeExpression(ret.expr().get(), table, currentCallable);
                Type valueType = value.type();
                
                if (retType instanceof ReturnType.Void) {
                    throw new SemanticAnalysisException("Cannot return a value from void " + currentCallable.name());
                }
                
                Type targetType = ((Type) retType);
                
                // Addressing targetType: if it's an ArrayType with RecordType base, use RecordType
                if (targetType instanceof ArrayType at && at.baseType() instanceof RecordType rt) {
                    GlobalSymbolTable globalTable = extractGlobalSymbolTable(table);
                    RecordType resolvedRt = globalTable.resolveRecordType(rt.name())
                        .orElse(rt); // Fallback to original if not found
                    if (resolvedRt != rt) {
                        targetType = new ArrayType(resolvedRt, at.dimensions());
                    }
                } else if (targetType instanceof RecordType rt) {
                    GlobalSymbolTable globalTable = extractGlobalSymbolTable(table);
                    RecordType resolvedRt = globalTable.resolveRecordType(rt.name())
                        .orElse(rt); // Fallback to original if not found
                    if (resolvedRt != rt) {
                        targetType = resolvedRt;
                    }
                }
                
                // Apply same widening/toString rules as assignment
                if (valueType == BuiltInType.INT && targetType == BuiltInType.DOUBLE) {
                    value = new IrWiden(value);
                } else if (targetType == BuiltInType.STRING && valueType != BuiltInType.STRING) {
                    value = new IrToString(value);
                } else if (!valueType.equals(targetType)) {
                    throw new SemanticAnalysisException("Type mismatch in return: cannot return " + valueType + " from " + currentCallable.name() + " expecting " + targetType);
                }
                
                return List.of(new IrReturn(Optional.of(value)));
            }
            case AstVariableAssignment assign -> {
                // Analyze the lvalue (can be variable, array subscript, or field access)
                IrExpression lvalue = analyzeExpression(assign.lvalue(), table, currentCallable);
                
                // Verify it's an lvalue (implements IrLvalue)
                if (!(lvalue instanceof IrLvalue)) {
                    throw new SemanticAnalysisException("Left-hand side of assignment must be an lvalue, found: " + lvalue.getClass().getSimpleName());
                }
                
                Type targetType = lvalue.type();
                IrExpression valueExpr;

                // Special handling: input() as RHS should adopt the target lvalue's type
                if (assign.expr() instanceof AstInput inputExpr) {
                    // Analyze prompt and coerce to string if needed
                    IrExpression promptExpr = analyzeExpression(inputExpr.prompt(), table, currentCallable);
                    if (promptExpr.type() != BuiltInType.STRING) {
                        promptExpr = new IrToString(promptExpr);
                    }
                    valueExpr = new IrInput(targetType, promptExpr);
                } else {
                    valueExpr = analyzeExpression(assign.expr(), table, currentCallable);
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
                else if (!valueType.equals(targetType)) {
                    throw new SemanticAnalysisException("Type mismatch: cannot assign " + valueType + " to " + targetType);
                }

                IrVariableAssignment varAssign = new IrVariableAssignment(lvalue, valueExpr);
                return List.of(varAssign);
            }
            case AstVariableDeclaration decl -> {
                // Types can be used as-is since RecordType equality is name-based
                Type declType = decl.type();
                
                // Check if we're in a local scope (function) or global scope
                Variable var;
                if (table instanceof LocalSymbolTable) {
                    // Inside a function or block scope, define a new local variable
                    try {
                        var = table.defineVariable(declType, decl.name());
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
                            var = table.defineVariable(declType, decl.name());
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
                        IrExpression promptExpr = analyzeExpression(inputExpr.prompt(), table, currentCallable);
                        if (promptExpr.type() != BuiltInType.STRING) {
                            promptExpr = new IrToString(promptExpr);
                        }
                        valueExpr = new IrInput(targetType, promptExpr);
                    } else {
                        valueExpr = analyzeExpression(decl.expr().get(), table, currentCallable);
                    }
                    Type valueType = valueExpr.type();
                    
                    // Widen int to double if needed
                    if (valueType == BuiltInType.INT && targetType == BuiltInType.DOUBLE) {
                        valueExpr = new IrWiden(valueExpr);
                    }
                    // Convert to string if needed
                    else if (targetType == BuiltInType.STRING && !valueType.equals(BuiltInType.STRING)) {
                        valueExpr = new IrToString(valueExpr);
                    }
                    // Error if types don't match and can't be widened/converted
                    else if (!valueType.equals(targetType)) {
                        throw new SemanticAnalysisException("Type mismatch: cannot assign " + valueType + " to " + targetType);
                    }
                    
                    irInit = Optional.of(valueExpr);
                } else {
                    irInit = Optional.empty();
                }
                return List.of(new IrVariableDeclaration(var, irInit));
            }
            
            case AstIf astIf -> {
                IrExpression condition = analyzeExpression(astIf.condition(), table, currentCallable);
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
                result.addAll(analyzeBlock(astIf.thenBody(), ifElseScope, currentCallable).statements());

                if (astIf.elseBody().isPresent()) {
                    result.add(new IrGoto(endLabel));
                    result.add(new IrBranchTarget(elseLabel));

                    // Use the same parent scope for else block
                    result.addAll(analyzeBlock(astIf.elseBody().get(), ifElseScope, currentCallable).statements());
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

                IrExpression condition = analyzeExpression(astWhile.condition(), whileScope, currentCallable);
                if (condition.type() != BuiltInType.BOOLEAN) {
                    throw new SemanticAnalysisException("Condition of while loop must be BOOLEAN, found " + condition.type());
                }
                condition = invertCondition(condition);

                result.add(new IrBranch(condition, endLabel));

                result.addAll(analyzeBlock(astWhile.body(), whileScope, currentCallable).statements());

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
                return analyzeStatement(desugaredFor, table, currentCallable);
            }
            case AstFor astFor -> {
                SymbolTable loopScope = enterScope(table);
                List<IrStatement> result = new ArrayList<>();

                if (astFor.init().isPresent()) {
                    List<IrStatement> initIr = analyzeStatement(astFor.init().get(), loopScope, currentCallable);
                    if (initIr != null) {
                        result.addAll(initIr);
                    }
                }

                String startLabel = newLabel("forStart");
                String endLabel = newLabel("forEnd");

                result.add(new IrBranchTarget(startLabel));

                if (astFor.condition().isPresent()) {
                    IrExpression condition = analyzeExpression(astFor.condition().get(), loopScope, currentCallable);
                    if (condition.type() != BuiltInType.BOOLEAN) {
                        throw new SemanticAnalysisException("Condition of for loop must be BOOLEAN, found " + condition.type());
                    }
                    condition = invertCondition(condition);
                    result.add(new IrBranch(condition, endLabel));
                }

                result.addAll(analyzeBlock(astFor.body(), loopScope, currentCallable).statements());

                if (astFor.update().isPresent()) {
                    List<IrStatement> updateIr = analyzeStatement(astFor.update().get(), loopScope, currentCallable);
                    if (updateIr != null) {
                        result.addAll(updateIr);
                    }
                }

                result.add(new IrGoto(startLabel));
                result.add(new IrBranchTarget(endLabel));

                return result;
            }
            case AstIncrement increment -> {
                IrIncrement irInc = analyzeIncrement(increment, table, currentCallable);
                return List.of(new IrExpressionStatement(irInc));
            }
            default -> throw new SemanticAnalysisException("Unknown statement oldType: " + astStatement.getClass().getSimpleName());
        }
    }

    /**
     * Analyzes an AST expression and generates an IR expression.
     * @param expr The AST expression to analyze.
     * @param table The symbol table.
     * @param currentCallable The current function or method.
     * @return The generated IR expression.
     * @throws SemanticAnalysisException If there is a semantic analysis error.
     */
    public IrExpression analyzeExpression(AstExpression expr, SymbolTable table, Callable currentCallable) throws SemanticAnalysisException{
        if (expr == null) {
            throw new SemanticAnalysisException("Cannot analyze null expression");
        }
        switch(expr){
            case AstBinary binary -> {
                IrExpression left = analyzeExpression(binary.left(), table, currentCallable);
                IrExpression right = analyzeExpression(binary.right(), table, currentCallable);
                
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
                        } else if (!left.type().equals(right.type())) {
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
                        } else if (!left.type().equals(right.type())) {
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
                        } else if (!left.type().equals(right.type())) {
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
                IrExpression inner = analyzeExpression(unary.expr(), table, currentCallable);
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
                // Check if we're in a record method context and this might be a method call
                if (currentCallable instanceof Method) {
                    // Try to resolve as a method first
                    Optional<? extends Variable> thisVar = table.resolveVariable("this");
                    if (thisVar.isPresent() && thisVar.get().type() instanceof RecordType recordType) {
                        GlobalSymbolTable globalTable = extractGlobalSymbolTable(table);
                        RecordSymbolTable recordTable = globalTable.resolveRecord(recordType.name())
                            .orElse(null);
                        if (recordTable != null && recordTable.resolveMethod(call.name()).isPresent()) {
                            // This is a method call, not a function call
                            Method method = recordTable.resolveMethod(call.name()).get();
                            IrVariable thisIrVar = new IrVariable(thisVar.get());
                            List<IrExpression> irArgs = analyzeArguments(call.args(), method.parameterTypes(), table, currentCallable,
                                "Method " + call.name() + " in record " + recordType.name());
                            Type returnType = switch (method.returnType()) {
                                case Type t -> t;
                                case ReturnType.Void _ -> BuiltInType.VOID;
                            };
                            return new IrMethodCall(thisIrVar, call.name(), irArgs, returnType);
                        }
                    }
                }
                
                // Not a method call, try as function
                Function func = table.resolveFunction(call.name())
                        .orElseThrow(() -> new SemanticAnalysisException("Function not defined: " + call.name()));
                // Check if function returns a non-void oldType
                if (func.returnType() instanceof ReturnType.Void) {
                    throw new SemanticAnalysisException("Cannot use void function call as an expression: " + call.name());
                } else if (func.returnType() instanceof Type t && t != BuiltInType.VOID) {
                    List<IrExpression> irArgs = analyzeArguments(call.args(), func.parameterTypes(), table, currentCallable,
                        "Function " + func.name());
                    return new IrFunctionCall(t, func.name(), irArgs);
                } else {
                    throw new SemanticAnalysisException("Cannot use void function call as an expression: " + call.name());
                }
            }
            case AstCast cast -> {
                IrExpression inner = analyzeExpression(cast.expr(), table, currentCallable);
                Type sourceType = inner.type();
                Type targetType = cast.targetType();
                
                // If types already match, no conversion needed
                if (sourceType.equals(targetType)) {
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
                } else if (targetType instanceof ArrayType) {
                    // Array casting: only allow casting to same array type or widening conversions
                    // For now, only allow casting to the same array type
                    if (sourceType.equals(targetType)) {
                        return inner; // Already the right type
                    }
                    throw new SemanticAnalysisException("Cannot cast " + sourceType + " to array type " + targetType);
                } else if (targetType instanceof RecordType) {
                    // Record casting: only allow casting to same record type
                    if (sourceType.equals(targetType)) {
                        return inner; // Already the right type
                    }
                    throw new SemanticAnalysisException("Cannot cast " + sourceType + " to record type " + targetType);
                } else {
                    throw new SemanticAnalysisException("Cannot cast to type: " + targetType);
                }
            }
            case AstVariable variable -> {
                String varName = variable.variable();
                
                // Special case: "this" expression (allowed only inside record methods)
                if ("this".equals(varName)) {
                    // Try to resolve "this" as a variable first (it should be defined as a local variable in record methods)
                    Optional<? extends Variable> resolved = table.resolveVariable("this");
                    if (resolved.isPresent()) {
                        return new IrVariable(resolved.get());
                    }
                    
                    // If "this" is not defined, check if we're in a method context
                    // In record methods, "this" should be available as local variable 0
                    // For now, throw an error if "this" is not defined
                    throw new SemanticAnalysisException("'this' can only be used inside record methods");
                }
                
                // Normal variable resolution
                Optional<? extends Variable> resolved = table.resolveVariable(varName);
                if (resolved.isEmpty()) {
                    // If we're in a record method, check if this is a field name
                    if (currentCallable instanceof Method) {
                        // Check if "this" is defined (should be in record methods)
                        Optional<? extends Variable> thisVar = table.resolveVariable("this");
                        if (thisVar.isPresent() && thisVar.get().type() instanceof RecordType recordType) {
                            // Check if the variable name is a field of this record
                            GlobalSymbolTable globalTable = extractGlobalSymbolTable(table);
                            RecordSymbolTable recordTable = globalTable.resolveRecord(recordType.name())
                                .orElse(null);
                            if (recordTable != null && recordTable.resolveField(varName).isPresent()) {
                                // Convert to field access: this.fieldName
                                IrVariable thisIrVar = new IrVariable(thisVar.get());
                                return new IrFieldAccess(thisIrVar, varName, recordTable.resolveField(varName).get());
                            }
                        }
                    }
                    throw new SemanticAnalysisException("Variable not defined: " + varName);
                }
                Variable var = resolved.get();
                return new IrVariable(var);
            }
            case AstIncrement increment -> {
                return analyzeIncrement(increment, table, currentCallable);
            }
            case AstInput input -> {
                // Analyze prompt and coerce to string if needed
                IrExpression promptExpr = analyzeExpression(input.prompt(), table, currentCallable);
                if (promptExpr.type() != BuiltInType.STRING) {
                    promptExpr = new IrToString(promptExpr);
                }
                // Default to STRING for standalone input() expressions
                return new IrInput(BuiltInType.STRING, promptExpr);
            }
            case AstMethodCall methodCall -> {
                String methodName = methodCall.methodName();
                List<AstExpression> args = methodCall.arguments();
                
                // Special case: Math methods (check before analyzing record expression)
                if (methodCall.record() instanceof AstVariable var && "Math".equals(var.variable())) {
                    Optional<? extends Variable> mathVar = table.resolveVariable("Math");
                    if (mathVar.isEmpty()) {
                        // Built-in Math methods
                        return handleBuiltInMathMethod(methodName, args, table, currentCallable);
                    }
                    // If Math is a variable, fall through to normal handling
                }
                
                // Special case: System.exit() (check before analyzing record expression)
                if (methodCall.record() instanceof AstVariable var && "System".equals(var.variable())) {
                    Optional<? extends Variable> systemVar = table.resolveVariable("System");
                    if (systemVar.isEmpty() && "exit".equals(methodName)) {
                        // Built-in System.exit()
                        if (args.size() != 1) {
                            throw new SemanticAnalysisException("System.exit() expects 1 argument, got " + args.size());
                        }
                        IrExpression exitCode = analyzeExpression(args.get(0), table, currentCallable);
                        if (exitCode.type() != BuiltInType.INT) {
                            throw new SemanticAnalysisException("System.exit() expects int argument, got " + exitCode.type());
                        }
                        // Create a placeholder IrVariable for System (not resolved from symbol table)
                        IrVariable systemPlaceholder = new IrVariable(new GlobalVariable(BuiltInType.VOID, "System"));
                        // System.exit() is void - handle as statement, but also as expression
                        return new IrMethodCall(systemPlaceholder, methodName, List.of(exitCode), BuiltInType.VOID);
                    }
                }
                
                // Analyze the record expression
                IrExpression recordExpr = analyzeExpression(methodCall.record(), table, currentCallable);
                Type recordType = recordExpr.type();
                
                // Special case: String methods
                if (recordType == BuiltInType.STRING) {
                    return handleBuiltInStringMethod(methodName, args, recordExpr, table, currentCallable);
                }
                
                // Normal case: Record method call
                if (!(recordType instanceof RecordType)) {
                    throw new SemanticAnalysisException("Method call requires record type, found: " + recordType);
                }
                
                RecordType rt = (RecordType) recordType;
                GlobalSymbolTable globalTable = extractGlobalSymbolTable(table);
                
                // Look up record symbol table directly by name
                RecordSymbolTable recordTable = globalTable.resolveRecord(rt.name())
                    .orElseThrow(() -> new SemanticAnalysisException("Record symbol table not found: " + rt.name()));
                
                // Resolve method from record symbol table
                Method method = recordTable.resolveMethod(methodName)
                    .orElseThrow(() -> new SemanticAnalysisException("Method " + methodName + " not found in record " + rt.name()));
                
                // Analyze and type-check arguments
                List<IrExpression> irArgs = analyzeArguments(args, method.parameterTypes(), table, currentCallable,
                    "Method " + methodName + " in record " + rt.name());
                
                // Get return type
                Type returnType = switch (method.returnType()) {
                    case Type t -> t;
                    case ReturnType.Void _ -> BuiltInType.VOID;
                };
                
                return new IrMethodCall(recordExpr, methodName, irArgs, returnType);
            }
            case AstArrayConstruction arrayConstruction -> {
                // Analyze all dimension expressions (each must be int)
                List<IrExpression> dimensionExprs = new ArrayList<>();
                for (AstExpression dimExpr : arrayConstruction.dimensions()) {
                    IrExpression irDim = analyzeExpression(dimExpr, table, currentCallable);
                    if (irDim.type() != BuiltInType.INT) {
                        throw new SemanticAnalysisException("Array dimension must be int, found: " + irDim.type());
                    }
                    dimensionExprs.add(irDim);
                }
                
                // Verify baseType is valid (not void)
                ScalarType baseType = arrayConstruction.baseType();
                if (baseType == BuiltInType.VOID) {
                    throw new SemanticAnalysisException("Cannot create array of void type");
                }
                
                // Create IrArrayConstruction with baseType and dimensions
                return new IrArrayConstruction(baseType, dimensionExprs);
            }
            case AstArraySubscript arraySubscript -> {
                // Analyze the array expression
                IrExpression arrayExpr = analyzeExpression(arraySubscript.array(), table, currentCallable);
                Type arrayType = arrayExpr.type();

                // Verify it's an ArrayType
                if (!(arrayType instanceof ArrayType)) {
                    throw new SemanticAnalysisException("Array subscript requires array type, found: " + arrayType);
                }

                // Analyze the index expression
                IrExpression indexExpr = analyzeExpression(arraySubscript.index(), table, currentCallable);
                Type indexType = indexExpr.type();

                // Coerce index to INT where makes sense
                if (indexType != BuiltInType.INT) {
                    System.err.println("DEBUG: arrayExpr class = " + arraySubscript.array().getClass().getSimpleName());
                    if (arrayExpr instanceof IrVariable v && v.variable() instanceof GlobalVariable gv) {
                        System.err.println("DEBUG: array global name = " + gv.name());
                    }
                    System.err.println("DEBUG: array index AST = " + arraySubscript.index().getClass().getSimpleName());
                    if (arraySubscript.index() instanceof AstBinary b) {
                        System.err.println("DEBUG: index binary op = " + b.op());
                        System.err.println("DEBUG: left AST  = " + b.left().getClass().getSimpleName());
                        System.err.println("DEBUG: right AST = " + b.right().getClass().getSimpleName());
                    }
                    System.err.println("DEBUG: index IR type = " + indexType);
                    throw new SemanticAnalysisException("Array index must be int, found: " + indexType);
                }
                

                // Extract element type from ArrayType
                ArrayType at = (ArrayType) arrayType;
                Type elementType = at.componentType();

                // Return IrArraySubscript with element type (implements IrLvalue)
                return new IrArraySubscript(arrayExpr, indexExpr, elementType);
            }
            case AstFieldAccess fieldAccess -> {
                String fieldName = fieldAccess.fieldName();
                
                // Special case: Math.PI and Math.E (check before analyzing record expression)
                // Math is a built-in identifier, handle it specially before variable resolution
                if (fieldAccess.record() instanceof AstVariable var && "Math".equals(var.variable())) {
                    // Check if Math is defined as a variable first
                    Optional<? extends Variable> mathVar = table.resolveVariable("Math");
                    if (mathVar.isEmpty()) {
                        // Built-in Math - handle PI and E fields only
                        if ("PI".equals(fieldName) || "E".equals(fieldName)) {
                            // Create a placeholder IrVariable for built-in Math
                            // Codegen will recognize this and generate static field access
                            IrExpression mathPlaceholder = new IrVariable(
                                new GlobalVariable(BuiltInType.STRING, "Math"));
                            return new IrFieldAccess(mathPlaceholder, fieldName, BuiltInType.DOUBLE);
                        } else {
                            throw new SemanticAnalysisException("Built-in Math only supports PI and E fields, got: " + fieldName);
                        }
                    }
                    // If Math is defined as a variable, fall through to normal handling
                }
                
                // Analyze the record expression
                IrExpression recordExpr = analyzeExpression(fieldAccess.record(), table, currentCallable);
                Type recordType = recordExpr.type();
                
                // Special case: Array .length field
                if (recordType instanceof ArrayType && "length".equals(fieldName)) {
                    // Array.length returns int - use IrFieldAccess (codegen will handle the special case)
                    return new IrFieldAccess(recordExpr, fieldName, BuiltInType.INT);
                }
                
                // Normal case: Record field access
                if (!(recordType instanceof RecordType)) {
                    throw new SemanticAnalysisException("Field access requires record type or array type, found: " + recordType);
                }
                
                RecordType rt = (RecordType) recordType;
                // Get the global symbol table to resolve records
                GlobalSymbolTable globalTable = extractGlobalSymbolTable(table);
                
                // Look up record symbol table directly by name
                RecordSymbolTable recordTable = globalTable.resolveRecord(rt.name())
                    .orElseThrow(() -> new SemanticAnalysisException("Record symbol table not found: " + rt.name()));
                
                // Look up field in record symbol table
                Type fieldType = recordTable.resolveField(fieldName)
                    .orElseThrow(() -> new SemanticAnalysisException("Field " + fieldName + " not found in record " + rt.name()));
                
                return new IrFieldAccess(recordExpr, fieldName, fieldType);
            }
            case AstRecordConstruction recordConstruction -> {
                // Resolve record name via GlobalSymbolTable
                GlobalSymbolTable globalTable = extractGlobalSymbolTable(table);
                String recordName = recordConstruction.recordName();
                
                RecordSymbolTable recordTable = globalTable.resolveRecord(recordName)
                    .orElseThrow(() -> new SemanticAnalysisException("Record type not found: " + recordName));
                
                // Get field types in order
                Map<String, Type> fields = recordTable.getFields();
                List<String> fieldNames = new ArrayList<>(fields.keySet());
                
                // Verify argument count matches field count
                List<AstExpression> args = recordConstruction.arguments();
                if (args.size() != fields.size()) {
                    throw new SemanticAnalysisException("Record " + recordName + " expects " + fields.size() + 
                        " arguments but got " + args.size());
                }
                
                // Analyze each argument expression and type check
                // Build list of field types in order
                List<Type> fieldTypes = new ArrayList<>();
                for (String fieldName : fieldNames) {
                    fieldTypes.add(fields.get(fieldName));
                }
                
                List<IrExpression> irArgs = analyzeArguments(args, fieldTypes, table, currentCallable,
                    "Record " + recordName + " construction");
                
                // Use RecordType from GlobalSymbolTable
                RecordType recordType = globalTable.resolveRecordType(recordName)
                    .orElseThrow(() -> new SemanticAnalysisException("Record type " + recordName + " not found in symbol table."));
                
                return new IrRecordConstruction(recordName, irArgs, recordType);
            }
            default -> throw new SemanticAnalysisException("Unknown expression oldType: " + expr.getClass().getSimpleName());
        }
    }

    /**
     * Helper method to analyze and type-check a list of arguments against parameter types.
     * Applies widening (int -> double) and conversion (any -> string) rules as needed.
     * @param args The AST expressions to analyze.
     * @param paramTypes The expected parameter types.
     * @param table The symbol table.
     * @param currentCallable The current function or method.
     * @param contextName Name for error messages (e.g., "function call", "method call").
     * @return List of analyzed and type-checked IR expressions.
     * @throws SemanticAnalysisException If argument count doesn't match or types are incompatible.
     */
    private List<IrExpression> analyzeArguments(
            List<AstExpression> args,
            List<Type> paramTypes,
            SymbolTable table,
            Callable currentCallable,
            String contextName) throws SemanticAnalysisException {
        // Validate argument count
        if (args.size() != paramTypes.size()) {
            throw new SemanticAnalysisException(
                contextName + " expects " + paramTypes.size() +
                " arguments but got " + args.size());
        }
        
        List<IrExpression> irArgs = new ArrayList<>();
        for (int i = 0; i < args.size(); i++) {
            IrExpression arg = analyzeExpression(args.get(i), table, currentCallable);
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
            else if (!argType.equals(paramType)) {
                throw new SemanticAnalysisException("Type mismatch in " + contextName + ": argument " + (i+1) + 
                    " has type " + argType + " but expects " + paramType);
            }
            
            irArgs.add(arg);
        }
        return irArgs;
    }

    /**
     * Analyzes an increment/decrement expression.
     * @param increment The increment AST node.
     * @param table The symbol table.
     * @param currentCallable The current function or method.
     * @return The generated IR increment expression.
     * @throws SemanticAnalysisException If there is a semantic analysis error.
     */
    private IrIncrement analyzeIncrement(AstIncrement increment,
                                         SymbolTable table,
                                         Callable currentCallable) throws SemanticAnalysisException {
        // Analyze the lvalue (can be variable, array subscript, or field access)
        IrExpression lvalue = analyzeExpression(increment.lvalue(), table, currentCallable);
        
        // If the lvalue is itself an increment, extract its underlying lvalue
        // This handles cases like (numbers[x++])++ where the parentheses cause
        // the inner expression to be treated as an increment
        if (lvalue instanceof IrIncrement innerInc) {
            lvalue = innerInc.lvalue();
        }
        
        // Verify it's an lvalue
        if (!(lvalue instanceof IrLvalue)) {
            throw new SemanticAnalysisException("Increment/decrement requires an lvalue, found: " 
                + lvalue.getClass().getSimpleName());
        }
        
        // Lvalue type must be INT or DOUBLE
        Type lvalueType = lvalue.type();
        if (lvalueType != BuiltInType.INT && lvalueType != BuiltInType.DOUBLE) {
            throw new SemanticAnalysisException("Increment/decrement only works on int and double lvalues, got: " + lvalueType);
        }
        
        return new IrIncrement(increment.op(), (IrLvalue) lvalue, increment.isPrefix());
    }

    /**
     * Helper method to invert a condition expression.
     * @param condition The condition expression to invert.
     * @return The inverted condition expression.
     * @throws SemanticAnalysisException If there is a semantic analysis error.
     */
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

    /**
     * Helper method to handle built-in Math methods.
     * @param methodName The name of the Math method.
     * @param args The arguments to the Math method.
     * @param table The symbol table.
     * @param currentCallable The current function or method.
     * @return The generated IR expression.
     * @throws SemanticAnalysisException If there is a semantic analysis error.
     */
    private IrExpression handleBuiltInMathMethod(String methodName, List<AstExpression> args, 
                                                 SymbolTable table, Callable currentCallable) throws SemanticAnalysisException {
        // Create placeholder for Math
        IrExpression mathPlaceholder = new IrVariable(new GlobalVariable(BuiltInType.STRING, "Math"));
        
        List<IrExpression> irArgs = new ArrayList<>();
        Type returnType;
        
        switch (methodName) {
            case "sqrt", "pow", "exp", "log", "sin", "cos", "tan" -> {
                // All these methods take double and return double
                returnType = BuiltInType.DOUBLE;
                
                int expectedArgs = "pow".equals(methodName) ? 2 : 1;
                if (args.size() != expectedArgs) {
                    throw new SemanticAnalysisException("Math." + methodName + "() expects " + expectedArgs + 
                        " argument(s), got " + args.size());
                }
                
                for (AstExpression arg : args) {
                    IrExpression irArg = analyzeExpression(arg, table, currentCallable);
                    // Convert int to double if needed
                    if (irArg.type() == BuiltInType.INT) {
                        irArg = new IrWiden(irArg);
                    } else if (irArg.type() != BuiltInType.DOUBLE) {
                        throw new SemanticAnalysisException("Math." + methodName + "() expects double argument(s), got " + irArg.type());
                    }
                    irArgs.add(irArg);
                }
            }
            default -> throw new SemanticAnalysisException("Unknown Math method: " + methodName);
        }
        
        return new IrMethodCall(mathPlaceholder, methodName, irArgs, returnType);
    }

    /**
     * Helper method to handle built-in String methods.
     * @param methodName The name of the String method.
     * @param args The arguments to the String method.
     * @param stringExpr The string expression.
     * @param table The symbol table.
     * @param currentCallable The current function or method.
     * @return The generated IR expression.
     * @throws SemanticAnalysisException If there is a semantic analysis error.
     */
    private IrExpression handleBuiltInStringMethod(String methodName, List<AstExpression> args,
                                                   IrExpression stringExpr, SymbolTable table, Callable currentCallable) throws SemanticAnalysisException {
        List<IrExpression> irArgs = new ArrayList<>();
        Type returnType;
        
        switch (methodName) {
            case "length" -> {
                returnType = BuiltInType.INT;
                if (args.size() != 0) {
                    throw new SemanticAnalysisException("String.length() expects 0 arguments, got " + args.size());
                }
            }
            case "indexOf" -> {
                returnType = BuiltInType.INT;
                if (args.size() != 1) {
                    throw new SemanticAnalysisException("String.indexOf() expects 1 argument, got " + args.size());
                }
                IrExpression arg = analyzeExpression(args.get(0), table, currentCallable);
                if (arg.type() != BuiltInType.STRING) {
                    throw new SemanticAnalysisException("String.indexOf() expects string argument, got " + arg.type());
                }
                irArgs.add(arg);
            }
            case "substring" -> {
                returnType = BuiltInType.STRING;
                if (args.size() != 2) {
                    throw new SemanticAnalysisException("String.substring() expects 2 arguments, got " + args.size());
                }
                IrExpression arg1 = analyzeExpression(args.get(0), table, currentCallable);
                IrExpression arg2 = analyzeExpression(args.get(1), table, currentCallable);
                if (arg1.type() != BuiltInType.INT) {
                    throw new SemanticAnalysisException("String.substring() expects int for first argument, got " + arg1.type());
                }
                if (arg2.type() != BuiltInType.INT) {
                    throw new SemanticAnalysisException("String.substring() expects int for second argument, got " + arg2.type());
                }
                irArgs.add(arg1);
                irArgs.add(arg2);
            }
            default -> throw new SemanticAnalysisException("Unknown String method: " + methodName);
        }
        
        return new IrMethodCall(stringExpr, methodName, irArgs, returnType);
    }

}
