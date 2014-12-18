package edu.caltech.ipac.visualize.net;

public class SkyViewImageParams extends    BaseIrsaParams  {


    //private float  _size= 5.0F;
    private int    _pixelWidth= 300;
    private int    _pixelHeight= 300;
    private double _sizeInDeg= 0.0F;
    private String _projection= "Gnomonic";
    private String _survey;

    public SkyViewImageParams() { }

    
    //public void   setSize(float s)      { _size= s; }
    //public float  getSize()             { return _size; }

    public void   setSurvey(String s)   { _survey= s; }
    public String getSurvey()           { return _survey; }

    public void   setPixelWidth(int w)   { _pixelWidth= w; }
    public int    getPixelWidth()        { return _pixelWidth; }

    public void   setPixelHeight(int h)   { _pixelHeight= h; }
    public int    getPixelHeight()        { return _pixelHeight; }

    public void   setSizeInDegree(double s) {_sizeInDeg= s; }
    public double getSizeInDegree()         {return _sizeInDeg; }

    public String getUniqueString() {
         String retval= "SkyvImage" + "-" + _survey + "-" + super.toString() + 
                          _pixelWidth+ _pixelHeight + _sizeInDeg + _projection;
         return retval.replaceAll("[ :\\[\\]\\/\\\\|\\*\\?<>]", "");
    }

    public String toString() {
         return getUniqueString();
    }
}
