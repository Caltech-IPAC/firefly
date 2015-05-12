/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize;
/**
 * User: roby
 * Date: 5/11/12
 * Time: 10:20 AM
 */


import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.visualize.draw.StaticDrawInfo;
import edu.caltech.ipac.util.dd.Region;
import edu.caltech.ipac.visualize.draw.FixedObjectGroup;
import edu.caltech.ipac.visualize.draw.GridLayer;
import edu.caltech.ipac.visualize.draw.ScalableObjectPosition;
import edu.caltech.ipac.visualize.draw.SkyShape;
import edu.caltech.ipac.visualize.draw.SkyShapeFactory;
import edu.caltech.ipac.visualize.draw.VectorObject;
import edu.caltech.ipac.visualize.plot.ActiveFitsReadGroup;
import edu.caltech.ipac.visualize.plot.ImagePlot;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Trey Roby
 */
public class PlotPngCreator {
    private static final Logger.LoggerImpl logger = Logger.getLogger();
//    private static final int ARROW_LENTH = 60;
    private final List<StaticDrawInfo> drawInfoList;
    private final List<FixedObjectGroup> fgList= new ArrayList<FixedObjectGroup>(10);
    private final List<VectorObject> vectorList= new ArrayList<VectorObject>(10);
    private final List<ScalableObjectPosition> scaleList= new ArrayList<ScalableObjectPosition>(10);
    private GridLayer gridLayer= null;

    private PlotPngCreator (List<StaticDrawInfo> drawInfoList) {
        this.drawInfoList= drawInfoList;
    }

    public static String createImagePng(ImagePlot plot, ActiveFitsReadGroup frGroup, List<StaticDrawInfo> drawInfoList) throws IOException {
        PlotPngCreator ppC= new PlotPngCreator(drawInfoList);
        return ppC.create(plot,frGroup);
    }


    private String create(ImagePlot plot, ActiveFitsReadGroup frGroup) throws IOException {
        for(StaticDrawInfo drawInfo : drawInfoList) {
            switch (drawInfo.getDrawType()) {
                case SYMBOL:
                    addSymbolDrawer(drawInfo);
                    break;
                case GRID:
                    addGridDrawer(drawInfo);
                    break;
                case REGION:
                    addRegion(drawInfo, plot);
                    break;
            }
        }

        File f= PlotServUtils.getUniquePngFileName("imageDownload", ServerContext.getVisSessionDir());
        File retFile= PlotServUtils.createFullTile(plot, frGroup, f, fgList,vectorList, scaleList, gridLayer);
        return ServerContext.replaceWithPrefix(retFile);
    }


//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================


    private void addSymbolDrawer(StaticDrawInfo drawInfo) {
        FixedObjectGroup fog= makeFixedObjectGroup(drawInfo);
        if (fog!=null) fgList.add(fog);
    }

    private void addGridDrawer(StaticDrawInfo drawInfo) {
        GridLayer gridLayer= new GridLayer();
        gridLayer.setCoordSystem(drawInfo.getGridType());
        Color c= convertColor(drawInfo.getColor());
        if (c!=null) gridLayer.getGrid().setGridColor(c);
        this.gridLayer= gridLayer;
    }

    private void addRegion(StaticDrawInfo drawInfo, ImagePlot plot) {
        List<Region> regList= drawInfo.getRegionList();
        RegionPng regionPng= new RegionPng(regList,plot,fgList, vectorList,scaleList);
        regionPng.drawRegions();
    }



    private FixedObjectGroup makeFixedObjectGroup(StaticDrawInfo drawInfo) {
        FixedObjectGroup fg= new FixedObjectGroup();
        SkyShapeFactory factory= SkyShapeFactory.getInstance();
        for(WorldPt pt : drawInfo) {
            fg.add(fg.makeFixedObject(pt));
        }
        SkyShape shape;
        switch (drawInfo.getSymbol()) {
            case X:
                shape= factory.getSkyShape("x");
                break;
            case SQUARE:
                shape=factory.getSkyShape("square");
                break;
            case CROSS:
                shape= factory.getSkyShape("cross");
                break;
            case EMP_CROSS:
                shape= factory.getSkyShape("cross");
                break;
            case DIAMOND:
                shape= factory.getSkyShape("diamond");
                break;
            case DOT:
                shape= factory.getSkyShape("dot");
                break;
            case CIRCLE:
                shape= factory.getSkyShape("circle");
                break;
            default:
                shape= factory.getSkyShape("x");
                break;
        }
        fg.setAllShapes(shape);
        String color= drawInfo.getColor();
        Color c= convertColor(color);
        if (c!=null) fg.setAllColor(FixedObjectGroup.COLOR_TYPE_STANDARD, c);
        return fg;
    }

    public static Color convertColor(String color) {
        Color c;
        if (edu.caltech.ipac.firefly.visualize.ui.color.Color.isHexColor(color)) {
            int rgb[]=  edu.caltech.ipac.firefly.visualize.ui.color.Color.toRGB(color);
            c= new Color(rgb[0],rgb[1],rgb[2]);
        }
        else {
            if      (color.equals("black"))   c= Color.black;
            else if (color.equals("aqua"))    c= new Color(0,255,255);
            else if (color.equals("blue"))    c= Color.blue;
            else if (color.equals("cyan"))    c= Color.cyan;
            else if (color.equals("fuchsia")) c= new Color(255,0,255);
            else if (color.equals("gray"))    c= new Color(128,128,128);
            else if (color.equals("green"))   c= new Color(0,128,0);
            else if (color.equals("lime"))    c= Color.green;  // this is correct, lime is 0,255,0
            else if (color.equals("magenta")) c= Color.magenta;
            else if (color.equals("maroon"))  c= new Color(128,0,0);
            else if (color.equals("navy"))    c= new Color(0,0,128);
            else if (color.equals("olive"))   c= new Color(128,128,0);
            else if (color.equals("orange"))  c= Color.orange;
            else if (color.equals("pink"))    c= Color.pink;
            else if (color.equals("purple"))  c= new Color(128,0,128);
            else if (color.equals("red"))     c= Color.red;
            else if (color.equals("silver"))  c= new Color(192,192,192);
            else if (color.equals("teal"))    c= new Color(0,128,128);
            else if (color.equals("white"))   c= Color.white;
            else if (color.equals("yellow"))  c= Color.yellow;
            else {
                // lightGray or white is a better presentation for "unknown" color string. -TLau
                c= Color.lightGray;
                logger.debug("convertColor(String color) does not understand "+color+".  Color.lightGray is assigned.");
            }
        }
        return c;
    }

}

