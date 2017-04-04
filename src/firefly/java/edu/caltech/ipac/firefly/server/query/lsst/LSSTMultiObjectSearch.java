package edu.caltech.ipac.firefly.server.query.lsst;

import edu.caltech.ipac.firefly.server.query.*;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupWriter;
import edu.caltech.ipac.util.*;

import java.io.File;
import java.io.IOException;


/**
 * Created by zhang on 3/16/17.
 * The LSSTMultiObjectSearch reads in the .tbl or .csv input file and then does a parallel search.  The search results
 * is an array of DataGroup.  The array of DataGroup is combined to one big data group.  The redundancy  is nto removed.
 *
 * The input file can be a .tbl or .csv format.
 * 1. If the input table contains only ra and dec, the radius in the UI will be used, the search type is cone.
 * 2. If the input table contains ra, dec, major,  the major will be used as a radius, the search type is cone.
 * 3. If the input table contains ra, dec, major, ratio, the major is used as the semi-major and the serach type is
 *  elliptical. Since there is no angle column, the angle will be 0.
 * 4. If the input table contains ra, dec, major, ratio, angle, the search type is elliptical and the angle will be used.
 * NOTE:
 * The unit for ra, dec and angle is in degree and the major is in arcsec, major means semi-major
 */

@SearchProcessorImpl(id = "LSSTMultiObjectSearch")
public class LSSTMultiObjectSearch extends  LSSTConcurrentSearch {

    @Override
    protected File loadDataFile(TableServerRequest request) throws IOException, DataAccessException {

        int nThread = Integer.parseInt(AppProperties.getProperty("lsst.concurrent.nthread", "2"));

        try {
            DataGroup dg =  doConcurrentSearch(request, nThread);
            dg.shrinkToFitData();
            File outFile = createFile(request, ".tbl");
            DataGroupWriter.write(outFile, dg);
            return  outFile;

        } catch (Exception e) {
            e.printStackTrace();
            throw new DataAccessException("ERROR:" + e.getMessage(), e);
        }

    }

}
