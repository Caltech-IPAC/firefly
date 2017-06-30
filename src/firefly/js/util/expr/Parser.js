// Operator-precedence parser.
// Copyright 2002 by Darius Bacon <darius@wry.me>
// Altered, added parsedVariables

import * as SyntaxException from './SyntaxException.js';
import {makeVariable} from './Variable.js';
import * as Token from './Token.js';
import * as Expr from './Expr.js';
import {Scanner} from './Scanner.js';

// Built-in constants
const pi = makeVariable('pi');
pi.setValue(Math.PI);

const operatorChars = '*/+-^<>=,()';


const procs1 = [
        'abs', 'acos', 'asin', 'atan',
        'ceil', 'cos', 'exp', 'floor',
        'log', 'ln', 'lg', 'log10', 'round', 'sin', 'sqrt',
        'tan'
];

const rators1 = [
        Expr.ABS, Expr.ACOS, Expr.ASIN, Expr.ATAN,
        Expr.CEIL, Expr.COS, Expr.EXP, Expr.FLOOR,
        Expr.LOG, Expr.LN, Expr.LG, Expr.LOG10, Expr.ROUND, Expr.SIN, Expr.SQRT,
        Expr.TAN
];

const procs2 = [
    'atan2', 'max', 'min'
];

const rators2 = [
        Expr.ATAN2, Expr.MAX, Expr.MIN
];

/**
 Parses strings representing mathematical formulas with variables.
 The following operators, in descending order of precedence, are
 defined:

 <UL>
 <LI>^ (raise to a power)
 <LI>* /
 <LI>Unary minus (-x)
 <LI>+ -
 <LI>&lt; &lt;= = &lt;&gt; &gt;= &gt;
 <LI>and
 <LI>or
 </UL>

 ^ associates right-to-left; other operators associate left-to-right.

 <P>These unary functions are defined:
 abs, acos, asin, atan,
 ceil, cos, exp, floor,
 log, round, sin, sqrt,
 tan.  Each requires one argument enclosed in parentheses.

 <P>There are also binary functions: atan2, min, max; and a ternary
 conditional function: if(test, then, else).

 <P>Whitespace outside identifiers is ignored.

 <P>Examples:
 <UL>
 <LI>42
 <LI>2-3
 <LI>cos(x^2) + sin(x^2)
 <UL>
 */
 export class Parser {

    constructor() {
        // Set of Variable's that are allowed to appear in input expressions.
        // If null, any variable is allowed. */
        this.allowedVariables = null;
        // Set of parsed variables
        this.parsedVariables = null;
        //Scanner
        this.tokens = null;
        //token
        this.token = null;
    }

    /** Return the expression denoted by the input string.
     *
     *  @param input the unparsed expression
     *  @exception SyntaxException if the input is unparsable */
    static parse(input) {
        return new Parser().parseString(input);
    }


    /** Adjust the set of allowed variables: create it (if not yet
     * existent) and add optVariable (if it's nonnull).  If the
     * allowed-variable set exists, the parser will reject input
     * strings that use any other variables.
     *
     * @param optVariable the variable to be allowed, or null */
    allow(optVariable) {
        if (!optVariable) {
            this.allowedVariables = null;
            return;
        } else if (!this.allowedVariables) {
            this.allowedVariables = new Set();
            this.allowedVariables.add(pi);
        }
        this.allowedVariables.add(optVariable);
    }

    getParsedVariables() {
        return this.parsedVariables;
    }

     /*
     * @param {Variable} v - variable
      */
    addParsedVariable(v) {
        if (!this.parsedVariables) {
            this.parsedVariables = new Map();
        }
        this.parsedVariables.set(v.toString(), v);
    }



    /** Return the expression denoted by the input string.
     *  @param {string} input the unparsed expression
     *  @return expression
     *  @exception SyntaxException if the input is unparsable */
    parseString(input) {
        this.tokens = new Scanner(input, operatorChars);
        return this.reparse();
    }



    reparse() {
        this.tokens.index = -1;
        this.nextToken();
        const expr = this.parseExpr(0);
        if (this.token.ttype != Token.TT_EOF) {
            throw this.error('Incomplete expression',
                SyntaxException.INCOMPLETE);
        }
        return expr;
    }

    nextToken() {
        this.token = this.tokens.nextToken();
    }

    parseExpr(precedence) {
        let expr = this.parseFactor();
        loop1:
        for (;;) {
            var l, r, rator;

            // The operator precedence table.
            // l = left precedence, r = right precedence, rator = operator.
            // Higher precedence values mean tighter binding of arguments.
            // To associate left-to-right, let r = l+1;
            // to associate right-to-left, let r = l.

            switch (this.token.ttype) {
                case '<':         l = 20; r = 21; rator = Expr.LT; break;
                case Token.TT_LE: l = 20; r = 21; rator = Expr.LE; break;
                case '=':         l = 20; r = 21; rator = Expr.EQ; break;
                case Token.TT_NE: l = 20; r = 21; rator = Expr.NE; break;
                case Token.TT_GE: l = 20; r = 21; rator = Expr.GE; break;
                case '>':         l = 20; r = 21; rator = Expr.GT; break;

                case '+': l = 30; r = 31; rator = Expr.ADD; break;
                case '-': l = 30; r = 31; rator = Expr.SUB; break;

                case '/': l = 40; r = 41; rator = Expr.DIV; break;
                case '*': l = 40; r = 41; rator = Expr.MUL; break;

                case '^': l = 50; r = 50; rator = Expr.POW; break;

                default:
                    if (this.token.ttype === Token.TT_WORD && this.token.sval==='and') {
                        l = 5; r = 6; rator = Expr.AND; break;
                    }
                    if (this.token.ttype === Token.TT_WORD && this.token.sval==='or') {
                        l = 10; r = 11; rator = Expr.OR; break;
                    }
                    break loop1;
            }

            if (l < precedence) {
                break;
            }

            this.nextToken();
            expr = Expr.makeApp2(rator, expr, this.parseExpr(r));
        }

        return expr;
    }


    parseFactor() {
        var i;
        switch (this.token.ttype) {
            case Token.TT_NUMBER: {
                const lit = Expr.makeLiteral(this.token.nval);
                this.nextToken();
                return lit;
            }
            case Token.TT_WORD: {
                for (i = 0; i < procs1.length; ++i) {
                    if (procs1[i]===this.token.sval) {
                        this.nextToken();
                        this.expect('(');
                        const rand = this.parseExpr(0);
                        this.expect(')');
                        return Expr.makeApp1(rators1[i], rand);
                    }
                }
                for (i = 0; i < procs2.length; ++i) {
                    if (procs2[i]===this.token.sval) {
                        this.nextToken();
                        this.expect('(');
                        const rand1 = this.parseExpr(0);
                        this.expect(',');
                        const rand2 = this.parseExpr(0);
                        this.expect(')');
                        return Expr.makeApp2(rators2[i], rand1, rand2);
                    }
                }
                if (this.token.sval === 'if') {
                    this.nextToken();
                    this.expect('(');
                    const test = this.parseExpr(0);
                    this.expect(',');
                    const consequent = this.parseExpr(0);
                    this.expect(',');
                    const alternative = this.parseExpr(0);
                    this.expect(')');
                    return Expr.makeIfThenElse(test, consequent, alternative);
                }

                const v = makeVariable(this.token.sval);
                if (this.allowedVariables && !this.allowedVariables.has(v)) {
                    throw this.error('Unknown variable',
                        SyntaxException.UNKNOWN_VARIABLE);
                }
                this.addParsedVariable(v);
                this.nextToken();
                return v;
            }
            case '(': {
                this.nextToken();
                const enclosed = this.parseExpr(0);
                this.expect(')');
                return enclosed;
            }
            case '-':
                this.nextToken();
                return Expr.makeApp1(Expr.NEG, this.parseExpr(35));
            case Token.TT_EOF:
                throw this.error('Premature end',
                        SyntaxException.PREMATURE_EOF);
            default:
                throw this.error('Expected a factor',
                        SyntaxException.BAD_FACTOR);
        }
    }

    error(complaint, reason, expected) {
        return SyntaxException.getException(complaint, this, reason, expected);
    }

    expect(ttype) {
        if (this.token.ttype != ttype) {
            throw this.error(ttype + ' expected',
                SyntaxException.EXPECTED, '' + ttype);
        }
        this.nextToken();
    }

}

