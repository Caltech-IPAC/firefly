/*
 * Scratch module for probing the ESLint GitHub workflow (.github/workflows/lint.yml).
 * Every block below breaks a rule configured at severity "error" in eslint.config.mjs,
 * and none of them are recorded in eslint-suppressions.json, so the workflow run
 * should go red.
 */
import {useState} from 'react';

// @stylistic/js/quotes -- double quotes where the config requires single
const greeting = "probe";

// @stylistic/js/semi -- missing semicolon
const count = 1

// @stylistic/js/arrow-parens -- a lone parameter still needs parens
const double = n => n * 2;

// no-undef -- identifier that is neither declared here nor a known global
export function ping() {
    return notDefinedAnywhere(greeting);
}

// curly (multi-line) -- brace-less body on its own line
export function classify(value) {
    if (value > 10)
        return 'big';
    return 'small';
}

// no-dupe-keys (from eslint:recommended) -- key repeated in an object literal
export const options = {
    mode: 'probe',
    mode: 'duplicate',
};

/**
 * jsdoc/check-param-names -- the documented name does not match the signature.
 * @param {number} wrongName
 * @returns {number}
 */
export function scale(factor) {
    return factor * double(count);
}

// react-hooks/rules-of-hooks -- a hook called behind a condition
export function useProbe(enabled) {
    if (enabled) {
        const [value, setValue] = useState(0);
        return value;
    }
    return 0;
}
