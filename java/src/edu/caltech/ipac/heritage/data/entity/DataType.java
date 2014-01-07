package edu.caltech.ipac.heritage.data.entity;

import edu.caltech.ipac.firefly.data.DataEntry;


/**
 * @author Trey Roby
 */
public enum DataType implements DataEntry {
            AOR("Observation Requests (AOR)", "Sets of observations with some or all data satisfying the search criteria."),
            BCD("Level 1 (BCD)", "The smallest units of calibrated data."),
            PBCD("Level 2 (PBCD)", "Data products made by combining together level 1 data, e.g. mosaics and coadded spectra."),
            SM("Super Mosaics", "Super Mosaics, created by combining together level 1 data from adjacent observations"),
            SOURCE_LIST("Source List", "Sources, extracted from enhanced images"),
            LEGACY("Contributed Products", "Enhanced products, created by Legacy Teams"),
            IRS_ENHANCED("IRS Enhanced", "IRS Enhanced data, created by Spitzer Team"),
            CAL, RAW, BCD_ANCIL, PBCD_ANCIL, SM_ANCIL, LEGACY_ANCIL, ASSORTED,
            MOS;

    private String title;
    private String shortDesc;

    DataType() {
        title = name();
        shortDesc = name();
    }

    DataType(String title, String shortDesc) {
        this.title = title;
        this.shortDesc = shortDesc;
    }

    public String getTitle() {
        return title;
    }
    public String getShortDesc() {
        return shortDesc;
    }

    public static DataType parse(String s) {
        DataType retval;
        try {
            retval= Enum.valueOf(DataType.class,s);
        } catch (Exception e) {
            retval= null;
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
