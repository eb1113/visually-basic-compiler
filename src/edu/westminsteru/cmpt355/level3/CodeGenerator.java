package edu.westminsteru.cmpt355.level3;

import edu.westminsteru.cmpt355.level3.ast.BinaryOp;
import edu.westminsteru.cmpt355.level3.ast.IncrementOp;
import edu.westminsteru.cmpt355.level3.ir.*;
import edu.westminsteru.cmpt355.level3.lang.*;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/** Code generation stage of the compiler: given an AST, print jasm code */
public class CodeGenerator {

    /** The PrintWriter to print the output (jasm) code to */
    private final PrintWriter out;
    /** The IR of the program */
    private final IrProgram program;
    /** Name of the original source file */
    private final String sourceFilename;
    /** Name of the class being compiled */
    private final String outClassName;
    /** Counter for generating unique labels */
    private int labelCounter = 0;

    /** Builder class for CodeGenerator instances */
    public static class Builder {
    private IrProgram program;
        private String sourceFilename;
        private String outClassName;
        private PrintWriter out = new PrintWriter(System.out);

        private Builder() {}

        public Builder program(IrProgram program) {
            this.program = program;
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
            if (outClassName == null)
                throw new IllegalStateException("Output class name must be specified");
            if (out == null)
                throw new IllegalStateException("Output must be specified");
            return new CodeGenerator(program, sourceFilename, outClassName, out);
        }
    }

    private CodeGenerator(
        IrProgram program,
        String sourceFilename,
        String outClassName,
        PrintWriter out
    ) {
        this.program = program;
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
        } else {
            // ArrayType or RecordType - use Object println
            out.println("    invokevirtual java/io/PrintStream println (Ljava/lang/Object;)V");
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
        } else {
            // ArrayType or RecordType - use Object.toString()
            out.println("    invokevirtual java/lang/Object toString ()Ljava/lang/String;");
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
        out.println("    getstatic java/lang/System out Ljava/io/PrintStream;");
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
        generateExpression(assign.value());
        
        // Handle different types of lvalues
        if (assign.lvalue() instanceof IrVariable var) {
            // Check if it's a local or global variable
            if (var.variable() instanceof LocalVariable local) {
                // Use local variable store instruction
                emitStore(var.variable().type(), local.index());
            } else {
                // Use static field access
                String descriptor = var.variable().type().getDescriptor();
                out.printf("    putstatic %s %s %s%n", outClassName, var.variable().name(), descriptor);
            }
        } else {
            // TODO: Handle array subscripts and field accesses
            throw new InternalCompilerException("Assignment to non-variable lvalue not yet implemented: " + assign.lvalue());
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
                } else {
                    // TODO: Handle array subscripts and field accesses
                    throw new InternalCompilerException("Increment on non-variable lvalue not yet implemented: " + inc.lvalue());
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
        if (sourceType == targetType) {
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
}
