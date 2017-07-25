package edu.caltech.ipac.firefly.server.query.lsst;

import edu.caltech.ipac.firefly.core.EndUserException;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.MetaConst;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.util.DataType;

import java.util.List;

/**
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 *
 * @author tatianag
 */

@SearchProcessorImpl(id="LSSTLightCurveQuery")
public class LSSTLightCurveQuery extends LSSTQuery {

    @Override
    String buildSqlQueryString(TableServerRequest request) throws Exception {
        //Sample query from https://confluence.lsstcorp.org/display/DM/PDAC+sample+queries+and+test+cases :
        //get time series table based on the objectId (id in RunDeepSource and cntr in allwise_p3as_psd) from the object table
        //for sdss:
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

        String database = request.getParam("database");
        String tableName = request.getParam("table_name");
        String forcedTable = LSSTQuery.getTableColumn(database, tableName, "forcedSourceTable");
        String fsrc = (database != null && forcedTable != null) ?  database+'.'+forcedTable : null;

        if (fsrc == null) {
            throw new EndUserException("Invalid parameter", "Missing forced source table");
        }

        String objectId = request.getParam("objectId");
        if (objectId == null) {
            throw new EndUserException("Invalid parameter", "Missing objectId");
        }

        String forcedObjectColumn = LSSTQuery.getTableColumn(database, forcedTable, "objectColumn");
        String forcedFilterColumn = LSSTQuery.getTableColumn(database, forcedTable, "filterColumn");
        String filterId = forcedFilterColumn != null ? request.getParam("filterId") : null;

        String requestStr = new String();
        String mission = (String)LSSTQuery.getDatasetInfo(database, tableName, new String[]{MetaConst.DATASET_CONVERTER});

        if (mission.toLowerCase().contains("sdss")) {
            String exp = database + ".Science_Ccd_Exposure";
            requestStr = "SELECT fsrc.exposure_time_mid, fsrc.coord_ra, fsrc.coord_decl, "+
                    "scisql_dnToFlux(fsrc.flux_psf, exp.fluxMag0) AS tsv_flux, "+
                    "scisql_dnToFluxSigma(fsrc.flux_psf, fsrc.flux_psf_err, exp.fluxMag0, exp.fluxMag0Sigma) AS tsv_fluxErr, "+
                    "scisql_dnToAbMag(fsrc.flux_psf,exp.fluxMag0) AS mag, "+
                    "scisql_dnToAbMagSigma(fsrc.flux_psf, fsrc.flux_psf_err, exp.fluxMag0, exp.fluxMag0Sigma) AS magErr, "+
                    "exp.run, exp.camcol, exp.field, exp.filterName, exp.scienceCcdExposureId, " + forcedObjectColumn + ", id "+
                    " FROM " + fsrc + " AS fsrc, " +  exp + " AS exp "+
                    " WHERE exp.scienceCcdExposureId = fsrc.exposure_id "+
                    " AND " + forcedObjectColumn+"="+objectId+(filterId == null ? "" : " AND fsrc." + forcedFilterColumn + "=" + filterId)+
                    " ORDER BY exposure_time_mid";
        } else if (mission.toLowerCase().contains("wise")) {
            String objectIdConstraints = forcedObjectColumn+" = "+objectId;
            String constraints = buildExistingConstraints(request, objectIdConstraints );

            requestStr  = "SELECT *" +
                          " FROM " + fsrc +
                          " WHERE " + constraints +";";
        }
        return requestStr;
    }

    String buildExistingConstraints(TableServerRequest request, String objectIdConstraints) throws Exception {
        String constraints = LSSTCataLogSearch.getConstraints(request);
        String searchMethod = LSSTCataLogSearch.getSearchMethodCatalog(request);
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
        meta.setAttribute(MetaConst.DATASET_CONVERTER, "lsst_sdss");
    }

}
