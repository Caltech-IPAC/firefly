/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.net;

import edu.caltech.ipac.util.download.NetParams;

public class NedImQueryParams implements NetParams {

    private String _name;

    public NedImQueryParams(String name) {
       _name= name;
    }

    public String getName() { return _name; }

    public String getUniqueString() {
       return "NED_Image_query_" + _name;
    }

    public String toString() {
       return getUniqueString();
    }
}
