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
