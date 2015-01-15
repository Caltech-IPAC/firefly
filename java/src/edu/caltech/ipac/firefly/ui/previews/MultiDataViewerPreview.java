/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.previews;
/**
 * User: roby
 * Date: 9/2/14
 * Time: 9:18 AM
 */


import edu.caltech.ipac.firefly.ui.table.AbstractTablePreview;
import edu.caltech.ipac.firefly.ui.table.EventHub;
import edu.caltech.ipac.firefly.ui.table.TablePanel;

/**
 * @author Trey Roby
 */
public class MultiDataViewerPreview extends AbstractTablePreview {

    private MultiDataViewer viewer= new MultiDataViewer();


    public MultiDataViewerPreview() {
        setDisplay(viewer.getWidget());
    }

    @Override
    public void bind(EventHub hub) {
        super.bind(hub);
        viewer.bind(hub);
    }

    @Override
    protected void updateDisplay(TablePanel table) {
        viewer.updateGridWithTable(table);
    }

    public MultiDataViewer getViewer() {
        return viewer;
    }
}

