package edu.caltech.ipac.hydra.data;

import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.visualize.plot.WorldPt;
import edu.caltech.ipac.visualize.plot.CoordinateSys;


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

/**
 * Created by IntelliJ IDEA.
 * User: wmi
 * Date: Nov 15, 2011
 * Time: 1:34:03 PM
 * To change this template use File | Settings | File Templates.
 */



public class PlanckCutoutRequest extends TableServerRequest {

    public final static String PLANCK_CUTOUT_PROCESSOR = "PlanckCutoutQuery";

    public final static String CUTOUT_HOST         = "cutoutHost";
    //public final static String URL                = "url";
    public final static String POS                = "POS";
    public final static String MISSION            = "mission";
    public final static String SEARCH_REGION_SIZE = "radius";
    public final static String OBJ_NAME           = "obj_name";
    public final static String SUBSIZE            = "subsize";
    public final static String PIXSIZE            = "pixsize";
    public final static String MAP_SCALE           = "mapscale";
    public final static String MAP_TYPE            = "maptype";
    public final static String OPTBAND            = "band";
    public final static String RA_DEC_J2000       = "RaDecJ2000";




    public PlanckCutoutRequest() {
        super(PLANCK_CUTOUT_PROCESSOR);
    }

    public void setHost(String value) {
        setParam(CUTOUT_HOST, value);
    }
    public String getHost() { return getParam(CUTOUT_HOST); }

    public void setObjName(String value) {
        setParam(OBJ_NAME, value);
    }
    public String getObjName() { return getParam(OBJ_NAME); }


    public void setPos(String value) {
        setParam(POS, value);
    }
    public String getPos() { return getParam(POS); }

    public void setMission(String value) {
        setParam(MISSION, value);
    }
    public String getMission() { return getParam(MISSION); }


    public void setSearchSize(String value) {
        setParam(SEARCH_REGION_SIZE, value);
    }
    public String getSearchSize() { return getParam(SEARCH_REGION_SIZE); }

    public void setSubSize(String value) {
    setParam(SUBSIZE, value);
    }
    public String getSubSize() { return getParam(SUBSIZE); }

    public void setPixSize(String value) {
    setParam(PIXSIZE, value);
    }
    public String getPIXSize() { return getParam(PIXSIZE); }

    public void setMapscale(String value) {
        setParam(MAP_SCALE, value);
    }

    public String getMapscale() { return getParam(MAP_SCALE); }

    public void setMaptype(String value) {
        setParam(MAP_TYPE, value);
    }

    public String getMaptype() { return getParam(MAP_TYPE); }


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

