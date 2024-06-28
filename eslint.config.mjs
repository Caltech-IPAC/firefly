import react from "eslint-plugin-react";
import jsdoc from "eslint-plugin-jsdoc";
import reactHooks from "eslint-plugin-react-hooks";
import { fixupPluginRules } from "@eslint/compat";
import globals from "globals";
import babelParser from "@babel/eslint-parser";

export default [{
    plugins: {
        react: fixupPluginRules(react),
        jsdoc,
        "react-hooks": fixupPluginRules(reactHooks),
    },

    languageOptions: {
        globals: {
            ...globals.browser,
            ...globals.jest,
        },

        parser: babelParser,
        ecmaVersion: 2020,
        sourceType: "module",

        parserOptions: {
            requireConfigFile: false,

            babelOptions: {
                presets: ["@babel/preset-react"],
            },
        },
    },

    settings: {
        react: {
            createClass: "createReactClass", // Regex for Component Factory to use
            version: "detect", // React version. "detect" automatically picks the version you have installed.
        },
    },

    rules: {
        "arrow-parens": [2, "always"],
        "comma-spacing": 0,
        curly: [2, "multi-line"],
        "jsx-quotes": [1, "prefer-single"],
        "key-spacing": 0,
        "no-multi-spaces": 0,
        "no-console": 0,
        "no-empty": 1,
        "no-undef": 2,
        "no-use-before-define": 0,
        "no-unused-vars": 1,
        "object-shorthand": 1,
        "prefer-const": 1,
        "prefer-spread": 1,
        "prefer-reflect": 1,
        "prefer-template": 0,
        "react/jsx-boolean-value": [1, "always"],
        "react/jsx-no-duplicate-props": 1,
        "react/jsx-no-undef": 1,
        "react/jsx-uses-react": 1,
        "react/jsx-uses-vars": 1,
        "react/jsx-wrap-multilines": 1,
        "react/no-danger": 1,
        "react/no-did-mount-set-state": 1,
        "react/no-did-update-set-state": 1,
        "react/no-direct-mutation-state": 1,
        "react/no-unknown-property": 1,

        "react/prop-types": [1, {
            ignore: ["children"],
            skipUndeclared: true,
        }],

        "react/react-in-jsx-scope": 1,
        "react/self-closing-comp": 1,
        "react-hooks/rules-of-hooks": "error", // Checks rules of Hooks
        semi: [2, "always"],
        "space-infix-ops": 0,
        quotes: [2, "single", "avoid-escape"],
        eqeqeq: 1,
        "jsdoc/check-param-names": 2,
        "jsdoc/check-tag-names": 0,
        "jsdoc/check-types": 0,
        "jsdoc/newline-after-description": 0,
        "jsdoc/require-description-complete-sentence": 0,
        "jsdoc/require-hyphen-before-param-description": 0,
        "jsdoc/require-param": 2,
        "jsdoc/require-param-description": 0,
        "jsdoc/require-param-type": 0,
        "jsdoc/require-returns-description": 0,
        "jsdoc/require-returns-type": 1,
    },
}];