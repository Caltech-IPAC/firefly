/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.net;

public class DssImageParams extends  BaseIrsaParams  {

    public static final int HST_PHASE_2_SURVEY         = 0;
    public static final int FIRST_GEN_SURVEY           = 1;
    public static final int SECOND_GEN_SURVEY          = 2;
    public static final int SECOND_OR_FIRST_GEN_SURVEY = 3;


   //  poss2ukstu_red poss2ukstu_ir poss2ukstu_blue poss1_red poss1_blue all                                     p

    private float  _height = 30.0F;
    private float  _width  = 30.0F;
    private int  _timeout  = 0;
    //private int    _survey = FIRST_GEN_SURVEY;
    private String    _survey = "poss2ukstu_red";
    private String    _name = "POSS2/UKSTU Red";

    public DssImageParams() { }

    public void   setWidth(float  w)      { _width= w; }
    public void   setHeight(float h)      { _height= h; }
    public void   setTimeout(int timeout) { _timeout= timeout;}
   /*
    public void   setSurvey(int s)        { 
         Assert.tst( s==FIRST_GEN_SURVEY      ||
                     s==SECOND_GEN_SURVEY     ||
                     s==HST_PHASE_2_SURVEY    ||
                     s==SECOND_OR_FIRST_GEN_SURVEY);
         _survey= s;
    }
    */
   public void   setSurvey(String s)        {
      _survey= s;
   }
   public void   setName(String s)        {
      _name= s;
   }
    public float  getWidth()  { return _width; }
    public float  getHeight() { return _height; }
    public int    getTimeout() { return _timeout;}
    //public int    getSurvey() { return _survey; }
   public String    getSurvey() { return _survey; }
   public String    getName() { return _name; }

    public String getUniqueString() {
         String retval= "DssImage-" + super.toString() + 
                        _width + _height + _survey;
         return retval;
    }

    public String toString() {
         return getUniqueString();
    }
}
