package edu.caltech.ipac.firefly.visualize;

import edu.caltech.ipac.firefly.data.ReqConst;

import java.io.Serializable;
/**
 * User: roby
 * Date: Aug 8, 2008
 * Time: 1:37:26 PM
 */


/**
 * @author Trey Roby
 */
@Deprecated
public class PlotCreationResult implements Serializable {



    private boolean _success;
    private String _briefFailReason;
    private String _userFailReason;
    private String _detailFailReason;
    private String[] initAry;

//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    @Deprecated
    public PlotCreationResult()  { this(true,"", "",""); }

    public static PlotCreationResult makeFail(String briefFailReason,
                                              String userFailReason,
                                              String detailFailReason)  {
        return new PlotCreationResult(false, briefFailReason, userFailReason,detailFailReason);
    }
    public  PlotCreationResult(WebPlotInitializer wpAry[] )  {
        _success= true;
        this.initAry= new String[wpAry.length];
        for(int i=0; (i<wpAry.length); i++) {
            initAry[i]= wpAry[i].toString();
        }
    }

    private PlotCreationResult(boolean success,
                               String briefFailReason,
                               String userFailReason,
                               String detailFailReason)  {
        _success= success;
        _briefFailReason= briefFailReason;
        _userFailReason= userFailReason;
        _detailFailReason= detailFailReason;
    }
//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    public String[] getResultStrAry() {
        return this.initAry;
    }

    public WebPlotInitializer[] getResultAry() {
       WebPlotInitializer wpInitAry[]= new WebPlotInitializer[initAry.length];
        for(int i=0; (i<initAry.length); i++) {
           wpInitAry[i] = WebPlotInitializer.parse(initAry[i]);
        }
        return wpInitAry;
    }

    public boolean isSuccess() { return _success; }
    public String getBriefFailReason() { return _briefFailReason; }
    public String getUserFailReason() { return _userFailReason; }
    public String getDetailFailReason() { return _detailFailReason; }

    public static PlotCreationResult parse(String s) {
        if (s==null) return null;

        PlotCreationResult retval= null;
        String sAry[]= s.split(ReqConst.PLOT_RESULT_SEP,500);
        if (s.startsWith("true")) {
            if (sAry.length<500) {
                String resStr[]= new String[sAry.length-1];
                for(int i=0; (i<resStr.length); i++) {
                    resStr[i]= sAry[i+1];
                }
                retval= new PlotCreationResult();
                retval.initAry= resStr;
            }
        }
        else {
            String bMsg= "Failed";
            String uMsg= "Failed";
            String dMsg= "Failed";
            String failAry[]= s.split(ReqConst.PLOT_RESULT_SEP,5);
            if (sAry.length==4) {
                bMsg= failAry[1];
                uMsg= failAry[2];
                dMsg= failAry[3];
            }
            retval= PlotCreationResult.makeFail(bMsg,uMsg,dMsg);
        }

        return retval;



    }


    public String toString() {
        String retval;
        if (_success) {
            StringBuilder sb= new StringBuilder(2000);
            sb.append("true").append(ReqConst.PLOT_RESULT_SEP);
            for(int i= 0; (i<initAry.length); i++) {
                sb.append(initAry[i].toString());
                if (i<initAry.length-1) sb.append(ReqConst.PLOT_RESULT_SEP);
            }
            retval= sb.toString();
        }
        else {
            if (_briefFailReason==null) _briefFailReason= " ";
            if (_userFailReason==null) _userFailReason= " ";
            if (_detailFailReason==null) _detailFailReason= " ";
            retval= "false"+ReqConst.PLOT_RESULT_SEP+
                    _briefFailReason+ReqConst.PLOT_RESULT_SEP+
                    _userFailReason+ReqConst.PLOT_RESULT_SEP+
                    _detailFailReason;
        }
        return retval;
    }
}

