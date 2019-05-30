/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.net;

/**
 * Created by wmi
 * on 2019-04-04 11:43
 * edu.caltech.ipac.visualize.net
 */

public class PtfImageParams extends ImageServiceParams {

    public static final String PTF_LEVEL1 = "level1";
    public static final String PTF_LEVEL2= "level2";

    private String _band= "1";
    private float  _size= 500;
    private String _productLevel = PTF_LEVEL2;

    public PtfImageParams() {super(ImageSourceTypes.PTF); }

    public String getProductLevel()          { return _productLevel; }

    public void setProductLevel(String productLevel) { _productLevel = productLevel; }

    public String getBand()             { return _band; }

    public void   setBand(String b)     { _band= b; }

    public float  getSize()             { return _size; }

    public void   setSize(float s)      { _size= s; }

    public String getUniqueString() {
        return "Ptf-" + super.toString()+ "-" + _productLevel + "-" + _band + "-" +_size;
    }

    public String toString() { return getUniqueString(); }
}
