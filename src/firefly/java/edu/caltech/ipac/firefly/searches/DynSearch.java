/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.searches;

import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.ui.TargetPanel;
import edu.caltech.ipac.firefly.ui.table.builder.BaseTableConfig;
import edu.caltech.ipac.util.StringUtils;

/**
 * Date: Jun 8, 2009
 *
 * @author Trey
 * @version $Id: DynSearch.java,v 1.3 2010/04/24 01:13:04 loi Exp $
 */
public class DynSearch extends BaseTableConfig<TableServerRequest> {


    public DynSearch(TableServerRequest req) {
        super(req, "2 MASS", "Searching 2 MASS", null, null, null);
    }

    public String getDownloadFilePrefix() {
        String tname = this.getSearchRequest().getParam(TargetPanel.TARGET_NAME_KEY);
        if (!StringUtils.isEmpty(tname)) {
            return tname.replaceAll("\\s+", "") + "-";
        } else {
            return "tgt-";
        }
    }

    public String getDownloadTitlePrefix() {
        String tname = this.getSearchRequest().getParam(TargetPanel.TARGET_NAME_KEY);
        if (!StringUtils.isEmpty(tname)) {
            return tname + ": ";
        } else {
            return "";
        }
    }
}