package edu.caltech.ipac.firefly.data.sofia;

import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.server.ServerCommandAccess;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.SrvParam;
import edu.caltech.ipac.firefly.server.cache.UserCache;
import edu.caltech.ipac.firefly.server.query.EmbeddedDbProcessor;
import edu.caltech.ipac.firefly.server.util.multipart.UploadFileInfo;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.StringKey;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;

/**
 * Should implement child that returns datagroups,
 * source file from request or overwritten in child or unit/integration tests
 * Could be called as link service or return a datalink table
 */
public abstract class DataExtractUtil {


    /**
     * @param f File to extract data from
     * @return File the data extracted/transformed
     * @throws Exception
     */
    public abstract DataGroup extract(File inf) throws Exception;

    /**
     * @param req
     * @return
     */
    public File getSourceFile(ServerRequest req) {
        String fileKey = req.getParam("file");
        Cache sessionCache = UserCache.getInstance();
        File sourceFile = ServerContext.convertToFile(fileKey);
        if (sourceFile == null || !sourceFile.canRead()) {
            UploadFileInfo tmp = (UploadFileInfo) (sessionCache.get(new StringKey(fileKey)));
            sourceFile = tmp.getFile();
        }
        return sourceFile;
    }
}
