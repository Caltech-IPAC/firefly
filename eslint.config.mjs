import react from "eslint-plugin-react";
import jsdoc from "eslint-plugin-jsdoc";
import reactHooks from "eslint-plugin-react-hooks";
import stylisticJs from '@stylistic/eslint-plugin-js'
import { fileURLToPath } from 'url';
import path from 'path';
import { fixupPluginRules, includeIgnoreFile } from "@eslint/compat";
import globals from "globals";
import babelParser from "@babel/eslint-parser";


const __dirname = path.dirname(fileURLToPath(import.meta.url));
const gitIgnorePath = path.resolve(__dirname, ".gitignore")


// To better understand what this configuration is doing and which files it applies to, run:
// yarn run eslint --inspect-config
// It opens a UI client that updates dynamically when making changes in this file, which is also helpful in testing.

export default [
    includeIgnoreFile(gitIgnorePath), //ignore git-ignored dirs/files
    {
        files: ["**/*.js", "**/*.jsx"],

        plugins: {
            jsdoc,
            '@stylistic/js': stylisticJs,
            react: fixupPluginRules(react), // compatibility fix for the plugins not yet supporting eslint's new config model: https://eslint.org/blog/2024/05/eslint-compatibility-utilities/
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
            // 0="off", 1="warn", 2="error"
            curly: [2, "multi-line"],
            eqeqeq: 1,
            "no-console": 0,
            "no-empty": 1,
            "no-undef": 2,
            "no-use-before-define": 0,
            "no-unused-vars": 1,
            "object-shorthand": 1,
            "prefer-const": 1,
            "prefer-spread": 1,
            "prefer-template": 0,
            "@stylistic/js/arrow-parens": [2, "always"],
            "@stylistic/js/comma-spacing": 0,
            "@stylistic/js/jsx-quotes": [1, "prefer-single"],
            "@stylistic/js/key-spacing": 0,
            "@stylistic/js/no-multi-spaces": 0,
            "@stylistic/js/semi": [2, "always"],
            "@stylistic/js/space-infix-ops": 0,
            "@stylistic/js/quotes": [2, "single", "avoid-escape"],
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
    }
];