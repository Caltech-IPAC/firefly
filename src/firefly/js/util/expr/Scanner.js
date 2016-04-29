// Scan lexical tokens in input strings.
// Copyright 2002 by Darius Bacon <darius@wry.me>
// Altered, converted to JS

import * as Token from './Token.js';

/*
 * @param {Object} s - token
 * @param {Object} t - token
 * @return {boolean}
 */
function joinable(s, t) {
    return !(isAlphanumeric(s) && isAlphanumeric(t));
}

/*
 * @param {Object} t - token
 * @return {boolean}
 */
function isAlphanumeric(t) {
    return t.ttype === Token.TT_WORD || t.ttype === Token.TT_NUMBER;
}

export class Scanner {

    constructor(string, operatorChars) {
        this.tokens = [];
        this.index = -1;
        this.s = string;
        this.operatorChars = operatorChars;

        var i = 0;
        do {
            i = this.scanToken(i);
        } while (i < this.s.length);
    }

    getInput() {
        return this.s;
    }

    // The tokens may have been diddled, so this can be different from
    // getInput().
    toString() {
        let sb = '';
        let whitespace = 0;
        for (var i = 0; i < this.tokens.length; ++i) {
            const t = this.tokens[i];

            let spaces = (whitespace != 0 ? whitespace : t.leadingWhitespace);
            if (i === 0) {
                spaces = 0;
            } else if (spaces === 0 && !joinable(this.tokens[i-1], t)) {
                spaces = 1;
            }
            for (var j = spaces; 0 < j; --j) {
                sb += ' ';
            }
            sb += t.sval;
            whitespace = t.trailingWhitespace;
        }
        return sb;
    }

    /*
     * @return {boolean}
     */
    isEmpty() {
        return this.tokens.length === 0;
    }

    /*
     * @return {boolean}
     */
    atStart() {
        return this.index <= 0;
    }

    /*
     * @return {boolean}
     */
    atEnd() {
        return this.tokens.length <= this.index;
    }

    /*
     * @return {Object} token
     */
    nextToken() {
        ++this.index;
        return this.getCurrentToken();
    }

    /*
     * @return {Object} token
     */
    getCurrentToken() {
        const {s, index} = this;
        if (this.atEnd()) {
            return Token.tokenFromString(Token.TT_EOF, 0, s, s.length, s.length);
        }
        return this.tokens[index];
    }

    /*
     * @param {number} i
     * @return {number}
     */
    scanToken(i) {
        const {s, operatorChars, tokens} = this;

        while (i < s.length && /\s/.test(s.charAt(i))) {
            ++i;
        }

        if (i === s.length) {
            return i;
        } else if (0 <= operatorChars.indexOf(s.charAt(i))) {
            if (i+1 < s.length) {
                const pair = s.substring(i, i+2);
                let ttype = 0;
                if (pair === '<=') {
                    ttype = Token.TT_LE;
                } else if (pair === '>=') {
                    ttype = Token.TT_GE;
                } else if (pair === '<>') {
                    ttype = Token.TT_NE;
                }
                if (0 !== ttype) {
                    tokens.push(Token.tokenFromString(ttype, 0, s, i, i+2));
                    return i+2;
                }
            }
            tokens.push(Token.tokenFromString(s.charAt(i), 0, s, i, i+1));
            return i+1;
        } else if (/[a-zA-Z]/.test(s.charAt(i))) {
            return this.scanSymbol(i);
        } else if (/\d/.test(s.charAt(i)) || '.' === s.charAt(i)) {
            return this.scanNumber(i);
        } else {
            tokens.push(this.makeErrorToken(i, i+1));
            return i+1;
        }
    }

    /*
     * @param {number} i
     * @return {number}
     */
    scanSymbol(i) {
        const {s, tokens} = this;
        const from = i;
        while (i < s.length && /[A-Za-z\d_]/.test(s.charAt(i)) ) {
            ++i;
        }
        tokens.push(Token.tokenFromString(Token.TT_WORD, 0, s, from, i));
        return i;
    }

    /*
     * @param {number} i
     * @return {number}
     */
    scanNumber(i) {
        const {s, tokens} = this;

        const from = i;

        // We include letters in our purview because otherwise we'd
        // accept a word following with no intervening space.
        for (; i < s.length; ++i) {
            if (!/[\.\da-zA-Z]/.test(s.charAt(i))) {
                break;
            }
        }

        const text = s.substring(from, i);
        const nval = parseFloat(text);
        if (Number.isNaN(nval)) {
            tokens.push(this.makeErrorToken(from, i));
            return i;
        }
        tokens.push(Token.tokenFromString(Token.TT_NUMBER, nval, s, from, i));
        return i;
    }

    /*
     * @param {number} from
     * @param {number} i
     * @return {Object} token
     */
    makeErrorToken(from, i) {
        return Token.tokenFromString(Token.TT_ERROR, 0, this.s, from, i);
    }
}

