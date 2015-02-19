/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.creator;

import edu.caltech.ipac.firefly.ui.previews.BasicTablePreview;
import edu.caltech.ipac.firefly.ui.previews.TableFilteringPreview;
import edu.caltech.ipac.firefly.ui.table.TablePreview;

import java.util.Map;

/**
 * Date: Dec 13, 2011
 *
 * @author loi
 * @version $Id: TableFilteringPreviewCreator.java,v 1.2 2012/04/28 01:10:16 tlau Exp $
 */
public class TableFilteringPreviewCreator implements ObsResultCreator {
    public static final String TITLE = "Title";
    public static final String SHORT_DESC = "ShortDesc";
    public static final String QUERY_SOURCE = "QuerySource";
    public static final String EVENT_WORKER_ID = "EVENT_WORKER_ID";

    public TablePreview create(Map<String, String> params) {
        String title = params.get(TITLE);
        String desc = params.get(SHORT_DESC);
        String tname = params.get(QUERY_SOURCE);
        String id = params.get(EVENT_WORKER_ID);
        TableFilteringPreview preview = new TableFilteringPreview(title, id);
        return preview;
    }

    
}

