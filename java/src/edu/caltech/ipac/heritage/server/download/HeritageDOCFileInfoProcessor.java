package edu.caltech.ipac.heritage.server.download;

import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.db.DbInstance;
import edu.caltech.ipac.firefly.server.packagedata.FileInfo;
import edu.caltech.ipac.firefly.server.query.FileInfoProcessor;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.heritage.data.entity.download.HeritageDOCFileRequest;
import edu.caltech.ipac.heritage.server.persistence.HeritageSecurityModule;

import java.sql.ResultSet;

/**
 * @author trey
 *         $Id: HeritageDOCFileInfoProcessor.java,v 1.2 2010/08/04 20:18:51 roby Exp $
 */
@SearchProcessorImpl(id ="heritageDOCFileRequest")
public class HeritageDOCFileInfoProcessor extends FileInfoProcessor {
    protected FileInfoRowMapper makeRowMapper(TableServerRequest request) {
        return new FileInfoRowMapper(){

            public FileInfo mapRow(ResultSet rs, int rowNum) {
                try {
                    FileInfo fi = new FileInfo(rs.getString(1), rs.getString(2), rs.getLong(3));
                    //TODO: setAccess for proprietary data owners
                    fi.setHasAccess(HeritageSecurityModule.checkHasAccess(rs.getString(4)));
                    return fi;
                } catch (Exception e) {
                    Logger.error(e, "Failed to map result set to file info");
                    return null;
                }
            }
        };
    }

    public DbInstance getDbInstance() {
        return DbInstance.archive;
    }

    public String getSql(TableServerRequest request) {
        HeritageDOCFileRequest req= QueryUtil.assureType(HeritageDOCFileRequest.class,request);
        return "select depthofcoverage , docexternalname, docfilesize, reqkey from requestinformation where reqkey="+ req.getReqkey();
    }

    public Object[] getSqlParams(TableServerRequest request) {
        return new Object[0];
    }
}