package edu.caltech.ipac.hydra.data;

import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.WorldPt;
import edu.caltech.ipac.firefly.data.TableServerRequest;

public class PtfRequest extends TableServerRequest {

    public final static String PTF_PROCESSOR  = "PtfQuery";

    public final static String HOST            = "host";
    public final static String SERVICE         = "service";
    public final static String SCHEMA_GROUP    = "schemaGroup";
    public final static String SCHEMA          = "schema";
    public final static String TABLE           = "table";
    public final static String FILENAME        = "filename";
    public final static String POS             = "POS";
    public final static String SIZE            = "SIZE";
    public final static String MCEN            = "mcen";
    public final static String INTERSECT       = "INTERSECT";
    public final static String RA_DEC_J2000    = "RaDecJ2000";
    public final static String PRODUCT_LEVEL   = "ProductLevel";

    public final static String EXP_ID          = "expid";
    public final static String FIELD_ID        = "ptffield";
    public final static String CCD_ID          = "ccdid";
    public final static String FILTER          = "filter";
    public final static String FILTER_ID       = "fid";
    public final static String PIMAGE          = "pfilename";
    public final static String RIMAGE          = "rfilename";
    public final static String MIMAGE          = "afilename1";
    public final static String SECATL          = "afilename3";
    public final static String PSFCATL         = "afilename4";

    public final static String REFIMAGE        = "filename";
    public final static String RAWPSF          = "rawpsffilename";
    public final static String PSFGRID         = "psfgridfilename";
    public final static String PSFDS9REG       = "psfds9regfilename";
    public final static String DEPTH           = "depthfilename";
    public final static String UNCT            = "uncfilename";
    public final static String SEXRDCAT        = "sexrfcatfilename";
    public final static String PSFRFCAT        = "psfrfcatfilename";





//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public PtfRequest() {
        super(PTF_PROCESSOR);
    }

    public void setHost(String value) {
        setParam(HOST, value);
    }

    public String getHost() { return getParam(HOST); }


    public void setFilename(String value) {
        setParam(FILENAME, value);
    }

    public String getFilename() { return getParam(FILENAME); }


    public void setSchemaGroup(String value) {
        setParam(SCHEMA_GROUP, value);
    }

    public String getSchemaGroup() { return getParam(SCHEMA_GROUP); }


    public void setSchema(String value) {
        setParam(SCHEMA, value);
    }

    public String getSchema() { return getParam(SCHEMA); }


    public void setTable(String value) {
        setParam(TABLE, value);
    }

    public String getTable() { return getParam(TABLE); }


    public void setPos(String value) {
        setParam(POS, value);
    }

    public String getPos() { return getParam(POS); }


    public void setSize(String xValue) {
        setSize(xValue, null);
    }

    public void setSize(String xValue, String yValue) {
        setParam(SIZE, (yValue==null) ? xValue : xValue + "," + yValue);
    }

    public String getSize() { return getParam(SIZE); }


    public void setIntersect(String value) {
        setParam(INTERSECT, value);
    }

    public String getIntersect() { return getParam(INTERSECT); }


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
