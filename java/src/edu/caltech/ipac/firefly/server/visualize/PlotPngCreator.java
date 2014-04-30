package edu.caltech.ipac.firefly.server.visualize;
/**
 * User: roby
 * Date: 5/11/12
 * Time: 10:20 AM
 */


import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.visualize.draw.StaticDrawInfo;
import edu.caltech.ipac.util.dd.Region;
import edu.caltech.ipac.visualize.draw.FixedObjectGroup;
import edu.caltech.ipac.visualize.draw.GridLayer;
import edu.caltech.ipac.visualize.draw.ScalableObjectPosition;
import edu.caltech.ipac.visualize.draw.SkyShape;
import edu.caltech.ipac.visualize.draw.SkyShapeFactory;
import edu.caltech.ipac.visualize.draw.VectorObject;
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

    public static String createImagePng(ImagePlot plot, List<StaticDrawInfo> drawInfoList) throws IOException {
        PlotPngCreator ppC= new PlotPngCreator(drawInfoList);
        return ppC.create(plot);
    }


    private String create(ImagePlot plot) throws IOException {
        for(StaticDrawInfo drawInfo : drawInfoList) {
            switch (drawInfo.getDrawType()) {
                case SYMBOL:
                    addSymbolDrawer(drawInfo);
                    break;
                case GRID:
                    addGridDrawer(drawInfo);
                    break;
//                case VECTOR:
//                    addVectorDrawer(drawInfo);
//                    break;
//                case SHAPE:
//                    addShapeDrawer(drawInfo, plot);
//                    break;
//                case NORTH_ARROW:
//                    addNorthArrowDrawer(drawInfo, plot);
//                    break;
//                case LABEL:
//                    addTextLabel(drawInfo);
//                    break;
                case REGION:
                    addRegion(drawInfo, plot);
                    break;
            }
        }

        File f= PlotServUtils.getUniquePngfileName("imageDownload", VisContext.getVisSessionDir());
        File retFile= PlotServUtils.createFullTile(plot, f, fgList,vectorList, scaleList, gridLayer);
        return VisContext.replaceWithPrefix(retFile);
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

//    private void addVectorDrawer(StaticDrawInfo drawInfo) {
//        List<WorldPt> drawList= drawInfo.getList();
//        if (drawList.size()>1) {
//            WorldPt wptAry[]= drawList.toArray(new WorldPt[drawList.size()]);
//            VectorObject vo= new VectorObject(wptAry);
//            Color c= convertColor(drawInfo.getColor());
//            if (c!=null) vo.getLineShape().setColor(c);
//            if (drawInfo.getLabel()!=null) {
//                vo.setLabelStrings(new String[] { drawInfo.getLabel()} );
//                vo.getStringShape().setDrawWithBackground(false);
//            }
//            vectorList.add(vo);
//        }
//    }
//


//
//    private void addNorthArrowDrawer(StaticDrawInfo drawInfo, ImagePlot plot) {
//        try {
//            double iWidth= plot.getImageDataWidth();
//            double iHeight= plot.getImageDataHeight();
//            double ix= (iWidth<100) ? iWidth*.5 : iWidth*.25;
//            double iy= (iHeight<100) ? iHeight*.5 : iWidth*.25;
//            WorldPt wpStart= plot.getWorldCoords(new Point2D.Double(ix,iy));
//            double cdelt1 = plot.getPixelScale()/3600;
//            float zf= plot.getPlotGroup().getZoomFact();
//            WorldPt wpt2= new WorldPt(wpStart.getLon(), wpStart.getLat() + (Math.abs(cdelt1)/zf)*(ARROW_LENTH/2));
//            WorldPt wptE2= new WorldPt(wpStart.getLon()+(Math.abs(cdelt1)/zf)*(ARROW_LENTH/2), wpStart.getLat());
//
//            ImagePt sptStart= plot.getImageCoords(plot.getImageCoords(wpStart));
//            ImagePt spt2= plot.getImageCoords(plot.getImageCoords(wpt2));
//
//            ImageWorkSpacePt sptE2= plot.getImageCoords(wptE2);
//
//            VisUtil.NorthEastCoords retN=VisUtil.getArrowCoords((int)sptStart.getX(),(int)sptStart.getY(),
//                                                                (int)spt2.getX(), (int)spt2.getY());
//            VisUtil.NorthEastCoords retE=VisUtil.getArrowCoords((int)sptStart.getX(),(int)sptStart.getY(),
//                                                                (int)sptE2.getX(), (int)sptE2.getY());
//
//            ImagePt northArrow[]= new ImagePt[] { new ImagePt(retN.x1,retN.y1), new ImagePt(retN.x2, retN.y2),
//                                                  new ImagePt(retN.barbX2,retN.barbY2)};
//            ImagePt eastArrow[]= new ImagePt[] { new ImagePt(retE.x1,retE.y1), new ImagePt(retE.x2, retE.y2),
//                                                 new ImagePt(retE.barbX2,retE.barbY2)};
//
//            VectorObject vN= new VectorObject(northArrow);
//            VectorObject vE= new VectorObject(eastArrow);
//
//            vectorList.add(vN);
//            vectorList.add(vE);
//
//            FixedObjectGroup fg= new FixedObjectGroup();
//            addTextObj(fg, new ImagePt(retN.textX, retN.textY), "N", null);
//            addTextObj(fg, new ImagePt(retE.textX, retE.textY), "E", null);
//            fg.setAllShapes(SkyShapeFactory.getInstance().getSkyShape("dot"));
//            fgList.add(fg);
//
//        } catch (Exception e) {
//            // ignore, don't draw anything
//        }
//    }

//    private void addTextLabel(StaticDrawInfo drawInfo) {
//        WorldPt wp= drawInfo.getList().get(0);
//        String text= drawInfo.getLabel();
//        FixedObjectGroup fg= new FixedObjectGroup();
//        addTextObj(fg,wp,text, drawInfo.getTextOffset());
//        fgList.add(fg);
//    }

    private void addRegion(StaticDrawInfo drawInfo, ImagePlot plot) {
        List<Region> regList= drawInfo.getRegionList();
        RegionPng regionPng= new RegionPng(regList,plot,fgList, vectorList,scaleList);
        regionPng.drawRegions();
    }



//    private void addTextObj(FixedObjectGroup fg, ImagePt pt, String text, OffsetScreenPt offPt) {
//        addTextObj(fg.makeFixedObject(pt), fg, text, offPt);
//    }
//
//    private void addTextObj(FixedObjectGroup fg, WorldPt pt, String text, OffsetScreenPt offPt) {
//        addTextObj(fg.makeFixedObject(pt),fg,text, offPt);
//    }

//    private void addTextObj(FixedObject fo, FixedObjectGroup fg, String text, OffsetScreenPt offPt) {
//        fo.setShowName(true);
//        fo.setShowPoint(false);
//        fo.setTargetName(text);
//        StringShape ss= fo.getDrawer().getStringShape();
//        ss.setDrawWithBackground(false);
//        ss.setOffsetDirection(StringShape.CENTER);
//        ss.setOffsetDistance(offPt == null ? 0 : offPt.getIX());
//        fg.add(fo);
//    }




////    private void addShapeDrawer(StaticDrawInfo drawInfo, ImagePlot plot) {
////        if (drawInfo.getShapeType()== ShapeDataObj.ShapeType.Circle) {
////            WorldPt wp= drawInfo.getList().get(0);
////            Color c= convertColor(drawInfo.getColor());
////            if (c!=null) {
////                scaleList.add(makeCircleObj(wp,drawInfo.getDim1(),c,plot));
////            }
////        }
//    }

//    private static ScalableObjectPosition makeCircleObj(WorldPt wp, float radius, Color c, ImagePlot plot) {
//        // this code is not as clean as the other in the class
//        // all three of the add methods have to happen before the position can be set
//        // before that three others classes have to be created
//        // this is all way more work than it should be
//        Shape shape= new Ellipse2D.Double(-radius/2,-radius/2, radius, radius); //radius set on the circle creation
//        ShapeInfo si[]= new ShapeInfo[] { new ShapeInfo(shape,c)};
//        ScalableObject scaleObj= new ScalableObject(si);
//        ScalableObjectPosition pos= new ScalableObjectPosition(scaleObj);
//
//        PlotContainerImpl container= new PlotContainerImpl();
//        container.getPlotList().add(plot);
//        scaleObj.addPlotView(container);
//        pos.addPlotView(container);
//        pos.setPosition(wp.getLon(),wp.getLat()); // world pt is set here
//        return pos;
//    }


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
