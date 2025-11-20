# Level 3 compiler: CMPT 355, Fall 2025

## Features supported at this level:
- All level 0 features
- All level 1 features
- All level 2 features
- Type constructors (programmer-defined types):
  - Arrays (`int[]`, etc.)
    - Multi-dimensional arrays (arrays of arrays) are **optional** (your code needs to support `int[]` but not necessarily `int[][]`)
    - Arrays of records are possible, e.g. `Point[]`
  - Records, e.g.
    ```
    record Point(double x, double y) {
        double distance(Point other) {
            return Math.sqrt((x - other.x) * (x - other.x) + (y - other.y) * (y - other.y));
        }
    }
    ```
- Features related to arrays:
  - Array construction (`new int[100]`)
    - Construction by specifying values (`new int[] { 1, 2, 3 }`) is **optional**
  - Array subscripting (`array[15]`)
  - Array length built-in field (`array.length`)
    - Note that syntactically this is just a field access; your semantic analyzer should notice that the `.length` of an array is being requested and act accordingly.
    - Do *not* try to handle this in the parser (don't turn `length` into a special token type since it will then be illegal to name a variable `length`!).
  - An improvement over Java: converting an array to a string should nicely format the array contents, for example `print(new int[3]);` should print `[0,0,0]`.
  - `input()` does *not* need to work with arrays.
- Features related to records:
  - Record definition (`record Point(double x, double y) { /* methods */ }`)
    - Between the `{ }`, record definitions can contain methods but not variables
    - Records should be implemented as classes (`.class`) where the fields and methods are `public` (and non-static).
    - Record definitions should automatically come with a constructor (having a parameter for each field) and a `toString()` method that nicely formats it (e.g. `Point[x=1.2,y=3.4]`).
  - Record construction (`new Point(1.2, 3.4)`)
  - Field access (`point.x`)
  - Method calls (`point.distance(otherPoint)`)
  - `this` expression (allowed only inside methods of records)
  - Converting a record object to string or printing it should invoke its `toString()` method
  - `input()` does *not* need to work with records.
- Built-in fields and methods: similar to an array `.length`, the semantic analyzer will need to handle these specially (but do not try to handle these in the parser!):
  - `.length()`, `.indexOf(string)`, `.substring(int, int)` methods for strings
  - `Math.PI` and `Math.E` fields
  - `Math.sqrt(double)`, `Math.pow(double, double)`, `Math.exp(double)`, `Math.log(double)`, `Math.sin(double)`, `Math.cos(double)`, `Math.tan(double)` methods
    - Since we can now do square roots using `Math.sqrt()`, you can get rid of the top-level `sqrt()` function that we've had since level 0!
  - `System.exit(int)` method

## Recommended implementation notes

### Notes on records
Methods (belong to a record) are different from the functions we have implemented so far in that they should be implemented as **non-**`static` JVM methods. This means that they are called using `invokevirtual` rather than `invokestatic`, and it also means that each one will have a hidden first parameter: the object the method is called on. (When you write `this` in Java code, you're just getting the value of local variable 0: `aload_0`.) So in a method `void f(int x, double y)`, `this` will be local 0, `x` will be local 1, and `y` will be local 2.

In Java code, a constructor has the same name as the class and no return type, but in the JVM, a constructor is a method named `<init>` with return type `void`. The constructor **must** start by invoking its superclass constructor (`Object`'s default constructor) using `invokespecial`.

### Notes on arrays
Accesses to array elements are done with the `*aload/*astore` family of instructions (`iaload`/`iastore` for `int` arrays, etc.). Note that Boolean arrays use `baload`/`bastore` (Booleans are treated as `int`s on the stack but not within arrays). The stack will need to contain the array and the index (and for a store, the value to place in the array).

To implement the pretty-printing of arrays (`[1,2,3]`), it is recommended that you bake static methods for printing `int`, `double`, `boolean`, and `Object` arrays into the generated jasm code and call them appropriately.

In jasm, arrays are created using either `newarray` (if the elements are primitives, e.g. `newarray int`) or `anewarray` (if the elements are objects, e.g. `anewarray Ljava/lang/String;`).

### Types
Since level 3 adds type constructors, the way you model types in the language will need more nuance; a simple `enum` of the four predefined types will no longer suffice (although you may find it helpful to define an `enum BuiltinType` to cover the predefined types). Your life will be easier if you use a `sealed interface`/`record`/`enum` hierarchy to represent the types, as we have done for AST and IR nodes, as the compiler can then better help you cover all cases.

You are **not** required to support arrays of arrays; for the most part, they would not be difficult to implement, but creation of nested arrays is somewhat complex in terms of both syntax (`new int[10][10][][]` is allowed but not `new int[10][][10][]`) and implementation (the `multianewarray` instruction).

### Lvalues
An *lvalue* is something that can appear on the left-hand side of an assignment — it is the analog of an expression (also known as an *rvalue*) as it is something that can *accept* a value, rather than *produce* one. Until level 3 the only lvalues were simple variables, but now there are two more kinds:
  - array subscripts, e.g. `a[i]`;
  - record fields, e.g. `point.x`.
Keep in mind that in both cases, the array or record object could itself come from a complex expression, so all of the following are also syntactically valid:
  - `f()[99] = 1;`
  - `new Buffer(100)[0] = 1;`
  - `a.b.c.d[e().f[19]].g = 1;`
  - `a[i++]--;`

In the reference syntax for our level 3 language, all lvalues are also rvalues (expressions), so you could have `AstLvalue`/`IrLvalue` sealed interfaces descending directly from `AstExpression`/`IrExpression`. Keep in mind that assigning to general lvalues is more complex to implement in the JVM than simple variables (array subscripts need the array and index on the stack, while record fields need the record) and you will have to refactor your code generation for these. And don't forget that the `++` and `--` operators need to operate on all kinds of lvalues!

### Lvalues and parsing
Recall that Antlr does not permit mutual left recursion, e.g. rules of the form
```
a : b ...
b : a ...
```
On your first attempt at adding lvalues to the grammar, you are likely to inadvertently introduce mutual left recursion, so think carefully about how to adjust your grammar appropriately. (If you are using IntelliJ IDEA's Antlr plugin, it should automatically show you instances of this.) You will likely need to add some redundancy to your grammar (e.g. defining the syntax for an array subscript in more than one parser rule) in order to overcome this. 

## Suggested plan of attack
0. Copy your level1 code into this project to use as a starting point (make sure to rename the `level2` package to `level3`). (The code you are given in this starter project is essentially the same as in level 2.)
1. Refactor your type system (see the notes above). What you do here will likely guide the way you adjust types in the parser.
2. Update your grammar and AST nodes: you will need to refactor types, introduce lvalues (using them in assignments and increments), and add the needed syntax for arrays and records. Watch out for mutual left recursion!
3. Continue on to semantic analysis and IR nodes.  I recommend adding a `RecordSymbolTable` to handle record-specific symbols (fields and methods). 
3. Implement code generation and test!

## Hints and suggestions
- This assignment is provided as an IntelliJ IDEA project. You are *not* required to use IntelliJ and are free to delete the IntelliJ stuff (`level3.iml`, the `.idea` directory) if you wish.
- A `Main` class (`edu.westminsteru.cmpt355.level3.Main`) is already essentially written for you. You just need to fill in code in the `parse()` method to invoke your lexer and parser.
  - If you wish, it's probably OK to replace the `Main` class with the one you have from level 2 as no new features are added to it at this level.
- Essentially the same starting code is provided for you at this level as at level 2. (You will likely just replace all of it with your level 2 stuff anyway.)

## Running the compiler tester
The compiler-tester .jar has been updated in this level. You can use it to automate the process of testing your compiler.
1. Make sure that all the following are in your `CLASSPATH`:
  - `lib/antlr-4.13.2-complete.jar`,
  - `lib/jasm.jar`
  - `lib/compiler-tester-0.3.jar`
  - the compiled (.class) files of your compiler (in IntelliJ IDEA, by default these would be in `out/production/level3`)
2. If necessary, modify the `test-config` file so that it has the correct name of your compiler's main class.
3. Run the `edu.westminsteru.cmpt355.compiler_tester.Main` class, naming one or more test files or directories. To run all tests, you can just name the `test` directory.
