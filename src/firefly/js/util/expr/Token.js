// A lexical token from an input string.
// Copyright 2002 by Darius Bacon <darius@wry.me>
// Altered, converted to JS

export const TT_ERROR  = -1;
export const TT_EOF    = -2;
export const TT_NUMBER = -3;
export const TT_WORD   = -4;
export const TT_LE     = -5;
export const TT_NE     = -6;
export const TT_GE     = -7;

//Token(int ttype, double nval, String input, int start, int end)
export function tokenFromString(ttype, nval, input, start, end) {
    var i;
    var count;

    count = 0;
    for (i = start-1; 0 <= i; --i) {
        if (!/\s/.test(input.charAt(i))) {
            break;
        }
        ++count;
    }
    const leadingWhitespace = count;

    count = 0;
    for (i = end; i < input.length; ++i) {
        if (!/\s/.test(input.charAt(i))) {
            break;
        }
        ++count;
    }

    return {
        ttype,
        sval: input.substring(start, end),
        nval,
        location: start,
        leadingWhitespace,
        trailingWhitespace: count
    };
}



