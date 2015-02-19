/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.net;


import edu.caltech.ipac.util.download.NetParams;

public class IsoImageListParams extends BaseIrsaParams
                               implements NetParams {
    /**
     search radius, in degree
    */
    public IsoImageListParams(double raJ2000, double decJ2000) {
        this.setRaJ2000(raJ2000);
        this.setDecJ2000(decJ2000);
    }

    // maybe needs to replace spaces with some other characters
    public String getUniqueString() {
         return  "ISO" + "-" + super.toString();
    }

    public String getIsoObjectString() {
        return  getRaJ2000String() +","+ getDecJ2000String();
    }

    public String toString() {
         return getUniqueString();
    }
}
