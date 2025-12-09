lexer grammar VisuallyBasicLexer;

/** Lexer Rules **/

// Keywords
PRINT_KEYWORD : 'spill';
INPUT_KEYWORD : 'fetch';
PARAMETER_PREAMBLE : 'its giving';
EQUALS : 'istotes';
LIKE : 'like' -> skip;
PI : 'pi';
CONCAT : 'um'[m]*; //string concat
RETURN : 'period'; //return statement
FOR : 'fasho'; //for loop statement
WHILE : 'bet'; //while loop statment
TO : 'to'; //Pascal-style for loop: for (var = start to end by step)
BY : 'by'; //Pascal-style for loop step
//if statments
IF : 'if';
ELSE_IF : 'as if';
ELSE : 'whateva';

// Array and record keywords
ARRAY_DECL_FULL_OF : 'full of';
ARRAY_DECL_EXTRA : 'extra';
ARRAY_DECL_SUPER_DUPER : 'super duper';
ARRAY_DECL_THIC : 'thic'[c]*; // "thic" = 1D, each 'c' adds a dimension
ARRAY_INDEX_INTO : 'basic';
RECORD_DECL : 'phat';
FIELD_OR_METHOD_ACCESS : 'XO'[XO]*; // One or more XO tokens
NEW : 'OMG';
THIS : 'this';

//Types
VOID: 'slay';
INT_TYPE: 'int';
DOUBLE_TYPE: 'double';
STRING_TYPE: 'string';
BOOLEAN_TYPE: 'boolean';

NOT : 'not';
AND : 'and';
OR : 'or';
EQ : 'vibes with';
GE : 'better vibe';
GT : 'good vibe';
LE : 'bad vibe';
LT : 'worse vibe';
NE : 'not the vibe';

// Literals
BOOLEAN : 'true' | 'false';
INT : [0-9]+;
DOUBLE
    : [0-9]+ '.' [0-9]*
    | [0-9]* '.' [0-9]+
    ;
STRING_LITERAL : '"' ( ~'"' )* '"'; // Matches any string enclosed in double quotes
IDENTIFIER : [a-zA-Z_][a-zA-Z_0-9]*; // Matches any variable name

// Whitespace and comments (must come before operators to match ** before *)
LINE_COMMENT : (':)' | '<3') ~[\r\n]* -> skip;
BLOCK_COMMENT : '**' .*? '**' -> skip;
ASSEMBLY_COMMENT : '///' ~[;\r\n]*; // Captures '///' followed by any characters until semicolon or newline
WHITESPACE : [\t\r\n ]+ -> skip;

// Operators
ADD      : '+';
SUB      : '-';
MUL      : '*';
DIV      : '/';
MOD      : '%';

// Separators
LBRACE : '{';
RBRACE : '}';
LPAREN : '(';
RPAREN : ')';
COMMA : ',';
SEMI : ';';
