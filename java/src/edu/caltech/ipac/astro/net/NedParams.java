package edu.caltech.ipac.astro.net;

import edu.caltech.ipac.util.download.NetParams;

public class NedParams implements NetParams {

    private String _name;

    public NedParams(String name) {
       _name= name;
    }

    public String getName() { return _name; }

    public String getUniqueString() {
        return "NED__ATTRIBUTE_" + _name;
    }

    public String toString() {
       return getUniqueString();
    }
}
