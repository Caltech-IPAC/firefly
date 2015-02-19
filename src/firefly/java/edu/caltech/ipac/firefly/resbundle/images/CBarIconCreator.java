/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
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

    @Source("cbar/ct-0-gray.png")
    public ImageResource getColorTable0();

    @Source("cbar/ct-1-reversegray.png")
    public ImageResource getColorTable1();

    @Source("cbar/ct-2-colorcube.png")
    public ImageResource getColorTable2();

    @Source("cbar/ct-3-spectrum.png")
    public ImageResource getColorTable3();

    @Source("cbar/ct-4-false.png")
    public ImageResource getColorTable4();

    @Source("cbar/ct-5-reversefalse.png")
    public ImageResource getColorTable5();

    @Source("cbar/ct-6-falsecompressed.png")
    public ImageResource getColorTable6();

    @Source("cbar/ct-7-difference.png")
    public ImageResource getColorTable7();

    @Source("cbar/ct-8-a-ds9.png")
    public ImageResource getColorTable8();

    @Source("cbar/ct-9-b-ds9.png")
    public ImageResource getColorTable9();

    @Source("cbar/ct-10-bb-ds9.png")
    public ImageResource getColorTable10();

    @Source("cbar/ct-11-he-ds9.png")
    public ImageResource getColorTable11();

    @Source("cbar/ct-12-i8-ds9.png")
    public ImageResource getColorTable12();

    @Source("cbar/ct-13-aips-ds9.png")
    public ImageResource getColorTable13();

    @Source("cbar/ct-14-sls-ds9.png")
    public ImageResource getColorTable14();

    @Source("cbar/ct-15-hsv-ds9.png")
    public ImageResource getColorTable15();

    @Source("cbar/ct-16-heat-ds9.png")
    public ImageResource getColorTable16();

    @Source("cbar/ct-17-cool-ds9.png")
    public ImageResource getColorTable17();

    @Source("cbar/ct-18-rainbow-ds9.png")
    public ImageResource getColorTable18();

    @Source("cbar/ct-19-standard-ds9.png")
    public ImageResource getColorTable19();

    @Source("cbar/ct-20-staircase-ds9.png")
    public ImageResource getColorTable20();

    @Source("cbar/ct-21-color-ds9.png")
    public ImageResource getColorTable21();


    public static class Creator  {
        private final static CBarIconCreator _instance=
                (CBarIconCreator) GWT.create(CBarIconCreator.class);
        public static CBarIconCreator getInstance() {
            return _instance;
        }
    }
}
