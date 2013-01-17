package edu.caltech.ipac.irsabanner.resbundle.images;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.MenuBar;
/**
 * User: roby
 * Date: Sep 1, 2009
 * Time: 11:22:02 AM
 */


/**
 * @author Trey Roby
 */
public interface BannerImage extends MenuBar.Resources {

    @ClientBundle.Source("banner-main-8bit.png")
    public ImageResource getBannerMain();

    @ClientBundle.Source("bottom_bar-left.png")
    public ImageResource getDropDownBackLeft();

    @ClientBundle.Source("bottom_bar-right.png")
    public ImageResource getDropDownBackRight();

//    @ClientBundle.Source("banner-expand.png")
//    public ImageResource getBannerExpand();

    @ImageResource.ImageOptions(flipRtl = true)
    @ClientBundle.Source("white-cascade-13x13.png")
    public ImageResource menuBarSubMenuIcon();


    public static class Creator  {
        private final static BannerImage _instance=
                (BannerImage) GWT.create(BannerImage.class);
        public static BannerImage getInstance() {
            return _instance;
        }
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
