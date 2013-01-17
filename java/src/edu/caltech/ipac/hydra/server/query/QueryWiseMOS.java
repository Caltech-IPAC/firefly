package edu.caltech.ipac.hydra.server.query;

import edu.caltech.ipac.astro.DataGroupQueryStatement;
import edu.caltech.ipac.astro.IpacTableReader;
import edu.caltech.ipac.astro.IpacTableWriter;
import edu.caltech.ipac.firefly.data.MOSRequest;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.WiseRequest;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.ParamDoc;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.query.mos.QueryMOS;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.firefly.ui.creator.CommonParams;
import static edu.caltech.ipac.firefly.util.DataSetParser.LABEL_TAG;

import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static edu.caltech.ipac.firefly.util.DataSetParser.makeAttribKey;


@SearchProcessorImpl(id = "WiseMOSQuery", params = {
        @ParamDoc(name = WiseRequest.HOST, desc = "(optional) the hostname, including port")
})
public class QueryWiseMOS extends QueryMOS {

    private static final Logger.LoggerImpl _log = Logger.getLogger();


    @Override
    protected File loadDynDataFile(TableServerRequest request) throws IOException, DataAccessException {
        File retFile = super.loadDynDataFile(request);
        try {
            MOSRequest req = QueryUtil.assureType(MOSRequest.class, request);
            // nph-most cannot filter results based on 'band', so the results must get post-processed

            String tblType = req.getParam(MOSRequest.TABLE_NAME);

            if (isHeaderOnlyRequest(request)) {

                retFile = getOrbitalElements(retFile);

            } else if (tblType != null && tblType.equalsIgnoreCase(MOSRequest.RESULT_TABLE) && request.containsParam("band")) {
                retFile = removeBands(req, retFile, request.getParam("band"));
            }

        } catch (Exception e) {
            throw makeException(e, "WISE MOS Query Failed.");
        }

        return retFile;
    }

    @Override
    protected String getMosCatalog(MOSRequest req) {
        return req.getMosCatalog();
    }

    private File removeBands(MOSRequest req, File inFile, String bands) {
        File filteredFile = null;

        try {
            filteredFile = makeBandLimitedFileName(req);

            // must use col_idx = 0 because 'band' has the keyword 'and' in it, and this causes issues with DataGroupQueryStatement
            String sql = "select into " + filteredFile.getPath() + " col all from " + inFile.getPath() +
                    " for band IN (" + bands + ") with complete_header";

            DataGroupQueryStatement stmt = DataGroupQueryStatement.parseStatement(sql);
            stmt.execute();

        } catch (Exception e) {
        }

        return filteredFile;
    }

    private File getOrbitalElements(File inFile) {
        final String [] names = {"object_name", "element_epoch", "eccentricity", "inclination",
                "argument_perihelion", "ascending_node", "semimajor_axis", "semimajor_axis", "mean_anomaly",
                "perihelion_distance", "perihelion_time"};
        final List<String> namesLst = Arrays.asList(names);
        File newFile = null;
        try {
            DataGroup dg = IpacTableReader.readIpacTable(inFile, null, false, "Result Table", true);
            Map<String, DataGroup.Attribute> attrMap = dg.getAttributes();


            List<DataType> newDT = new ArrayList<DataType>();
            for (String s : attrMap.keySet()) {
                if (namesLst.contains(s)) {
                    DataGroup.Attribute attr = attrMap.get(s);
                    DataType dt = new DataType(s, attr.getTypeClass());
                    dt.getFormatInfo().setWidth(Math.max(s.length(), attr.formatValue().length()));
                    newDT.add(dt);
                }
            }
            DataGroup newDG = new DataGroup("Orbital Elements", newDT);
            DataObject obj = new DataObject(newDG);
            for (DataType dt : newDT) {
                String col = dt.getKeyName();
                obj.setDataElement(dt, attrMap.get(col).getValue());
                newDG.addAttributes(new DataGroup.Attribute(makeAttribKey(LABEL_TAG, col.toLowerCase()), getOrbitalElementLabel(col)));
            }
            newDG.add(obj);
            newFile = File.createTempFile("orbitalElements" + "-", ".tbl", ServerContext.getTempWorkDir());
            IpacTableWriter.save(newFile, newDG);

        } catch (Exception e) {
            _log.error(e);
        }
        return newFile;
    }

    private String getOrbitalElementLabel(String key) {
        if (key.equals("object_name")) {
            return "Object Name";
        } else if (key.equals("element_epoch")) {
            return "Epoch (MJD)";
        } else if (key.equals("eccentricity")) {
            return "Eccentricity";
        } else if (key.equals("inclination")) {
            return "Inclination";
        } else if (key.equals("argument_perihelion")) {
            return "Argument of Perihelion (deg)";
        } else if (key.equals("ascending_node")) {
            return "Ascending Node  (deg)";
        } else if (key.equals("semimajor_axis")) {
            return "Semi-major Axis (AU)";
        } else if (key.equals("mean_anomaly")) {
            return "Mean Anomaly (deg)";
        } else if (key.equals("perihelion_distance")) {
            return "Perihelion Distance (AU)";
        } else if (key.equals("perihelion_time")) {
            return "Perihelion Time (JD)";
        }
        return key;
    }


    @Override
    public void prepareTableMeta(TableMeta meta, List<DataType> columns, ServerRequest request) {
        super.prepareTableMeta(meta, columns, request);
        WiseRequest req = QueryUtil.assureType(WiseRequest.class, request);
        setXmlParams(req);

        meta.setAttribute(WiseRequest.SCHEMA, request.getParam(WiseRequest.SCHEMA));
        // add cutout parameters, if applicable
        String subsize= request.getParam("subsize");
        if (subsize!=null) {
            meta.setAttribute("subsize", request.getParam("subsize"));
            meta.setAttribute(CommonParams.ZOOM, "1.0");
        }
        String isFull =  StringUtils.isEmpty(subsize) ? "-full" : "-sub";
        String level = req.getSafeParam("ProductLevel");
        meta.setAttribute("ProductLevelAndSize", level + isFull );
        String tblType = req.getParam(MOSRequest.TABLE_NAME);
        if (tblType == null || tblType.equalsIgnoreCase(MOSRequest.RESULT_TABLE)) {
            meta.setCenterCoordColumns(new TableMeta.LonLatColumns("ra_obj", "dec_obj"));
        } else {
            meta.setCenterCoordColumns(new TableMeta.LonLatColumns("RA_obs", "Dec_obs"));
        }
    }

    private static File makeBandLimitedFileName(MOSRequest req) throws IOException {
        return File.createTempFile("wise-mos-catalog-original-bands", ".tbl", ServerContext.getPermWorkDir());
    }

    private static IOException makeException(Exception e, String reason) {
        IOException eio = new IOException(reason);
        eio.initCause(e);
        return eio;
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
