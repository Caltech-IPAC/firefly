package edu.caltech.ipac.heritage.server.persistence;

import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.heritage.searches.SearchAorInfo;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.StringUtils;

import java.util.Arrays;

/**
 * @author loi
 * $Id: QueryAorInfo.java,v 1.9 2010/08/04 20:18:51 roby Exp $
 */
@SearchProcessorImpl(id ="aorInfo")
public class QueryAorInfo extends HeritageQuery {
    private String templateName;
    private static final String[] REQ_MODES = { "iracmap", "iracmappc", "irsmap", "irspeakupimage",
                        "irsstare", "mipsphot", "mipsscan", "mipssed", "mipstp"};

    @Override
    public String getTemplateName() {
        return templateName;
    }

    protected SqlParams makeSqlParams(TableServerRequest request) {
        SearchAorInfo.Req req = QueryUtil.assureType(SearchAorInfo.Req.class, request);
        String aot = req.getInstMode();
        if (Arrays.binarySearch(REQ_MODES, aot.toLowerCase()) < 0) {
            return null;    // no aot info for this mode
        }

        int id = req.getReqID();
        if (!StringUtils.isEmpty(aot) && id > 0) {
            templateName = aot + "_dd";
            String sql = "select aot.*, r.qacomments, c.campid, c.campname from " + 
                    aot + " aot, requestinformation r, campaigninformation c where aot.reqkey = ? and aot.reqkey=r.reqkey and r.campid=c.campid";
            return new SqlParams(sql, id);
        }
        return null;
    }

    DataGroup.Attribute[] getAttributes() {
        return new DataGroup.Attribute[0];
    }

    @Override
    public boolean doLogging() {
        return false;
    }

    @Override
    public boolean doCache() {
        return false;
    }
}