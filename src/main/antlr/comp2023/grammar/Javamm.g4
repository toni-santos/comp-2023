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
    : 'import' importName ('.'importName)*';' #Import
    ;

importName
    : value=ID
    ;

classDeclaration
    : 'class' className ('extends' extendName)? '{' (varDeclaration)* (methodDeclaration)* '}' #Class
    ;

className
    : value=ID
    ;

extendName
    : value=ID
    ;

varDeclaration
    : type value=ID ';'
    ;

methodDeclaration
    : scope (mod)? type methodName '(' ( methodParam ( ',' methodParam )* )? ')' '{' (varDeclaration)* (statement)* (returnStatement)? '}' #GenericMethod
    | scope (mod)? type 'main' '(' methodParam ')' '{' (varDeclaration)* (statement)* '}' #MainMethod //TODO: Enforce type String later
    ;

methodParam
    : type methodParamName
    ;

returnStatement
    : 'return' expression ';'
    ;

methodName
    : value=ID
    ;

methodParamName
    : value=ID
    ;

scope
    : 'public'
    | 'private'
    ;

mod
    : 'static'
    ;

type returns [Boolean isArray]
    : 'int' '[' ']'{$isArray=true}
    | 'boolean'
    | 'int'
    | 'void'
    | value=ID
    | value=ID '[' ']'{$isArray=true}
    ;

statement
    : '{' (statement)* '}' #Brackets
    | 'if' '(' expression ')' statement 'else' statement #IfElse
    | 'while' '(' expression ')' statement #While
    | expression ';' #RegularStatement
    | type+ '=' expression ';' #DeclarationStatement
    | value=ID '['expression']' '=' expression ';' #ArrayStatement
    ;

expression
    : '(' expression ')' #Parenthesis
    | expression '[' expression ']' #ArraySubscript
    | expression '.' value=ID '(' ( expression ( ',' expression )* )? ')' #MemberSelection
    | expression '.' 'length' #MemberSelection
    | '!' expression #UnaryOp
    | expression op=('*' | '/') expression #BinaryOp
    | expression op=('+' | '-') expression #BinaryOp
    | expression op='<' expression #BinaryOp
    | expression op='&&' expression #BinaryOp
    | 'new' 'int' '[' expression ']' #NewArray
    | 'new' value=ID '(' ')' #NewObject
    | value=INT #IntValue
    | value=('true' | 'false') #BooleanValue
    | value=ID #Identifier
    | 'this' #This
    ;
