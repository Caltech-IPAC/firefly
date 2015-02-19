/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.net;


import edu.caltech.ipac.util.download.NetParams;

public class IrsaCatalogParams extends BaseIrsaParams
                               implements NetParams {
    private String _catName;
    /**
     search radius, in degree
    */
    private float  _size= 5.0F; // degree

    public IrsaCatalogParams() {}

    public void setCatalogName(String n)      { _catName= n; }
    public void setSize(float s)          { _size= s; }

    public String getCatalogName()      { return _catName; }
    public float  getSize()         { return _size; }

    // maybe needs to replace spaces with some other characters
    public String getUniqueString() {
         return  _catName + "-" + super.toString() + _size;
    }

    public String toString() {
         return getUniqueString();
    }
}
