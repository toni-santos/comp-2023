grammar Javamm;

@header {
    package pt.up.fe.comp2023;
}

INT : [0-9]+ ;
ID : [a-zA-Z][a-zA-Z_$0-9]* ;

WS : [ \t\n\r\f]+ -> skip ;
COMMENT_ALL : '/*' [.]* '*/' -> skip;
COMMENT_LINE : '//' [.]* [\n]-> skip;

program
    : (importDeclaration)* classDeclariation EOF
    ;

importDeclaration
    : 'import' ID ('.'ID)*';'
    ;

classDeclariation
    : 'class' ID ('extends' ID)? '{' (varDeclaration)* (methodDeclaration)* '}'
    ;

varDeclaration
    : type ID ';'
    ;

methodDeclaration
    : ('public')? type ID '(' ( type ID ( ',' type ID )* )? ')' '{' (varDeclaration)* (statement)* 'return' expression ';' '}'
    | ('public')? 'static' 'void' 'main' '(' type '[' ']' ID ')' '{' (varDeclaration)* (statement)* '}' //TODO: Enforce type String later
    ;

type
    : 'int' '[' ']' #IntArray
    | 'boolean' #Boolean
    | 'int' #Integer
    | ID #Object
    ;

statement
    : '{' (statement)* '}' #Brackets
    | 'if' '(' expression ')' statement 'else' statement #IfElse
    | 'while' '(' expression ')' statement #While
    | expression ';' #RegularStatement
    | type ID '=' expression ';' #DeclarationStatement
    | ID '['expression']' '=' expression ';' #ArrayStatement
    ;

expression
    : '(' expression ')' #Parenthesis
    | expression '[' expression ']' #ArraySubscript
    | expression '.' ID '(' ( expression ( ',' expression )* )? ')' #MemberSelection
    | expression '.' 'length' #MemberSelection
    | '!' expression #UnaryOp
    | expression op=('*' | '/') expression #BinaryOp
    | expression op=('+' | '-') expression #BinaryOp
    | expression '<' expression #BinaryOp
    | expression '&&' expression #BinaryOp
    | 'new' 'int' '[' expression ']' #NewArray
    | 'new' ID '(' ')' #NewObject
    | INT #IntValue
    | 'true' #BooleanValue
    | 'false'#BooleanValue
    | ID #Identifier
    | 'this' #Isto
    ;
