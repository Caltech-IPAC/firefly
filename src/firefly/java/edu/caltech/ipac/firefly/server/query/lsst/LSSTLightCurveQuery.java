package edu.caltech.ipac.firefly.server.query.lsst;

import edu.caltech.ipac.firefly.core.EndUserException;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.table.TableMeta;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.table.DataType;

import java.util.List;

import static edu.caltech.ipac.firefly.data.table.MetaConst.DATASET_CONVERTER;
import static edu.caltech.ipac.firefly.data.table.MetaConst.IMAGE_SOURCE_ID;

/**
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 *
 * @author tatianag
 */

@SearchProcessorImpl(id="LSSTLightCurveQuery")
public class LSSTLightCurveQuery extends LSSTQuery {

    @Override
    String buildSqlQueryString(TableServerRequest request) throws DataAccessException, EndUserException {
        //Sample query from https://confluence.lsstcorp.org/display/DM/PDAC+sample+queries+and+test+cases:
        //get time series table based on the objectId (id in RunDeepSource and cntr in allwise_p3as_psd) from the object table
        //for SDSS:
        //SELECT objectId, id, fsrc.exposure_id, fsrc.exposure_time_mid, exp.run,
        //   scisql_dnToAbMag(fsrc.flux_psf,exp.fluxMag0) AS g,
        //   scisql_dnToAbMagSigma(fsrc.flux_psf, fsrc.flux_psf_err, exp.fluxMag0, exp.fluxMag0Sigma) AS gErr
        // FROM RunDeepForcedSource AS fsrc, Science_Ccd_Exposure AS exp
        // WHERE exp.scienceCcdExposureId = fsrc.exposure_id
        //   AND fsrc.exposure_filter_id=1 AND objectId=3448068867358968
        // ORDER BY exposure_time_mid
        //for wise:
        //SELECT *
        //FROM  wise_00.allwise_p3as_mep
        //WHERE <inherit sql constraints and geometric constraints from object table search> AND cntr_mf=<objectId>

        String tableName = request.getParam("table_name");
        String forcedTable = LSSTQuery.getTableColumn(tableName, "forcedSourceTable");
        String [] parts = forcedTable.split("\\.");
        if (parts.length != 3) {
            throw new DataAccessException("Unsupported table name: "+forcedTable);
        }
        // table should be specified by 3 parts: logical database, resource schema, and table name
        String database = parts[0]+"."+parts[1];

        String objectId = request.getParam("objectId");
        if (objectId == null) {
            throw new EndUserException("Invalid parameter", "Missing objectId");
        }

        String forcedObjectColumn = LSSTQuery.getTableColumn(forcedTable, "objectColumn");
        String forcedFilterColumn = LSSTQuery.getTableColumn(forcedTable, "filterColumn");
        String filterId = forcedFilterColumn != null ? request.getParam("filterId") : null;

        String requestStr = "";
        String mission = (String)LSSTQuery.getDatasetInfo(tableName, new String[]{DATASET_CONVERTER});
        if (mission == null) {
            throw new DataAccessException(DATASET_CONVERTER + " is not specifiedl for " + tableName);
        }

        if (mission.toLowerCase().contains("sdss")) {
            String exp = database + ".Science_Ccd_Exposure";
            requestStr = "SELECT fsrc.exposure_time_mid, fsrc.coord_ra, fsrc.coord_decl, "+
                    "scisql_dnToFlux(fsrc.flux_psf, exp.fluxMag0) AS tsv_flux, "+
                    "scisql_dnToFluxSigma(fsrc.flux_psf, fsrc.flux_psf_err, exp.fluxMag0, exp.fluxMag0Sigma) AS tsv_fluxErr, "+
                    "scisql_dnToAbMag(fsrc.flux_psf,exp.fluxMag0) AS mag, "+
                    "scisql_dnToAbMagSigma(fsrc.flux_psf, fsrc.flux_psf_err, exp.fluxMag0, exp.fluxMag0Sigma) AS magErr, "+
                    "exp.run, exp.camcol, exp.field, exp.filterName, exp.scienceCcdExposureId, " + forcedObjectColumn + ", id "+
                    " FROM " + forcedTable + " AS fsrc, " +  exp + " AS exp "+
                    " WHERE exp.scienceCcdExposureId = fsrc.exposure_id "+
                    " AND " + forcedObjectColumn+"="+objectId+(filterId == null ? "" : " AND fsrc." + forcedFilterColumn + "=" + filterId)+
                    " ORDER BY exposure_time_mid";
        } else if (mission.toLowerCase().contains("wise")) {
            String objectIdConstraints = forcedObjectColumn+" = "+objectId;
            String constraints = buildExistingConstraints(request, objectIdConstraints );

            requestStr  = "SELECT *" +
                          " FROM " + forcedTable +
                          " WHERE " + constraints;
        }
        return requestStr;
    }

    private String buildExistingConstraints(TableServerRequest request, String objectIdConstraints) throws EndUserException {
        String constraints = LSSTCatalogSearch.getConstraints(request);
        String searchMethod = LSSTCatalogSearch.getSearchMethodCatalog(request);
        String whereStr;

        if (searchMethod.length()==0 && constraints.length()==0) {
            return objectIdConstraints;
        } else if (searchMethod.length()>0 && constraints.length()>0){
            whereStr = searchMethod +  " AND " + constraints;
        } else {
            whereStr = (searchMethod.length() > 0) ? searchMethod : constraints;
        }

        return whereStr + " AND " + objectIdConstraints;
    }

    @Override
    public void prepareTableMeta(TableMeta meta, List<DataType> columns, ServerRequest request) {
        super.prepareTableMeta(meta, columns, request);
        meta.setAttribute(IMAGE_SOURCE_ID, "lsst_sdss");
        
        String tableName = request.getParam("table_name");

        // add ra&dec column name info
        if (LSSTQuery.isCatalogTable(tableName)) {
            Object RA = LSSTQuery.getRA(tableName);
            Object DEC = LSSTQuery.getDEC(tableName);

            TableMeta.LonLatColumns llc = new TableMeta.LonLatColumns((String) RA, (String) DEC);
            meta.setCenterCoordColumns(llc);
        }
    }

}
