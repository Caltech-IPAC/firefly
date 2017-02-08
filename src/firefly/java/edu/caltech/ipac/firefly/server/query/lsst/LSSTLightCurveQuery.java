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

@SearchProcessorImpl(id = "LSSTLightCurveQuery")
public class LSSTLightCurveQuery extends LSSTQuery {

    @Override
    String buildSqlQueryString(TableServerRequest request) throws Exception {
        //Sample query from https://confluence.lsstcorp.org/display/DM/PDAC+sample+queries+and+test+cases :
        //SELECT objectId, id, fsrc.exposure_id, fsrc.exposure_time_mid, exp.run,
        //   scisql_dnToAbMag(fsrc.flux_psf,exp.fluxMag0) AS g,
        //   scisql_dnToAbMagSigma(fsrc.flux_psf, fsrc.flux_psf_err, exp.fluxMag0, exp.fluxMag0Sigma) AS gErr
        // FROM RunDeepForcedSource AS fsrc, Science_Ccd_Exposure AS exp
        // WHERE exp.scienceCcdExposureId = fsrc.exposure_id
        //   AND fsrc.exposure_filter_id=1 AND objectId=3448068867358968
        // ORDER BY exposure_time_mid
        String objectId = request.getParam("objectId");
        String filterId = request.getParam("filterId");

        if (objectId == null) {
            throw new EndUserException("Invalid parameter", "Missing objectId");
        }

        // flags are not yet checked
        return "SELECT fsrc.exposure_time_mid, fsrc.coord_ra, fsrc.coord_decl, "+
                "scisql_dnToFlux(fsrc.flux_psf, exp.fluxMag0) AS tsv_flux, "+
                "scisql_dnToFluxSigma(fsrc.flux_psf, fsrc.flux_psf_err, exp.fluxMag0, exp.fluxMag0Sigma) AS tsv_fluxErr, "+
                "scisql_dnToAbMag(fsrc.flux_psf,exp.fluxMag0) AS mag, "+
                "scisql_dnToAbMagSigma(fsrc.flux_psf, fsrc.flux_psf_err, exp.fluxMag0, exp.fluxMag0Sigma) AS magErr, "+
                "exp.run, exp.camcol, exp.field, exp.filterName, exp.scienceCcdExposureId, objectId, id "+
                " FROM RunDeepForcedSource AS fsrc, Science_Ccd_Exposure AS exp "+
                " WHERE exp.scienceCcdExposureId = fsrc.exposure_id "+
                " AND objectId="+objectId+(filterId == null ? "" : " AND fsrc.exposure_filter_id="+filterId)+
                " ORDER BY exposure_time_mid";
    }

    @Override
    public void prepareTableMeta(TableMeta meta, List<DataType> columns, ServerRequest request) {
        super.prepareTableMeta(meta, columns, request);
        meta.setAttribute(MetaConst.DATASET_CONVERTER, "lsst_sdss");
    }

}
