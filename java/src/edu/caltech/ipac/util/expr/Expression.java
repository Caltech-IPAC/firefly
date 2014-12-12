package edu.caltech.ipac.util.expr;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * This is an entry point class for expression evaluation
 * The implementation is using Darius Bacon's math expression parser
 * @author tatianag
 */
public class Expression {

    private String userInput;
    private SyntaxException syntaxException = null;
    private Expr expr = null;
    private HashMap<String,Variable> parsedVariablesMap;

    public Expression(String input, List<String> allowedVariables) {
        userInput = input;
        Parser parser = new Parser();
        parser.allow(null);
        if (allowedVariables != null) {
            for (String v : allowedVariables) {
                parser.allow(Variable.make(v));
            }
        }
        try {
            expr = parser.parseString(input);
            Set<Variable> parsedVariables = parser.getParsedVariables();
            parsedVariablesMap = new HashMap<String,Variable>(parsedVariables.size());
            for (Variable var : parsedVariables) {
                parsedVariablesMap.put(var.toString(), var);
            }
        } catch (SyntaxException e) {
            syntaxException = e;
        }
    }

    public String getInput() {
        return userInput;
    }

    public boolean isValid() {
        return (syntaxException == null);
    }

    public String getErrorMessage() {
        return (syntaxException == null) ? "" : syntaxException.getMessage();
    }

    public Set<String> getParsedVariables() {
        return parsedVariablesMap.keySet();
    }

    public void setVariableValue(String name, double value) throws IllegalArgumentException {
        Variable var = parsedVariablesMap.get(name);
        if (var != null) {
            var.setValue(value);
        } else {
            throw new IllegalArgumentException("Invalid variable: "+name);
        }
    }

    public double getValue() {
        return expr.value();
    }
}
