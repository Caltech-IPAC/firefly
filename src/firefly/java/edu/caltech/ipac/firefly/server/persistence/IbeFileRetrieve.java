/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.persistence;

/**
 * @author tatianag
 *         $Id: $
 */

import edu.caltech.ipac.astro.ibe.IBE;
import edu.caltech.ipac.astro.ibe.IbeDataParam;
import edu.caltech.ipac.astro.ibe.IbeDataSource;
import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.data.RelatedData;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.query.BaseFileInfoProcessor;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.ParamDoc;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.query.ibe.IbeQueryArtifact;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;


@SearchProcessorImpl(id = "ibe_file_retrieve", params =
        {@ParamDoc(name = "mission", desc = "mission"),
        })
public class IbeFileRetrieve extends BaseFileInfoProcessor {
    public static final String PROC_ID = IbeFileRetrieve.class.getAnnotation(SearchProcessorImpl.class).id();
    public static final String MISSION = "mission";

    @Override
    protected FileInfo loadData(ServerRequest request) throws IOException, DataAccessException {
        try {
            String mission = request.getParam(MISSION);
            Map<String, String> paramMap = IBEUtils.getParamMap(request.getParams());

            IBE ibe = IBEUtils.getIBE(mission, paramMap);
            IbeDataSource ibeDataSource = ibe.getIbeDataSource();
            IbeDataParam dataParam = ibeDataSource.makeDataParam(paramMap);
//            File ofile = makeOutputFile(dataParam);
            FileInfo ofile= ibe.getData(dataParam, paramMap);

            // no result found
            if (ofile == null ||  ofile.getSizeInBytes() == 0) {
                return null;
            } else {
                Map<String, String> cookies = ServerContext.getRequestOwner().getIdentityCookies();
                ofile.setCookies(cookies);
                ofile.addRelatedDataList( findRelatedDataList(request));
                return ofile;
            }
        } catch (Exception e) {
            throw new DataAccessException(e.getMessage(), e);
        }
    }

    private List<RelatedData> findRelatedDataList(ServerRequest r) {
        String mission = r.getParam(MISSION);
        WorldPt wp= getWorldPtFromCenterParam(r.getParam("center"));
        String subsize= r.getParam("subsize");
        if (wp==null || mission==null || subsize==null) return null;

        switch (mission.toLowerCase()) {
            case "wise":
                return IbeQueryArtifact.getWiseRelatedData(wp, subsize,r.getParam("band"));
            case "2mass":
                return IbeQueryArtifact.get2MassRelatedData(wp, subsize);
            default:
                return null;
        }
    }

    private static WorldPt getWorldPtFromCenterParam(String cen) {
        if (cen==null) return null;
        String parts[]= cen.split(",");
        if (parts.length!=2) return null;
        return WorldPt.parse(parts[0]+';' + parts[1]+";J2000");
    }

    protected File makeOutputFile(IbeDataParam params) throws IOException {
        String fname = params.getFileName();
        if (fname.contains(".tbl")) {
            return File.createTempFile("IbeFileRetrieve-", "-" + fname, ServerContext.getTempWorkDir());
        } else {
            ;
            return File.createTempFile(fname + "-", "", ServerContext.getVisCacheDir());
        }
    }

    @Override
    public boolean doCache() {
        return false;
    }

}
