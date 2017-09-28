/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.net;

public class DssImageParams extends ImageServiceParams {

    private float  _height = 30.0F;
    private float  _width  = 30.0F;
    private int  _timeout  = 0;
    private String    _survey = "poss2ukstu_red";
    private String    _name = "POSS2/UKSTU Red";

    public DssImageParams() {super(ImageSourceTypes.DSS); }

    public void   setWidth(float  w)      { _width= w; }
    public void   setHeight(float h)      { _height= h; }
    public void   setTimeout(int timeout) { _timeout= timeout;}

    /**
     * survey one of : poss2ukstu_red poss2ukstu_ir poss2ukstu_blue poss1_red poss1_blue quickv phase2_gsc2 phase2_gsc1
     */
    public void   setSurvey(String s)        { _survey= s; }

    public void   setName(String s)        { _name= s; }
    public float  getWidth()  { return _width; }
    public float  getHeight() { return _height; }
    public int    getTimeout() { return _timeout;}
    public String    getSurvey() { return _survey; }
    public String    getName() { return _name; }

    public String getUniqueString() {
         return "DssImage-" + super.toString() + _width + _height + _survey;
    }

    public String toString() { return getUniqueString(); }
}
