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
    : Identifier '=' constInitVal                       # scalarConstDef
    | Identifier ('[' constExp ']')+ '=' constInitVal   # arrConstDef
    ; // Separate the cases of scalar and array

constInitVal
    : constExp                                      # scalarConstInitVal
    | '{' (constInitVal (',' constInitVal)* )? '}'  # arrConstInitVal
    ;

varDecl
    : bType varDef (',' varDef)* ';'
    ;

varDef
    : Identifier ('=' initVal)?                         # scalarVarDef
    | Identifier ('[' constExp ']')+ ('=' initVal)?     # arrVarDef
    ; // Essentially, varDef with initVal is "varDef without initVal (Alloca) + Initialization (Store)"
    // Separate the cases of scalar and array

initVal
    : expr                                # scalarInitVal
    | '{' (initVal (',' initVal)* )? '}'  # arrInitval
    ; // Separate the cases of scalar and array

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
    : bType Identifier                          # scalarFuncFParam
    | bType Identifier '[' ']' ('[' expr ']')*  # arrFuncFParam
    ; // Separate the cases of scalar and array

block
    : '{' (blockItem)* '}'
    ;

blockItem
    : decl
    | stmt
    ;

stmt
    : lVal '=' expr ';'                   # assignStmt
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
    : Identifier                    # scalarLVal
    | Identifier ('[' expr ']')+    # arrLVal
    ; // Separate the cases of scalar and array

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
    : primaryExp                         # primUnaryExp
    | Identifier '(' (funcRParams)? ')'  # fcallUnaryExp
    | unaryOp unaryExp                   # oprUnaryExp
    ;

unaryOp
    : '+'
    | '-'
    | '!'
    ;

funcRParams
    : funcRParam (',' funcRParam)*
    ;

funcRParam
    : expr    # exprRParam
    | STRING  # strRParam
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