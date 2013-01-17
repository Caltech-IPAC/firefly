package edu.caltech.ipac.heritage.ui.image;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.ImageBundle;
/**
 * User: roby
 * Date: Sep 1, 2009
 * Time: 11:22:02 AM
 */


/**
 * @author Trey Roby
 */
public interface HeritageImages extends ImageBundle {

    @Resource("abstractSearch.png")
    public AbstractImagePrototype getAbstractSearch();

    @Resource("queryDate.png")
    public AbstractImagePrototype getQueryDate();

    @Resource("queryObserver.png")
    public AbstractImagePrototype getQueryObserver();

    @Resource("queryPosition.png")
    public AbstractImagePrototype getQueryPosition();

    @Resource("queryProgram.png")
    public AbstractImagePrototype getQueryProgram();

    @Resource("queryRequest.png")
    public AbstractImagePrototype getQueryRequest();

    @Resource("queryCampaign.png")
    public AbstractImagePrototype getQueryCampain();

    @Resource("queryNaifID.png")
    public AbstractImagePrototype getQueryNaifID();

    @Resource("spitzer_logo_x40.png")
    public AbstractImagePrototype getSpitzerLogoX40();


    public static class Creator  {
        private final static HeritageImages _instance=
                (HeritageImages) GWT.create(HeritageImages.class);
        public static HeritageImages getInstance() {
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
