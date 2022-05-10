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
    : Identifier ('[' constExp ']')*              # varDefUninit
    | Identifier ('[' constExp ']')* '=' initVal  # varDefInit
    ;

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
    : unaryExp                           # mul1
    | mulExp ('*' | '/' | '%') unaryExp  # mul2
    ;

addExp
    : mulExp                     # add1
    | addExp ('+' | '-') mulExp  # add2
    ;

relExp
    : addExp                                   # rel1
    | relExp ('<' | '>' | '<=' | '>=') addExp  # rel2
    ;

eqExp
    : relExp                      # eq1
    | eqExp ('==' | '!=') relExp  # eq2
    ;

lAndExp
    : eqExp               # lAnd1
    | lAndExp '&&' eqExp  # lAnd2
    ;

lOrExp
    : lAndExp              # lOr1
    | lOrExp '||' lAndExp  # lOr2
    ;

constExp
    : addExp
    ;