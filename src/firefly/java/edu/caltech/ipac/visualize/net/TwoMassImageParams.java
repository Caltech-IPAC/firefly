/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.net;

public class TwoMassImageParams extends ImageServiceParams {

    private String _band= "J";
    private float  _size= 500;
    private String ds = "asky"; //asky, askyw, sx, sxw, cal

    public TwoMassImageParams(String statusKey, String plotId) {super(ImageSourceTypes.TWOMASS, statusKey, plotId); }

    public void setDataset(String ds1) { this.ds = ds1; }
    public String getDataset()          { return this.ds; }
    public void   setBand(String b)     { _band= b; }
    public void   setSize(float s)      { _size= s; }
    public String getBand()             { return _band; }
    public float  getSize()             { return _size; }

    public String getUniqueString() {
        return "2mass-" + super.toString()+ "-" + this.ds + "-" + _band + "-" +_size;
    }

    public String toString() { return getUniqueString(); }
}
