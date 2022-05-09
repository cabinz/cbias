lexer grammar LexTokens;

/*
Integer constants
*/
IntConst
    : DecIntConst
    | OctIntConst
    | HexIntConst
    ;

fragment
DecIntConst
    : [1-9] DigitSeq
    ;

fragment
OctIntConst
    : '0'
    | [0-7]+
    ;

fragment
HexIntConst
    : ('0x'|'0X') HexDigitSeq
    ;

/*
Float constants
*/
FloatConst
    : DecFloatConst
    | HexFloatConst
    ;

fragment
DecFloatConst
    : FracConst Exp? FloatSuffix?
    | DigitSeq Exp FloatSuffix?
    ;

fragment
HexFloatConst
    : ('0x'|'0X') (HexDigitSeq|HexFracConst) BinaryExp FloatSuffix?
    ;

fragment
FracConst
    : DigitSeq? '.' DigitSeq
    | DigitSeq '.'
    ;

fragment
Exp
    : [eE] Sign? DigitSeq
    ;

fragment
Sign
    : [+-]
    ;

fragment
DigitSeq
    : [0-9]+
    ;

fragment
HexFracConst
    : HexDigitSeq? '.' HexDigitSeq
    | HexDigitSeq '.'
    ;

fragment
BinaryExp
    : [pP] Sign? DigitSeq
    ;

fragment
HexDigitSeq
    : [0-9a-fA-F]+
    ;

fragment
FloatSuffix
    : [flFL]
    ;

Identifier
    : [a-zA-Z_][a-zA-Z_0-9]*
    ;


/*
Others
*/

// String is for the 1st parementer of the putf() in SysY runtime
// even if it doesn't exist in the given grammar documentation.
STRING
    : '"' (ESC | .)*? '"'
    ;

fragment
ESC
    : '\\' ["\\]
    ;

// "-> skip" will make the lexer/parser automatically skip all
// the content recognized as corresponding tokens

WS
    : [ \t\r\n] -> skip
    ;

LINE_COMMENT
    : '//' .*? '\r'? '\n' -> skip
    ;

COMMENT
    : '/*'.*?'*/' -> skip
    ;