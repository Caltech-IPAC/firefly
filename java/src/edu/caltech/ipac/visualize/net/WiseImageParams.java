package edu.caltech.ipac.visualize.net;

public class WiseImageParams extends BaseIrsaParams  {

    public static final String WISE_1B = "1b";
    public static final String WISE_3A = "3a";

    private String _band= "1";
    private float  _size= 500;
    private String _type= WISE_3A;

    public WiseImageParams() { }

    public void setType(String type) { _type= type; }
    public String getType()          { return _type; }
    public void   setBand(String b)     { _band= b; }
    public void   setSize(float s)      { _size= s; }
    public String getBand()             { return _band; }
    public float  getSize()             { return _size; }

    public String getUniqueString() {
        return "Wise-" + super.toString()+ "-" + _type + "-" + _band + "-" +_size;
    }

    public String toString() { return getUniqueString(); }
}
