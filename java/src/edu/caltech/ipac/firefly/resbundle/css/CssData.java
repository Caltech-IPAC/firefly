package edu.caltech.ipac.firefly.resbundle.css;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;

/**
 * User: roby
 * Date: Jan 27, 2010
 * Time: 12:47:16 PM
 */
public interface CssData extends ClientBundle {
    @ClientBundle.Source("firefly-bundle.css")
    FireflyCss getFireflyCss();

    @Source("exp-border-left.png")
    @ImageResource.ImageOptions(repeatStyle= ImageResource.RepeatStyle.Vertical)
    public ImageResource popupBorderLeft();


    @Source("exp-border-right.png")
    @ImageResource.ImageOptions(repeatStyle= ImageResource.RepeatStyle.Vertical, width = 8)
    public ImageResource popupBorderRight();


    @Source("exp-border-top.png")
    @ImageResource.ImageOptions(repeatStyle= ImageResource.RepeatStyle.Horizontal)
    public ImageResource popupBorderTop();

    @Source("exp-border-bottom.png")
    @ImageResource.ImageOptions(repeatStyle= ImageResource.RepeatStyle.Horizontal)
    public ImageResource popupBorderBottom();


    @Source("exp-corner-with-shadow.png")
    public ImageResource popupCorner();

    @Source("tab_bg.png")
    @ImageResource.ImageOptions(repeatStyle= ImageResource.RepeatStyle.Horizontal)
    public ImageResource tabBackground();

    @Source("invalid_line.gif")
    @ImageResource.ImageOptions(repeatStyle= ImageResource.RepeatStyle.Horizontal)
    public ImageResource invalidLine();

    @Source("text-bg.gif")
    @ImageResource.ImageOptions(repeatStyle= ImageResource.RepeatStyle.Horizontal)
    public ImageResource textBackground();

    @Source("bg_listgradient.png")
    @ImageResource.ImageOptions(repeatStyle= ImageResource.RepeatStyle.Horizontal)
    public ImageResource bgListGradient();


    @Source("gxt/light-hd.gif")
    @ImageResource.ImageOptions(repeatStyle= ImageResource.RepeatStyle.Horizontal)
    public ImageResource lightHd();

    @Source("gxt/tb-blue.gif")
    @ImageResource.ImageOptions(repeatStyle= ImageResource.RepeatStyle.Horizontal)
    public ImageResource tbBlue();



    public static class Creator  {
        private final static CssData _instance= (CssData) GWT.create(CssData.class);
        public static CssData getInstance() { return _instance; }
    }

}
