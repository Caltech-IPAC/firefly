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

