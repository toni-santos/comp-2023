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
    : (importDeclaration)* classDeclaration EOF
    ;

importDeclaration
    : 'import' name=ID ('.'name=ID)*';' #Import
    ;

classDeclaration
    : 'class' ID ('extends' ID)? '{' (varDeclaration)* (methodDeclaration)* '}' #Class
    ;

varDeclaration
    : type ID ';' #Var
    ;

methodDeclaration
    : ('public')? type ID '(' ( type ID ( ',' type ID )* )? ')' '{' (varDeclaration)* (statement)* 'return' expression ';' '}' #GenericMethod
    | ('public')? 'static' 'void' 'main' '(' type '[' ']' ID ')' '{' (varDeclaration)* (statement)* '}' #MainMethod //TODO: Enforce type String later
    ;

type
    : 'int' '[' ']' #IntArray
    | 'boolean' #Boolean
    | 'int' #Integer
    | value=ID #Object
    ;

statement
    : '{' (statement)* '}' #Brackets
    | 'if' '(' expression ')' statement 'else' statement #IfElse
    | 'while' '(' expression ')' statement #While
    | expression ';' #RegularStatement
    | type+ '=' expression ';' #DeclarationStatement
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
    | expression op='<' expression #BinaryOp
    | expression op='&&' expression #BinaryOp
    | 'new' 'int' '[' expression ']' #NewArray
    | 'new' ID '(' ')' #NewObject
    | value=INT #IntValue
    | value=('true' | 'false') #BooleanValue
    | value=ID #Identifier
    | 'this' #This
    ;
