/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.net;

/**
 * Created by wmi
 * on 2019-04-04 11:43
 * edu.caltech.ipac.visualize.net
 */

public class ZtfImageParams extends ImageServiceParams {

    public static final String ZTF_SCI = "sci";
    public static final String ZTF_REF= "ref";

    private String _band= "zr";
    private float  _size= 500;
    private String _productLevel = ZTF_REF;

    public ZtfImageParams() {super(ImageSourceTypes.ZTF); }

    public String getProductLevel()          { return _productLevel; }

    public void setProductLevel(String productLevel) { _productLevel = productLevel; }

    public String getBand()             { return _band; }

    public void   setBand(String b)     { _band= b; }

    public float  getSize()             { return _size; }

    public void   setSize(float s)      { _size= s; }

    public String getUniqueString() {
        return "Ztf-" + super.toString()+ "-" + _productLevel + "-" + _band + "-" +_size;
    }

    public String toString() { return getUniqueString(); }
}
