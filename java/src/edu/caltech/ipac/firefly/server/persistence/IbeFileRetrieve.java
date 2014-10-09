package edu.caltech.ipac.firefly.server.persistence;

/**
 * @author tatianag
 *         $Id: $
 */

import edu.caltech.ipac.astro.IpacTableReader;
import edu.caltech.ipac.astro.ibe.IBE;
import edu.caltech.ipac.astro.ibe.IbeDataParam;
import edu.caltech.ipac.astro.ibe.IbeDataSource;
import edu.caltech.ipac.astro.ibe.IbeQueryParam;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.SortInfo;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.WspaceMeta;
import edu.caltech.ipac.firefly.data.table.MetaConst;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.WorkspaceManager;
import edu.caltech.ipac.firefly.server.packagedata.FileGroup;
import edu.caltech.ipac.firefly.server.packagedata.FileInfo;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.FileGroupsProcessor;
import edu.caltech.ipac.firefly.server.query.FileInfoProcessor;
import edu.caltech.ipac.firefly.server.query.IpacTablePartProcessor;
import edu.caltech.ipac.firefly.server.query.ParamDoc;
import edu.caltech.ipac.firefly.server.query.SearchProcessor;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.query.URLFileInfoProcessor;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupPart;
import edu.caltech.ipac.firefly.server.util.ipactable.IpacTableParser;
import edu.caltech.ipac.firefly.util.DataSetParser;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheKey;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.StringKey;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

@SearchProcessorImpl(id = "ibe_file_retrieve", params=
        {@ParamDoc(name="mission", desc="mission"),
        })
public class IbeFileRetrieve extends URLFileInfoProcessor {
    public static final String PROC_ID = IbeFileRetrieve.class.getAnnotation(SearchProcessorImpl.class).id();
    public static final String MISSION = "mission";

    @Override
    public FileInfo getData(ServerRequest request) throws DataAccessException {

        try {
            String mission = request.getParam(MISSION);
            Map<String,String> paramMap = IBEUtils.getParamMap(request.getParams());

            IBE ibe = IBEUtils.getIBE(mission, paramMap);
            IbeDataSource ibeDataSource = ibe.getIbeDataSource();
            IbeDataParam dataParam = ibeDataSource.makeDataParam(paramMap);
            File ofile = File.createTempFile("IbeFileRetrieve-",  dataParam.getFileName(), ServerContext.getTempWorkDir());
            ibe.getData(ofile, dataParam);

            // no result found
            if (ofile == null || !ofile.exists() || ofile.length() == 0) {
                return null;
            } else {
                return new FileInfo(ofile.getAbsolutePath(), dataParam.getFileName(), ofile.length());
            }
        } catch (Exception e) {
            throw new DataAccessException("Fail to retrieve file from IBE.", e);
        }
    }

    @Override
    public URL getURL(ServerRequest sr) throws MalformedURLException {
        return null;        // not supported.
    }
}
