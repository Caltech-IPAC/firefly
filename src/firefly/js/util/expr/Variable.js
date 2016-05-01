// Variables associate values with names.
// Copyright 2002 by Darius Bacon <darius@wry.me>
// Altered, converted to JS

//TODO: when should the variables get cleared?
const variables = new Map();

/**
  * Return a unique variable named `name'.  There can be only one
  * variable with the same name returned by this method; that is,
  * makeVariable(s1) === makeVariable(s2) if and only if s1===s2.
  * @param {String} name the variable's name
  * @return the variable; create it initialized to 0 if it doesn't
  *         yet exist */
export function makeVariable(name) {
     let result = variables.get(name);
     if (!result) {
         result = new Variable(name);
         variables.set(name, result);
     }
     return result;
 }

/**
 * A variable is a simple expression with a name (like "x") and a
 * settable value.
 */
export class Variable {

    /**
     * Create a new variable, with initial value 0.
     * @param {String} name the variable's name
     */
    constructor(name) {
        this.name = name;
        this.val = 0;
    }

    /** Return the name. */
    toString() { return this.name; }

    /** Get the value.
     * @return {Number} the current value */
    value() {
        return this.val;
    }

    /**
     * Set the value.
     * @param {Number} value the new value
     */
    setValue(value) {
        this.val = value;
    }
}