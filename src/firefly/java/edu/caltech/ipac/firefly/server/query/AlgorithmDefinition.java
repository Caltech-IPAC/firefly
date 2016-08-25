package edu.caltech.ipac.firefly.server.query;

/**
 * Created by ejoliet on 8/22/16.
 */
public enum AlgorithmDefinition {

    LS("Lomb-Scargle");

    String name = null;
    int nParameters = 0;

    AlgorithmDefinition(String name) {
        this.name = name;
    }

    AlgorithmDefinition(String name, int npars) {
        this.name = name;
        this.nParameters = npars;
    }

    public int getNParameters() {
        return this.nParameters;
    }

    public String getName() {
        return this.name;
    }
}