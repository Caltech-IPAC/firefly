/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.catquery;


import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.MetaConst;
import edu.caltech.ipac.firefly.server.network.IpacTableHandler;
import edu.caltech.ipac.firefly.server.network.HttpServiceInput;
import edu.caltech.ipac.firefly.server.network.HttpServices;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.EmbeddedDbProcessor;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.TableMeta;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.CoordinateSys;

import static edu.caltech.ipac.firefly.server.catquery.GatorMOS.PROC_ID;
import static edu.caltech.ipac.util.StringUtils.applyIfNotEmpty;
import static edu.caltech.ipac.util.StringUtils.isEmpty;

/**
 * https://irsadev.ipac.caltech.edu/applications/Gator/GatorAid/irsa/catsearch.html#moving
 * NOTE: Catalog needs to be moving-object search enabled
 */
@SearchProcessorImpl(id = PROC_ID)
public class GatorMOS extends EmbeddedDbProcessor {

    /**
     * URL format
     * https://irsa.ipac.caltech.edu/cgi-bin/Gator/nph-query?searchForm=MO&spatial=cone&
     * [keyword1=value1]&[keyword2=value2]&...[keywordn=valuen]
     */
    private static final String GATOR_HOST = AppProperties.getProperty("irsa.gator.hostname", "https://irsa.ipac.caltech.edu");
    private static final String SRV_URL = "/cgi-bin/Gator/nph-query";


    public static final String PROC_ID = "GatorMOS";
    public enum Param {
        catalog,        // required; Catalog name.
        mobj,           // required; Type of input
                        //      smo = by name or number
                        //      mpc = MPC format
                        //      obt = orbital elements
        moradius,       // optional;  Cone search radius (arcsec)
        mobjstr,        // required for smo;	Name or numeric designation of object (see above)
        mobjtype,       // required for mpc or obt	Solar-system object type
        mpc,            // required for mpc input;	MPC string
        mobjmaj,        // required for obt;	Semi-major axis (asteroid) or perihelion distance (comet) in AU
        perih_dist,     // required for obt;	Perihelion distance (comet) in AU
        mobjecc,        // required for obt;	Eccentricity of orbit
        mobjinc,        // required for obt;	Inclination of orbit (deg)
        mobjper,        // required for obt;	Argument of perihelion (deg)
        mobjasc,        // required for obt;	Longitude of ascending node (deg)
        mobjanom,       // required for obt;	Mean anomaly (asteroid) in deg
        perih_time,     // required for obt;	Perihelion time (comet) in yyyy+mm+dd+hh:mm:ss
        mobjdsg,        // required for obt;	Designation for returned ephemeris
        mobjepo,        // required for obt;	Epoch of coordinates in MJD (Modified Julian Date)
        btime,          // optional;	Earliest observation date (UT) to include
        etime,          // optional;	Latest observation date (UT) to include
        outfmt,         // optional; 	Output format
                        //      0: HTML (default)
                        //      1: ASCII table
                        //      2: SVC (software handshaking structure) message
                        //      3: VO Table
                        //      6: XML output
        selcols,        // optional;  Comma-separated list of output columns desired
        outrows         // optional;  Maximum number of rows to return
    }

    private static final String RA = "ra";
    private static final String DEC = "dec";
    public DataGroup fetchDataGroup(TableServerRequest req) throws DataAccessException {
        HttpServiceInput input = createInput(req);
        if (req.getParam(Param.outfmt.name()) == null) {
            input.setParam(Param.outfmt.name(), "1");       // defaults to ASCII table, or it will fail because API default is 0(HTML).
        }

        IpacTableHandler handler = new IpacTableHandler();
        HttpServices.Status status = HttpServices.getData(input, handler);
        if (status.isError()) {
            handleError(status.getErrMsg());
        }
        DataGroup results = handler.results;
        TableMeta.LonLatColumns centcol = new TableMeta.LonLatColumns(RA, DEC, CoordinateSys.EQ_J2000);
        results.getTableMeta().setCenterCoordColumns(centcol);
        results.getTableMeta().setAttribute(MetaConst.CATALOG_OVERLAY_TYPE, "TRUE");
        return handler.results;
    }

    private void handleError(String msg) throws DataAccessException{
        String[] parts = StringUtils.groupMatch(".*msg=\"(.+)\".*", msg);
        msg = parts.length > 0 ? parts[0] : msg;
        throw new DataAccessException(msg);
    }

    /**
     * @param treq
     * @return
     */
    private static HttpServiceInput createInput(TableServerRequest treq) throws DataAccessException {

        HttpServiceInput input = HttpServiceInput.createWithCredential(GATOR_HOST + SRV_URL);
        input.setParam("searchForm", "MO")
             .setParam("spatial", "cone");


        for (Param p : Param.values()) {
            applyIfNotEmpty(treq.getParam(p.name()), v -> input.setParam(p.name(), v));
        }
        String errMsg = isValidRequest(input);
        if (isEmpty(errMsg)) {
            return input;
        } else {
            throw new DataAccessException(errMsg);
        }
    }

    private static String isValidRequest(HttpServiceInput input) {
        //  todo: optional... implements validation based on Param's API documentation
        return null;
    }

}

