package edu.caltech.ipac.firefly.data;

public class MOSRequest extends TableServerRequest { // extends WiseRequest {

    public final static String WISE_MOS_PROCESSOR = "MOSQuery";

    public final static String TABLE_NAME         = "table_name";
    public final static String URL                = "url";
    public final static String OBJ_NAME           = "obj_name";
    public final static String OBJ_NAIF_ID        = "obj_naifid";
    public final static String OBJ_PRIM_DES       = "obj_prim_designation";
    public final static String CATALOG            = "catalog";
    public final static String OBS_BEGIN          = "obs_begin";
    public final static String OBS_END            = "obs_end";
    public final static String EPHEM_STEP         = "ephem_step";
    public final static String SEARCH_REGION_SIZE = "search_region_size";
    public final static String OBJ_TYPE           = "obj_type";
    public final static String OUTPUT_MODE        = "output_mode";
    public final static String MPC_DATA           = "mpc_data";
    public final static String INPUT_TYPE         = "input_type";
    public final static String BODY_DESIGNATION   = "body_designation";
    public final static String EPOCH              = "epoch";
    public final static String SEMIMAJOR_AXIS     = "semimajor_axis";
    public final static String ECCENTRICITY       = "eccentricity";
    public final static String PERIH_DIST         = "perih_dist";
    public final static String PERIH_TIME         = "perih_time";
    public final static String INCLINATION        = "inclination";
    public final static String ARG_PERIHELION     = "arg_perihelion";
    public final static String ASCEND_NODE        = "ascend_node";
    public final static String MEAN_ANOMALY       = "mean_anomaly";

    public static final String RESULT_TABLE = "result_table";
    public static final String ORBITAL_PATH_TABLE = "orbital_path_table";

    public MOSRequest() {
        this.setRequestId(WISE_MOS_PROCESSOR);
    }


    public void setTableName(String value) {
        setParam(TABLE_NAME, value);
    }
    public String getTableName() { return getParam(TABLE_NAME); }

    public void setUrl(String value) {
        setParam(URL, value);
    }
    public String getUrl() { return getParam(URL); }


    public void setObjName(String value) {
        setParam(OBJ_NAME, value);
    }
    public String getObjName() { return getParam(OBJ_NAME); }

    public void setNaifID(String value) {
        setParam(OBJ_NAIF_ID, value);
    }
    public String getNaifID() { return getParam(OBJ_NAIF_ID); }

    public void setCatalog(String value) {
        setParam(CATALOG, value);
    }
    public String getCatalog() { return getParam(CATALOG); }


    public void setObsBegin(String value) {
        setParam(OBS_BEGIN, value);
    }
    public String getObsBegin() { return getParam(OBS_BEGIN); }


    public void setObsEnd(String value) {
        setParam(OBS_END, value);
    }
    public String getObsEnd() { return getParam(OBS_END); }


    public void setEphemStep(String value) {
        setParam(EPHEM_STEP, value);
    }
    public String getEphemStep() { return getParam(EPHEM_STEP); }


    public void setSearchRegionSize(String value) {
        setParam(SEARCH_REGION_SIZE, value);
    }
    public String getSearchRegionSize() { return getParam(SEARCH_REGION_SIZE); }


    public void setObjType(String value) {
        setParam(OBJ_TYPE, value);
    }
    public String getObjType() { return getParam(OBJ_TYPE); }


    public void setOutputMode(String value) {
        setParam(OUTPUT_MODE, value);
    }
    public String getOutputMode() { return getParam(OUTPUT_MODE); }

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

