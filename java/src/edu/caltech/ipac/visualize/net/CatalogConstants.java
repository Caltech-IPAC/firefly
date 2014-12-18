package edu.caltech.ipac.visualize.net;

/**
 * This class defines contatnts that are used by catalog getter.
 * @author Michael Nguyen
 */

public interface CatalogConstants
{
    /* IRAS Point Source Catalog */
    public final static String PT_SRC_CAT_IRAS = "iraspsc";

    /* IRAS Faint Source Catalog */
    public final static String FT_SRC_CAT_IRAS = "irasfsc";

    /* 2MASS Extended Source Catalog */
    public final static String EXT_SRC_CAT_2MASS = "ext_src_cat";
    public final static String EXT_SRC_DB_2MASS = "ntmass";
    public final static String
            EXT_SRC_QUERY_2MASS = "select ra,dec,designation,j_m,h_m,k_m";

    /* 2MASS Point Source Catalog */
    public final static String PT_SRC_CAT_2MASS = "pt_src_cat";

    /* MSX catalog */
    public final static String MSX = "msx";

    /* NED Catalog */
    public final static String CAT_NED = "ned_cat";

    /* ALLWISE Source Catalog */
    public final static String ALLWISE = "wise_allwise_p3as_psd";

    /* SEIP Source Catalog */
    public final static String SEIP = "slphotdr4";

}

