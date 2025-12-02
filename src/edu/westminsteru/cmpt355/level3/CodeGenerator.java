package edu.westminsteru.cmpt355.level3;

import edu.westminsteru.cmpt355.level3.ast.BinaryOp;
import edu.westminsteru.cmpt355.level3.ast.IncrementOp;
import edu.westminsteru.cmpt355.level3.ir.*;
import edu.westminsteru.cmpt355.level3.lang.*;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** Code generation stage of the compiler: given an AST, print jasm code */
public class CodeGenerator {

    /** The PrintWriter to print the output (jasm) code to */
    private final PrintWriter out;
    /** The IR of the program */
    private final IrProgram program;
    /** The global symbol table */
    private final GlobalSymbolTable globalTable;
    /** Name of the original source file */
    private final String sourceFilename;
    /** Name of the class being compiled */
    private final String outClassName;
    /** Counter for generating unique labels */
    private int labelCounter = 0;

    /** Builder class for CodeGenerator instances */
    public static class Builder {
    private IrProgram program;
        private GlobalSymbolTable globalTable;
        private String sourceFilename;
        private String outClassName;
        private PrintWriter out = new PrintWriter(System.out);

        private Builder() {}

        public Builder program(IrProgram program) {
            this.program = program;
            return this;
        }

        public Builder globalTable(GlobalSymbolTable globalTable) {
            this.globalTable = globalTable;
            return this;
        }

        public Builder sourceFilename(String sourceFilename) {
            this.sourceFilename = sourceFilename;
            return this;
        }

        public Builder outClassName(String outClassName) {
            this.outClassName = outClassName;
            return this;
        }

        public Builder output(PrintWriter out) {
            this.out = out;
            return this;
        }

        public CodeGenerator build() {
            if (program == null)
                throw new IllegalStateException("Program must be specified");
            if (globalTable == null)
                throw new IllegalStateException("GlobalSymbolTable must be specified");
            if (outClassName == null)
                throw new IllegalStateException("Output class name must be specified");
            if (out == null)
                throw new IllegalStateException("Output must be specified");
            return new CodeGenerator(program, globalTable, sourceFilename, outClassName, out);
        }
    }

    private CodeGenerator(
        IrProgram program,
        GlobalSymbolTable globalTable,
        String sourceFilename,
        String outClassName,
        PrintWriter out
    ) {
        this.program = program;
        this.globalTable = globalTable;
        this.sourceFilename = sourceFilename;
        this.outClassName = outClassName;
        this.out = out;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Generate a unique label name */
    private String newLabel() {
        return "L" + (labelCounter++);
    }

    // ========== Type-Dependent Helper Methods ==========

    /** Emit load instruction for a local variable */
    private void emitLoad(Type type, int index) {
        if (type instanceof BuiltInType builtIn) {
            switch (builtIn) {
                case INT, BOOLEAN -> out.printf("    iload %d%n", index);
                case DOUBLE -> out.printf("    dload %d%n", index);
                case STRING -> out.printf("    aload %d%n", index);
                default -> throw new IllegalArgumentException("Cannot load local variable of type: " + type);
            }
        } else {
            // ArrayType or RecordType - use aload
            out.printf("    aload %d%n", index);
        }
    }

    /** Emit store instruction for a local variable */
    private void emitStore(Type type, int index) {
        if (type instanceof BuiltInType builtIn) {
            switch (builtIn) {
                case INT, BOOLEAN -> out.printf("    istore %d%n", index);
                case DOUBLE -> out.printf("    dstore %d%n", index);
                case STRING -> out.printf("    astore %d%n", index);
                default -> throw new IllegalArgumentException("Cannot store local variable of type: " + type);
            }
        } else {
            // ArrayType or RecordType - use astore
            out.printf("    astore %d%n", index);
        }
    }

    /** Emit return instruction based on type */
    private void emitReturn(ReturnType returnType) {
        if (returnType instanceof BuiltInType builtIn) {
            switch (builtIn) {
                case INT, BOOLEAN -> out.println("    ireturn");
                case DOUBLE -> out.println("    dreturn");
                case STRING -> out.println("    areturn");
                case VOID -> out.println("    return");
            }
        } else if (returnType instanceof ArrayType || returnType instanceof RecordType) {
            out.println("    areturn");
        } else if (returnType == ReturnType.VOID) {
            out.println("    return");
        } else {
            throw new IllegalArgumentException("Cannot return type: " + returnType);
        }
    }

    /** Emit pop instruction based on oldType */
    private void emitPop(Type oldType) {
        if (oldType == BuiltInType.DOUBLE) {
            out.println("    pop2");
        } else if (oldType != BuiltInType.VOID) {
            out.println("    pop");
        }
        // Void expressions leave nothing on stack, so no pop needed
    }

    /** Emit integer constant */
    private void emitIntConst(int value) {
        out.printf("    ldc %d%n", value);
    }

    /** Emit double constant */
    private void emitDoubleConst(double value) {
        String s = Double.toString(value);
        if (!s.contains(".")) {
            s += ".0";
        }
        out.printf("    ldc2 %s%n", s);
    }

    /** Emit boolean constant */
    private void emitBool(boolean value) {
        out.println(value ? "    iconst_1" : "    iconst_0");
    }

    /** Emit string constant */
    private void emitString(String value) {
        out.printf("    ldc %s%n", value);
    }

    /** Emit println for a single argument of given type */
    private void emitPrintOne(Type type) {
        if (type instanceof BuiltInType builtIn) {
            switch (builtIn) {
                case INT -> out.println("    invokevirtual java/io/PrintStream println (I)V");
                case DOUBLE -> out.println("    invokevirtual java/io/PrintStream println (D)V");
                case BOOLEAN -> out.println("    invokevirtual java/io/PrintStream println (Z)V");
                case STRING -> out.println("    invokevirtual java/io/PrintStream println (Ljava/lang/String;)V");
                default -> throw new IllegalArgumentException("Cannot println type: " + type);
            }
        } else if (type instanceof ArrayType) {
            // Array - convert to string first, then print
            emitToString(type);
            out.println("    invokevirtual java/io/PrintStream println (Ljava/lang/String;)V");
        } else if (type instanceof RecordType) {
            // Record - convert to string first, then print
            emitToString(type);
            out.println("    invokevirtual java/io/PrintStream println (Ljava/lang/String;)V");
        } else {
            throw new IllegalArgumentException("Cannot println type: " + type);
        }
    }

    /** Emit toString conversion for given type (assumes value is already on stack) */
    private void emitToString(Type type) {
        if (type instanceof BuiltInType builtIn) {
            switch (builtIn) {
                case INT -> out.println("    invokestatic java/lang/Integer toString (I)Ljava/lang/String;");
                case DOUBLE -> out.println("    invokestatic java/lang/Double toString (D)Ljava/lang/String;");
                case BOOLEAN -> out.println("    invokestatic java/lang/Boolean toString (Z)Ljava/lang/String;");
                case STRING -> { /* Already a string, no conversion needed */ }
                default -> throw new IllegalArgumentException("Cannot convert " + type + " to string");
            }
        } else if (type instanceof ArrayType) {
            // Array - call appropriate arrayToString helper
            ArrayType at = (ArrayType) type;
            Type componentType = at.componentType();
            
            if (componentType instanceof BuiltInType builtIn) {
                switch (builtIn) {
                    case INT -> out.printf("    invokestatic %s arrayToStringInt ([I)Ljava/lang/String;%n", outClassName);
                    case DOUBLE -> out.printf("    invokestatic %s arrayToStringDouble ([D)Ljava/lang/String;%n", outClassName);
                    case BOOLEAN -> out.printf("    invokestatic %s arrayToStringBoolean ([Z)Ljava/lang/String;%n", outClassName);
                    case STRING -> out.printf("    invokestatic %s arrayToStringString ([Ljava/lang/String;)Ljava/lang/String;%n", outClassName);
                    default -> out.printf("    invokestatic %s arrayToStringObject ([Ljava/lang/Object;)Ljava/lang/String;%n", outClassName);
                }
            } else {
                // Array of arrays or records
                out.printf("    invokestatic %s arrayToStringObject ([Ljava/lang/Object;)Ljava/lang/String;%n", outClassName);
            }
        } else if (type instanceof RecordType) {
            // Record - call its toString() method
            out.println("    invokevirtual java/lang/Object toString ()Ljava/lang/String;");
        } else {
            throw new IllegalArgumentException("Cannot convert " + type + " to string");
        }
    }

    public void generateCode() {
        // your code goes here :)
        // (print jasm code to _out_)
        IrProgram program = this.program;

        // Track variables with their types for proper field declarations
        Map<String, Type> variableTypes = new HashMap<>();
        for (IrStatement stmt : program.statements()) {
            if (stmt instanceof IrVariableAssignment assign) {
                if (assign.lvalue() instanceof IrVariable var) {
                    variableTypes.put(var.variable().name(), var.variable().type());
                }
            } else if (stmt instanceof IrVariableDeclaration decl) {
                variableTypes.put(decl.variable().name(), decl.variable().type());
            }
        }

        // Class preamble
        out.printf(".class public %s\n", outClassName);
        if (sourceFilename != null)
            out.printf(".source %s\n", sourceFilename);
        out.println(".super java/lang/Object");
        
        // Generate field declarations for variables
        for (Map.Entry<String, Type> entry : variableTypes.entrySet()) {
            String varName = entry.getKey();
            Type varType = entry.getValue();
            out.printf(".field public static %s %s\n", varName, varType.getDescriptor());
        }
        
        // Generate global Scanner field for input operations
        out.println(".field private static scanner Ljava/util/Scanner;");
        out.println();

        // *** BEGIN MAIN METHOD ***
        out.println(".method public static main ([Ljava/lang/String;)V");
        out.println(".code");

        // Initialize scanner fresh each time main runs
        out.println("    new java/util/Scanner");
        out.println("    dup");
        out.println("    getstatic java/lang/System in Ljava/io/InputStream;");
        out.println("    invokespecial java/util/Scanner <init> (Ljava/io/InputStream;)V");
        out.printf("    putstatic %s scanner Ljava/util/Scanner;%n", outClassName);

        // Code for statements and expressions (NO function declarations here)
        for (IrStatement stmt : program.statements()) {
            generateStatement(stmt);
        }

        // Exit code
        out.println("    return");
        out.println(".end code");
        // *** END MAIN METHOD ***
        out.println();

        // Generate Scanner/System.in for input statements
        generateSystemIn();

        // Generate function methods
        for (IrFunctionDeclaration funcDecl : program.functions()) {
            generateFunctionMethod(funcDecl);
        }

        // Generate record classes
        generateRecordClasses();

        // Generate array printing helpers
        generateArrayPrintHelpers();

        out.flush();
    }

    private void generateStatement(IrStatement stmt){
        switch(stmt) {
            case IrFunctionCall call -> generateFunctionCall(call);
            case IrBlock block -> generateBlock(block);
            case IrPrint print -> generatePrint(print);
            case IrVariableDeclaration decl -> generateVariableDeclaration(decl);
            case IrVariableAssignment assign -> generateAssignment(assign);
            case IrAssembly asm -> out.printf("    # %s\n", asm.comment());
            case IrReturn ret -> generateReturn(ret);
            case IrBranch branch -> emitBranchIf(branch.condition(), branch.targetLabel());
            case IrBranchTarget target -> out.printf("%s:%n", target.label());
            case IrGoto gotoStmt -> out.printf("    goto %s%n", gotoStmt.targetLabel());
            case IrExpressionStatement exprStmt -> {
                generateExpression(exprStmt.expression());
                emitPop(exprStmt.expression().type());
            }
            case IrMethodCall methodCall -> {
                // Handle method calls as statements (e.g., System.exit())
                // Special case: System.exit()
                if (methodCall.record() instanceof IrVariable var && 
                    var.variable() instanceof GlobalVariable gv && 
                    "System".equals(gv.name()) &&
                    "exit".equals(methodCall.methodName())) {
                    // Generate argument expression
                    for (IrExpression arg : methodCall.args()) {
                        generateExpression(arg);
                    }
                    out.println("    invokestatic java/lang/System exit (I)V");
                    return;
                }
                // For other method calls, generate as expression and pop result
                generateExpression(methodCall);
                if (methodCall.returnType() != BuiltInType.VOID) {
                    emitPop(methodCall.returnType());
                }
            }
            default -> throw new IllegalArgumentException("Unknown statement: " + stmt);
        }
    }


    private void generateFunctionMethod(IrFunctionDeclaration decl) {
        // Generate method signature
        String paramTypes = decl.parameters().stream()
            .map(p -> p.variable().type().getDescriptor())
            .collect(Collectors.joining(""));
        String returnType = decl.type().getDescriptor();
        
        out.printf(".method public static %s (%s)%s\n", decl.name(), paramTypes, returnType);
        out.println(".code");

        // Generate function body
        generateBlock(decl.body());
        
        // Add a default return if the function is void and doesn't have an explicit return
        if (decl.type() == ReturnType.VOID) {
            out.println("    return");
        }
        
        out.println(".end code");
        out.println();
    }

    /** Emit function call and leave result on stack (for use in expressions) */
    private void emitCall(IrFunctionCall call) {
        // Generate arguments
        for (IrExpression arg : call.args()) {
            generateExpression(arg);
        }
        // Generate function call
        String paramTypes = call.args().stream()
            .map(a -> a.type().getDescriptor())
            .collect(Collectors.joining(""));
        String returnType = call.type().getDescriptor();
        out.printf("    invokestatic %s %s (%s)%s%n", outClassName, call.name(), paramTypes, returnType);
    }

    /** Generate function call as a statement (pops non-void results) */
    private void generateFunctionCall(IrFunctionCall call) {
        emitCall(call);
        emitPop(call.type());
    }

    private void generateBlock(IrBlock block) {
        for (IrStatement stmt : block.statements()) {
            generateStatement(stmt);
        }
    }

    private void generatePrint(IrPrint print) {
        if (print.args().isEmpty()) {
            // Print an empty line
            out.println("    getstatic java/lang/System out Ljava/io/PrintStream;");
            emitString("\"\"");
            out.println("    invokevirtual java/io/PrintStream println (Ljava/lang/String;)V");
            return;
        }

        // Single argument
        if (print.args().size() == 1) {
            IrExpression arg = print.args().get(0);
            out.println("    getstatic java/lang/System out Ljava/io/PrintStream;");
            generateExpression(arg);
            emitPrintOne(arg.type());
            return;
        }

        // Multiple arguments: concatenate strings
        boolean first = true;
        for (IrExpression arg : print.args()) {
            if (first) {
                // Start with the first argument
                if (arg instanceof IrString s) {
                    emitString(s.value());
                } else {
                    // If first arg is not a string, start with empty string and concat
                    emitString("\"\"");
                    generateExpression(arg);
                    emitToString(arg.type());
                    out.println("    invokevirtual java/lang/String concat (Ljava/lang/String;)Ljava/lang/String;");
                }
                first = false;
            } else { // Concatenate subsequent arguments
                if (arg instanceof IrString s) {
                    emitString(s.value());
                } else {
                    generateExpression(arg);
                    emitToString(arg.type());
                }
                out.println("    invokevirtual java/lang/String concat (Ljava/lang/String;)Ljava/lang/String;");
            }
        }
        // Load PrintStream, swap with string, and call println
        // Stack after string building: [String]
        out.println("    getstatic java/lang/System out Ljava/io/PrintStream;");
        // Stack now: [String, PrintStream]
        out.println("    swap");  // Swap to get [PrintStream, String]
        out.println("    invokevirtual java/io/PrintStream println (Ljava/lang/String;)V");
    }

    private void generateVariableDeclaration(IrVariableDeclaration decl) {
        if(decl.initializer().isPresent()) {
            generateExpression(decl.initializer().get());

            // Check if it's a local or global variable
            if (decl.variable() instanceof LocalVariable local) {
                // Use local variable store instruction
                emitStore(decl.variable().type(), local.index());
            } else {
                // Use static field access
                String descriptor = decl.variable().type().getDescriptor();
                out.printf("    putstatic %s %s %s%n", outClassName, decl.variable().name(), descriptor);
            }
        } else {
            // Initialize with default value based on type
            Type varType = decl.variable().type();
            if (varType == BuiltInType.VOID) {
                throw new IllegalArgumentException("Cannot declare a variable of type void");
            }
            
            // Emit default value
            if (varType instanceof BuiltInType builtIn) {
                switch (builtIn) {
                    case INT -> emitIntConst(0);
                    case BOOLEAN -> emitBool(false);
                    case DOUBLE -> emitDoubleConst(0.0);
                    case STRING -> emitString("\"\"");
                    default -> throw new IllegalArgumentException("Cannot declare a variable of type: " + varType);
                }
            } else {
                // ArrayType or RecordType - emit null
                out.println("    aconst_null");
            }
            
            // Store default value
            if (decl.variable() instanceof LocalVariable local) {
                emitStore(varType, local.index());
            } else {
                String descriptor = varType.getDescriptor();
                out.printf("    putstatic %s %s %s%n", outClassName, decl.variable().name(), descriptor);
            }
        }
    }

    private void generateAssignment(IrVariableAssignment assign) {
        // Handle different types of lvalues
        if (assign.lvalue() instanceof IrVariable var) {
            // For simple variable assignments, generate value first, then store
            generateExpression(assign.value());
            // Check if it's a local or global variable
            if (var.variable() instanceof LocalVariable local) {
                // Use local variable store instruction
                emitStore(var.variable().type(), local.index());
            } else {
                // Use static field access
                String descriptor = var.variable().type().getDescriptor();
                out.printf("    putstatic %s %s %s%n", outClassName, var.variable().name(), descriptor);
            }
        } else if (assign.lvalue() instanceof IrArraySubscript arraySubscript) {
            // Array subscript assignment
            // Need: array, index, value on stack for *astore
            // For input() with side effects, handle prompt separately, then generate in correct order
            
            if (assign.value() instanceof IrInput input) {
                // Special handling for input: generate prompt first, then array/index, then read
                // This avoids stack manipulation issues
                
                // Generate prompt (side effect: prints to console)
                out.println("    getstatic java/lang/System out Ljava/io/PrintStream;");
                generateExpression(input.prompt());
                out.println("    invokevirtual java/io/PrintStream print (Ljava/lang/String;)V");
                
                // Now generate array and index in correct order
                generateExpression(arraySubscript.array());
                generateExpression(arraySubscript.index());
                
                // Finally, read the value (leaves it on stack)
                Type inputType = input.type();
                if (inputType instanceof BuiltInType builtIn) {
                    switch (builtIn) {
                        case INT -> out.printf("    invokestatic %s readInt ()I\n", outClassName);
                        case DOUBLE -> out.printf("    invokestatic %s readDouble ()D\n", outClassName);
                        case STRING -> out.printf("    invokestatic %s readString ()Ljava/lang/String;\n", outClassName);
                        default -> throw new IllegalArgumentException("Cannot input type: " + inputType);
                    }
                } else {
                    throw new IllegalArgumentException("Cannot input type: " + inputType);
                }
                // Stack: [array, index, value] - correct order for *astore
            } else {
                // No side effects: generate in correct order directly
                generateExpression(arraySubscript.array());
                generateExpression(arraySubscript.index());
                generateExpression(assign.value());
                // Stack: [array, index, value] - correct order
            }
            
            Type elementType = arraySubscript.elementType();
            if (elementType instanceof BuiltInType builtIn) {
                switch (builtIn) {
                    case INT -> out.println("    iastore");
                    case DOUBLE -> out.println("    dastore");
                    case BOOLEAN -> out.println("    bastore");
                    case STRING -> out.println("    aastore");
                    default -> throw new IllegalArgumentException("Cannot store array element of type: " + elementType);
                }
            } else {
                // ArrayType or RecordType - use aastore
                out.println("    aastore");
            }
        } else if (assign.lvalue() instanceof IrFieldAccess fieldAccess) {
            // Field access assignment
            // Need: record, value on stack for putfield
            // Strategy: generate record first, then value
            
            // Generate record expression
            generateExpression(fieldAccess.record());
            // Stack: [record]
            
            // Generate value expression
            generateExpression(assign.value());
            // Stack: [record, value] - correct order for putfield
            
            Type recordType = fieldAccess.record().type();
            
            // Special case: Math.PI and Math.E (static fields) - not assignable, skip
            if (recordType instanceof RecordType rt) {
                String recordName = rt.name().replace('.', '/');
                String fieldDescriptor = fieldAccess.fieldType().getDescriptor();
                out.printf("    putfield %s %s %s%n", recordName, fieldAccess.fieldName(), fieldDescriptor);
            } else {
                throw new IllegalArgumentException("Field access assignment requires record type, found: " + recordType);
            }
        } else {
            throw new InternalCompilerException("Assignment to unknown lvalue type: " + assign.lvalue().getClass().getSimpleName());
        }
    }

    private void generateReturn(IrReturn ret) {
        if (ret.value().isPresent()) {
            generateExpression(ret.value().get());
            emitReturn(ret.value().get().type());
        } else {
            emitReturn(BuiltInType.VOID);
        }
    }

    private void generateSystemIn() {
        out.println();

        // int readInt(): use the global Scanner
        out.println(".method public static readInt ()I");
        out.println(".code");
        out.printf("    getstatic %s scanner Ljava/util/Scanner;%n", outClassName);
        out.println("    invokevirtual java/util/Scanner nextInt ()I");
        out.println("    ireturn");
        out.println(".end code");
        out.println();

        // double readDouble(): use the global Scanner
        out.println(".method public static readDouble ()D");
        out.println(".code");
        out.printf("    getstatic %s scanner Ljava/util/Scanner;%n", outClassName);
        out.println("    invokevirtual java/util/Scanner nextDouble ()D");
        out.println("    dreturn");
        out.println(".end code");
        out.println();

        // string readString(): use the global Scanner
        out.println(".method public static readString ()Ljava/lang/String;");
        out.println(".code");
        out.printf("    getstatic %s scanner Ljava/util/Scanner;%n", outClassName);
        out.println("    invokevirtual java/util/Scanner next ()Ljava/lang/String;");
        out.println("    areturn");
        out.println(".end code");

    }

    // Print out JVM assembly code to evaluate expr
    private void generateExpression(IrExpression expr) {
        switch (expr) {
            case IrInput input -> {
                // Print the prompt
                out.println("    getstatic java/lang/System out Ljava/io/PrintStream;");
                generateExpression(input.prompt());
                out.println("    invokevirtual java/io/PrintStream print (Ljava/lang/String;)V");

                // Read the value, leaving it on the stack as an expression result
                Type inputType = input.type();
                if (inputType instanceof BuiltInType builtIn) {
                    switch (builtIn) {
                        case INT -> out.printf("    invokestatic %s readInt ()I\n", outClassName);
                        case DOUBLE -> out.printf("    invokestatic %s readDouble ()D\n", outClassName);
                        case STRING -> out.printf("    invokestatic %s readString ()Ljava/lang/String;\n", outClassName);
                        default -> throw new IllegalArgumentException("Cannot input type: " + inputType);
                    }
                } else {
                    throw new IllegalArgumentException("Cannot input type: " + inputType);
                }
            }
            case IrInt i -> {
                emitIntConst(i.value());
            }
            case IrDouble d -> {
                emitDoubleConst(d.value());
            }
            case IrString s -> {
                emitString(s.value());
            }
            case IrBoolean b -> {
                emitBool(b.value());
            }
            case IrVariable v -> {
                // Check if it's a local or global variable
                if (v.variable() instanceof LocalVariable local) {
                    // Use local variable load instruction
                    emitLoad(v.variable().type(), local.index());
                } else {
                    // Use static field access
                    String descriptor = v.variable().type().getDescriptor();
                    out.printf("    getstatic %s %s %s%n", outClassName, v.variable().name(), descriptor);
                }
            }
            case IrUnary unary -> {
                generateExpression(unary.expr());
                switch (unary.op()) {
                    case NEGATE -> {
                        Type exprType = unary.expr().type();
                        if (exprType instanceof BuiltInType builtIn) {
                            switch (builtIn) {
                                case INT -> out.printf("    ineg\n");
                                case DOUBLE -> out.printf("    dneg\n");
                                default -> throw new IllegalArgumentException("Cannot negate " + exprType);
                            }
                        } else {
                            throw new IllegalArgumentException("Cannot negate " + exprType);
                        }
                    }
                    case NOT -> {
                        // Boolean NOT: flip 0↔1 using XOR with 1
                        // expr is already on stack (0 or 1)
                        out.println("    iconst_1");
                        out.println("    ixor");
                    }
                }
            }
            case IrBinary binary -> {
                generateExpression(binary.left());
                generateExpression(binary.right());
                switch (binary.op()) {
                    case ADD -> {
                        Type leftType = binary.left().type();
                        if (leftType instanceof BuiltInType builtIn) {
                            switch (builtIn) {
                                case INT -> out.println("    iadd");
                                case DOUBLE -> out.println("    dadd");
                                default -> throw new IllegalArgumentException("Cannot add " + leftType);
                            }
                        } else {
                            throw new IllegalArgumentException("Cannot add " + leftType);
                        }
                    }
                    case SUB -> {
                        Type leftType = binary.left().type();
                        if (leftType instanceof BuiltInType builtIn) {
                            switch (builtIn) {
                                case INT -> out.printf("    isub\n");
                                case DOUBLE -> out.printf("    dsub\n");
                                default -> throw new IllegalArgumentException("Cannot subtract " + leftType);
                            }
                        } else {
                            throw new IllegalArgumentException("Cannot subtract " + leftType);
                        }
                    }
                    case MUL -> {
                        Type leftType = binary.left().type();
                        if (leftType instanceof BuiltInType builtIn) {
                            switch (builtIn) {
                                case INT -> out.printf("    imul\n");
                                case DOUBLE -> out.printf("    dmul\n");
                                default -> throw new IllegalArgumentException("Cannot multiply " + leftType);
                            }
                        } else {
                            throw new IllegalArgumentException("Cannot multiply " + leftType);
                        }
                    }
                    case DIV -> {
                        Type leftType = binary.left().type();
                        if (leftType instanceof BuiltInType builtIn) {
                            switch (builtIn) {
                                case INT -> out.printf("    idiv\n");
                                case DOUBLE -> out.printf("    ddiv\n");
                                default -> throw new IllegalArgumentException("Cannot divide " + leftType);
                            }
                        } else {
                            throw new IllegalArgumentException("Cannot divide " + leftType);
                        }
                    }
                    case MOD -> {
                        Type leftType = binary.left().type();
                        if (leftType instanceof BuiltInType builtIn) {
                            switch (builtIn) {
                                case INT -> out.printf("    irem\n");
                                case DOUBLE -> out.printf("    drem\n");
                                default -> throw new IllegalArgumentException("Cannot modulo " + leftType);
                            }
                        } else {
                            throw new IllegalArgumentException("Cannot modulo " + leftType);
                        }
                    }
                    case CONCAT -> {
                        out.printf("    invokevirtual java/lang/String concat (Ljava/lang/String;)Ljava/lang/String;\n");
                    }
                    // Comparison Ops - all return boolean (0 or 1)
                    case EQ, NE, LT, LE, GT, GE -> {
                        generateComparisonAsBoolean(binary);
                    }
                    // Logical Ops - both return boolean (0 or 1)
                    case AND -> {
                        out.println("    iand");
                    }
                    case OR -> {
                        out.println("    ior");
                    }
                }
            }
            case IrWiden w -> {
                generateExpression(w.expr());
                out.printf("    i2d\n"); // Convert int to double
            }
            case IrToString t -> {
                generateExpression(t.expr());
                emitToString(t.expr().type());
            }
            case IrCast cast -> {
                generateCast(cast);
            }
            case IrFunctionCall call -> {
                // Use emitCall to leave result on stack (for expressions)
                emitCall(call);
            }
            case IrArrayConstruction arrayConstruction -> {
                List<IrExpression> dimensions = arrayConstruction.dimensions();
                ScalarType baseType = arrayConstruction.baseType();
                Type arrayType = arrayConstruction.type();
                
                if (dimensions.size() == 1) {
                    // Single dimension array - use newarray or anewarray
                    generateExpression(dimensions.get(0));
                    
                    if (baseType instanceof BuiltInType builtIn) {
                        switch (builtIn) {
                            case INT -> out.println("    newarray int");
                            case DOUBLE -> out.println("    newarray double");
                            case BOOLEAN -> out.println("    newarray boolean");
                            case STRING -> out.println("    anewarray Ljava/lang/String;");
                            default -> throw new IllegalArgumentException("Cannot create array of type: " + baseType);
                        }
                    } else if (baseType instanceof RecordType rt) {
                        // Array of records - use anewarray with record descriptor
                        String recordName = rt.name().replace('.', '/');
                        out.printf("    anewarray L%s;%n", recordName);
                    } else {
                        throw new IllegalArgumentException("Cannot create array of type: " + baseType);
                    }
                } else {
                    // Multi-dimensional array - use multianewarray
                    // Generate all dimension expressions (on stack in order)
                    for (IrExpression dim : dimensions) {
                        generateExpression(dim);
                    }
                    
                    // Get the descriptor for the full array type
                    String descriptor = arrayType.getDescriptor();
                    int dimCount = dimensions.size();
                    
                    out.printf("    multianewarray %s %d%n", descriptor, dimCount);
                }
            }
            case IrArraySubscript arraySubscript -> {
                // Generate array and index expressions
                generateExpression(arraySubscript.array());
                generateExpression(arraySubscript.index());
                
                // Load element based on element type
                Type elementType = arraySubscript.elementType();
                if (elementType instanceof BuiltInType builtIn) {
                    switch (builtIn) {
                        case INT -> out.println("    iaload");
                        case DOUBLE -> out.println("    daload");
                        case BOOLEAN -> out.println("    baload");
                        case STRING -> out.println("    aaload");
                        default -> throw new IllegalArgumentException("Cannot load array element of type: " + elementType);
                    }
                } else {
                    // ArrayType or RecordType - use aaload
                    out.println("    aaload");
                }
            }
            case IrRecordConstruction recordConstruction -> {
                // Create new record instance first
                String recordName = recordConstruction.name().replace('.', '/');
                out.printf("    new %s%n", recordName);
                
                // Duplicate reference for constructor call
                out.println("    dup");
                
                // Generate all argument expressions (after NEW and DUP)
                for (IrExpression arg : recordConstruction.args()) {
                    generateExpression(arg);
                }
                
                // Build constructor descriptor
                String paramTypes = recordConstruction.args().stream()
                    .map(a -> a.type().getDescriptor())
                    .collect(Collectors.joining(""));
                
                // Call constructor
                out.printf("    invokespecial %s <init> (%s)V%n", recordName, paramTypes);
                // Object reference remains on stack
            }
            case IrFieldAccess fieldAccess -> {
                Type recordType = fieldAccess.record().type();
                String fieldName = fieldAccess.fieldName();
                
                // Special case: Math.PI and Math.E (static fields) - check before generating expression
                if (fieldAccess.record() instanceof IrVariable var) {
                    String varName = var.variable().name();
                    if ("Math".equals(varName) && ("PI".equals(fieldName) || "E".equals(fieldName))) {
                        // Check if Math is actually defined as a variable in global scope
                        Optional<? extends Variable> mathVar = globalTable.resolveVariable("Math");
                        if (mathVar.isEmpty()) {
                            // Built-in Math - use static field access (no need to generate record expression)
                            out.printf("    getstatic java/lang/Math %s D%n", fieldName);
                            return;
                        }
                        // If Math is defined as a variable, fall through to normal field access
                    }
                }
                
                // Generate record expression
                generateExpression(fieldAccess.record());
                
                // Special case: array.length
                if (recordType instanceof ArrayType && "length".equals(fieldName)) {
                    out.println("    arraylength");
                    return;
                }
                
                // Normal case: record field access
                if (!(recordType instanceof RecordType)) {
                    throw new IllegalArgumentException("Field access requires record type, found: " + recordType);
                }
                
                RecordType rt = (RecordType) recordType;
                String recordName = rt.name().replace('.', '/');
                String fieldDescriptor = fieldAccess.fieldType().getDescriptor();
                
                out.printf("    getfield %s %s %s%n", recordName, fieldName, fieldDescriptor);
            }
            case IrMethodCall methodCall -> {
                String methodName = methodCall.methodName();
                
                // Special case: Math methods (static calls, no receiver needed)
                if (methodCall.record() instanceof IrVariable var && 
                    var.variable() instanceof GlobalVariable gv && 
                    "Math".equals(gv.name())) {
                    // Generate all argument expressions
                    for (IrExpression arg : methodCall.args()) {
                        generateExpression(arg);
                    }
                    String paramTypes = methodCall.args().stream()
                        .map(a -> a.type().getDescriptor())
                        .collect(Collectors.joining(""));
                    String returnType = methodCall.returnType().getDescriptor();
                    out.printf("    invokestatic java/lang/Math %s (%s)%s%n", methodName, paramTypes, returnType);
                    return;
                }
                
                // Special case: System.exit() (static call, no receiver needed)
                if (methodCall.record() instanceof IrVariable var && 
                    var.variable() instanceof GlobalVariable gv && 
                    "System".equals(gv.name()) &&
                    "exit".equals(methodName)) {
                    // Generate argument expression
                    for (IrExpression arg : methodCall.args()) {
                        generateExpression(arg);
                    }
                    out.println("    invokestatic java/lang/System exit (I)V");
                    return;
                }
                
                // Generate record expression (receiver)
                generateExpression(methodCall.record());
                
                // Generate all argument expressions
                for (IrExpression arg : methodCall.args()) {
                    generateExpression(arg);
                }
                
                Type recordType = methodCall.record().type();
                
                // Special case: String methods
                if (recordType == BuiltInType.STRING) {
                    String paramTypes = methodCall.args().stream()
                        .map(a -> a.type().getDescriptor())
                        .collect(Collectors.joining(""));
                    String returnType = methodCall.returnType().getDescriptor();
                    out.printf("    invokevirtual java/lang/String %s (%s)%s%n", methodName, paramTypes, returnType);
                    return;
                }
                
                // Normal case: record method call
                if (!(recordType instanceof RecordType)) {
                    throw new IllegalArgumentException("Method call requires record type, found: " + recordType);
                }
                
                RecordType rt = (RecordType) recordType;
                String recordName = rt.name().replace('.', '/');
                String paramTypes = methodCall.args().stream()
                    .map(a -> a.type().getDescriptor())
                    .collect(Collectors.joining(""));
                String returnType = methodCall.returnType().getDescriptor();
                
                out.printf("    invokevirtual %s %s (%s)%s%n", recordName, methodName, paramTypes, returnType);
            }
            case IrIncrement inc -> {
                // Handle different types of lvalues
                if (inc.lvalue() instanceof IrVariable var) {
                    Type varType = var.variable().type();
                    boolean isPrefix = inc.isPrefix();
                    
                    // Load variable
                    if (var.variable() instanceof LocalVariable local) {
                    int index = local.index();
                    if (varType instanceof BuiltInType builtIn) {
                        switch (builtIn) {
                            case INT -> out.printf("    iload %d\n", index);
                            case DOUBLE -> out.printf("    dload %d\n", index);
                            default -> throw new IllegalArgumentException("Cannot increment " + varType);
                        }
                    } else {
                        throw new IllegalArgumentException("Cannot increment " + varType);
                    }
                    } else {
                        String descriptor = var.variable().type().getDescriptor();
                        out.printf("    getstatic %s %s %s\n", outClassName, var.variable().name(), descriptor);
                    }
                
                if (isPrefix) {
                    // Prefix: increment first, then use new value
                    if (inc.op() == IncrementOp.INCREMENT) {
                        if (varType == BuiltInType.INT) {
                            out.println("    iconst_1");
                            out.println("    iadd");
                        } else if (varType == BuiltInType.DOUBLE) {
                            out.println("    dconst_1");
                            out.println("    dadd");
                        }
                    } else { // DECREMENT
                        if (varType == BuiltInType.INT) {
                            out.println("    iconst_1");
                            out.println("    isub");
                        } else if (varType == BuiltInType.DOUBLE) {
                            out.println("    dconst_1");
                            out.println("    dsub");
                        }
                    }
                    // Store back and leave incremented value on stack
                    if (var.variable() instanceof LocalVariable local) {
                        int index = local.index();
                        if (varType instanceof BuiltInType builtIn) {
                            switch (builtIn) {
                                case INT -> out.printf("    dup\n    istore %d\n", index);
                                case DOUBLE -> out.printf("    dup2\n    dstore %d\n", index);
                                default -> throw new IllegalArgumentException("Cannot increment " + varType);
                            }
                        } else {
                            throw new IllegalArgumentException("Cannot increment " + varType);
                        }
                    } else {
                        String descriptor = var.variable().type().getDescriptor();
                        out.println("    dup" + (varType == BuiltInType.DOUBLE ? "2" : ""));
                        out.printf("    putstatic %s %s %s\n", outClassName, var.variable().name(), descriptor);
                        // Incremented value remains on stack from dup
                    }
                } else {
                    // Postfix: use old value, then increment
                    // Duplicate the original value first
                    if (varType == BuiltInType.INT) {
                        out.println("    dup");
                    } else if (varType == BuiltInType.DOUBLE) {
                        out.println("    dup2");
                    }
                    // Now increment
                    if (inc.op() == IncrementOp.INCREMENT) {
                        if (varType == BuiltInType.INT) {
                            out.println("    iconst_1");
                            out.println("    iadd");
                        } else if (varType == BuiltInType.DOUBLE) {
                            out.println("    dconst_1");
                            out.println("    dadd");
                        }
                    } else { // DECREMENT
                        if (varType == BuiltInType.INT) {
                            out.println("    iconst_1");
                            out.println("    isub");
                        } else if (varType == BuiltInType.DOUBLE) {
                            out.println("    dconst_1");
                            out.println("    dsub");
                        }
                    }
                    // Store incremented value back
                    if (var.variable() instanceof LocalVariable local) {
                        int index = local.index();
                        if (varType instanceof BuiltInType builtIn) {
                            switch (builtIn) {
                                case INT -> out.printf("    istore %d\n", index);
                                case DOUBLE -> out.printf("    dstore %d\n", index);
                                default -> throw new IllegalArgumentException("Cannot increment " + varType);
                            }
                        } else {
                            throw new IllegalArgumentException("Cannot increment " + varType);
                        }
                    } else {
                        String descriptor = var.variable().type().getDescriptor();
                        out.printf("    putstatic %s %s %s\n", outClassName, var.variable().name(), descriptor);
                    }
                    // Original value is still on stack (from dup before increment)
                }
                } else if (inc.lvalue() instanceof IrArraySubscript arraySubscript) {
                    // Array subscript increment
                    Type elementType = arraySubscript.elementType();
                    boolean isPrefix = inc.isPrefix();
                    
                    if (elementType != BuiltInType.INT && elementType != BuiltInType.DOUBLE) {
                        throw new IllegalArgumentException("Increment only works on int and double array elements");
                    }
                    
                    // Load array and index
                    generateExpression(arraySubscript.array());
                    generateExpression(arraySubscript.index());
                    // Stack: [array, index]
                    
                    // Duplicate array and index before loading element
                    out.println("    dup2");
                    // Stack: [array, index, array, index]
                    
                    // Load current value (pops array and index from top, pushes value)
                    if (elementType == BuiltInType.INT) {
                        out.println("    iaload");
                    } else {
                        out.println("    daload");
                    }
                    // Stack: [array, index, value]
                    
                    if (isPrefix) {
                        // Prefix: increment first, then use new value
                        if (inc.op() == IncrementOp.INCREMENT) {
                            if (elementType == BuiltInType.INT) {
                                out.println("    iconst_1");
                                out.println("    iadd");
                            } else {
                                out.println("    dconst_1");
                                out.println("    dadd");
                            }
                        } else { // DECREMENT
                            if (elementType == BuiltInType.INT) {
                                out.println("    iconst_1");
                                out.println("    isub");
                            } else {
                                out.println("    dconst_1");
                                out.println("    dsub");
                            }
                        }
                        // Stack: [array, index, newValue]
                        // Duplicate new value for return, then store
                        if (elementType == BuiltInType.INT) {
                            out.println("    dup");
                            out.println("    iastore");
                        } else {
                            out.println("    dup2");
                            out.println("    dastore");
                        }
                        // Stack: [newValue] - prefix returns new value
                    } else {
                        // Postfix: use old value, then increment
                        // Duplicate old value first (for return)
                        if (elementType == BuiltInType.INT) {
                            out.println("    dup");
                        } else {
                            out.println("    dup2");
                        }
                        // Stack: [array, index, value, value] (oldValue duplicated)
                        
                        // Increment
                        if (inc.op() == IncrementOp.INCREMENT) {
                            if (elementType == BuiltInType.INT) {
                                out.println("    iconst_1");
                                out.println("    iadd");
                            } else {
                                out.println("    dconst_1");
                                out.println("    dadd");
                            }
                        } else { // DECREMENT
                            if (elementType == BuiltInType.INT) {
                                out.println("    iconst_1");
                                out.println("    isub");
                            } else {
                                out.println("    dconst_1");
                                out.println("    dsub");
                            }
                        }
                        // Stack: [array, index, oldValue, newValue]
                        
                        // Store new value: need array, index, newValue on top
                        // Stack after increment: [array, index, oldValue, newValue]
                        // We need to rearrange to [array, index, newValue] for store, keeping oldValue for return
                        if (elementType == BuiltInType.INT) {
                            // Stack: [array, index, oldValue, newValue]
                            // Swap oldValue and newValue to get newValue in position for store
                            out.println("    swap");      // Swap: [array, index, newValue, oldValue]
                            // Now iastore needs [array, index, newValue] on top
                            // iastore pops 3 values: array, index, newValue
                            // After iastore, oldValue will remain on stack: [oldValue]
                            out.println("    iastore");   // Store: pops array, index, newValue, leaves oldValue
                        } else {
                            // Stack: [array, index, oldValue, newValue]
                            // Swap oldValue and newValue
                            out.println("    swap2");     // Swap: [array, index, newValue, oldValue]
                            // dastore pops 3 2-slot values: array, index, newValue
                            // After dastore, oldValue will remain on stack: [oldValue]
                            out.println("    dastore");   // Store: pops array, index, newValue, leaves oldValue
                        }
                        // Stack: [oldValue] - postfix returns old value
                    }
                } else if (inc.lvalue() instanceof IrFieldAccess fieldAccess) {
                    // Field access increment
                    Type fieldType = fieldAccess.fieldType();
                    boolean isPrefix = inc.isPrefix();
                    
                    if (fieldType != BuiltInType.INT && fieldType != BuiltInType.DOUBLE) {
                        throw new IllegalArgumentException("Increment only works on int and double fields");
                    }
                    
                    // Load record and current field value
                    generateExpression(fieldAccess.record());
                    // Stack: [record]
                    
                    Type recordType = fieldAccess.record().type();
                    if (!(recordType instanceof RecordType)) {
                        throw new IllegalArgumentException("Field access increment requires record type");
                    }
                    
                    RecordType rt = (RecordType) recordType;
                    String recordName = rt.name().replace('.', '/');
                    String fieldDescriptor = fieldAccess.fieldType().getDescriptor();
                    
                    // Get current field value
                    out.printf("    dup%n"); // Duplicate record for later putfield
                    out.printf("    getfield %s %s %s%n", recordName, fieldAccess.fieldName(), fieldDescriptor);
                    // Stack: [record, value]
                    
                    if (isPrefix) {
                        // Prefix: increment first, then use new value
                        if (inc.op() == IncrementOp.INCREMENT) {
                            if (fieldType == BuiltInType.INT) {
                                out.println("    iconst_1");
                                out.println("    iadd");
                            } else {
                                out.println("    dconst_1");
                                out.println("    dadd");
                            }
                        } else { // DECREMENT
                            if (fieldType == BuiltInType.INT) {
                                out.println("    iconst_1");
                                out.println("    isub");
                            } else {
                                out.println("    dconst_1");
                                out.println("    dsub");
                            }
                        }
                        // Stack: [record, newValue]
                        // Duplicate new value for return, then store
                        if (fieldType == BuiltInType.INT) {
                            out.println("    dup_x1"); // Duplicate newValue below record
                            out.println("    swap");   // Swap to get: newValue, record, newValue
                            out.println("    pop");    // Remove duplicate
                            out.printf("    putfield %s %s %s%n", recordName, fieldAccess.fieldName(), fieldDescriptor);
                        } else {
                            out.println("    dup2_x1"); // Duplicate newValue below record
                            out.println("    pop2");     // Remove duplicate
                            out.printf("    putfield %s %s %s%n", recordName, fieldAccess.fieldName(), fieldDescriptor);
                            out.println("    dup2");     // Duplicate newValue for return
                        }
                        // Stack: [newValue] - prefix returns new value
                    } else {
                        // Postfix: use old value, then increment
                        // Duplicate old value first
                        if (fieldType == BuiltInType.INT) {
                            out.println("    dup");
                        } else {
                            out.println("    dup2");
                        }
                        // Stack: [record, value, value]
                        
                        // Increment
                        if (inc.op() == IncrementOp.INCREMENT) {
                            if (fieldType == BuiltInType.INT) {
                                out.println("    iconst_1");
                                out.println("    iadd");
                            } else {
                                out.println("    dconst_1");
                                out.println("    dadd");
                            }
                        } else { // DECREMENT
                            if (fieldType == BuiltInType.INT) {
                                out.println("    iconst_1");
                                out.println("    isub");
                            } else {
                                out.println("    dconst_1");
                                out.println("    dsub");
                            }
                        }
                        // Stack: [record, oldValue, newValue]
                        
                        // Store new value: swap to get record, newValue, then store
                        if (fieldType == BuiltInType.INT) {
                            out.println("    swap"); // Swap oldValue and newValue
                            out.println("    dup_x1"); // Duplicate newValue below oldValue
                            out.println("    swap"); // Swap back
                            out.printf("    putfield %s %s %s%n", recordName, fieldAccess.fieldName(), fieldDescriptor);
                        } else {
                            out.println("    dup2_x1"); // Duplicate newValue below oldValue
                            out.println("    pop2");    // Remove duplicate
                            out.printf("    putfield %s %s %s%n", recordName, fieldAccess.fieldName(), fieldDescriptor);
                        }
                        // Stack: [oldValue] - postfix returns old value
                    }
                } else {
                    throw new InternalCompilerException("Increment on unknown lvalue type: " + inc.lvalue().getClass().getSimpleName());
                }
            }
            default -> throw new IllegalArgumentException("Unknown expression: " + expr);
        }
    }

    /** Generate code for a type cast */
    private void generateCast(IrCast cast) {
        Type sourceType = cast.expr().type();
        Type targetType = cast.targetType();
        
        // Generate the source expression
        generateExpression(cast.expr());
        
        // Handle conversions
        if (sourceType.equals(targetType)) {
            // No conversion needed
            return;
        }
        
        if (targetType instanceof BuiltInType targetBuiltIn && sourceType instanceof BuiltInType sourceBuiltIn) {
            switch (targetBuiltIn) {
                case INT -> {
                    switch (sourceBuiltIn) {
                        case DOUBLE -> {
                            // Double to int: truncate (d2i)
                            out.println("    d2i");
                        }
                        case STRING -> {
                            // String to int: use Integer.parseInt
                            out.println("    invokestatic java/lang/Integer parseInt (Ljava/lang/String;)I");
                        }
                        case BOOLEAN -> {
                            // Boolean to int: already 0 or 1, no conversion needed
                            // (booleans are represented as ints)
                        }
                        default -> throw new IllegalArgumentException("Cannot cast " + sourceType + " to " + targetType);
                    }
                }
                case DOUBLE -> {
                    switch (sourceBuiltIn) {
                        case INT -> {
                            // Int to double: convert using i2d
                            out.println("    i2d");
                        }
                        case STRING -> {
                            // String to double: use Double.parseDouble
                            out.println("    invokestatic java/lang/Double parseDouble (Ljava/lang/String;)D");
                        }
                        case BOOLEAN -> {
                            // Boolean to double: convert 0/1 to 0.0/1.0
                            out.println("    i2d"); // Convert int (0 or 1) to double
                        }
                        default -> throw new IllegalArgumentException("Cannot cast " + sourceType + " to " + targetType);
                    }
                }
                case STRING -> {
                    // Any to string: convert using toString
                    emitToString(sourceType);
                }
                case BOOLEAN -> {
                    switch (sourceBuiltIn) {
                        case INT -> {
                        // Int to boolean: non-zero -> true, zero -> false
                        // Compare with 0, push 1 if != 0, else 0
                        String trueLabel = newLabel();
                        String endLabel = newLabel();
                        out.println("    dup"); // Duplicate the int value
                        out.println("    ifne " + trueLabel); // If != 0, go to true
                        out.println("    iconst_0"); // Push 0 (false)
                        out.println("    goto " + endLabel);
                        out.println(trueLabel + ":");
                        out.println("    iconst_1"); // Push 1 (true)
                        out.println(endLabel + ":");
                    }
                    case DOUBLE -> {
                        // Double to boolean: non-zero -> true, zero -> false
                        String trueLabel = newLabel();
                        String endLabel = newLabel();
                        out.println("    dup2"); // Duplicate the double value
                        out.println("    dconst_0"); // Push 0.0
                        out.println("    dcmpg"); // Compare: >0 if non-zero, ==0 if zero
                        out.println("    ifne " + trueLabel); // If != 0, go to true
                        out.println("    iconst_0"); // Push 0 (false)
                        out.println("    goto " + endLabel);
                        out.println(trueLabel + ":");
                        out.println("    iconst_1"); // Push 1 (true)
                        out.println(endLabel + ":");
                    }
                    case STRING -> {
                        // String to boolean: check if non-empty
                        // String is already on stack
                        String trueLabel = newLabel();
                        String endLabel = newLabel();
                        out.println("    invokevirtual java/lang/String length ()I");
                        out.println("    ifne " + trueLabel); // If length != 0, go to true
                        out.println("    iconst_0"); // Push 0 (false)
                        out.println("    goto " + endLabel);
                        out.println(trueLabel + ":");
                        out.println("    iconst_1"); // Push 1 (true)
                        out.println(endLabel + ":");
                    }
                        default -> throw new IllegalArgumentException("Cannot cast " + sourceType + " to " + targetType);
                    }
                }
                default -> throw new IllegalArgumentException("Cannot cast to type: " + targetType);
            }
        } else {
            // ArrayType or RecordType casting - for now, just pass through
            // TODO: Implement proper casting for arrays and records
            throw new IllegalArgumentException("Cannot cast " + sourceType + " to " + targetType);
        }
    }

    /** Get integer comparison opcode (branches if true) */
    private String intCmpTrue(BinaryOp op) {
        return switch (op) {
            case LT -> "if_icmplt";
            case LE -> "if_icmple";
            case GT -> "if_icmpgt";
            case GE -> "if_icmpge";
            case EQ -> "if_icmpeq";
            case NE -> "if_icmpne";
            default -> throw new IllegalStateException("Not a comparison operator: " + op);
        };
    }

    /** Get double comparison opcode (after dcmpg, branches if true) */
    private String dblCmpTrue(BinaryOp op) {
        return switch (op) {
            case LT -> "iflt";
            case LE -> "ifle";
            case GT -> "ifgt";
            case GE -> "ifge";
            case EQ -> "ifeq";
            case NE -> "ifne";
            default -> throw new IllegalStateException("Not a comparison operator: " + op);
        };
    }

    /**
     * Emit a single-target conditional branch for an already-boolean IR expression.
     * Stack effect:
     *   stack ──(evaluate cond)──> [ stack, 0|1 ]
     *   ifne targetLabel            // branch when condition is true; fall through otherwise
     */
    private void emitBranchIf(IrExpression cond, String targetLabel) {
        // All boolean IR expressions via generateExpression() put 0/1 on the stack
        generateExpression(cond);
        out.printf("    ifne %s%n", targetLabel);
    }

    /** Generate comparison as a boolean expression (0 or 1 on stack)
     *  Left & right expressions are already on the stack */
    private void generateComparisonAsBoolean(IrBinary binary) {
        String trueLabel = newLabel();
        String endLabel = newLabel();
        Type t = binary.left().type();
        
        // Generate comparison branch (left and right already on stack)
        if (t == BuiltInType.INT || t == BuiltInType.BOOLEAN) {
            out.printf("    %s %s%n", intCmpTrue(binary.op()), trueLabel);
        } else if (t == BuiltInType.DOUBLE) {
            out.println("    dcmpg");
            out.printf("    %s %s%n", dblCmpTrue(binary.op()), trueLabel);
        } else if (t == BuiltInType.STRING) {
            if (binary.op() == BinaryOp.EQ || binary.op() == BinaryOp.NE) {
                // Use equals() for == and !=
                out.println("    invokevirtual java/lang/String equals (Ljava/lang/Object;)Z");
                out.printf("    %s %s%n", (binary.op() == BinaryOp.EQ ? "ifne" : "ifeq"), trueLabel);
            } else {
                // Use compareTo() for <, >, <=, >=
                // compareTo returns: negative if left < right, 0 if equal, positive if left > right
                out.println("    invokevirtual java/lang/String compareTo (Ljava/lang/String;)I");
                // Int on stack: negative (<0), zero (==0), or positive (>0)
                switch (binary.op()) {
                    case LT -> out.printf("    iflt %s%n", trueLabel);
                    case LE -> out.printf("    ifle %s%n", trueLabel);
                    case GT -> out.printf("    ifgt %s%n", trueLabel);
                    case GE -> out.printf("    ifge %s%n", trueLabel);
                    default -> throw new IllegalArgumentException("Unsupported string comparison: " + binary.op());
                }
            }
        } else {
            throw new IllegalArgumentException("Unsupported comparison: " + t + " " + binary.op());
        }
        
        // falseLabel: push 0
        out.println("    iconst_0");
        out.printf("    goto %s%n", endLabel);
        
        // trueLabel: push 1
        out.printf("%s:%n", trueLabel);
        out.println("    iconst_1");
        out.printf("%s:%n", endLabel);
    }

    /**
     * Generates JASM code for all record classes.
     */
    private void generateRecordClasses() {
        for (String recordName : globalTable.getAllRecordNames()) {
            RecordSymbolTable recordTable = globalTable.resolveRecord(recordName)
                .orElseThrow(() -> new InternalCompilerException("Record " + recordName + " not found"));
            
            generateRecordClass(recordName, recordTable);
        }
    }

    /**
     * Generates JASM code for a single record class.
     */
    private void generateRecordClass(String recordName, RecordSymbolTable recordTable) {
        String jvmName = recordName.replace('.', '/');
        
        // Class declaration
        out.printf(".class public %s%n", jvmName);
        out.println(".super java/lang/Object");
        out.println();
        
        // Generate field declarations (public, non-static)
        Map<String, Type> fields = recordTable.getFields();
        List<String> fieldNames = new ArrayList<>(fields.keySet());
        for (String fieldName : fieldNames) {
            Type fieldType = fields.get(fieldName);
            out.printf(".field public %s %s%n", fieldName, fieldType.getDescriptor());
        }
        out.println();
        
        // Generate constructor
        generateRecordConstructor(recordName, jvmName, fieldNames, fields);
        
        // Generate toString() method
        generateRecordToString(recordName, jvmName, fieldNames, fields);
        
        // Generate user-defined methods
        // Get method IR bodies from IrProgram
        List<IrFunctionDeclaration> methodBodies = program.recordMethods().getOrDefault(recordName, List.of());
        Map<String, Method> methods = recordTable.getMethods();
        
        // Match method bodies with method signatures
        for (IrFunctionDeclaration methodBody : methodBodies) {
            Method method = methods.get(methodBody.name());
            if (method != null) {
                generateRecordMethod(recordName, jvmName, method, methodBody);
            }
        }
    }

    /**
     * Generates the constructor for a record class.
     */
    private void generateRecordConstructor(String recordName, String jvmName, 
                                          List<String> fieldNames, Map<String, Type> fields) {
        // Build constructor descriptor
        String paramTypes = fieldNames.stream()
            .map(fields::get)
            .map(Type::getDescriptor)
            .collect(Collectors.joining(""));
        
        out.printf(".method public <init> (%s)V%n", paramTypes);
        out.println(".code");
        
        // Call superclass constructor
        out.println("    aload_0");
        out.println("    invokespecial java/lang/Object <init> ()V");
        
        // Store each parameter to corresponding field
        int localIndex = 1; // local 0 is 'this'
        for (String fieldName : fieldNames) {
            Type fieldType = fields.get(fieldName);
            out.println("    aload_0"); // Load 'this'
            
            // Load parameter
            if (fieldType instanceof BuiltInType builtIn) {
                switch (builtIn) {
                    case INT, BOOLEAN -> out.printf("    iload %d%n", localIndex);
                    case DOUBLE -> out.printf("    dload %d%n", localIndex);
                    case STRING -> out.printf("    aload %d%n", localIndex);
                    default -> throw new IllegalArgumentException("Cannot load field parameter: " + fieldType);
                }
            } else {
                out.printf("    aload %d%n", localIndex);
            }
            
            // Store to field
            String fieldDescriptor = fieldType.getDescriptor();
            out.printf("    putfield %s %s %s%n", jvmName, fieldName, fieldDescriptor);
            
            localIndex += fieldType.size();
        }
        
        out.println("    return");
        out.println(".end code");
        out.println();
    }

    /**
     * Generates the toString() method for a record class.
     */
    private void generateRecordToString(String recordName, String jvmName,
                                       List<String> fieldNames, Map<String, Type> fields) {
        out.println(".method public toString ()Ljava/lang/String;");
        out.println(".code");
        
        // Start with record name and '['
        out.printf("    ldc \"%s[\"%n", recordName);
        
        boolean first = true;
        for (String fieldName : fieldNames) {
            if (!first) {
                // Add comma separator
                out.println("    ldc \",\"");
                out.println("    invokevirtual java/lang/String concat (Ljava/lang/String;)Ljava/lang/String;");
            }
            first = false;
            
            // Add field name and '='
            out.printf("    ldc \"%s=\"%n", fieldName);
            out.println("    invokevirtual java/lang/String concat (Ljava/lang/String;)Ljava/lang/String;");
            
            // Load 'this' and get field value
            out.println("    aload_0");
            Type fieldType = fields.get(fieldName);
            String fieldDescriptor = fieldType.getDescriptor();
            out.printf("    getfield %s %s %s%n", jvmName, fieldName, fieldDescriptor);
            
            // Convert field value to string
            emitToString(fieldType);
            
            // Concatenate
            out.println("    invokevirtual java/lang/String concat (Ljava/lang/String;)Ljava/lang/String;");
        }
        
        // Add closing ']'
        out.println("    ldc \"]\"");
        out.println("    invokevirtual java/lang/String concat (Ljava/lang/String;)Ljava/lang/String;");
        
        out.println("    areturn");
        out.println(".end code");
        out.println();
    }

    /**
     * Generates a user-defined method for a record class.
     */
    private void generateRecordMethod(String recordName, String jvmName, 
                                     Method method, IrFunctionDeclaration methodBody) {
        // Build method descriptor
        String paramTypes = method.parameterTypes().stream()
            .map(Type::getDescriptor)
            .collect(Collectors.joining(""));
        String returnType = method.returnType().getDescriptor();
        
        out.printf(".method public %s (%s)%s%n", method.name(), paramTypes, returnType);
        out.println(".code");
        
        // Generate method body
        // Note: 'this' is already local 0, parameters start at local 1
        generateBlock(methodBody.body());
        
        // Add default return if method is void and doesn't have explicit return
        if (method.returnType() == ReturnType.VOID) {
            out.println("    return");
        }
        
        out.println(".end code");
        out.println();
    }

    /**
     * Generates static helper methods for converting arrays to strings.
     */
    private void generateArrayPrintHelpers() {
        // arrayToStringInt([I)Ljava/lang/String;
        generateArrayToStringHelper("Int", "[I", "iaload", "java/lang/Integer", "I");
        
        // arrayToStringDouble([D)Ljava/lang/String;
        generateArrayToStringHelper("Double", "[D", "daload", "java/lang/Double", "D");
        
        // arrayToStringBoolean([Z)Ljava/lang/String;
        generateArrayToStringHelper("Boolean", "[Z", "baload", "java/lang/Boolean", "Z");
        
        // arrayToStringString([Ljava/lang/String;)Ljava/lang/String;
        generateArrayToStringHelper("String", "[Ljava/lang/String;", "aaload", null, null);
        
        // arrayToStringObject([Ljava/lang/Object;)Ljava/lang/String;
        generateArrayToStringHelper("Object", "[Ljava/lang/Object;", "aaload", "java/lang/Object", "Ljava/lang/Object;");
    }

    /**
     * Generates a helper method to convert an array to a string.
     */
    private void generateArrayToStringHelper(String name, String arrayDesc, String loadOp, 
                                            String toStringClass, String elementDesc) {
        out.printf(".method public static arrayToString%s (%s)Ljava/lang/String;%n", name, arrayDesc);
        out.println(".code");
        out.println("    ldc \"[\"");
        out.println("    astore_1"); // Store result string in local 1
        out.println("    aload_0"); // Load array
        out.println("    arraylength");
        out.println("    istore_2"); // Store length in local 2
        out.println("    iconst_0");
        out.println("    istore_3"); // Store index in local 3
        String loopStart = newLabel();
        String loopEnd = newLabel();
        out.printf("%s:%n", loopStart);
        out.println("    iload_3");
        out.println("    iload_2");
        out.printf("    if_icmpge %s%n", loopEnd);
        out.println("    iload_3");
        out.println("    ifeq skip_comma_" + loopStart);
        out.println("    aload_1");
        out.println("    ldc \",\"");
        out.println("    invokevirtual java/lang/String concat (Ljava/lang/String;)Ljava/lang/String;");
        out.println("    astore_1");
        out.println("skip_comma_" + loopStart + ":");
        if ("java/lang/Object".equals(toStringClass)) {
            // Object array - first compute element string, then concat with base
            // Load element and compute its string representation
            out.println("    aload_0");
            out.println("    iload_3");
            out.printf("    %s%n", loadOp);
            String nullLabel = "not_null_" + loopStart;
            String joinLabel = "join_" + loopStart;
            out.println("    dup");
            out.printf("    ifnonnull %s%n", nullLabel);
            out.println("    pop");
            out.println("    ldc \"null\"");
            out.printf("    goto %s%n", joinLabel);
            out.printf("%s:%n", nullLabel);
            out.println("    invokevirtual java/lang/Object toString ()Ljava/lang/String;");
            out.printf("%s:%n", joinLabel);
            // Stack: [String elemStr]
            // Now load base and concat
            out.println("    aload_1");
            out.println("    swap");
            out.println("    invokevirtual java/lang/String concat (Ljava/lang/String;)Ljava/lang/String;");
            out.println("    astore_1");
        } else {
            // For String and primitive arrays, load base first
            out.println("    aload_1");
            out.println("    aload_0");
            out.println("    iload_3");
            out.printf("    %s%n", loadOp);
            if (toStringClass == null) {
                // String array - value is already a String, no conversion needed
                // (but handle null case)
                // Use dup/pop pattern to ensure both branches have same stack state at join
                String nullLabel = "not_null_" + loopStart;
                String joinLabel = "append_" + loopStart;
                out.println("    dup");
                out.printf("    ifnonnull %s%n", nullLabel);
                out.println("    pop");
                out.println("    ldc \"null\"");
                out.printf("    goto %s%n", joinLabel);
                out.printf("%s:%n", nullLabel);
                // String is already on stack, use it directly
                out.printf("%s:%n", joinLabel);
            } else {
                out.printf("    invokestatic %s toString (%s)Ljava/lang/String;%n", toStringClass, elementDesc);
            }
            out.println("    invokevirtual java/lang/String concat (Ljava/lang/String;)Ljava/lang/String;");
            out.println("    astore_1");
        }
        out.println("    iinc 3 1");
        out.printf("    goto %s%n", loopStart);
        out.printf("%s:%n", loopEnd);
        out.println("    aload_1");
        out.println("    ldc \"]\"");
        out.println("    invokevirtual java/lang/String concat (Ljava/lang/String;)Ljava/lang/String;");
        out.println("    areturn");
        out.println(".end code");
        out.println();
    }
}
