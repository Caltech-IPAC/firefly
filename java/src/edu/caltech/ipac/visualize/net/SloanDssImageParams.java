package edu.caltech.ipac.visualize.net;

public class SloanDssImageParams extends  BaseIrsaParams  {


    public enum SDSSBand {u,g,r,i,z}

    private float  _sizeInDeg = .1F;
    private SDSSBand _band = SDSSBand.r;
    private int  _timeout  = 0;
    private boolean _queryKey= false;

    public SloanDssImageParams() { }

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
        newParam.setRaJ2000(this.getRaJ2000());
        newParam.setDecJ2000(this.getDecJ2000());
        return newParam;
    }
}
