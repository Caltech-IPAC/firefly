/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.persistence;


import edu.caltech.ipac.firefly.core.NotLoggedInException;
import edu.caltech.ipac.firefly.data.SearchInfo;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.userdata.UserInfo;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.cache.UserCache;
import edu.caltech.ipac.firefly.server.db.DbInstance;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.IpacFileQuery;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupWriter;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.cache.Cache;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;


/**
 * @author tatianag
 *         $Id: QuerySearchHistory.java,v 1.12 2011/09/30 18:22:34 loi Exp $
 */
@SearchProcessorImpl(id ="searchHistory")
public class QuerySearchHistory  extends IpacFileQuery {

    public DbInstance getDbInstance() {
        return DbInstance.operation;
    }

    @Override
    protected File loadDataFile(TableServerRequest request) throws IOException, DataAccessException {

        UserInfo userInfo = ServerContext.getRequestOwner().getUserInfo();
        if (userInfo.isGuestUser()) {

            DataType[] cols = new DataType[]{
                        new DataType("queryid", Integer.class),
                        new DataType("favorite", String.class),
                        new DataType("timeadded", Date.class),
                        new DataType("description", String.class),
                        new DataType("historytoken", String.class)
                    };
            cols[2].getFormatInfo().setDataFormat("%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS"); // date: yyyy-mm-dd hh:mm:ss
            cols[3].getFormatInfo().setWidth(2000);
            cols[4].getFormatInfo().setWidth(2000);

            DataGroup dg = new DataGroup("Search History", cols);

            // order by timeadded desc
            List<SearchInfo> list = GuestHistoryCache.getSearchHistory(ServerContext.getRequestOwner().getUserKey());
            Collections.sort(list, new Comparator<SearchInfo>(){
                        public int compare(SearchInfo o1, SearchInfo o2) {
                            return -1 * o1.getTimeAdded().compareTo(o2.getTimeAdded());
                        }
                    });
            for(SearchInfo si : list) {
                DataObject row = new DataObject(dg);
                row.setDataElement(cols[0], si.getQueryID());
                row.setDataElement(cols[1], (si.isFavorite() ? "yes" : "no"));
                row.setDataElement(cols[2], si.getTimeAdded());
                row.setDataElement(cols[3], si.getDescription());
                row.setDataElement(cols[4], si.getHistoryToken());
                dg.add(row);
            }
            File f = File.createTempFile(getFilePrefix(request), ".tbl", ServerContext.getTempWorkDir());
            DataGroupWriter.write(f, dg);
            return f;
        } else {
            return super.loadDataFile(request);
        }
    }

    public String getSql(TableServerRequest request) {
        return "select queryid, CASE WHEN favorite THEN 'yes' ELSE 'no' END favorite, timeadded, description, historytoken from queryhistory where loginname = ? and appname= ? order by timeadded desc";
    }

    public Object[] getSqlParams(TableServerRequest request) {
        UserInfo userInfo = ServerContext.getRequestOwner().getUserInfo();
        try {
            if (userInfo.isGuestUser()) {
                throw new NotLoggedInException();
            }
            return new Object[]{userInfo.getLoginName(), ServerContext.getAppName()};
        } catch (NotLoggedInException e) {
            Logger.error(e, "unable to get search history for user " + userInfo.getLoginName());
            return new Object[0];
        }
    }

    @Override
    public boolean doCache() {
        return false;
    }

    @Override
    public String getUniqueID(ServerRequest request) {
        return "searchHistory";
    }

    @Override
    public Cache getCache() {
        return UserCache.getInstance();
    }
    
}
