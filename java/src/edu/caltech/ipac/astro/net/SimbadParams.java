/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.astro.net;

import edu.caltech.ipac.util.download.NetParams;

public class SimbadParams implements NetParams {

    private String _name;

    public SimbadParams(String name) {
       _name= name;
    }

    public String getName() { return _name; }

    public String getUniqueString() {
       return "SIMBAD__ATTRIBUTE_" + _name;
    }

    public String toString() {
       return getUniqueString();
    }
}
