/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.astro.net;

import edu.caltech.ipac.util.download.NetParams;

public class HorizonsParams implements NetParams {

    private String _key;

    public HorizonsParams(String key) {
       _key= key;
    }

    public String getKey() { return _key; }

    public String getUniqueString() {
        return "HORIZONS" + _key;
    }

    public String toString() {
       return getUniqueString();
    }
}
