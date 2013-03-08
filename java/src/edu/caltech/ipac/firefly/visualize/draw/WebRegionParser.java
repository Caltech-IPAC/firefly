package edu.caltech.ipac.firefly.visualize.draw;
/**
 * User: roby
 * Date: 2/13/13
 * Time: 2:39 PM
 */


import edu.caltech.ipac.astro.CoordException;
import edu.caltech.ipac.firefly.visualize.conv.CoordUtil;
import edu.caltech.ipac.util.RegionFactory;
import edu.caltech.ipac.util.StringTokenizer;
import edu.caltech.ipac.util.dd.Global;
import edu.caltech.ipac.util.dd.RegionFileElement;
import edu.caltech.ipac.util.dd.RegParseException;
import edu.caltech.ipac.util.dd.RegionCsys;
import edu.caltech.ipac.util.dd.RegionOptions;

import java.util.List;

/**
 * @author Trey Roby
 */
public class WebRegionParser {

    private static boolean init= false;

    public RegionFactory.ParseRet processInput(String inData) {
        final StringTokenizer st= new StringTokenizer(inData,"\n");
        return RegionFactory.processInput(new RegionFactory.LineGetter() {
            public String getLine() {
                return st.hasMoreToken() ? st.nextToken() : null;
            }
        });
    }


    public static List<RegionFileElement> parsePart(String inString) throws RegParseException {
        checkInit();
        return RegionFactory.parsePart(inString, RegionCsys.PHYSICAL, new Global(new RegionOptions()),false);
    }


    public static List<RegionFileElement> parsePart(String inString, RegionCsys coordSys, Global global, boolean allowHeader)
            throws RegParseException{
        checkInit();
        return RegionFactory.parsePart(inString, coordSys,global,allowHeader);
    }


    private static void checkInit() {
        if (!init) {
            RegionFactory.setCoordConverter(new RegionFactory.CoordConverter() {
                public double convertStringToLon(String hms) throws CoordException {
                    return CoordUtil.convertStringToLon(hms,true);
                }

                public double convertStringToLat(String dms) throws CoordException {
                    return CoordUtil.convertStringToLon(dms,true);
                }
            });
            init= true;
        }
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
