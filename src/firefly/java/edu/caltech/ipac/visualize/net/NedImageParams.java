/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.net;

import edu.caltech.ipac.util.download.BaseNetParams;
import edu.caltech.ipac.util.Assert;

public class NedImageParams extends BaseNetParams {

    private String _url;
    private String _band;

    public NedImageParams(String url, String band) {
       _url= url;
       _band= band;
    }

    public String getURL() { return _url; }
    public String getBand() { return _band; }

    public String getUniqueString() {
       int idx= _url.lastIndexOf('/');
       Assert.tst( idx > 0 );
       StringBuffer workstr= new StringBuffer (
                           _url.substring(idx+1, _url.length() ) );
       int length= workstr.length();
       for(int i=0; (i<length); i++) {
           if (workstr.charAt(i) == ':') workstr.setCharAt(i,'_');
       }
       return "NED_Image_" + workstr.toString();
    }

}
