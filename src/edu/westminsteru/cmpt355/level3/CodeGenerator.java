package edu.westminsteru.cmpt355.level3;

import edu.westminsteru.cmpt355.level3.ir.*;

import java.io.PrintWriter;

/** Code generation stage of the compiler: given an IR, print jasm code */
public class CodeGenerator {

    /** The PrintWriter to print the output (jasm) code to */
    private final PrintWriter out;
    /** The IR of the program */
    private final IrProgram program;
    /** Name of the original source file */
    private final String sourceFilename;
    /** Name of the class being compiled */
    private final String outClassName;

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

    public void generateCode() {
        // your code goes here :)
        // (print jasm code to _out_)


        out.flush();
    }
}
