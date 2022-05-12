grammar SysY;

import LexTokens;

compUnit
    : (decl | funcDef)* EOF
    ;

decl
    : constDecl
    | varDecl
    ;

constDecl
    : 'const' bType constDef (',' constDef)* ';'
    ;

bType
    : 'int'
    | 'float'
    ;

constDef
    : Identifier ('[' constExp ']')* '=' constInitVal
    ;

constInitVal
    : constExp                                      # scalarConstInitVal
    | '{' (constInitVal (',' constInitVal)* )? '}'  # listConstInitVal
    ;

varDecl
    : bType varDef (',' varDef)* ';'
    ;

varDef
    : Identifier ('[' constExp ']')* ('=' initVal)?
    ; // Essentially, varDef with initVal is "varDef without initVal (Alloca) + Initialization (Store)"

initVal
    : expr                                # scalarInitVal
    | '{' (initVal (',' initVal)* )? '}'  # listInitval
    ;

funcDef
    : funcType Identifier '(' (funcFParams)? ')' block
    ;

funcType
    : 'void'
    | 'int'
    | 'float'
    ;

funcFParams
    : funcFParam (',' funcFParam)*
    ;

funcFParam
    : bType Identifier ('[' ']' ('[' expr ']')* )?
    ;

block
    : '{' (blockItem)* '}'
    ;

blockItem
    : decl
    | stmt
    ;

stmt
    : lVal '=' expr ';'                   # assignment
    | (expr)? ';'                         # exprStmt
    | block                               # blkStmt
    | 'if' '(' cond ')' stmt              # ifStmt
    | 'if' '(' cond ')' stmt 'else' stmt  # ifElseStmt
    | 'while' '(' cond ')' stmt           # whileStmt
    | 'break' ';'                         # breakStmt
    | 'continue' ';'                      # contStmt
    | 'return' (expr)? ';'                # retStmt
    ;

expr
    : addExp
    ;

cond
    : lOrExp
    ;

lVal
    : Identifier ('[' expr ']')*
    ;

primaryExp
    : '(' expr ')'  # primExpr1
    | lVal          # primExpr2
    | number        # primExpr3
    ;

number
    : intConst
    | floatConst
    ;

intConst
    : DecIntConst
    | OctIntConst
    | HexIntConst
    ;

floatConst
    : DecFloatConst
    | HexFloatConst
    ;

unaryExp
    : primaryExp                         # unary1
    | Identifier '(' (funcRParams)? ')'  # unary2
    | unaryOp unaryExp                   # unary3
    ;

unaryOp
    : '+'
    | '-'
    | '!'
    ;

funcRParams
    : funcRParam (',' funcRParam)*;

funcRParam
    : expr    # expAsRParam
    | STRING  # stringAsRParam
    ;

mulExp
    : unaryExp (('*' | '/' | '%') unaryExp)*
    ; // Eliminate left recursion.

addExp
    : mulExp (('+' | '-') mulExp)*
    ; // Eliminate left recursion.

relExp
    : addExp (('<' | '>' | '<=' | '>=') addExp)*
    ; // Eliminate left recursion.

eqExp
    : relExp *(('==' | '!=') relExp)*
    ; // Eliminate left recursion.

lAndExp
    : eqExp ('&&' eqExp)*
    ; // Eliminate left recursion.

lOrExp
    : lAndExp ('||' lAndExp)*
    ; // Eliminate left recursion.

constExp
    : addExp
    ;