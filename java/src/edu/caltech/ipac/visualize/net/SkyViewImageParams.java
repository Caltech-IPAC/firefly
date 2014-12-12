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
/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313) 
 * OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS, 
 * HOWEVER USED.
 * 
 * IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE 
 * FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL 
 * OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO 
 * PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE 
 * ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
 * 
 * RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE 
 * AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR 
 * ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE 
 * OF THE SOFTWARE. 
 */
