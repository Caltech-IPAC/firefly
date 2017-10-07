/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.net;

public class WiseImageParams extends ImageServiceParams {

    public static final String WISE_1B = "1b";
    public static final String WISE_3A = "3a";

    private String _band= "1";
    private float  _size= 500;
    private String _productLevel = WISE_3A;

    public WiseImageParams() {super(ImageSourceTypes.WISE); }

    public void setProductLevel(String productLevel) { _productLevel = productLevel; }
    public String getProductLevel()          { return _productLevel; }
    public void   setBand(String b)     { _band= b; }
    public void   setSize(float s)      { _size= s; }
    public String getBand()             { return _band; }
    public float  getSize()             { return _size; }

    public String getUniqueString() {
        return "Wise-" + super.toString()+ "-" + _productLevel + "-" + _band + "-" +_size;
    }

    public String toString() { return getUniqueString(); }
}
