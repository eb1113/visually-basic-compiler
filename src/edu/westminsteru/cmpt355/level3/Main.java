package edu.westminsteru.cmpt355.level3;

import edu.westminsteru.jasm.Bytecode;
import edu.westminsteru.jasm.JasmAssembler;
import edu.westminsteru.jasm.Status;
import edu.westminsteru.cmpt355.level3.VisuallyBasicLexer;
import edu.westminsteru.cmpt355.level3.VisuallyBasicParser;
import edu.westminsteru.cmpt355.level3.ast.AstProgram;
import edu.westminsteru.cmpt355.level3.util.Tree;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;

/** Main class for compiler */
/* All you should have to do here is fill in the parse() method to invoke your
 * lexer and parser */
public class Main {

    public static void main(String... args) throws Exception {
        boolean saveAst = false, saveIr = false;
        boolean runAtOnce = false;
        Path inFile = null;
        Path outPath = Path.of("test/out");
        for (int i = 0; i < args.length; ++i) {
            if (args[i].equals("-h") || args[i].equals("-help") || args[i].equals("--help")) {
                printUsage();
                System.exit(0);
            } else if (args[i].equals("-ast"))
                saveAst = true;
            else if (args[i].equals("-ir"))
                saveIr = true;
            else if (args[i].equals("-r"))
                runAtOnce = true;
            else if (args[i].equals("-d") && i + 1 < args.length)
                outPath = Path.of(args[++i]);
            else if (inFile == null)
                inFile = Path.of(args[i]);
            else {
                System.err.printf("Unknown argument: %s\n", args[i]);
                System.exit(1);
            }
        }

        if (inFile == null) {
            System.out.println("No filename specified.");
            printUsage();
            System.exit(1);
        }

        Files.createDirectories(outPath);

        String inFilenameStem = filenameStem(inFile.getFileName().toString());

        var ast = parse(inFile);

        if (saveAst) {
            Path astPath = outPath.resolve(inFilenameStem + ".ast.txt");
            try (BufferedWriter bw = Files.newBufferedWriter(astPath);
                 PrintWriter astOut = new PrintWriter(bw)) {
                Tree.print(ast, astOut);
                System.out.printf("AST saved to %s.\n", astPath);
            }
        }

        var analyzer = new SemanticAnalyzer();
        var ir = analyzer.analyzeProgram(ast);

        if (saveIr) {
            Path irPath = outPath.resolve(inFilenameStem + ".ir.txt");
            try (BufferedWriter bw = Files.newBufferedWriter(irPath);
                 PrintWriter irOut = new PrintWriter(bw)) {
                Tree.print(ir, irOut);
                System.out.printf("IR saved to %s.\n", irPath);
            }
        }

        Path codePath = outPath.resolve(inFilenameStem + ".jasm");
        String outClassName = inFilenameStem.replaceAll("[^a-zA-Z0-9_$]", "_");
        try (BufferedWriter bw = Files.newBufferedWriter(codePath);
             PrintWriter codeOut = new PrintWriter(bw)) {
            var codeGenerator = CodeGenerator.builder()
                .program(ir)
                .globalTable(analyzer.getGlobalSymbolTable())
                .sourceFilename(inFile.toString())
                .outClassName(inFilenameStem)
                .output(codeOut)
                .build();
            codeGenerator.generateCode();
            System.out.printf("Code saved to %s.\n", codePath);

            try (var reader = Files.newBufferedReader(codePath)) {
                var assembler = JasmAssembler.reading(reader);
                var status = assembler.assemble();
                if (status == Status.Failure) {
                    assembler.getErrorMessages().forEach(msg -> {
                        msg.print(System.err);
                        System.err.println();
                    });
                    System.exit(1);
                }

                for (var bytecode : assembler.getAssembledBytecodes())
                    saveBytecode(bytecode, outPath);

                if (runAtOnce) {
                    System.out.println("Executing program...");
                    var classLoader = new ClassLoader() {
                        Class<?> load(String className, byte[] bytecode) {
                            return super.defineClass(className, bytecode, 0, bytecode.length);
                        }
                    };

                    Class<?> mainClass = null;

                    for (var bytecode : assembler.getAssembledBytecodes()) {
                        Class<?> c = classLoader.load(bytecode.className(), bytecode.data());
                        if (c.getCanonicalName().replaceAll("\\.", "/").equals(outClassName))
                            mainClass = c;
                    }

                    if (mainClass == null) {
                        System.err.println("Internal error (please report): cannot find compiled class " + outClassName);
                        System.exit(2);
                    }

                    var method = mainClass.getMethod("main", String[].class);
                    method.invoke(null, new Object[] { new String[0] });
                }
            }
        }
    }

    private static String filenameStem(String filename) {
        int slashIndex = filename.lastIndexOf('/');
        if (slashIndex < 0)
            slashIndex = -1;

        int dotIndex = filename.lastIndexOf('.');

        if (dotIndex < 0 || dotIndex < slashIndex)
            // no ., or it was part of a path component before the filename
            return filename;

        return filename.substring(slashIndex + 1, dotIndex);
    }

    private static AstProgram parse(Path file) throws Exception {
        var stream = CharStreams.fromFileName(file.toString());
        var lexer = new VisuallyBasicLexer(stream);
        var parser = new VisuallyBasicParser(new CommonTokenStream(lexer));
        var programContext = parser.program();
        var program = programContext.n;

        if (program == null)
            throw new Exception("Parsing failed.");

        return program;
    }

    private static void printUsage() {
        System.out.printf("""
                Usage: java %s [options] filename
                
                Options:
                    -h      Show this help message
                    -ast    Save the generated AST to a file
                    -ir     Save the generated IR to a file
                    -d dir  Save output in the given directory
                    -r      Run the compiled program at once after compiling
                    
                    
                """, Main.class.getName());
    }

    private static void saveBytecode(Bytecode bytecode, Path outDirectory) throws IOException {
        Files.createDirectories(outDirectory);
        Path outFile = outDirectory.resolve(bytecode.className() + ".class");
        Files.write(outFile, bytecode.data());
    }
}
