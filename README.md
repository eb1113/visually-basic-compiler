# Visually Basic Compiler

<div align="center">

**A fully-functional compiler for the Visually Basic programming language**

*Translating playful syntax into JVM bytecode - it's so fetch!*

[![Java](https://img.shields.io/badge/Java-8%2B-orange)](https://www.oracle.com/java/)
[![ANTLR](https://img.shields.io/badge/ANTLR-4.13.2-blue)](https://www.antlr.org/)
[![License](https://img.shields.io/badge/License-Academic-lightgrey)]()

</div>

A complete compiler implementation that translates **Visually Basic** source code into Java Virtual Machine (JVM) bytecode. Inspired by Valley Girl culture, this language uses playful slang keywords (like `spill`, `istotes`, `OMG`, and `like`) to create a super duper cool programming experience. The compiler demonstrates advanced compiler design principles including lexical analysis, parsing, semantic analysis, and code generation using industry-standard tools like ANTLR and JASM.

## 📋 Table of Contents

- [Overview](#overview)
- [Language Features](#language-features)
- [Technologies Used](#technologies-used)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Usage](#usage)
- [Compilation Pipeline](#compilation-pipeline)
- [Example Programs](#example-programs)
- [Testing](#testing)
- [Architecture](#architecture)

## 🎯 Overview

Visually Basic is a totally rad programming language inspired by Valley Girl speak that compiles to JVM bytecode. Like, it's basically the most fun you'll have writing code! The compiler implements a complete compilation pipeline from source code to executable `.class` files, supporting:

- **Primitive types**: `int`, `double`, `boolean`, `string`
- **Control flow**: `if/else`, `while`, `for` loops (Java-style and Pascal-style)
- **Functions**: User-defined functions with parameters and return values
- **Arrays**: Single and multi-dimensional arrays with length support
- **Records**: User-defined types with fields and methods
- **Built-in methods**: String manipulation, mathematical operations, and system utilities

The compiler is built using **ANTLR 4** for parsing and generates **JASM** (Java Assembly) code, which is then assembled into JVM bytecode. It's so fetch how it all comes together!

## ✨ Language Features

### Core Features

- **Variables and Types**: Support for `int`, `double`, `boolean`, and `string` types
- **Operators**: Arithmetic (`+`, `-`, `*`, `/`, `%`), relational (`==`, `!=`, `<`, `>`, `<=`, `>=`), logical (`and`, `or`, `not`)
- **Type Conversions**: Automatic widening and explicit casting
- **Input/Output**: `spill()` for printing and `input()` for reading user input
- **Functions**: First-class function support with parameters and return values
- **Control Structures**: 
  - `if/else` statements
  - `while` loops
  - `for` loops (both Java-style and Pascal-style)
- **Increment/Decrement**: Pre and post `++` and `--` operators

### Advanced Features (Level 3)

- **Arrays**: 
  - Array construction (`new int[100]`)
  - Array subscripting (`array[15]`)
  - Array length field (`array.length`)
  - Multi-dimensional arrays (arrays of arrays)
  - Arrays of records
  
- **Records**:
  - Record definitions with fields and methods
  - Record construction (`new Point(1.2, 3.4)`)
  - Field access (`point.x`)
  - Method calls (`point.distance(otherPoint)`)
  - `this` expression support
  - Automatic constructor and `toString()` generation

- **Built-in Methods**:
  - String methods: `.length()`, `.indexOf(string)`, `.substring(int, int)`
  - Math fields: `Math.PI`, `Math.E`
  - Math methods: `Math.sqrt()`, `Math.pow()`, `Math.exp()`, `Math.log()`, `Math.sin()`, `Math.cos()`, `Math.tan()`
  - System methods: `System.exit(int)`

### Language Syntax Examples

```visually-basic
// Variables and printing
int x istotes 42;
spill("Hello, world!");
spill("x = ", x);

// Functions
int add(its giving int a, its giving int b) {
    period a + b;
}

// Control flow
if (x vibes with 42) {
    spill("Perfect!");
} whateva {
    spill("Not quite");
}

// Arrays
int full of arr istotes OMG int basic 10;
arr basic 0 istotes 100;
spill(arr.length);

// String concatenation
string msg istotes "Hello" um " " um "world";

// Records
phat Point(double x, double y) {
    double distance(Point other) {
        period Math.sqrt((x - other XO x) * (x - other XO x) + (y - other XO y) * (y - other XO y));
    }
}

Point p istotes OMG Point(1.0, 2.0);
spill(p XO x, ", ", p XO y);
```

**Key Language Keywords**:
- `spill()` - print function
- `istotes` - assignment operator (=)
- `OMG` - new keyword for object/array creation
- `phat` - record type definition
- `slay` - void return type
- `period` - return statement
- `its giving` - function parameter keyword
- `vibes with` - equality operator (==)
- `bad vibe` - less than or equal (<=)
- `full of` - array type declaration
- `basic` - array subscript operator
- `XO` - field access operator (.)
- `um` - string concatenation operator
- `as if` - else if
- `whateva` - else

## 🛠 Technologies Used

### ANTLR (Another Tool for Language Recognition)

[ANTLR](https://www.antlr.org/) is a powerful parser generator that reads grammar files and generates lexers and parsers. It's like totes the best tool for this! In this project, ANTLR 4.13.2 is used to:

- **Lexical Analysis**: Tokenize source code into meaningful units (keywords, identifiers, operators, literals)
- **Syntax Analysis**: Parse tokens according to the language grammar defined in `.g4` files
- **AST Generation**: Build Abstract Syntax Trees (ASTs) that represent the program structure

ANTLR generates Java classes (`VisuallyBasicLexer` and `VisuallyBasicParser`) from grammar files (`VisuallyBasicLexer.g4` and `VisuallyBasicParser.g4`), which handle the first two phases of compilation.

**Key Benefits**:
- Declarative grammar specification
- Automatic error recovery and reporting
- Support for complex language constructs
- Well-documented and widely-used in industry

### JASM (Java Assembler)

[JASM](https://github.com/westminsteru/jasm) is a Java bytecode assembler that converts human-readable assembly-like syntax into JVM bytecode. It's super duper cool! Thanks Kathy :) This compiler:

- **Code Generation**: Emits JASM assembly code from the Intermediate Representation (IR)
- **Bytecode Assembly**: Uses JASM to convert `.jasm` files into `.class` files
- **JVM Execution**: Produces standard JVM bytecode that can run on any Java Virtual Machine

JASM provides a more readable alternative to writing bytecode directly, making code generation more maintainable and debuggable.

**Key Benefits**:
- Human-readable bytecode representation
- Direct mapping to JVM instructions
- Supports all JVM features (classes, methods, fields, etc.)
- Enables execution on standard JVM

## 📁 Project Structure

```
visually-basic-compiler/
├── src/
│   └── edu/westminsteru/cmpt355/level3/
│       ├── Main.java                    # Compiler entry point
│       ├── VisuallyBasicLexer.g4        # ANTLR lexer grammar
│       ├── VisuallyBasicParser.g4       # ANTLR parser grammar
│       ├── SemanticAnalyzer.java        # Semantic analysis and IR generation
│       ├── CodeGenerator.java           # JASM code generation
│       ├── ast/                         # Abstract Syntax Tree nodes
│       ├── ir/                          # Intermediate Representation nodes
│       ├── lang/                        # Type system and symbol tables
│       └── util/                        # Utility classes
├── lib/
│   ├── antlr-4.13.2-complete.jar        # ANTLR runtime and tool
│   ├── jasm.jar                         # JASM assembler
│   └── compiler-tester-0.3.jar          # Automated testing framework
├── test/                                # Test programs organized by level
│   ├── level0/                          # Basic features
│   ├── level1/                          # Functions and types
│   ├── level2/                          # Control flow
│   └── level3/                          # Arrays and records
├── test-compiled/                       # Compiled test outputs
├── out/                                 # Build output directory
└── README.md                            # This file
```

## 📦 Prerequisites

Before building and running the compiler, ensure you have:

- **Java Development Kit (JDK) 8 or higher**
  - Download from [Oracle](https://www.oracle.com/java/technologies/downloads/) or [OpenJDK](https://openjdk.org/)
  - Verify installation: `java -version` and `javac -version`

- **ANTLR 4.13.2** (included in `lib/` directory)
  - The JAR file is provided, but you can download from [ANTLR website](https://www.antlr.org/download.html) if needed

- **JASM** (included in `lib/` directory)
  - The JAR file is provided with the project

## 🚀 Installation

1. **Clone the repository**:
   ```bash
   git clone https://github.com/lmorgenthaler/visually-basic-compiler.git
   cd visually-basic-compiler
   ```

2. **Set up the classpath**:
   The compiler requires the following JARs in the classpath:
   - `lib/antlr-4.13.2-complete.jar`
   - `lib/jasm.jar`
   - `lib/compiler-tester-0.3.jar` (for testing)

3. **Build the compiler**:
   
   **Using Command Line**:
   ```bash
   # Compile all Java source files
   javac -cp "lib/antlr-4.13.2-complete.jar:lib/jasm.jar" \
         -d out/production/level3 \
         src/edu/westminsteru/cmpt355/level3/**/*.java
   ```
   
   **Using IntelliJ IDEA**:
   - Open the project in IntelliJ IDEA (the `.iml` file is already configured)
   - The IDE will automatically detect ANTLR grammar files and generate lexer/parser classes
   - Build the project using `Build > Build Project` or `Cmd+B` (Mac) / `Ctrl+B` (Windows/Linux)
   - Compiled classes will be in `out/production/level3/`
   
   **Note**: ANTLR generates Java classes from `.g4` grammar files. These generated files are typically placed in a `gen/` directory (which is gitignored). Make sure your IDE or build process includes these generated files in the classpath.

## 💻 Usage

### Basic Compilation

Ready to compile some totally awesome Visually Basic code? Here's how:

```bash
java -cp "out/production/level3:lib/antlr-4.13.2-complete.jar:lib/jasm.jar" \
     edu.westminsteru.cmpt355.level3.Main \
     test/level0/print.l3
```

This will:
1. Parse the source file
2. Perform semantic analysis
3. Generate JASM code (saved to `test/out/print.jasm`)
4. Assemble the bytecode (saved to `test/out/print.class`)

And boom! You've got yourself some executable bytecode. So fetch!

### Compiler Options

The compiler supports several command-line options:

```bash
java -cp "out/production/level3:lib/antlr-4.13.2-complete.jar:lib/jasm.jar" \
     edu.westminsteru.cmpt355.level3.Main [options] filename
```

**Options**:
- `-h`, `-help`, `--help`: Display usage information
- `-ast`: Save the generated Abstract Syntax Tree to a file (`.ast.txt`)
- `-ir`: Save the generated Intermediate Representation to a file (`.ir.txt`)
- `-d dir`: Specify output directory (default: `test/out`)
- `-r`: Run the compiled program immediately after compilation

### Running Compiled Programs

After compilation, execute the generated bytecode:

```bash
java -cp test/out Print
```

Or use the `-r` flag to compile and run in one step:

```bash
java -cp "out/production/level3:lib/antlr-4.13.2-complete.jar:lib/jasm.jar" \
     edu.westminsteru.cmpt355.level3.Main -r test/level0/print.l3
```

### Example: Compile and Run FizzBuzz

```bash
# Compile
java -cp "out/production/level3:lib/antlr-4.13.2-complete.jar:lib/jasm.jar" \
     edu.westminsteru.cmpt355.level3.Main \
     test/level2/fizzbuzz.l3

# Run
java -cp test/out Fizzbuzz
```

## 🔄 Compilation Pipeline

The Visually Basic compiler follows a traditional multi-pass compilation approach:

### 1. **Lexical Analysis** (Lexer)
- **Tool**: ANTLR-generated `VisuallyBasicLexer`
- **Input**: Source code (`.l3` files)
- **Output**: Token stream
- **Process**: Recognizes keywords (like `spill`, `istotes`, `OMG` - so cute!), identifiers, operators, literals, and whitespace

### 2. **Syntax Analysis** (Parser)
- **Tool**: ANTLR-generated `VisuallyBasicParser`
- **Input**: Token stream
- **Output**: Abstract Syntax Tree (AST)
- **Process**: Validates syntax according to grammar rules and builds tree structure

### 3. **Semantic Analysis**
- **Component**: `SemanticAnalyzer`
- **Input**: AST
- **Output**: Intermediate Representation (IR) + Symbol Tables
- **Process**:
  - Type checking and type inference
  - Symbol resolution (variables, functions, records)
  - Scope analysis
  - Lvalue/rvalue analysis
  - Generates IR with explicit type information

### 4. **Code Generation**
- **Component**: `CodeGenerator`
- **Input**: IR + Symbol Tables
- **Output**: JASM assembly code (`.jasm` files)
- **Process**:
  - Traverses IR nodes
  - Generates JVM instructions (load/store, arithmetic, control flow, method calls)
  - Manages local variables and stack operations
  - Handles array operations, record methods, and built-in functions

### 5. **Assembly**
- **Tool**: JASM assembler
- **Input**: JASM code (`.jasm` files)
- **Output**: JVM bytecode (`.class` files)
- **Process**: Converts assembly syntax to binary bytecode

### 6. **Execution**
- **Runtime**: Java Virtual Machine
- **Input**: Bytecode (`.class` files)
- **Output**: Program execution results

## 📚 Example Programs

The `test/` directory contains numerous example programs demonstrating various language features. Check out these sweet examples:

### Level 0: Basics
- `print.l3`: Basic printing
- `variables.l3`: Variable declarations and assignments
- `operators.l3`: Arithmetic and logical operations
- `input.l3`: User input handling

### Level 1: Functions and Types
- `functions.l3`: Function definitions and calls
- `conversions.l3`: Type conversions
- `strings.l3`: String operations

### Level 2: Control Flow
- `if.l3`, `ifelse.l3`, `nestedif.l3`: Conditional statements
- `while.l3`: While loops
- `for_java.l3`, `for_pascal.l3`: For loop variants
- `fizzbuzz.l3`: Classic FizzBuzz implementation
- `collatz.l3`: Collatz conjecture example

### Level 3: Advanced Features
- `arrays_basics.l3`, `arrays_full.l3`: Array operations
- `records_basics.l3`, `records_full.l3`: Record definitions and usage
- `arrays_of_records.l3`, `records_of_arrays.l3`: Complex type combinations
- `builtin_methods.l3`: Built-in method demonstrations
- `string_methods.l3`: String manipulation methods

## 🧪 Testing

The project includes an automated testing framework that's super duper helpful. To run all tests:

```bash
java -cp "out/production/level3:lib/antlr-4.13.2-complete.jar:lib/jasm.jar:lib/compiler-tester-0.3.jar" \
     edu.westminsteru.cmpt355.compiler_tester.Main \
     test
```

To test a specific directory or file:

```bash
# Test all level 3 features
java -cp "out/production/level3:lib/antlr-4.13.2-complete.jar:lib/jasm.jar:lib/compiler-tester-0.3.jar" \
     edu.westminsteru.cmpt355.compiler_tester.Main \
     test/level3

# Test a specific file
java -cp "out/production/level3:lib/antlr-4.13.2-complete.jar:lib/jasm.jar:lib/compiler-tester-0.3.jar" \
     edu.westminsteru.cmpt355.compiler_tester.Main \
     test/level2/fizzbuzz.l3
```

The test framework:
- Compiles each test file
- Runs the compiled program
- Compares output against expected results
- Reports pass/fail status

## 🏗 Architecture

The compiler architecture is like, totally well-designed! Here's how it all works:

### Type System

The compiler uses a sophisticated type system implemented in the `lang/` package:

- **`Type`**: Sealed interface representing all types
- **`BuiltInType`**: Primitive types (`int`, `double`, `boolean`, `string`)
- **`ArrayType`**: Array types (`int[]`, `double[][]`, etc.)
- **`RecordType`**: User-defined record types with fields and methods
- **`ScalarType`**: Base type for non-constructed types

### Symbol Tables

Three-level symbol table hierarchy:

- **`GlobalSymbolTable`**: Global variables, functions, and record types
- **`LocalSymbolTable`**: Local variables within functions
- **`RecordSymbolTable`**: Fields and methods within records

### AST and IR

- **AST (Abstract Syntax Tree)**: Tree representation of source code structure
  - Located in `ast/` package
  - Closely mirrors grammar structure
  - Contains minimal semantic information

- **IR (Intermediate Representation)**: Lower-level representation with explicit type information
  - Located in `ir/` package
  - Includes type conversions, widening operations
  - Optimized for code generation

### Code Generation Strategy

The code generator (`CodeGenerator.java`) uses a visitor-like pattern to traverse IR nodes:

- **Expressions**: Generate stack-based operations
- **Statements**: Generate control flow and side effects
- **Functions**: Generate method definitions with proper signatures
- **Records**: Generate class definitions with fields and methods
- **Arrays**: Use JVM array instructions (`newarray`, `anewarray`, `*aload`, `*astore`)
- **Built-ins**: Generate calls to Java standard library methods

## 📝 Notes

- The compiler generates standard JVM bytecode, so compiled programs can run on any JVM
- Multi-dimensional arrays are supported (arrays of arrays)
- Record types are compiled to Java classes with public fields and methods
- The `this` keyword is supported within record methods
- Automatic type widening is performed (e.g., `int` to `double`)
- Array and record printing uses custom `toString()` implementations
- ANTLR-generated lexer and parser classes are stored in the `gen/` directory (gitignored)
- The project structure follows standard Java package conventions

## 🎓 Educational Context

This compiler was developed as part of a compilers course, implementing a complete compilation pipeline from source code to executable bytecode. It's like totes fetch! The project demonstrates:

- **Lexical and Syntax Analysis**: Using ANTLR to define and parse a custom language grammar (with totally unique Valley Girl-inspired keywords)
- **Semantic Analysis**: Type checking, symbol resolution, and scope management (making sure everything vibes correctly)
- **Code Generation**: Translating high-level language constructs to JVM bytecode (super duper cool stuff!)
- **Compiler Architecture**: Multi-pass compilation with AST and IR representations
- **Language Design**: Implementing a complete programming language with modern features, all while keeping it fun and expressive

The playful "girly" syntax makes the language memorable while maintaining the complexity and rigor of a real compiler implementation. Like, it's basically the best of both worlds!


---

**Built with ❤️ using ANTLR 4 and JASM**

*Like, I'm obsessed! So chic, period* ✨
