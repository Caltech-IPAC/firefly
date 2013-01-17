package edu.caltech.ipac.heritage.server.query;

import edu.caltech.ipac.astro.IpacTableException;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.ipactable.IpacTableParser;
import edu.caltech.ipac.heritage.server.persistence.UtilDao;
import edu.caltech.ipac.heritage.server.persistence.ProprietaryInfo;
import edu.caltech.ipac.visualize.plot.CircleException;
import edu.caltech.ipac.visualize.plot.CombinePolygons;
import edu.caltech.ipac.visualize.plot.CoveragePolygons;
import edu.caltech.ipac.visualize.plot.ImageCorners;

import java.io.File;
import java.io.IOException;
import java.util.*;


/**
 * Date: Jul 15, 2008
 *
 * @author loi
 * @version $Id: SearchManager.java,v 1.41 2010/12/03 19:09:46 roby Exp $
 */
public class SearchManager {

   public static final Logger.LoggerImpl SEARCH_LOGGER = Logger.getLogger(Logger.SEARCH_LOGGER);


    public Map<String, String> getAbstractInfo(int progId) throws IOException, IpacTableException {
        return UtilDao.getAbstractInfo(progId);
    }

    public List<Integer> getBcdIds(String pbcdFile, List<Integer> fileRowIndices) throws IOException, IpacTableException {
        File inf = new File(pbcdFile);
        if (!inf.canRead()) {
            throw new IOException("File not found:" + inf.getAbsoluteFile());
        }
        IpacTableParser.MappedData values = IpacTableParser.getData(inf, fileRowIndices, "pbcdid");
        Integer[] pbcdids = new Integer[values.values().size()];

        if (pbcdids.length == 0) {
            throw new IpacTableException("No data found:" + inf.getAbsoluteFile());
        }

        int i = 0;
        for(Iterator itr = values.values().iterator(); itr.hasNext(); i++) {
            pbcdids[i] = Integer.parseInt(String.valueOf(itr.next()));
        }
        return UtilDao.getBcdIds(pbcdids);
    }



    public CoveragePolygons getAorCoverage(int requestID) {
        List<ImageCorners> corners = UtilDao.getBcdCornersByRequestID(requestID);
        CombinePolygons combine_polygons = new CombinePolygons();
        CoveragePolygons coverage = null;
        try
        {
            coverage = combine_polygons.getCoverage(corners);
        }
        catch (CircleException ce)
        {
            // do something
        }
        return coverage;
    }

    public List<String> getObservers() {
        return UtilDao.getObservers();
    }

    public ProprietaryInfo getProprietary() {
        return UtilDao.getPropriertaryReqkeys();
    }

    public Map<String, List<String>> getProgramReqkeys() {
        return UtilDao.getProgramReqkeys();
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
