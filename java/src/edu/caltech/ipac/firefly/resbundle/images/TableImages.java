/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.resbundle.images;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;

/**
 * Images used by paging table
 *
 * @author loi
 */
public interface TableImages extends ClientBundle {

    @Source("icons-2014/16x16_BackwardToEnd.png")
    public ImageResource getFirstPage();

    @Source("icons-2014/16x16_ForwardToEnd.png")
    public ImageResource getLastPage();

    @Source("icons-2014/16x16_Forward.png")
    public ImageResource getNextPage();

    @Source("icons-2014/16x16_Backward.png")
    public ImageResource getPrevPage();

    @Source("sort_asc.gif")
    public ImageResource getSortAsc();

    @Source("sort_desc.gif")
    public ImageResource getSortDesc();

    @Source("transparent.gif")
    public ImageResource getTransImage();

//    @Source("table_column.gif")
//    public ImageResource getColumnOptions();

    @Source("icons-2014/24x24_TextView.png")
    public ImageResource getTextViewImage();

    @Source("icons-2014/24x24_TableView.png")
    public ImageResource getTableViewImage();

    @Source("icons-2014/24x24_Save.png")
    public ImageResource getSaveImage();

    @Source("icons-2014/16x16_Filter.png")
    public ImageResource getFilterIn();

    @Source("icons-2014/24x24_Filter.png")
    public ImageResource getFilterImage();

    @Source("icons-2014/24x24_FilterOff_Circle.png")
    public ImageResource getClearFilters();

    public static class Creator  {
        private final static TableImages _instance=
                (TableImages) GWT.create(TableImages.class);
        public static TableImages getInstance() {
            return _instance;
        }
    }
}

