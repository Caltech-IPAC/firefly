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
import edu.caltech.ipac.visualize.plot.PlotContainer;

import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class PlotOutput {

    public static final int JPEG= 99;
    public static final int PNG=  84;
    public static final int BMP=  82;

    public enum Quality {HIGH, MEDIUM}
    private static final int[] _trySizes= {512,640,500,630,748,760,494,600,700,420,800,825,650};
    public static final int CREATE_ALL= 1;
    private final ImagePlot _plot;
    private final ActiveFitsReadGroup _frGroup;
    private GridLayer _gridLayer;
    private List<FixedObjectGroup> _fogList= null;
    private List<VectorObject> _vectorList= null;
    private List<ScalableObjectPosition> _scaleList= null;

    public PlotOutput(ImagePlot plot, ActiveFitsReadGroup frGroup) {
        _plot= plot;
        _frGroup= frGroup;
    }

    public void setFixedObjectGroupList(List<FixedObjectGroup> fogList) {
        _fogList= fogList;
    }

    public void setGridLayer(GridLayer gridLayer) { _gridLayer= gridLayer; }
    public void setVectorList(List<VectorObject> vectorList) { _vectorList= vectorList; }
    public void setScaleList(List<ScalableObjectPosition> scaleList) { _scaleList= scaleList; }

    private int findTileSize() { return findTileSize(_plot.getZoomFactor()); }

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
        return switch (outType) {
            case JPEG -> FileUtil.jpg;
            case PNG -> FileUtil.png;
            case BMP -> FileUtil.bmp;
            default -> FileUtil.png;
        };
    }


    public void writeTilesFullScreen(File dir,
                                     String baseName,
                                     int outType,
                                     boolean requiresTransparency,
                                     boolean createTile) throws IOException {
        if (_plot.getZoomFactor()<1) {
            int tileSize= findTileSize();
            writeTiles(dir,baseName,outType,requiresTransparency, tileSize,10);
        }
        else {
            int width= _plot.getScreenWidth();
            int height= _plot.getScreenHeight();
            BufferedImage image= createImage(width,height, requiresTransparency?Quality.HIGH:Quality.MEDIUM);
            File f= getTileFile(dir,baseName,0,0,getExt(outType));
            if (createTile) {
                writeTile(f,outType,requiresTransparency,0,0,width,height,image);
            }
        }
    }

    public List<TileFileInfo> writeTiles(File dir,
                                         String baseName,
                                         int outType,
                                         boolean requiresTransparency,
                                         int defTileSize,
                                         int createOnly) throws IOException {
        Assert.argTst( (createOnly>=-1 || createOnly==CREATE_ALL),
                       "createOnly must be greater than 0 or be constant CREATE_ALL");

        String ext= getExt(outType);



        ImagePlot plot= _plot;
        PlotContainer container= new PlotContainer();
        container.getPlotList().add(_plot);


        Assert.tst(outType == JPEG || outType == BMP  || outType == PNG);
        int width= defTileSize;
        int height;
        File file;
        BufferedImage defImage= null;
        if (createOnly>0) defImage= createImage(defTileSize,defTileSize, requiresTransparency?Quality.HIGH:Quality.MEDIUM);

        ImageDataGroup imageDataGrp= plot.getImageData();
        List<TileFileInfo> retList= new ArrayList<>(imageDataGrp.size());
        BufferedImage image;
        int screenWidth= plot.getScreenWidth();
        int screenHeight= plot.getScreenHeight();
        int totalCreated= 0;
        boolean createTile= (totalCreated<createOnly || createOnly==CREATE_ALL);


        for(int x= 0; (x<screenWidth);  x+=width) {
            for(int y= 0; (y<screenHeight);  y+=height) {
                image= null;
                width= ((x+defTileSize) > screenWidth-defTileSize) ? screenWidth - x : defTileSize;
                height= ((y+defTileSize) > screenHeight-defTileSize) ? screenHeight - y : defTileSize;
                if (width==defTileSize && height==defTileSize) {
                    image= defImage;
                }
                else if (createTile) {
                    image= createImage(width,height, requiresTransparency?Quality.HIGH:Quality.MEDIUM);
                }
                file= getTileFile(dir,baseName,x,y,ext);
                if (createTile) {
                    writeTile(file,outType,requiresTransparency,x,y,width,height,image);
                    totalCreated++;
                    createTile= (totalCreated<createOnly || createOnly==CREATE_ALL);
                }
                retList.add(new TileFileInfo(x,y,width, height, file,createTile));
            }
        }

        return retList;
    }

    private static File getTileFile(File dir, String baseName, int x, int y, String ext) {
        return new File(dir,baseName+"_"+x+"_"+y+"."+ext);
    }

    private BufferedImage createImage(int width, int height, Quality quality) {
        return switch (quality) {
            case HIGH ->  new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            case MEDIUM ->  new BufferedImage(width, height, BufferedImage.TYPE_USHORT_565_RGB);
        };
    }




    public void writeTile( File file,
                           int outType,
                           boolean requiresTransparency,
                           int x,
                           int y,
                           int width,
                           int height,
                           BufferedImage image ) throws IOException {
        if (image==null)  image= createImage(width,height, requiresTransparency?Quality.HIGH:Quality.MEDIUM);
        Graphics2D g2= image.createGraphics();
        g2.setComposite(AlphaComposite.Clear);
        g2.fillRect(0, 0, width, height);


        g2.setClip(0, 0, width, height);
        _plot.getPlotGroup().beginPainting(g2);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_OFF);
        g2.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);
        g2.setComposite(AlphaComposite.Src);
        _plot.preProcessImageTiles(_frGroup);
        _plot.paintTile(g2,_frGroup,x,y,width,height);

        PlotContainer container= new PlotContainer();
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


    private void saveTileToFile(File f, BufferedImage image, int outType) throws IOException {
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
            var writers = ImageIO.getImageWritersByFormatName("png");
            ImageWriter writer = (ImageWriter)writers.next();
            ImageOutputStream ios = ImageIO.createImageOutputStream(out);
            writer.setOutput(ios);
            ImageWriteParam param= writer.getDefaultWriteParam();
            param.setSourceSubsampling(1,1,0,0);
            param.setDestinationType(new ImageTypeSpecifier(image));
            writer.write(image);
            FileUtil.silentClose(ios);
        }
        else if (outType == JPEG) {
            try {
                var writers = ImageIO.getImageWritersByFormatName("jpg");
                ImageWriter writer = writers.next();
                ImageOutputStream ios = ImageIO.createImageOutputStream(out);
                writer.setOutput(ios);
                ImageWriteParam param= writer.getDefaultWriteParam();
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT) ;
                param.setCompressionQuality(1.0F);
                param.setSourceSubsampling(1,1,0,0);
                writer.write(image);
                FileUtil.silentClose(ios);
            } catch (Exception e) {
                System.out.println("PlotJpeg: " + e);
            }
        }
        else {
            Assert.tst(false);
        }
    }

    public record TileFileInfo(int x, int y, int width, int height, File file, boolean created) { }


}
