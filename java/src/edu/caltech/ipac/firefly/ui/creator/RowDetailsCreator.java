package edu.caltech.ipac.firefly.ui.creator;

import edu.caltech.ipac.firefly.ui.table.RowDetailPlusNotesPreview;
import edu.caltech.ipac.firefly.ui.table.RowDetailPreview;
import edu.caltech.ipac.firefly.ui.table.TablePreview;
import edu.caltech.ipac.util.StringUtils;

import java.util.Map;
/**
 * Date: Aug 5, 2010
 *
 * @author loi
 * @version $Id: RowDetailsCreator.java,v 1.2 2012/11/13 01:35:23 tlau Exp $
 */
public class RowDetailsCreator implements ObsResultCreator {

    public TablePreview create(Map<String, String> params) {

        String name = params.get("TITLE");
        String searchProcId = params.get("NOTES_PROC_ID");
        String stateId = params.get("STATE_ID");
        if (StringUtils.isEmpty(searchProcId)) {
            return new RowDetailPreview(name);
        } else {
            if (StringUtils.isEmpty(stateId))
                return new RowDetailPlusNotesPreview(name, searchProcId);
            else
                return new RowDetailPlusNotesPreview(name, searchProcId, stateId);
        }
    }

}
