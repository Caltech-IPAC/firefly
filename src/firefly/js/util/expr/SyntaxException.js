// Syntax-error exception.
// Copyright 2002 by Darius Bacon <darius@wry.me>
// Altered (no error correction), convered to JS

import * as Token from './Token.js';

/** An error code meaning the input string couldn't reach the end
 of the input; the beginning constituted a legal expression,
 but there was unparsable stuff left over. */
export const INCOMPLETE = 0;

/** An error code meaning the parser ran into a non-value token
 (like "/") at a point it was expecting a value (like "42" or
 "x^2"). */
export const BAD_FACTOR = 1;

/** An error code meaning the parser hit the end of its input
 before it had parsed a full expression. */
export const PREMATURE_EOF = 2;

/** An error code meaning the parser hit an unexpected token at a
 point where it expected to see some particular other token. */
export const EXPECTED = 3;

/** An error code meaning the expression includes a variable not
 on the `allowed' list. */
export const UNKNOWN_VARIABLE = 4;


function quotify(s) {
    return `"${s}"`;
}

function asFarAs(scanner) {
    const t = scanner.getCurrentToken();
    const point = t.location - t.leadingWhitespace;
    return scanner.getInput().substring(0, point);
}

function theToken(scanner) {
    return scanner.getCurrentToken().sval;
}

function isLegalToken(scanner) {
    const t = scanner.getCurrentToken();
    return t.ttype !== Token.TT_EOF
            && t.ttype != Token.TT_ERROR;
}

function explainWhere(scanner) {
    const sb = [];
    if (scanner.isEmpty()) {
        sb.push('It is empty!');
    } else if (scanner.atStart()) {
        sb.push('It starts with '+quotify(theToken(scanner)));
        if (isLegalToken(scanner)) {
            sb.push(', which can never be the start of an expression.');
        } else {
            sb.push(', which is a meaningless symbol to me.');
        }
    } else {
        sb.push('I got as far as ');
        sb.push(quotify(asFarAs(scanner)));
        sb.push(' and then ');
        if (scanner.atEnd()) {
            sb.push('reached the end unexpectedly.');
        } else {
            sb.push('saw ');
            sb.push(quotify(theToken(scanner)));
            if (isLegalToken(scanner)) {
                sb.push('.');
            } else {
                sb.push(', which is a meaningless symbol to me.');
            }
        }
    }
    return ''.concat(...sb);
}

function explainWhy(reason, expected, scanner) {
    const sb = [];
    switch (reason) {
        case INCOMPLETE:
            if (isLegalToken(scanner)) {
                sb.push('The first part makes sense, but I do not see ' +
                    'how the rest connects to it.');
            }
            break;
        case BAD_FACTOR:
        case PREMATURE_EOF:
            sb.push('I expected a value');
            if (!scanner.atStart()) sb.push(' to follow');
            sb.push(', instead.');
            break;
        case EXPECTED:
            sb.push('I expected ');
            sb.push(quotify(expected));
            sb.push(' at that point, instead.');
            break;
        case UNKNOWN_VARIABLE:
            sb.push('That variable is unknown or has no value.');
            break;
        default:
            throw 'Can not happen';
    }
    return ''.concat(...sb);
}


/**
 * An exception indicating a problem in parsing an expression.  It can
 * produce a short, cryptic error message (with getMessage()) or a
 * long, hopefully helpful one (with explain()).
 * @param {string} complaint short error message
 * @param {Parser} parser the parser that hit this snag
 * @param {number} reason one of the error codes defined in this class
 * @param {string} expected if nonnull, the token the parser expected to
 *        see (in place of the erroneous token it did see)
 */
export function getException (complaint, parser, reason, expected) {

    const scanner = parser.tokens;

    const details = `I do not understand your expression ${scanner.getInput()}.`;
    const errWhere = explainWhere(scanner);
    const errWhy = explainWhy(reason, expected, scanner);

    return {
        error: complaint,
        details,
        where: errWhere,
        why: errWhy
    };
}


