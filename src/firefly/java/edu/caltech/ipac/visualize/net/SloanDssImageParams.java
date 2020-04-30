/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.net;

public class SloanDssImageParams extends ImageServiceParams {


    public enum SDSSBand {u,g,r,i,z}

    private float  _sizeInDeg = 3.F; //SDSS measured over a 3 degree wide area according to http://cas.sdss.org/dr16/en/tools/getimg/getimghome.aspx
    private SDSSBand _band = SDSSBand.r;
    private int  _timeout  = 0;
    private boolean _queryKey= false;

    public SloanDssImageParams() { super(ImageSourceTypes.SDSS); }

    public void  setSizeInDeg(float s)      { _sizeInDeg= s; }
    public float getSizeInDeg()      { return _sizeInDeg; }

    public void setBand(SDSSBand band) { _band = band; }
    public SDSSBand getBand() { return _band; }

    public void   setTimeout(int timeout) { _timeout= timeout;}
    public int    getTimeout() { return _timeout;}

    public String getUniqueString() {
         return "SloanDssImage-" + super.toString() + "--" + _sizeInDeg + "--" + _band +
                 (_queryKey? "--queryKey": "");
    }

    public String toString() {
         return getUniqueString();
    }

    public SloanDssImageParams makeQueryKey() {
        SloanDssImageParams newParam= new SloanDssImageParams();
        newParam._queryKey= true;
        newParam.setSizeInDeg(_sizeInDeg);
        newParam.setBand(_band);
        newParam.setTimeout(_timeout);
        newParam.setWorldPt(this.getWorldPt());
        return newParam;
    }
}
