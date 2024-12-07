%option c++ prefix="og_scanner_" outfile="og_scanner.cpp"
%option stack noyywrap yylineno 8bit
%{ 
// make relevant includes before including the parser's tab file
#include <string>
#include <cdk/ast/sequence_node.h>
#include <cdk/ast/expression_node.h>
#include <cdk/ast/lvalue_node.h>
#include "og_parser.tab.h"

// don't change this
#define yyerror LexerError
%}

SYMBOL                  [,;()[\]<>=+\-?*/%!|&@{}]
IDENTIFIER              [A-Za-z]([A-Za-z]|"_"|[0-9])*
INT                     [0-9]+
HEX_INT                 0x[0-9a-fA-F]+
REAL                    ([0-9]*\.[0-9]+|[0-9]+\.[0-9]*)([Ee]([-+])?[0-9]+)?|([0-9]+[Ee]([-+])?[0-9]+)

%x X_COMMENT X_STRING X_ESCAPE X_IGNORE
%%

"int"                   return tINT_TYPE;
"real"                  return tREAL_TYPE;
"string"                return tSTRING_TYPE;
"auto"                  return tAUTO_TYPE;
"ptr"                   return tPTR_TYPE;
"public"                return tPUBLIC;
"require"               return tREQUIRE;
"sizeof"                return tSIZEOF;
"input"                 return tINPUT;
"nullptr"               return tNULLPTR;
"procedure"             return tPROCEDURE;
"break"                 return tBREAK;
"continue"              return tCONTINUE;
"return"                return tRETURN;
"if"                    return tIF;
"then"                  return tTHEN;
"elif"                  return tELIF;
"else"                  return tELSE;
"for"                   return tFOR;
"do"                    return tDO;
"write"                 return tWRITE;
"writeln"               return tWRITELN;

"<="                    return tLE;
">="                    return tGE;
"!="                    return tNE;
"=="                    return tEQ;
"&&"                    return tAND;
"||"                    return tOR;

{SYMBOL}                return *yytext;

{IDENTIFIER}            yylval.s = new std::string(yytext); return tIDENTIFIER;
{INT}                   try { yylval.i = std::stoul(yytext, nullptr, 10); } catch (const std::out_of_range& e) { yyerror(e.what()); } return tINTEGER;
{HEX_INT}               try { yylval.i = std::stoul(yytext, nullptr, 16); } catch (const std::out_of_range& e) { yyerror(e.what()); } return tINTEGER;
{REAL}                  try { yylval.d = std::stod(yytext, nullptr); } catch (const std::out_of_range& e) { yyerror(e.what()); } return tREAL;

"//".*\n                ; /* ignore comments */
"/*"                    yy_push_state(X_COMMENT);
<X_COMMENT>"/*"         yy_push_state(X_COMMENT);
<X_COMMENT>"*/"         yy_pop_state();
<X_COMMENT>.|\n         ; /* ignore content */

\"                      yy_push_state(X_STRING); yylval.s = new std::string("");
<X_STRING>\"            yy_pop_state(); return tSTRING;
<X_STRING>\\            yy_push_state(X_ESCAPE);
<X_STRING>.             *yylval.s += yytext;

<X_ESCAPE>n             yy_pop_state(); *yylval.s += '\n';
<X_ESCAPE>r             yy_pop_state(); *yylval.s += '\r';
<X_ESCAPE>t             yy_pop_state(); *yylval.s += '\t';
<X_ESCAPE>\"            yy_pop_state(); *yylval.s += '"';
<X_ESCAPE>\\            yy_pop_state(); *yylval.s += '\\';
<X_ESCAPE>0             yy_pop_state(); yy_pop_state(); yy_push_state(X_IGNORE);
<X_ESCAPE>[0-9a-fA-F]{1,2} yy_pop_state(); *yylval.s += std::string(1, (char) std::stoi(yytext, nullptr, 16));

<X_IGNORE>\"            yy_pop_state(); /* stop when end of string is reached */
<X_IGNORE>.|\n          ; /* ignore everything */

[ \t\n\r]+              ; /* ignore whitespace */

.                       yyerror("Unknown character");

%%
