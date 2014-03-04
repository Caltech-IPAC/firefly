package edu.caltech.ipac.hydra.server.query;

import edu.caltech.ipac.astro.DataGroupQueryStatement;
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
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;


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

            if (!isHeaderOnlyRequest(request) && tblType != null && tblType.equalsIgnoreCase(MOSRequest.RESULT_TABLE) && request.containsParam("band")) {
                retFile = removeBandsAndSets(req, retFile, request.getParam("band"), request.getParam(WiseRequest.SCHEMA));
            }

        } catch (Exception e) {
            throw makeException(e, "WISE MOS Query Failed.");
        }

        return retFile;
    }

    @Override
    protected String getMosCatalog(MOSRequest req) {
        return WiseRequest.getMosCatalog(req);
    }

    private File removeBandsAndSets(MOSRequest req, File inFile, String bands, String schema) {
        File filteredFile = null;

        try {
            filteredFile = makeBandLimitedFileName(req);

            String imageSetConstraint = getImageSetConstraint(schema);

            // must use col_idx = 0 because 'band' has the keyword 'and' in it, and this causes issues with DataGroupQueryStatement
            String sql = "select into " + filteredFile.getPath() + " col all from " + inFile.getPath() +
                    " for band IN (" + bands + ")" + (imageSetConstraint.length()<2 ? "" : " and "+imageSetConstraint)
                    + " with complete_header";

            DataGroupQueryStatement stmt = DataGroupQueryStatement.parseStatement(sql);
            stmt.execute();

        } catch (Exception e) {
        }

        return filteredFile;
    }

    private String getImageSetConstraint(String schema) {
        String imageSets[] = schema.split(",");
        String imageSetConstraint = "";
        if (WiseRequest.useMergedTable(schema) && imageSets.length<3) {
            int n = 0;
            if (imageSets.length > 1) {
                imageSetConstraint += "image_set IN (";
            } else {
                imageSetConstraint += "image_set=";
            }
            if (schema.contains(WiseRequest.ALLSKY_4BAND)) {
                imageSetConstraint += "4";
                n++;
            }
            if (schema.contains(WiseRequest.CRYO_3BAND)) {
                if (n>0) imageSetConstraint += ",3";
                else imageSetConstraint += "3";
                n++;
            }
            if (schema.contains(WiseRequest.POSTCRYO)) {
                if (n>0) imageSetConstraint += ",2";
                else imageSetConstraint += "2";
                n++;
            }

            if (imageSets.length > 1) {
                imageSetConstraint += ")";
            }
        }
        return imageSetConstraint;
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
    }

    private static File makeBandLimitedFileName(MOSRequest req) throws IOException {
        return File.createTempFile("wise-mos-catalog-original-bands", ".tbl", ServerContext.getPermWorkDir());
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
