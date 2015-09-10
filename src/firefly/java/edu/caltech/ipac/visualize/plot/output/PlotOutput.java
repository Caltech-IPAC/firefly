/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot.output;


import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.visualize.draw.FixedObjectGroup;
import edu.caltech.ipac.visualize.draw.GridLayer;
import edu.caltech.ipac.visualize.draw.ScalableObjectPosition;
import edu.caltech.ipac.visualize.draw.VectorObject;
import edu.caltech.ipac.visualize.plot.ActiveFitsReadGroup;
import edu.caltech.ipac.visualize.plot.ImageDataGroup;
import edu.caltech.ipac.visualize.plot.ImagePlot;
import edu.caltech.ipac.visualize.plot.Plot;
import edu.caltech.ipac.visualize.plot.PlotContainerImpl;

import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PlotOutput {

    public static final int JPEG= 99;
    public static final int GIF=  88;
    public static final int PNG=  84;
    public static final int BMP=  82;

    public enum Quality {HIGH, MEDIUM, LOW}
    private static final int _trySizes[]= {512,640,500,630,748,760,494,600,700,420,800,825,650};
    public static final int CREATE_ALL= -1;
    private final ImagePlot _plot;
    private final ActiveFitsReadGroup _frGroup;
    private GridLayer _gridLayer;
    private List<FixedObjectGroup> _fogList= null;
    private List<VectorObject> _vectorList= null;
    private List<ScalableObjectPosition> _scaleList= null;

    public PlotOutput(Plot plot, ActiveFitsReadGroup frGroup) {
        _plot= (ImagePlot)plot;
        _frGroup= frGroup;
    }

    public void setFixedObjectGroupList(List<FixedObjectGroup> fogList) {
        _fogList= fogList;
    }

    public void setGridLayer(GridLayer gridLayer) { _gridLayer= gridLayer; }
    public void setVectorList(List<VectorObject> vectorList) { _vectorList= vectorList; }
    public void setScaleList(List<ScalableObjectPosition> scaleList) { _scaleList= scaleList; }

//    public void save(OutputStream out, int outType) throws IOException {
//
//        Assert.tst(outType == JPEG ||
//                   outType == BMP  ||
//                   outType == PNG  ||
//                   outType == GIF);
//
//        BufferedImage   image;
//        if (outType==JPEG) {
//            image= new BufferedImage(_plot.getScreenWidth(),
//                                     _plot.getScreenHeight(),
//                                     BufferedImage.TYPE_BYTE_INDEXED);
//        }
//        else {
//            image= createImage(_plot.getScreenWidth(),
//                                    _plot.getScreenHeight(),
//                                    Quality.HIGH);
//        }
//        Graphics2D g2= image.createGraphics();
//        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
//        g2.setClip(0,0, _plot.getScreenWidth(), _plot.getScreenHeight() );
//        _plot.getPlotGroup().beginPainting(g2);
//
//        PlotContainer container= new PlotContainerImpl();
//        ((PlotContainerImpl)container).getPlotList().add(_plot);
//        container.firePlotPaint(_plot, _frGroup, g2);
//        saveImage(image,outType,out);
//    }


    /**
     * Find a tiles size the divides evenly into the the zoom factor.
     * This is to prevent round off errors in the image
     * @return the file size
     */

//    private int findTileSizeEXPERIMENTAL() {
//
//        int retval= -1;
//        float zoomLevel= _plot.getZoomFactor();
//        int intZoomLevel= (int)zoomLevel;
//
//        int baseTile;
//        int w= _plot.getImageDataWidth();
//        if (w>4000) baseTile= 200;
//        else if (w>2000) baseTile= 160;
//        else if (w>1000) baseTile= 100;
//        else if (w>500) baseTile= 80;
//        else baseTile= 60;
//
//        if (intZoomLevel<.50001 && intZoomLevel>.49999) {
//            retval= baseTile/2;
//        }
//
//        if (intZoomLevel<.250001 && intZoomLevel>.24999) {
//            retval= baseTile/4;
//        }
//
//
//        if (intZoomLevel==1 ||
//            intZoomLevel==2 ||
//            intZoomLevel==4 ||
//            intZoomLevel==8 ||
//            intZoomLevel==16 ) {
//            retval= baseTile*intZoomLevel;
//        }
//
//
//
//        if (retval==-1) {
//            if (_plot.getZoomFactor()<0) {
//                retval= 500;
//            }
//            else {
//
//                for(int size : _trySizes) {
//                    if ((size % intZoomLevel) == 0) {
//                        retval= size;
//                        break;
//                    }
//                }
//
//
//                if (retval==-1) {
//                    if (intZoomLevel < 21) {
//                        retval= intZoomLevel*35;
//                    }
//                    else if (intZoomLevel < 31) {
//                        retval= intZoomLevel*25;
//                    }
//                    else if (intZoomLevel < 41) {
//                        retval= intZoomLevel*15;
//                    }
//                    else if (intZoomLevel < 51) {
//                        retval= intZoomLevel*12;
//                    }
//                    else {
//                        retval= intZoomLevel*10;
//                    }
//                }
//            }
//        }
//
//
//
//        return retval;
//    }

    private int findTileSize() {

        return findTileSize(_plot.getZoomFactor());
    }


    private static int findTileSize(float zfact) {

        int retval= -1;

        if (zfact<1) {
            retval= 500;
        }
        else {
            int intZoomLevel= (int)zfact;

            for(int size : _trySizes) {
                if ((size % intZoomLevel) == 0) {
                    retval= size;
                    break;
                }
            }


            if (retval==-1) {
                if (intZoomLevel < 21) {
                    retval= intZoomLevel*35;
                }
                else if (intZoomLevel < 31) {
                    retval= intZoomLevel*25;
                }
                else if (intZoomLevel < 41) {
                    retval= intZoomLevel*15;
                }
                else if (intZoomLevel < 51) {
                    retval= intZoomLevel*12;
                }
                else {
                    retval= intZoomLevel*10;
                }
            }
        }


        return retval;
    }

    private static String getExt(int outType) {
        String retval= FileUtil.png;
        switch (outType) {
            case JPEG : retval= FileUtil.jpg; break;
            case PNG : retval= FileUtil.png; break;
            case GIF : retval= FileUtil.gif; break;
            case BMP : retval= FileUtil.bmp; break;
        }
        return retval;

    }


    public List<TileFileInfo> writeTilesFullScreen(File dir,
                                                  String baseName,
                                                  int outType,
                                                  boolean createTile) throws IOException {




        List<TileFileInfo> retval;
        if (_plot.getZoomFactor()<1) {
            int tileSize= findTileSize();
            retval= writeTiles(dir,baseName,outType,tileSize,10);
        }
        else {
            retval= new ArrayList<TileFileInfo>(1);
            int width= _plot.getScreenWidth();
            int height= _plot.getScreenHeight();
            BufferedImage image= createImage(width,height, Quality.MEDIUM);
            File f= getTileFile(dir,baseName,0,0,getExt(outType));
            if (createTile) {
                writeTile(f,outType,0,0,width,height,image);
            }
            retval.add(new TileFileInfo(0,0,width, height,f,createTile));
        }

        return retval;
    }



    public List<TileFileInfo> writeTiles(File dir,
                                         String baseName,
                                         int outType,
                                         int createOnly ) throws IOException {


        // this is because of a bug in ImagePlot.paintTile, the tile size needs to
        // divid evenly into the zoom level
        int tileSize= findTileSize();

        return writeTiles(dir,baseName,outType,tileSize,createOnly);
    }


    public List<TileFileInfo> writeTiles(File dir,
                                         String baseName,
                                         int outType,
                                         int defTileSize,
                                         int createOnly) throws IOException {
        Assert.argTst( (createOnly>=-1 || createOnly==CREATE_ALL),
                       "createOnly must be greater than 0 or be constant CREATE_ALL");

        String ext= getExt(outType);



        ImagePlot plot= _plot;
        PlotContainerImpl container= new PlotContainerImpl();
        container.getPlotList().add(_plot);


        Assert.tst(outType == JPEG ||
                   outType == BMP  ||
                   outType == PNG  ||
                   outType == GIF);
        int width= defTileSize;
        int height;
        File file;
        BufferedImage defImage= createImage(defTileSize,defTileSize, Quality.MEDIUM);

        ImageDataGroup imageDataGrp= plot.getImageData();
        List<TileFileInfo> retList= new ArrayList<TileFileInfo>(imageDataGrp.size());
        BufferedImage image;
        int screenWidth= plot.getScreenWidth();
        int screenHeight= plot.getScreenHeight();
        int totalCreated= 0;
        boolean createTile= (totalCreated<createOnly || createOnly==CREATE_ALL);


        for(int x= 0; (x<screenWidth);  x+=width) {
            for(int y= 0; (y<screenHeight);  y+=height) {
                width= ((x+defTileSize) > screenWidth-defTileSize) ? screenWidth - x : defTileSize;
                height= ((y+defTileSize) > screenHeight-defTileSize) ? screenHeight - y : defTileSize;
                if (width==defTileSize && height==defTileSize) {
                    image= defImage;
                }
                else {
                    image= createImage(width,height, Quality.MEDIUM);
                }
                file= getTileFile(dir,baseName,x,y,ext);
                if (createTile) {
                    writeTile(file,outType,x,y,width,height,image);
                    totalCreated++;
                    createTile= (totalCreated<createOnly || createOnly==CREATE_ALL);
                }
                retList.add(new TileFileInfo(x,y,width, height, file,createTile));
            }
        }

        return retList;
    }


    public static List<TileFileInfo> makeTilesNoCreation(File dir,
                                                         float zfact,
                                                         String baseName,
                                                         int outType,
                                                         int screenWidth,
                                                         int screenHeight) {

        String ext= getExt(outType);
        Assert.argTst(outType == JPEG ||
                      outType == BMP  ||
                      outType == PNG  ||
                      outType == GIF, "ext must be jpeg, bmp, png, or gif");
        int defTileSize= findTileSize(zfact);
        int width= defTileSize;
        int height;
        File file;

        List<TileFileInfo> retList= new ArrayList<TileFileInfo>(20);


        for(int x= 0; (x<screenWidth);  x+=width) {
            for(int y= 0; (y<screenHeight);  y+=height) {
                width= ((x+defTileSize) > screenWidth-defTileSize) ? screenWidth - x : defTileSize;
                height= ((y+defTileSize) > screenHeight-defTileSize) ? screenHeight - y : defTileSize;
                file= getTileFile(dir,baseName,x,y,ext);
                retList.add(new TileFileInfo(x,y,width, height, file,false));
            }
        }

        return retList;
    }



    private static File getTileFile(File dir, String baseName, int x, int y, String ext) {
        return new File(dir,baseName+"_"+x+"_"+y+"."+ext);
    }

    public void writeThumbnail(File f, int outType ) throws IOException {
        int screenWidth= _plot.getScreenWidth();
        int screenHeight= _plot.getScreenHeight();
        BufferedImage image= createImage(screenWidth,screenHeight, Quality.MEDIUM);
        writeTile(f,outType,0,0,screenWidth,screenHeight,image);
    }


    public BufferedImage createImage(int width, int height, Quality quality) {
        BufferedImage retval;

        switch (quality) {
            case HIGH:
                retval=  new BufferedImage(width,height, BufferedImage.TYPE_INT_ARGB);
                break;
            case LOW:
                IndexColorModel cm= _plot.getImageData().getColorModel();
                retval= new BufferedImage(width,height, BufferedImage.TYPE_BYTE_INDEXED, cm);
                break;
            case MEDIUM:
                retval= new BufferedImage(width,height, BufferedImage.TYPE_USHORT_565_RGB);
                break;
            default :
                Assert.argTst(false, "quality must be HIGH, MEDIUM, or LOW");
                retval= null;
                break;
        }
        return retval;
    }




    public void writeTile( File file,
                           int outType,
                           int x,
                           int y,
                           int width,
                           int height,
                           BufferedImage image ) throws IOException {
        if (image==null)  image= createImage(width,height, Quality.MEDIUM);
        Graphics2D g2= image.createGraphics();
        g2.setClip(0, 0, width, height);
        _plot.getPlotGroup().beginPainting(g2);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_OFF);
        g2.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);
//        g2.setComposite(AlphaComposite.Src);
        _plot.preProcessImageTiles(_frGroup);
        _plot.paintTile(g2,_frGroup,x,y,width,height);

        PlotContainerImpl container= new PlotContainerImpl();
        container.getPlotList().add(_plot);

        if (_fogList!=null && _fogList.size()>0) {
            for(FixedObjectGroup fog : _fogList) {
                fog.addPlotView(container);
                fog.drawOnPlot(_plot, g2);
            }
        }

        if (_gridLayer!=null) {
            _gridLayer.drawOnPlot(_plot, g2);
        }

        if (_vectorList!=null && _vectorList.size()>0) {
            for(VectorObject v : _vectorList) {
                v.addPlotView(container);
                v.drawOnPlot(_plot,g2);
            }
        }

        if (_scaleList!=null && _scaleList.size()>0) {
            for(ScalableObjectPosition s : _scaleList) {
                s.drawOnPlot(_plot,_frGroup, g2);
            }

        }

        saveTileToFile(file, image,outType);
    }


    private void saveTileToFile(File f,
                           BufferedImage image,
                           int outType) throws IOException {
        BufferedOutputStream stream= null;
        try {
            stream= new BufferedOutputStream( new FileOutputStream(f),4096);
            saveImage(image,outType,stream);
        } finally {
            FileUtil.silentClose(stream);
        }
    }

    private void saveImage(BufferedImage   image, int outType, OutputStream out)
                                  throws IOException {
        if (outType == BMP) {
            ImageIO.write(image, "bmp", out);
        }
        else if (outType == PNG) {
//            ImageIO.write(image, "png", out);

            Iterator writers = ImageIO.getImageWritersByFormatName("png");
            ImageWriter writer = (ImageWriter)writers.next();
            ImageOutputStream ios = ImageIO.createImageOutputStream(out);
            writer.setOutput(ios);
            ImageWriteParam param= writer.getDefaultWriteParam();
//            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT) ;
//            param.setCompressionQuality(1.0F);
            param.setSourceSubsampling(1,1,0,0);
//            param.setDestinationType(new ImageTypeSpecifier(
//                    new DirectColorModel(24, 0x0000ff00, 0x0000ff00, 0x000000ff ),
//                    new SinglePixelPackedSampleModel(DataBuffer.TYPE_INT,image.getWidth(), image.getHeight(),)
            param.setDestinationType(new ImageTypeSpecifier(image));

            writer.write(image);


            FileUtil.silentClose(ios);

        }
        else if (outType == JPEG) {
            try {
                //ImageIO.write(image, "jpg", out);
                Iterator writers = ImageIO.getImageWritersByFormatName("jpg");
                ImageWriter writer = (ImageWriter)writers.next();
                ImageOutputStream ios = ImageIO.createImageOutputStream(out);
                writer.setOutput(ios);
                ImageWriteParam param= writer.getDefaultWriteParam();
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT) ;
                param.setCompressionQuality(1.0F);
                param.setSourceSubsampling(1,1,0,0);


                writer.write(image);


                FileUtil.silentClose(ios);
                //JPEGImageEncoder jpeg= JPEGCodec.createJPEGEncoder(out);
                //jpeg.encode(image);
            } catch (Exception e) {
                System.out.println("PlotJpeg: " + e);
            }
        }
        else if (outType == GIF) {
            GifEncoder encoder= new GifEncoder(image, out);
            encoder.encode();
        }
        else {
            Assert.tst(false);
        }
    }

    public static class TileFileInfo {
        private final int _x;
        private final int _y;
        private final int _width;
        private final int _height;
        private final File _file;
        private final boolean _created;

        public TileFileInfo( int x,
                             int y,
                             int width,
                             int height,
                             File file,
                             boolean created) {
            _x= x;
            _y= y;
            _width= width;
            _height= height;
            _file= file;
            _created= created;
        }
        public int getX() { return _x; }
        public int getY() { return _y; }
        public int getWidth() { return _width; }
        public int getHeight() { return _height; }
        public File getFile() { return _file; }
        public boolean isCreated() { return _created; }
    }


}
