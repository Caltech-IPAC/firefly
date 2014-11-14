package edu.caltech.ipac.hydra.data;

import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.WorldPt;

/**
 * Created by IntelliJ IDEA.
 * User: wmi
 * Date: Apr 15, 2011
 * Time: 1:34:03 PM
 * To change this template use File | Settings | File Templates.
 */



public class PlanckTOITAPRequest extends TableServerRequest {

    public final static String PLANCK_TOITAP_PROCESSOR = "planckTOITAPQuery";

    public final static String TOITAP_HOST        = "toitapHost";
    public final static String TOIMinimap_HOST    = "toiminimapHost";
    public final static String URL                = "url";
    public final static String POS                = "locstr";
    public final static String TYPE               = "type";
    public final static String SSOFLAG            = "ssoflag";
    public final static String SEARCH_REGION_SIZE = "radius";
    public final static String SEARCH_BOX_SIZE    = "boxsize";
    public final static String OBJ_NAME           = "obj_name";
    public final static String OPTBAND            = "planckfreq";
    public final static String RA_DEC_J2000       = "RaDecJ2000";
    public final static String CFRAME             = "cframe";
    public final static String CDELT             = "cdelt";
    public final static String ROTANG            = "rotang";
    public final static String ITERATIONS        = "iterations";
    public final static String DETC100           = "detc100";
    public final static String DETC143           = "detc143";
    public final static String DETC217           = "detc217";
    public final static String DETC30            = "detc30";
    public final static String DETC44            = "detc44";
    public final static String DETC70            = "detc70";
    public final static String DETC353           = "detc53";
    public final static String DETC545           = "detc545";
    public final static String DETC857           = "detc857";

    public final static String DETECTOR            = "detector";
    public final static String TIMESTART          = "timeStart";
    public final static String TIMEEND            = "timeEnd";

    public final static String detc030_all = "27M,27S,28M,28S";
    public final static String detc044_all = "24M,24S,25M,25S,26M,26S";
    public final static String detc070_all = "18M,18S,19M,19S,20M,20S, 21M,21S,22M,22S,23M,23S";
    public final static String detc100_all = "1A,1B,2A,2B,3A,3B,4A,4B";
    public final static String detc143_all = "1A,1B,2A,2B,3A,3B,4A,4B,5,6,7";
    public final static String detc217_all = "1,2,3,4,,5A,5B,6A,6B,7A,7B,8A,8B";


    public PlanckTOITAPRequest() {
        super(PLANCK_TOITAP_PROCESSOR);
    }

    public void setHost(String value) {
        setParam(TOITAP_HOST, value);
    }
    public String getHost() { return getParam(TOITAP_HOST); }

    public void setTOIMinimap_HOST(String value) {
        setParam(TOIMinimap_HOST, value);
    }
    public String getTOIMinimap_HOST() { return getParam(TOIMinimap_HOST); }

    public void setUrl(String value) {
        setParam(URL, value);
    }
    public String getUrl() { return getParam(URL); }


    public void setObjName(String value) {
        setParam(OBJ_NAME, value);
    }
    public String getObjName() { return getParam(OBJ_NAME); }


    public void setPos(String value) {
        setParam(POS, value);
    }
    public String getPos() { return getParam(POS); }

    public void setType(String value) {
        setParam(TYPE, value);
    }
    public String getType() { return getParam(TYPE); }


    public void setSearchSize(String value) {
        setParam(SEARCH_REGION_SIZE, value);
    }
    public String getSearchSize() { return getParam(SEARCH_REGION_SIZE); }

    public void setSearchBoxSize(String value) {
        setParam(SEARCH_BOX_SIZE, value);
    }
    public String getSearchBoxSize() { return getParam(SEARCH_BOX_SIZE); }

    public void setDetector(String value) {
        setParam(DETECTOR, value);
    }
    public String getDetector() { return getParam(DETECTOR); }

    public void setOptband(String value) {
            setParam(OPTBAND, value);
        }
        public String getOptband() { return getParam(OPTBAND); }

    public void settBegin(String value) {
            setParam(TIMESTART, value);
        }
        public String gettBegin() { return getParam(TIMESTART); }

    public void settEnd(String value) {
            setParam(TIMEEND, value);
        }
        public String gettEnd() { return getParam(TIMEEND); }

    public void setWorldPtJ2000(WorldPt pt) {
        assert pt.getCoordSys()== CoordinateSys.EQ_J2000;
        setParam(RA_DEC_J2000, pt.getLon() + " " + pt.getLat());
    }

    public WorldPt getWorldPtJ2000() {
        String ptStr= getParam(RA_DEC_J2000);
        String s[]= ptStr.split(" ");
        return new WorldPt(asDouble(s[0]), asDouble(s[1]));
    }


    private static double asDouble(String dStr) {
        double retval;
        try {
            retval= Double.parseDouble(dStr);
        } catch (NumberFormatException e) {
            retval= 0.0;
        }
        return retval;
    }


}

/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313)
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