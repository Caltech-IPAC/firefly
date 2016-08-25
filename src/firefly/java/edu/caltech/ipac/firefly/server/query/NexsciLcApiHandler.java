/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import org.apache.commons.lang.NotImplementedException;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Class to provide and handle call to NexSci LC api, returning 2 main files
 * (after parsing VOtable result)
 *
 * @author ejoliet
 */
public class NexsciLcApiHandler implements LightCurveHandler {


    /**
     * TODO Update with correct API root url
     */
    public final String rootApiUrl = "http://irsa.ipac.caltech.edu/periodogram";

    /**
     * TODO check ordinals when API call is implemented
     * Represent the tables in votable result from API: 0 for periodogram, 1 for peaks
     */
    enum RESULT_TABLES_IDX {
        PERIODOGRAM, PEAKS
    }

    @Override
    public File getPeriodogramTable(PeriodogramAPIRequest request) {
        throw new NotImplementedException("Not yet implemented");
    }

    /**
     * Return the API URL to be called to get VOTable from Nexsci, which will contain 2 tables: periodogram and peaks tables.
     * <p>
     * TODO need to be implemented
     *
     * @param request object to contain the required paramter to make the API call
     * @return the URL object
     * @throws MalformedURLException
     * @see edu.caltech.ipac.firefly.server.query.LightCurveProcessor#computePeriodogram(edu.caltech.ipac.firefly.server.query.PeriodogramAPIRequest, java.lang.String)
     * @see IrsaLightCurveHandler#buildUrl(PeriodogramAPIRequest)
     */
    protected URL buildUrl(PeriodogramAPIRequest request) throws MalformedURLException {
        //loop other request and append to rootApiUrl
        throw new NotImplementedException("Not yet implemented");
    }

    @Override
    public File getPeaksTable(PeriodogramAPIRequest request) {
        throw new NotImplementedException("Not yet implemented");
    }
}
