// Mathematical expressions.
// Copyright 2002 by Darius Bacon <darius@wry.me>
// Altered and converted to JS

/** Binary operator: addition        */  export const ADD =  0;
/** Binary operator: subtraction     */  export const SUB =  1;
/** Binary operator: multiplication  */  export const MUL =  2;
/** Binary operator: division        */  export const DIV =  3;
/** Binary operator: exponentiation  */  export const POW =  4;
/** Binary operator: arctangent      */  export const ATAN2 = 5;
/** Binary operator: maximum         */  export const MAX =  6;
/** Binary operator: minimum         */  export const MIN =  7;
/** Binary operator: less than       */  export const LT  =  8;
/** Binary operator: less or equal   */  export const LE  =  9;
/** Binary operator: equality        */  export const EQ  = 10;
/** Binary operator: inequality      */  export const NE  = 11;
/** Binary operator: greater or equal*/  export const GE  = 12;
/** Binary operator: greater than    */  export const GT  = 13;
/** Binary operator: logical and     */  export const AND = 14;
/** Binary operator: logical or      */  export const OR  = 15;

/** Unary operator: absolute value*/   export const ABS   = 100;
/** Unary operator: arccosine */       export const ACOS  = 101;
/** Unary operator: arcsine   */       export const ASIN  = 102;
/** Unary operator: arctangent*/       export const ATAN  = 103;
/** Unary operator: ceiling   */       export const CEIL  = 104;
/** Unary operator: cosine    */       export const COS   = 105;
/** Unary operator: e to the x*/       export const EXP   = 106;
/** Unary operator: floor     */       export const FLOOR = 107;
/** Unary operator: log 10 */          export const LOG   = 108;
/** Unary operator: negation  */       export const NEG   = 109;
/** Unary operator: rounding  */       export const ROUND = 110;
/** Unary operator: sine      */       export const SIN   = 111;
/** Unary operator: square root */     export const SQRT  = 112;
/** Unary operator: tangent */         export const TAN   = 113;
/** Unary operator: natural log*/      export const LN    = 114;

/** Make a literal expression.
 * @param {number} v the constant value of the expression
 * @return an expression whose value is always v */
export function makeLiteral(v) {
    return new LiteralExpr(v);
}
/** Make an expression that applies a unary operator to an operand.
 * @param {number} rator (int) a code for a unary operator
 * @param rand operand (Expr)
 * @return an expression meaning rator(rand)
 */
export function makeApp1(rator, rand) {
    const app = new UnaryExpr(rator, rand);
    return rand instanceof LiteralExpr
            ? new LiteralExpr(app.value())
            : app;
}
/** Make an expression that applies a binary operator to two operands.
 * @param rator (int) a code for a binary operator
 * @param rand0 (Expr) left operand
 * @param rand1 (Expr) right operand
 * @return an expression meaning rator(rand0, rand1)
 */
export function makeApp2(rator, rand0, rand1) {
    const app = new BinaryExpr(rator, rand0, rand1);
    return rand0 instanceof LiteralExpr && rand1 instanceof LiteralExpr
            ? new LiteralExpr(app.value())
            : app;
}
/** Make a conditional expression.
 * @param test (Expr) `if' part
 * @param consequent (Expr) `then' part
 * @param alternative (Expr) `else' part
 * @return an expression meaning `if test, then consequent, else
 *         alternative' 
 */
export function makeIfThenElse(test,
                        consequent,
                        alternative) {
    const cond = new ConditionalExpr(test, consequent, alternative);
    if (test instanceof LiteralExpr) {
        return test.value() !== 0 ? consequent : alternative;
    } else {
        return cond;
    }
}

// These classes are all private to this module because we could
// plausibly want to do it in a completely different way, such as a
// stack machine.

class LiteralExpr {

    constructor(v) { this.v = v; }
    value() { return this.v; }
}

class UnaryExpr {

    constructor(rator, rand) {
        this.rator = rator;
        this.rand = rand;
    }

    value() {
        const arg = this.rand.value();
        switch (this.rator) {
            case ABS:   return Math.abs(arg);
            case ACOS:  return Math.acos(arg);
            case ASIN:  return Math.asin(arg);
            case ATAN:  return Math.atan(arg);
            case CEIL:  return Math.ceil(arg);
            case COS:   return Math.cos(arg);
            case EXP:   return Math.exp(arg);
            case FLOOR: return Math.floor(arg);
            case LOG:   return Math.log10(arg);
            case LN:   return Math.log(arg);
            case NEG:   return -arg;
            case ROUND: return Math.round(arg);
            case SIN:   return Math.sin(arg);
            case SQRT:  return Math.sqrt(arg);
            case TAN:   return Math.tan(arg);
            default: throw 'BUG: bad rator: '+this.rator;
        }
    }
}

class BinaryExpr {

    constructor(rator, rand0, rand1) {
        this.rator = rator;
        this.rand0 = rand0;
        this.rand1 = rand1;
    }
    value() {
        const arg0 = this.rand0.value();
        const arg1 = this.rand1.value();
        switch (this.rator) {
            case ADD:   return arg0 + arg1;
            case SUB:   return arg0 - arg1;
            case MUL:   return arg0 * arg1;
            case DIV:   return arg0 / arg1; // division by 0 has IEEE 754 behavior
            case POW:   return Math.pow(arg0, arg1);
            case ATAN2: return Math.atan2(arg0, arg1);
            case MAX:   return arg0 < arg1 ? arg1 : arg0;
            case MIN:   return arg0 < arg1 ? arg0 : arg1;
            case LT:    return arg0 <  arg1 ? 1.0 : 0.0;
            case LE:    return arg0 <= arg1 ? 1.0 : 0.0;
            case EQ:    return arg0 == arg1 ? 1.0 : 0.0;
            case NE:    return arg0 != arg1 ? 1.0 : 0.0;
            case GE:    return arg0 >= arg1 ? 1.0 : 0.0;
            case GT:    return arg0  > arg1 ? 1.0 : 0.0;
            case AND:   return arg0 != 0 && arg1 != 0 ? 1.0 : 0.0;
            case OR:    return arg0 != 0 || arg1 != 0 ? 1.0 : 0.0;
            default: throw 'BUG: bad rator: '+this.rator;
        }
    }
}

class ConditionalExpr {

    constructor(test, consequent, alternative) {
        this.test = test;
        this.consequent = consequent;
        this.alternative = alternative;
    }

    value() {
        return this.test.value() !== 0 ? this.consequent.value() : this.alternative.value();
    }
}
