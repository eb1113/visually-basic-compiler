parser grammar VisuallyBasicParser;

@header{
import edu.westminsteru.cmpt355.level3.ast.*;
import edu.westminsteru.cmpt355.level3.lang.*;
import java.util.*;
import java.util.Optional;
import static java.util.stream.Collectors.toList;
}

options {
    tokenVocab = VisuallyBasicLexer;
}

/** Parser rules **/

// Full program
program
returns [AstProgram n]
    : elements=mixedElements EOF
        // Separate functions and statements from all elements
        {
        List<AstFunctionDeclaration> funcs = new ArrayList<>();
        List<AstStatement> stmts = new ArrayList<>();
        for (AstStatement element : $elements.n) {
            if (element instanceof AstFunctionDeclaration funcDecl) {
                funcs.add(funcDecl);
            } else {
                stmts.add(element);
            }
        }
        $n = new AstProgram(funcs, stmts);
        }
    ;

// Mixed elements (functions and statements in any order)
mixedElements
returns [List<AstStatement> n]
    : elements+=mixedElement* {
        $n = $elements.stream()
            .map(node -> node.n)
            .collect(toList());
    }
    ;

// Mixed element (function, record, or statement)
mixedElement
returns [AstStatement n]
    : functionDeclarationStatement {
        $n = $functionDeclarationStatement.n;
    }
    | recordDeclaration {
        $n = $recordDeclaration.n;
    }
    | statement {
        $n = $statement.n;
    }
    ;

statement
returns [AstStatement n]
    : printStatement {
        $n = $printStatement.n;
    }
    | variableDeclarationStatement {
        $n = $variableDeclarationStatement.n;
    }
    | variableAssignmentStatement {
        $n = $variableAssignmentStatement.n;
    }
    | assemblyCommentStatement {
        $n = $assemblyCommentStatement.n;
    }
    | returnStatement {
        $n = $returnStatement.n;
    }
    | functionCallStatement {
        $n = $functionCallStatement.n;
    }
    | ifChain {
        $n = $ifChain.n;
    }
    | forLoopStatement {
        $n = $forLoopStatement.n;
    }
    | whileLoopStatement {
        $n = $whileLoopStatement.n;
    }
    | incrementStatement SEMI {
        $n = $incrementStatement.n;
    }
    ;

rhsValue
returns [AstExpression n] 
  : e=expr         { $n = $e.n; }
  | i=inputCall    { $n = $i.n; }
  ;

inputCall
returns [AstExpression n]
  : INPUT_KEYWORD LPAREN expr RPAREN {
        $n = new AstInput($expr.n);
    }
  | INPUT_KEYWORD LPAREN RPAREN {
        $n = new AstInput(new AstString(""));
    }
  ;

// Type parsing rule
type
returns [Type n]
    : arrayDecls+=arrayDeclToken* base=baseType {
        int dimensions = 0;
        // Count dimensions from array declaration tokens
        for (var decl : $arrayDecls) {
            if (decl != null && decl.text != null) {
                String text = decl.text;
                if ("full of".equals(text)) {
                    dimensions++;
                } else if ("extra".equals(text)) {
                    dimensions++;
                } else if ("super duper".equals(text)) {
                    dimensions++;
                } else if (text.startsWith("thic")) {
                    // Count 'c' characters: "thic" = 1, each 'c' adds 1
                    dimensions++; // "thic" itself is 1 dimension
                    int cCount = text.length() - 4; // Subtract "thic" length
                    dimensions += cCount;
                }
            }
        }
        // Build ArrayType using ArrayType.of() pattern
        Type result = $base.n;
        for (int i = 0; i < dimensions; i++) {
            result = ArrayType.of(result);
        }
        $n = result;
    }
    ;

arrayDeclToken
returns [String text]
    : ARRAY_DECL_FULL_OF { $text = $ARRAY_DECL_FULL_OF.text; }
    | ARRAY_DECL_EXTRA { $text = $ARRAY_DECL_EXTRA.text; }
    | ARRAY_DECL_SUPER_DUPER { $text = $ARRAY_DECL_SUPER_DUPER.text; }
    | ARRAY_DECL_THIC { $text = $ARRAY_DECL_THIC.text; }
    ;

baseType
returns [Type n]
    : INT_TYPE { $n = BuiltInType.INT; }
    | DOUBLE_TYPE { $n = BuiltInType.DOUBLE; }
    | STRING_TYPE { $n = BuiltInType.STRING; }
    | BOOLEAN_TYPE { $n = BuiltInType.BOOLEAN; }
    | id=IDENTIFIER {
        // Record type - will be resolved during semantic analysis
        // For now, we create a RecordType with empty fields map
        // Semantic analysis will replace this with the actual record definition
        $n = new RecordType($id.text, new java.util.HashMap<>());
    }
    ;

variableDeclarationStatement
returns [AstVariableDeclaration n]
    //int x = 2; int x = input("Hi");
    : t=type IDENTIFIER EQUALS rhs=rhsValue SEMI {
        $n = new AstVariableDeclaration($t.n, $IDENTIFIER.text, Optional.of($rhs.n));
    }
    //int x;
    | t=type IDENTIFIER SEMI {
        $n = new AstVariableDeclaration($t.n, $IDENTIFIER.text, Optional.empty());
    }
    ;

// Lvalue system - things that can appear on left side of assignment
// Structure: lvalue : ID | subscript | fieldAccess
// Uses iterative suffix approach to avoid left recursion
lvalue
returns [AstExpression n]
    : base=lvalueBase {
        $n = $base.n;
    } (ARRAY_INDEX_INTO idx=expr {
        $n = new AstArraySubscript($n, $idx.n);
    } | FIELD_OR_METHOD_ACCESS field=IDENTIFIER {
        $n = new AstFieldAccess($n, $field.text);
    })*
    ;

// Base for lvalues - things that can start an lvalue chain
lvalueBase
returns [AstExpression n]
    : IDENTIFIER {
        $n = new AstVariable($IDENTIFIER.text);
    }
    | '(' e=expr ')' {
        // Parenthesized expression can be an lvalue
        $n = $e.n;
    }
    ;

newExpr
returns [AstExpression n]
    : NEW t=type ARRAY_INDEX_INTO size=expr {
        // Array construction: OMG int basic 100
        $n = new AstArrayConstruction($t.n, $size.n);
    }
    | NEW id=IDENTIFIER LPAREN args=argumentsList RPAREN {
        // Record construction: OMG Point(1.2, 3.4)
        $n = new AstRecordConstruction($id.text, $args.n);
    }
    ;

variableAssignmentStatement
returns [AstVariableAssignment n]
    // x = 1; arr basic 5 = 1; point XO x = 1;
    : lv=lvalue EQUALS rhs=rhsValue SEMI {
        $n = new AstVariableAssignment($lv.n, $rhs.n);
    }
    ;

// Increment operation helper
incOp
returns [IncrementOp op]
    : ADD ADD { $op = IncrementOp.INCREMENT; }
    | SUB SUB { $op = IncrementOp.DECREMENT; }
    ;

// Prefix increment: ++x or --x
prefixInc
returns [AstIncrement n]
    : op=incOp lv=lvalue {
        $n = new AstIncrement($op.op, $lv.n, true);
    }
    ;

// Postfix increment: x++ or x--
postfixInc
returns [AstIncrement n]
    : lv=lvalue op=incOp {
        $n = new AstIncrement($op.op, $lv.n, false);
    }
    ;

incrementExpr
returns [AstIncrement n]
    // x++, --x (for use in expressions, no semicolon)
    : prefixInc  { $n = $prefixInc.n; }
    | postfixInc { $n = $postfixInc.n; }
    ;

incrementStatement
returns [AstIncrement n]
    // x++, --x; (prefix & postfix) - can be used with or without semicolon
    : incrementExpr { $n = $incrementExpr.n; }
    ;

// Print statements
printStatement
returns [AstPrint n]
    : PRINT_KEYWORD LPAREN args=printArgList RPAREN SEMI {
        $n = new AstPrint($args.n);
    }
    ;

printArgList
returns [List<AstPrintArgument> n]
    : args+=printArg (COMMA args+=printArg)* {
        $n = $args.stream()
            .map(node -> node.n)
            .collect(toList());
    }
    |  /* empty */ {
        $n = new ArrayList<>();
    }
    ;

printArg
returns [AstPrintArgument n]
    : e=expr {
        $n = $e.n;
    }
    | str=STRING_LITERAL {
        $n = new AstString($str.text);
    }
    ;
// Embedded statement: either a block or a single statement (wrapped in a block)
embeddedStatement
returns [AstBlock n]
    : blk=block {
        $n = $blk.n;
    }
    | stmt=statement {
        $n = new AstBlock(List.of($stmt.n));
    }
    ;

// If statements
ifChain
returns [AstIf n]
    //needs to have a seperate oldType of expression that always evaluates to a boolean
    : IF LPAREN cond=expr RPAREN thenPart=embeddedStatement tail=elseIfOrElse {
          // tail.n is Optional<AstBlock>
          $n = new AstIf($cond.n, $thenPart.n, $tail.n);
     }
    ;

// Else if statements
elseIfOrElse
returns [Optional<AstBlock> n]
    : ELSE_IF LPAREN cond=expr RPAREN thenBlk=block tail=elseIfOrElse {
        AstIf nested = new AstIf($cond.n, $thenBlk.n, $tail.n);
        AstBlock wrapper = new AstBlock(List.of(nested));
        $n = Optional.of(wrapper);
    }
    | ELSE blk=block {
        $n = Optional.of($blk.n);
    }
    | /* empty */ {
        $n = Optional.empty();
    }
    ;


// Grammar rules init & update inside for loops (without semicolons)
varDeclNoSemi
returns [AstVariableDeclaration n]
    : t=type IDENTIFIER EQUALS rhs=rhsValue {
        $n = new AstVariableDeclaration($t.n, $IDENTIFIER.text, Optional.of($rhs.n));
    }
    | t=type IDENTIFIER {
        $n = new AstVariableDeclaration($t.n, $IDENTIFIER.text, Optional.empty());
    }
    ;

varAssignNoSemi
returns [AstVariableAssignment n]
    : lv=lvalue EQUALS rhs=rhsValue {
        $n = new AstVariableAssignment($lv.n, $rhs.n);
    }
    ;

funcCallNoSemi
returns [AstFunctionCall n]
    : IDENTIFIER LPAREN args=argumentsList RPAREN {
        $n = new AstFunctionCall($IDENTIFIER.text, $args.n);
    }
    ;

// For loops
// init: decl | assign | function-call statement (but no trailing ';' here)
// update: ++/-- or assignment or function-call (no trailing ';' here)
forInit
returns [Optional<AstStatement> n]
    : v=varDeclNoSemi { $n = Optional.of($v.n); }
    | a=varAssignNoSemi { $n = Optional.of($a.n); }
    | f=funcCallNoSemi { $n = Optional.of($f.n); }
    | /* empty */ { $n = Optional.empty(); }
    ;

forCondition
returns [Optional<AstExpression> n]
    : e=expr { $n = Optional.of($e.n); }
    | /* empty */ { $n = Optional.empty(); }
    ;

forUpdate
returns [Optional<AstStatement> n]
    : i=incrementExpr { $n = Optional.of($i.n); }
    | a=varAssignNoSemi { $n = Optional.of($a.n); }
    | f=funcCallNoSemi { $n = Optional.of($f.n); }
    | /* empty */ { $n = Optional.empty(); }
    ;

// for ( init ; cond ; update ) block  (Java-style)
// for ( TYPE var = start to end by step ) block  (Pascal-style)
forLoopStatement
returns [AstStatement n]
    : FOR LPAREN init=forInit SEMI cond=forCondition SEMI upd=forUpdate RPAREN body=block {
        // Java-style: for ( init ; cond ; update )
        $n = new AstFor($init.n, $cond.n, $upd.n, $body.n);
    }
    | FOR LPAREN t=type id=IDENTIFIER EQUALS startVal=expr TO endVal=expr BY stepVal=expr RPAREN body=block {
        // Pascal-style: for ( TYPE var = startVal to endVal by stepVal )
        // Build AstPascalFor directly - desugaring happens in semantic analysis
        $n = new AstPascalFor(
            $t.n,
            $id.text,
            $startVal.n,
            $endVal.n,
            $stepVal.n,
            $body.n
        );
    }
    ;

 // While loops
 whileLoopStatement
 returns [AstWhile n]
    : WHILE LPAREN cond=expr RPAREN body=block {
        $n = new AstWhile($cond.n, $body.n);
    }
    ;

// Return statements
returnStatement
returns [AstReturn n]
    //period; period x;
    : RETURN expr SEMI {
        $n = new AstReturn(Optional.of($expr.n));
    }
    | RETURN SEMI {
        $n = new AstReturn(Optional.empty());
    }
    ;

// Block
block
returns [AstBlock n]
    : LBRACE stmts+=statement* RBRACE {
        $n = new AstBlock(
            $stmts.stream()
                .map(node -> node.n)
                .collect(toList())
        );
    }
    ;

// Assembly comments
assemblyCommentStatement
returns [AstAssembly n]
    : ASSEMBLY_COMMENT SEMI {
        // Remove the '///' prefix from the token text
        String commentText = $ASSEMBLY_COMMENT.text;
        if (commentText.startsWith("///")) {
            commentText = commentText.substring(3).trim();
        }
        $n = new AstAssembly(commentText);
    }
    ;

// Function Declaration
functionDeclarationStatement
returns [AstFunctionDeclaration n]
    //void f(int a, int b, int c) ()
    : VOID IDENTIFIER LPAREN params=parametersDeclarationList RPAREN body=block {
        $n = new AstFunctionDeclaration(
            ReturnType.VOID,
            $IDENTIFIER.text,
            $params.n,
            $body.n
        );
    }
    | t=type IDENTIFIER LPAREN params=parametersDeclarationList RPAREN body=block {
        $n = new AstFunctionDeclaration(
            $t.n,
            $IDENTIFIER.text,
            $params.n,
            $body.n
        );
    }
    ;

// Record Declaration
recordDeclaration
returns [AstRecordDeclaration n]
    // phat Point(double x, double y) { methods }
    : RECORD_DECL id=IDENTIFIER LPAREN fields=recordFieldList RPAREN LBRACE methods=recordBody RBRACE {
        java.util.Map<String, Type> fieldMap = new java.util.HashMap<>();
        for (var field : $fields.n) {
            fieldMap.put(field.name(), field.type());
        }
        $n = new AstRecordDeclaration($id.text, fieldMap, $methods.n);
    }
    ;

recordFieldList
returns [List<AstParameter> n]
    : fields+=recordField (COMMA fields+=recordField)* {
        $n = $fields.stream()
            .map(node -> node.n)
            .collect(toList());
    }
    | /* empty */ {
        $n = new ArrayList<>();
    }
    ;

recordField
returns [AstParameter n]
    : t=type id=IDENTIFIER {
        $n = new AstParameter($t.n, $id.text);
    }
    ;

recordBody
returns [List<AstFunctionDeclaration> n]
    : methods+=functionDeclarationStatement* {
        $n = $methods.stream()
            .map(node -> node.n)
            .collect(toList());
    }
    ;

parametersDeclarationList
returns [List<AstParameter> n]
    //list of parameters for function declaration
    : params+=parameterDeclaration (COMMA params+=parameterDeclaration)* {
        $n = $params.stream()
            .map(node -> node.n)
            .collect(toList());
    }
    | /* empty */ {
        $n = new ArrayList<>();
    }
    ;
parameterDeclaration
returns [AstParameter n]
    //its giving int x -- for function declaration
    : PARAMETER_PREAMBLE t=type IDENTIFIER {
        $n = new AstParameter($t.n, $IDENTIFIER.text);
    }
    ;


// Function calls
argumentsList
returns [List<AstExpression> n]
    //for function calls
    : exprs+=expr (COMMA exprs+=expr)* {
        $n = $exprs.stream()
            .map(node -> node.n)
            .collect(toList());
    }
    | /* empty */ {
        $n = new ArrayList<>();
    }
    ;

functionCallStatement
returns [AstStatement n]
    : IDENTIFIER LPAREN args=argumentsList RPAREN SEMI {
        $n = new AstFunctionCall($IDENTIFIER.text, $args.n);
    }
    ;

functionCallExpr
returns [AstFunctionCall n]
    : IDENTIFIER LPAREN args=argumentsList RPAREN {
        $n = new AstFunctionCall($IDENTIFIER.text, $args.n);
    }
    ;

// Expressions with layered precedence
// Precedence (lowest to highest): OR < AND < Comparisons < Additive < Multiplicative < Unary < Primary
expr
returns [AstExpression n]
    : orExpr {
        $n = $orExpr.n;
    }
    ;

orExpr
returns [AstExpression n]
    : l=orExpr OR r=andExpr {
        $n = new AstBinary(BinaryOp.OR, $l.n, $r.n);
    }
    | andExpr {
        $n = $andExpr.n;
    }
    ;

andExpr
returns [AstExpression n]
    : l=andExpr AND r=cmpExpr {
        $n = new AstBinary(BinaryOp.AND, $l.n, $r.n);
    }
    | cmpExpr {
        $n = $cmpExpr.n;
    }
    ;

cmpExpr
returns [AstExpression n]
    : l=addExpr (op=(EQ|NE|GE|GT|LE|LT) r=addExpr)? {
        if ($op == null) {
            $n = $l.n;
        } else {
            BinaryOp cmp;
            switch ($op.getType()) {
                case EQ: cmp = BinaryOp.EQ; break;
                case NE: cmp = BinaryOp.NE; break;
                case GE: cmp = BinaryOp.GE; break;
                case GT: cmp = BinaryOp.GT; break;
                case LE: cmp = BinaryOp.LE; break;
                case LT: cmp = BinaryOp.LT; break;
                default: throw new RuntimeException("Unknown comparison op: " + $op.getText());
            }
            $n = new AstBinary(cmp, $l.n, $r.n);
        }
    }
    ;

addExpr
returns [AstExpression n]
    : l=addExpr op=('+' | '-' | CONCAT) r=mulExpr {
        BinaryOp binOp;
        if("+".equals($op.text))
            binOp = BinaryOp.ADD;
        else if("-".equals($op.text))
            binOp = BinaryOp.SUB;
        else
            binOp = BinaryOp.CONCAT;
        $n = new AstBinary(binOp, $l.n, $r.n);
    }
    | mulExpr {
        $n = $mulExpr.n;
    }
    ;

mulExpr
returns [AstExpression n]
    : l=mulExpr op=('*' | '/' | '%') r=unaryExpr {
        BinaryOp binOp;
        if("*".equals($op.text))
            binOp = BinaryOp.MUL;
        else if("%".equals($op.text))
            binOp = BinaryOp.MOD;
        else
            binOp = BinaryOp.DIV;
        $n = new AstBinary(binOp, $l.n, $r.n);
    }
    | unaryExpr {
        $n = $unaryExpr.n;
    }
    ;

unaryExpr
returns [AstExpression n]
    : prefixInc { $n = $prefixInc.n; }
    | '-' x=unaryExpr {
        $n = new AstUnary(UnaryOp.NEGATE, $x.n);
    }
    | NOT x=unaryExpr {
        $n = new AstUnary(UnaryOp.NOT, $x.n);
    }
    | primary {
        $n = $primary.n;
    }
    ;

primary
returns [AstExpression n]
    : base=primaryBase {
        $n = $base.n;
    } (ARRAY_INDEX_INTO idx=expr {
        $n = new AstArraySubscript($n, $idx.n);
    } | FIELD_OR_METHOD_ACCESS field=IDENTIFIER LPAREN args=argumentsList RPAREN {
        // Method call: expr.field(args)
        $n = new AstMethodCall($n, $field.text, $args.n);
    } | FIELD_OR_METHOD_ACCESS field=IDENTIFIER {
        // Field access: expr.field (no parentheses)
        $n = new AstFieldAccess($n, $field.text);
    })*
    ;

primaryBase
returns [AstExpression n]
    : '(' t=type ')' ue=unaryExpr {
        $n = new AstCast($t.n, $ue.n);
    }
    | functionCallExpr {
        $n = $functionCallExpr.n;
    }
    | '(' e=expr ')' {
        $n = $e.n;
    }
    | postfixInc {
        $n = $postfixInc.n;
    }
    | THIS {
        $n = new AstVariable("this");
    }
    | newExpr {
        $n = $newExpr.n;
    }
    | var=IDENTIFIER {
        $n = new AstVariable($var.text);
    }
    | INT {
        $n = new AstInt(Integer.parseInt($INT.text));
    }
    | DOUBLE {
        $n = new AstDouble(Double.parseDouble($DOUBLE.text));
    }
    | BOOLEAN {
        $n = new AstBoolean(Boolean.parseBoolean($BOOLEAN.text));
    }
    | str=STRING_LITERAL {
        $n = new AstString($str.text);
    }
    ;