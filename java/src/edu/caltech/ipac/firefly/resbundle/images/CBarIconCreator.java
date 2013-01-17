package edu.caltech.ipac.firefly.resbundle.images;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
/**
 * User: roby
 * Date: Sep 1, 2009
 * Time: 11:22:02 AM
 */



/**
 * @author Trey Roby
 */
public interface CBarIconCreator extends ClientBundle {

    @Source("ct-0-gray.png")
    public ImageResource getColorTable0();

    @Source("ct-1-reversegray.png")
    public ImageResource getColorTable1();

    @Source("ct-2-colorcube.png")
    public ImageResource getColorTable2();

    @Source("ct-3-spectrum.png")
    public ImageResource getColorTable3();

    @Source("ct-4-false.png")
    public ImageResource getColorTable4();

    @Source("ct-5-reversefalse.png")
    public ImageResource getColorTable5();

    @Source("ct-6-falsecompressed.png")
    public ImageResource getColorTable6();

    @Source("ct-7-difference.png")
    public ImageResource getColorTable7();

    @Source("ct-8-a-ds9.png")
    public ImageResource getColorTable8();

    @Source("ct-9-b-ds9.png")
    public ImageResource getColorTable9();

    @Source("ct-10-bb-ds9.png")
    public ImageResource getColorTable10();

    @Source("ct-11-he-ds9.png")
    public ImageResource getColorTable11();

    @Source("ct-12-i8-ds9.png")
    public ImageResource getColorTable12();

    @Source("ct-13-aips-ds9.png")
    public ImageResource getColorTable13();

    @Source("ct-14-sls-ds9.png")
    public ImageResource getColorTable14();

    @Source("ct-15-hsv-ds9.png")
    public ImageResource getColorTable15();

    @Source("ct-16-heat-ds9.png")
    public ImageResource getColorTable16();

    @Source("ct-17-cool-ds9.png")
    public ImageResource getColorTable17();

    @Source("ct-18-rainbow-ds9.png")
    public ImageResource getColorTable18();

    @Source("ct-19-standard-ds9.png")
    public ImageResource getColorTable19();

    @Source("ct-20-staircase-ds9.png")
    public ImageResource getColorTable20();

    @Source("ct-21-color-ds9.png")
    public ImageResource getColorTable21();


    public static class Creator  {
        private final static CBarIconCreator _instance=
                (CBarIconCreator) GWT.create(CBarIconCreator.class);
        public static CBarIconCreator getInstance() {
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
