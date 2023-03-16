grammar Javamm;

@header {
    package pt.up.fe.comp2023;
}

MULT_COMMENT_START : '/*';
MULT_COMMENT_END : '*/';
COMMENT : '//';

COMMENT_ALL : MULT_COMMENT_START .*? MULT_COMMENT_END -> skip;
COMMENT_LINE : COMMENT ~[\r\n]* -> skip;

IMPORT : 'import';
SEMICOLON : ';';
DOT : '.';
CLASS : 'class';
EXTENDS : 'extends';
CURLY_LEFT : '{';
CURLY_RIGHT : '}';
BRACKET_LEFT : '(';
BRACKET_RIGHT : ')';
SQUARE_LEFT : '[';
SQUARE_RIGHT : ']';
PUBLIC : 'public';
STATIC : 'static';
MAIN : 'main';
COLON : ',';
RETURN : 'return';
BOOLEAN : 'boolean';
INTEGER : 'int';
VOID : 'void';
IF : 'if';
ELSE : 'else';
WHILE : 'while';
LENGTH : 'length';
NEGATE : '!';
EQUALS : '=';
TIMES : '*';
DIVIDE : '/';
PLUS : '+';
MINUS : '-';
LESS_THAN : '<';
AND : '&&';
NEW : 'new';
BOOL : ('true' | 'false');
THIS : 'this';


INT : ([0]|[1-9][0-9]*) ;
ID : [a-zA-Z$_][a-zA-Z_$0-9]* ;

WS : [ \t\n\r\f]+ -> skip ;

ANYCHAR : .;

program
    : (importDeclaration)* classDeclaration EOF
    ;

importDeclaration
    : IMPORT importName (DOT importName)* SEMICOLON #Import
    ;

importName
    : value=ID
    ;

classDeclaration
    : CLASS className=ID (EXTENDS extendName=ID)? CURLY_LEFT (varDeclaration)* (methodDeclaration)* CURLY_RIGHT #Class
    ;

varDeclaration
    : type value=ID SEMICOLON
    ;

methodDeclaration
    : (PUBLIC)? type methodName=ID BRACKET_LEFT ( methodParam ( COLON methodParam )* )? BRACKET_RIGHT CURLY_LEFT (varDeclaration)* (statement)* returnStatement CURLY_RIGHT
    | (PUBLIC)? STATIC type methodName=MAIN BRACKET_LEFT methodParam BRACKET_RIGHT CURLY_LEFT (varDeclaration)* (statement)* CURLY_RIGHT
    ;

methodParam
    : type name=ID
    ;

returnStatement
    : RETURN expression SEMICOLON
    ;

type
    : array=INTEGER SQUARE_LEFT SQUARE_RIGHT
    | value=INTEGER
    | value=BOOLEAN
    | value=VOID
    | array=ID SQUARE_LEFT SQUARE_RIGHT
    | value=ID
    ;

statement
    : CURLY_LEFT (statement)* CURLY_RIGHT #Brackets
    | IF BRACKET_LEFT expression BRACKET_RIGHT statement ELSE statement #IfElse
    | WHILE BRACKET_LEFT expression BRACKET_RIGHT statement #While
    | expression SEMICOLON #RegularStatement
    | type EQUALS expression SEMICOLON #DeclarationStatement
    | value=ID SQUARE_LEFT expression SQUARE_RIGHT EQUALS expression SEMICOLON #ArrayStatement
    ;

expression
    : BRACKET_LEFT expression BRACKET_RIGHT #Parenthesis
    | expression SQUARE_LEFT expression SQUARE_RIGHT #ArraySubscript
    | expression DOT value=ID BRACKET_LEFT ( expression ( COLON expression )* )? BRACKET_RIGHT #MethodCall
    | expression DOT LENGTH #Length
    | NEGATE expression #UnaryOp
    | expression op=(TIMES | DIVIDE) expression #BinaryOp
    | expression op=(PLUS | MINUS) expression #BinaryOp
    | expression op=LESS_THAN expression #BinaryOp
    | expression op=AND expression #BinaryOp
    | NEW INTEGER SQUARE_LEFT expression SQUARE_RIGHT #NewArray
    | NEW value=ID BRACKET_LEFT BRACKET_RIGHT #NewObject
    | value=INT #IntValue
    | value=BOOL #BooleanValue
    | value=ID #Identifier
    | THIS #This
    ;
