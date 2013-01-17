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
        return retval.toString();
    }

    public String getDownloadDesc(Request req) {
        return "unknown";
    }

    public static String toDegString(String s) {
        float sv = StringUtils.getFloat(s);
        return NumberFormat.getFormat("0.0000").format(sv) + " deg";
    }

}
/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
* OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS,
* HOWEVER USED.
*
* IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE
* FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL
* OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO
* PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE
* ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
*
* RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE
* AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR
* ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE
* OF THE SOFTWARE.
*/
