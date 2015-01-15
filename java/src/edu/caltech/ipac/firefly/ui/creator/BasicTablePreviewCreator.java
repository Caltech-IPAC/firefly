/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.creator;

import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.firefly.ui.table.Loader;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.ui.table.TablePreview;
import edu.caltech.ipac.firefly.ui.table.BasicTable;
import edu.caltech.ipac.firefly.ui.table.builder.BaseTableConfig;
import edu.caltech.ipac.firefly.ui.previews.BasicTablePreview;
import edu.caltech.ipac.firefly.util.Ref;
import edu.caltech.ipac.util.StringUtils;

import java.util.Map;

/**
 * Date: Dec 13, 2011
 *
 * @author loi
 * @version $Id: BasicTablePreviewCreator.java,v 1.1 2011/12/16 00:52:32 loi Exp $
 */
public class BasicTablePreviewCreator implements ObsResultCreator {
    public static final String TITLE = "Title";
    public static final String SHORT_DESC = "ShortDesc";
    public static final String QUERY_SOURCE = "QuerySource";


    public TablePreview create(Map<String, String> params) {
        String title = params.get(TITLE);
        String desc = params.get(SHORT_DESC);
        String tname = params.get(QUERY_SOURCE);

        BasicTablePreview preview = new BasicTablePreview(title);
        return preview;
    }

    
}

