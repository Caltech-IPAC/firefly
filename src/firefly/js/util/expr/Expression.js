/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {isArray} from 'lodash';
import {Parser} from './Parser.js';
import {makeVariable} from './Variable.js';


/**
 * This is an entry point class for expression evaluation
 * The implementation is using Darius Bacon's math expression parser
 * @author tatianag
 */
export class Expression {

    constructor(input, allowedVariables) {
        this.userInput = input;
        const parser = new Parser();
        parser.allow(null);
        if (isArray(allowedVariables)) {
            allowedVariables.forEach((v)=>{
                parser.allow(makeVariable(v));
            });
        }
        try {
            this.expr = parser.parseString(input);
            this.parsedVariablesMap = parser.getParsedVariables();
            this.canonicalInput = parser.getCanonicalInput();
        } catch (se) {
            this.syntaxException = se;
        }
    }

    getInput() {
        return this.userInput;
    }

    getCanonicalInput() {
        return this.canonicalInput;
    }

    isValid() {
        return (!this.syntaxException);
    }

    /*
     * @return {Object} with fields error, details, errWhere, errWhy, errWhat
     */
    getError() {
        return this.syntaxException;
    }

    /*
     *
     */
    getParsedVariables() {
        return this.parsedVariablesMap ? Array.from(this.parsedVariablesMap.keys()) : [];
    }

    /*
    * Set variable value
    * @param {string} name
    * @param {number} value
     */
    setVariableValue(name, value) {
        const v = this.parsedVariablesMap.get(name);
        if (v) {
            v.setValue(value);
        } else {
            throw ('Invalid variable: '+name);
        }
    }

    getValue() {
        return this.expr.value();
    }
}
