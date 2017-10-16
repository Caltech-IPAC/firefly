/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.core;

import com.google.gwt.i18n.client.NumberFormat;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.util.StringUtils;

import java.util.Arrays;
import java.util.List;

/**
 * Date: Sep 22, 2011
 *
 * @author loi
 * @version $Id: SearchDescResolver.java,v 1.3 2011/09/28 02:18:47 loi Exp $
 */
public class SearchDescResolver {
    List<String> kw = Arrays.asList(Request.DO_SEARCH, Request.BOOKMARKABLE, Request.DRILLDOWN, Request.DRILLDOWN_ROOT,
                                    Request.FILTERS, Request.ID_KEY, Request.PAGE_SIZE, Request.SEARCH_RESULT,
                                    Request.SHORT_DESC, Request.SORT_INFO, Request.START_IDX);

    public SearchDescResolver create() {
        return this;
    }

    public String getTitle(Request req) {
        String title = Application.getInstance().getProperties()
                        .getProperty(req.getCmdName() + ".Title", req.getShortDesc());

        return StringUtils.isEmpty(title) ? req.getShortDesc() : title;
    }

    public String getDesc(Request req) {
        StringBuffer retval = new StringBuffer();
        for(Param p : req.getParams()) {
            if (!kw.contains(p.getName())) {
                if (retval.length() > 0) {
                    retval.append("; ");
                }
                retval.append(p.getName()).append("=").append(p.getValue());
            }

        }
        return retval.length() > 100 ? retval.substring(0, 100) : retval.toString();
    }

    public String getDownloadDesc(Request req) {
        return "unknown";
    }

    public static String toDegString(String s) {
        float sv = StringUtils.getFloat(s);
        return NumberFormat.getFormat("0.0000").format(sv) + " deg";
    }

}
