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
public interface VisIconCreator extends ClientBundle {

//    @Source("cyan_left_arrow_20x20.png")
    @Source("icons-2014/20x20_PageLeft.png")
    public ImageResource getSideLeftArrow();

//    @Source("cyan_right_arrow_20x20.png")
    @Source("icons-2014/20x20_PageRight.png")
    public ImageResource getSideRightArrow();

//    @Source("cyan_down_arrow_20x20.png")
//    public ImageResource getSideDownArrow();

//    @Source("cyan_up_arrow_20x20.png")
//    public ImageResource getSideUpArrow();

    //    @Source("zoom-out_v2-20x20.png")
    @Source("icons-2014/ZoomOut.png")
    public ImageResource getZoomDown();

    //    @Source("zoom-in_v2-20x20.png")
    @Source("icons-2014/ZoomIn.png")
    public ImageResource getZoomUp();

    @Source("icons-2014/24x24_ZoomIn.png")
    public ImageResource getZoomUpSmall();


//    @Source("zoom-original_v2-20x20.png")
    @Source("icons-2014/Zoom1x.png")
    public ImageResource getZoomOriginal();

    @Source("icons-2014/Zoom1x-24x24-tmp.png")
    public ImageResource getZoomOriginalSmall();


//    @Source("zoom-fit_v2-20x20.png")
//    Source("icons-2014/ZoomFitToSpace.png")
    @Source("icons-2014/28x28_ZoomFitToSpace.png")
    public ImageResource getZoomFit();

//    @Source("zoom-fill_v2-20x20.png")
    @Source("icons-2014/ZoomFillWidth.png")
    public ImageResource getZoomFill();

//    @Source("restore-20x20.png")
    @Source("icons-2014/RevertToDefault.png")
    public ImageResource getRestore();

    @Source("lock-images.png")
    public ImageResource getLocked();

    @Source("unlock-images.png")
    public ImageResource getUnlocked();


//    @Source("icons-2014/BkgLocked.png")
    @Source("icons-2014/28x28_FITS_BkgLocked.png")
    public ImageResource getLockImages();

    @Source("icons-2014/28x28_FITS_BkgUnlocked.png")
    public ImageResource getUnlockedImages();



//    @Source("save_20x20.png")
    @Source("icons-2014/Save.png")
    public ImageResource getSave();

//    @Source("grid-20x20.png")
    @Source("icons-2014/GreenGrid.png")
    public ImageResource getGridOff();

//    @Source("grid-on-20x20.png")
    @Source("icons-2014/GreenGrid-ON.png")
    public ImageResource getGridOn();

//    @Source("select-20x20.png")
    @Source("icons-2014/Marquee.png")
    public ImageResource getSelectAreaOff();

//    @Source("select-dark-20x20.png")
    @Source("icons-2014/Marquee-ON.png")
    public ImageResource getSelectAreaOn();

//    @Source("selectrows-20x20.png")
    @Source("icons-2014/24x24_Checkmark.png")
    public ImageResource getSelectRows();

//    @Source("unselectrows-20x20.png")
//    @Source("icons-2014/24x24_CheckmarkX.png")
    @Source("icons-2014/24x24_CheckmarkOff_Circle.png")
    public ImageResource getUnselectRows();

    //@Source("filterselected-20x20.gif")
    //public ImageResource getFilterSelected();

//    @Source("settings-25x20.png")
//    @Source("settings-bw-24x24.png")
    @Source("icons-2014/24x24_GearsNEW.png")
    public ImageResource getSettings();

//    @Source("crop_20x20.png")
    @Source("icons-2014/24x24_Crop.png")
    public ImageResource getCrop();

//    @Source("palette_20x20.png")
    @Source("icons-2014/28x28_ColorPalette.png")
    public ImageResource getColorTable();

//    @Source("palette_stretch_20X20.png")
//    public ImageResource getStretch();

//    @Source("palette_stretch_quick_20X20.png")
    @Source("icons-2014/28x28_Log.png")
    public ImageResource getStretchQuick();


//    @Source("header-info_20x20.png")
//    @Source("icons-2014/Information.png")
    @Source("icons-2014/28x28_FITS_Information.png")
    public ImageResource getFitsHeader();

//    @Source("plot_layers_dim_20x20.png")
    @Source("icons-2014/TurnOnLayers.png")
    public ImageResource getLayer();

//    @Source("plot_layers_20x20.png")
    @Source("icons-2014/TurnOnLayers.png")
    public ImageResource getLayerBright();

//    @Source("rotate-north-on_20x20.png")
    @Source("icons-2014/RotateToNorth-ON.png")
    public ImageResource getRotateNorthOn();

//    @Source("rotate-north_20x20.png")
    @Source("icons-2014/RotateToNorth.png")
    public ImageResource getRotateNorth();

    @Source("icons-2014/Mirror.png")
    public ImageResource getFlip();

//    @Source("rotate_20x20.png")
    @Source("icons-2014/Rotate.png")
    public ImageResource getRotate();

//    @Source("distance_20x20.png")
    @Source("icons-2014/Measurement-ON.png")
    public ImageResource getDistanceOn();

//    @Source("distance-off_20x20.png")
    @Source("icons-2014/Measurement.png")
    public ImageResource getDistanceOff();


//    @Source("marker-off-20x20.png")
    @Source("icons-2014/Circles.png")
    public ImageResource getMarkerOff();

//    Source("marker-on-20x20.png")
    @Source("icons-2014/Circles-ON.png")
    public ImageResource getMarkerOn();




//    @Source("current-target_20x20.png")
    @Source("icons-2014/RecenterImage.png")
    public ImageResource getCurrentTarget();

    @Source("catalog_28x28.png")
    public ImageResource getCatalog();

    @Source("image-working-background-24x24.png")
    public ImageResource getImageWorkingBackground();


//    @Source("starry_sky_20x20.png")
//    @Source("icons-2014/NewImage.png")
    @Source("icons-2014/28x28_FITS_NewImage.png")
    public ImageResource getFITSNewModifyImage();

    @Source("icons-2014/28x28_FITS_Modify3Image.png")
    public ImageResource getFITSModify3Image();

    @Source("icons-2014/24x24_FITS_Insert3Image_plain.png")
    public ImageResource getFITSInsert3Image();

//    @Source("statistics_20x20.png")
    @Source("icons-2014/24x24_Statistics.png")
    public ImageResource getStatistics();

    @Source("filter_out_20x20.png")
    public ImageResource getFilterOut();

//    @Source("filter_in_20x20.png")
    @Source("icons-2014/24x24_FilterAdd.png")
    public ImageResource getFilterIn();

//    @Source("compass-20x20.png")
    @Source("icons-2014/28x28_Compass.png")
    public ImageResource getCompass();

//    @Source("compass-on-20x20.png")
    @Source("icons-2014/28x28_CompassON.png")
    public ImageResource getCompassOn();


//    @Source("sun-20x20.png")
    @Source("icons-2014/DS9.png")
    public ImageResource getDS9Symbol();

    @Source("mask_28x28.png")
    public ImageResource getImageMask();

//    @Source("step-right-16x16.png")
//    public ImageResource getStepRight();
//
//    @Source("step-left-16x16.png")
//    public ImageResource getStepLeft();


    public static class Creator  {
        private final static VisIconCreator _instance=
                (VisIconCreator) GWT.create(VisIconCreator.class);
        public static VisIconCreator getInstance() {
            return _instance;
        }
    }
}

