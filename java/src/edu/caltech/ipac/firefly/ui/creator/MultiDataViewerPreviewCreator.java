package edu.caltech.ipac.firefly.ui.creator;

import edu.caltech.ipac.firefly.ui.previews.BasicTablePreview;
import edu.caltech.ipac.firefly.ui.previews.MultiDataViewerPreview;
import edu.caltech.ipac.firefly.ui.table.TablePreview;

import java.util.Map;

/**
 * Date: Sept 4, 2014
 *
 * @author loi
 * @version $Id: BasicTablePreviewCreator.java,v 1.1 2011/12/16 00:52:32 loi Exp $
 */
public class MultiDataViewerPreviewCreator implements ObsResultCreator {


    public TablePreview create(Map<String, String> params) {

        MultiDataViewerPreview preview = new MultiDataViewerPreview();
        return preview;
    }

    
}

