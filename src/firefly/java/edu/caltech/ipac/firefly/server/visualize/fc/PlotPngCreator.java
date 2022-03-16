/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize.fc;
/**
 * User: roby
 * Date: 5/11/12
 * Time: 10:20 AM
 */


import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.visualize.PlotServUtils;
import edu.caltech.ipac.firefly.visualize.draw.StaticDrawInfo;
import edu.caltech.ipac.util.FileUtil;
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
import edu.caltech.ipac.visualize.plot.output.PlotOutput;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Trey Roby
 */
public class PlotPngCreator {
    private final List<StaticDrawInfo> drawInfoList;
    private final List<FixedObjectGroup> fgList= new ArrayList<>(10);
    private final List<VectorObject> vectorList= new ArrayList<>(10);
    private final List<ScalableObjectPosition> scaleList= new ArrayList<>(10);
    private GridLayer gridLayer= null;

    private PlotPngCreator (List<StaticDrawInfo> drawInfoList) {
        this.drawInfoList= drawInfoList;
    }

    public static String createImagePng(ImagePlot plot, ActiveFitsReadGroup frGroup, List<StaticDrawInfo> drawInfoList) throws IOException {
        PlotPngCreator ppC= new PlotPngCreator(drawInfoList);
        return ppC.create(plot,frGroup);
    }

    private static final AtomicLong _nameCnt= new AtomicLong(0);
    private static final String _hostname;
    private static final String _pngNameExt="." + FileUtil.png;
    static {
        _hostname= FileUtil.getHostname();
    }

    static File getUniquePngFileName(String nameBase, File dir) {
        File f= new File(dir,nameBase + "-" + _nameCnt.incrementAndGet() +"-"+ _hostname+ _pngNameExt);
        return FileUtil.createUniqueFileFromFile(f);
    }


    public static String createImagePngWithRegions(ImagePlot plot,
                                                   ActiveFitsReadGroup frGroup,
                                                   List<Region> regionList) throws IOException {
        StaticDrawInfo sdi= new StaticDrawInfo();
        sdi.addAllRegions(regionList);
        sdi.setDrawType(StaticDrawInfo.DrawType.REGION);
        PlotPngCreator ppC= new PlotPngCreator(Arrays.asList(sdi));

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

        File f= getUniquePngFileName("imageDownload", ServerContext.getVisSessionDir());
        File retFile= createFullTile(plot, frGroup, f, fgList,vectorList, scaleList, gridLayer);
        return ServerContext.replaceWithPrefix(retFile);
    }

    private static final int PLOT_FULL_WIDTH = -25;
    private static final int PLOT_FULL_HEIGHT = -25;

    static File createFullTile(ImagePlot plot,
                               ActiveFitsReadGroup frGroup,
                               File f,
                               List<FixedObjectGroup> fog,
                               List<VectorObject> vectorList,
                               List<ScalableObjectPosition> scaleList,
                               GridLayer gridLayer) throws IOException {
        return  createOneTile(plot,frGroup,f,0,0,PLOT_FULL_WIDTH,PLOT_FULL_HEIGHT, fog,vectorList, scaleList, gridLayer);
    }



    private static File createOneTile(ImagePlot plot,
                                      ActiveFitsReadGroup frGroup,
                                      File f,
                                      int x,
                                      int y,
                                      int width,
                                      int height,
                                      List<FixedObjectGroup> fogList,
                                      List<VectorObject> vectorList,
                                      List<ScalableObjectPosition> scaleList,
                                      GridLayer gridLayer) throws IOException {

        PlotOutput po= new PlotOutput(plot,frGroup);
        if (fogList!=null) po.setFixedObjectGroupList(fogList);
        if (gridLayer!=null) po.setGridLayer(gridLayer);
        if (vectorList!=null) po.setVectorList(vectorList);
        if (scaleList!=null) po.setScaleList(scaleList);
        int ext= f.getName().endsWith(FileUtil.jpg) ? PlotOutput.JPEG : PlotOutput.PNG;
        if (width== PLOT_FULL_WIDTH) width= plot.getScreenWidth();
        if (height== PLOT_FULL_HEIGHT) height= plot.getScreenHeight();

        po.writeTile(f, ext, plot.isUseForMask(),x, y, width, height, null);
        return f;

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
        Color c= PlotServUtils.convertColorHtmlToJava(drawInfo.getColor());
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
        Color c= PlotServUtils.convertColorHtmlToJava(color);
        if (c!=null) fg.setAllColor(FixedObjectGroup.COLOR_TYPE_STANDARD, c);
        return fg;
    }

}

