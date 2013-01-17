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

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313)
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

