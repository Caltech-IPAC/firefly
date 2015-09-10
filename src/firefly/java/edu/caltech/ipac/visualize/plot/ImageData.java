/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot;

import edu.caltech.ipac.util.Assert;
import nom.tam.fits.FitsException;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

public class ImageData implements Serializable {


    public enum ImageType {TYPE_8_BIT, TYPE_24_BIT}
    private       ImageType       _imageType;
    private RangeValues rangeValues;
    private IndexColorModel _cm;
    private int             _colorTableID= 0;   // this is not as flexible as color model and will be set to -1 when color model is set
    private BufferedImage   _bufferedImage;
    private boolean         _imageOutOfDate= true;
    private ImageMask[] imageMasks=null;
    private final int       _x;
    private final int       _y;
    private final int       _width;
    private final int       _height;
    private final int       _lastPixel;
    private final int       _lastLine;

    private AtomicInteger inUseCnt= new AtomicInteger(0);

    private WritableRaster  _raster; // currently only used with 24 bit images

   

    public ImageData(FitsRead fitsReadAry[],
                     ImageType imageType,
                     int colorTableID,
                     RangeValues rangeValues,
                     int x,
                     int y,
                     int width,
                     int height,
                     boolean constructNow) throws FitsException {

        _x= x;
        _y= y;
        _width= width;
        _height= height;
        _lastPixel= _x+_width-1;
        _lastLine= _y+_height-1;

        _imageType= imageType;
        _colorTableID= colorTableID;
        this.rangeValues= rangeValues;

        _cm = ColorTable.getColorModel(colorTableID);
        if (imageType==ImageType.TYPE_24_BIT) {
            _raster= Raster.createBandedRaster( DataBuffer.TYPE_BYTE, _width,_height,3, null);
        }
        if (constructNow) constructImage(fitsReadAry);
    }

    //LZ 7/20/15 add this to make an ImageData with given IndexColorModel
    public ImageData(FitsRead fitsReadAry[],
                     ImageType imageType,
                     ImageMask[] iMasks,
                     RangeValues rangeValues,
                     int x,
                     int y,
                     int width,
                     int height,
                     boolean constructNow) throws FitsException {

        _x= x;
        _y= y;
        _width= width;
        _height= height;
        _lastPixel= _x+_width-1;
        _lastLine= _y+_height-1;

        _imageType= imageType;

        this.rangeValues= rangeValues;

        imageMasks=iMasks;
       _cm=  getIndexColorModel(iMasks);// getIndexColorModelWithAlpha(iMasks); // getIndexColorModel(iMasks); //getIndexColorModel(iMasks); //getColorModelTest(iMasks);//


        if (imageType==ImageType.TYPE_24_BIT) {
            _raster= Raster.createBandedRaster( DataBuffer.TYPE_BYTE, _width,_height,3, null);
        }
        if (constructNow) constructImage(fitsReadAry);


    }

    public BufferedImage getImage(FitsRead fitsReadAry[])       {
        if (_imageOutOfDate) constructImage(fitsReadAry);
        return _bufferedImage;
    }


    public void freeResources() {
        _imageType= null;
        _cm= null;
        _bufferedImage= null;
        _raster= null;
        _imageOutOfDate= true;
    }

    public int getX() { return _x;}
    public int getY() { return _y;}

    public int getWidth() { return _width;}
    public int getHeight() { return _height;}

    private byte[] getDataArray(int idx) {
        DataBufferByte db;
        if (_raster==null) { // means an 8 bit image
            db= (DataBufferByte) _bufferedImage.getRaster().getDataBuffer();
        }
        else { // 24 bit image
            db= (DataBufferByte) _raster.getDataBuffer();
        }
        return db.getData(idx);
    }


    public void setColorModel(IndexColorModel color_model) {
        _colorTableID= -1;
        _cm=color_model;
        _imageOutOfDate=true;
    }

    public int getColorTableId() { return _colorTableID; }


    /**
     * don't compute the color model.  Should only be call from ImageDataGroup
     * @param colorTableID the id
     */
    void setColorTableIdOnly(int colorTableID) {
        _colorTableID= colorTableID;
    }


    public IndexColorModel getColorModel() { return _cm; }

    public void markImageOutOfDate() {
        _imageOutOfDate= true;
    }

    public boolean isImageOutOfDate() { return _imageOutOfDate; }

    public void recomputeStretch(FitsRead fitsReadAry[], int idx, RangeValues rangeValues, boolean force) {


        inUseCnt.incrementAndGet();
        boolean mapBlankPixelToZero= (_imageType == ImageType.TYPE_24_BIT);


        // if this is an 8 bit image I can recompute the stretch without rebuilding the image
        // if it is 24 bit, I will have to restretch and rebuild so don't both restretching now,
        // just mark the image as out of date


        if (_raster!=null || _imageOutOfDate) {  // raster!=null means a 24 bit image (3 color)
            _imageOutOfDate= true;
            if (force) {
                fitsReadAry[idx].doStretch(rangeValues, getDataArray(idx),
                             mapBlankPixelToZero, _x, _lastPixel, _y, _lastLine);
            }
        }
        else {
            fitsReadAry[idx].doStretch(rangeValues, getDataArray(idx),
                                       mapBlankPixelToZero, _x, _lastPixel, _y, _lastLine);
        }
        inUseCnt.decrementAndGet();
    }




    private void constructImage_orig(FitsRead fitsReadAry[]) {

        inUseCnt.incrementAndGet();
        if (_imageType==ImageType.TYPE_8_BIT) {
            _raster= null;
            _bufferedImage= new BufferedImage(_width,_height,
                                              BufferedImage.TYPE_BYTE_INDEXED, _cm);

            //LZ comment for my only understanding here
            /*the BufferedImage has a raster associated with it.  The getDataArray(0), point to the same data buffer
              db= (DataBufferByte) _bufferedImage.getRaster().getDataBuffer();
              when the array = getDataArray(0) is updated, the same data buffer is updated.
            */

            fitsReadAry[0].doStretch(rangeValues, getDataArray(0),false, _x,_lastPixel, _y, _lastLine);
        }
        else if (_imageType==ImageType.TYPE_24_BIT) {
            _bufferedImage= new BufferedImage(_width,_height,BufferedImage.TYPE_INT_RGB);

            for(int i=0; (i<fitsReadAry.length); i++) {
                byte array[]= getDataArray(i);
                if(fitsReadAry[i]!=null) {
                    fitsReadAry[i].doStretch(rangeValues, array,true, _x,_lastPixel, _y, _lastLine);
                }
                else {
                    for(int j=0; j<array.length; j++) array[j]= 0;
                }
            }
            _bufferedImage.setData(_raster);


        }
        else {
            Assert.tst(false, "image type must be TYPE_8_BIT or TYPE_24_BIT");
        }
        _imageOutOfDate=false;
        inUseCnt.decrementAndGet();

    }
   // Testing Mask 07/16/16 LZ


    /**
     * Build a dynamic IndexColorModel which contains the colors defined in the imageMask array plus a white background color.
     * The background color should be transparent.  The lsstMasks is sorted according to the index.  mask0=lsstMasks[0].getIndex()=1
     * and mask1=lsstMasks[1].getIndex=5:
     *
     *       pixel index   color
     *       0             mask0.color
     *       1             mask1.color
     *       3             white color
     *
     *
     * @param lsstMasks
     * @return
     */

    private static IndexColorModel getIndexColorModel(ImageMask[] lsstMasks){


        byte[] cmap=new byte[4*(lsstMasks.length+1) ];

        Arrays.fill( cmap, (byte)0);
        for (int i=0; i<lsstMasks.length; i++){
            cmap[4*i]  =(byte) lsstMasks[i].getColor().getRed();
            cmap[4*i+1]=(byte) lsstMasks[i].getColor().getGreen();
            cmap[4*i+2]=(byte) lsstMasks[i].getColor().getBlue();
            cmap[4*i+3]=(byte) 255; //opaque
        }

       // Color white = new Color(255, 255, 255, 0); //Color.WHITE.getRGB(), true);//0, 0, 0, 0); //
        //store the transparent white color at pixel index = lsstMasks.length
        cmap[4*lsstMasks.length]=(byte) 255;
        cmap[4*lsstMasks.length+1]= (byte) 255;
        cmap[4*lsstMasks.length+2]= (byte) 255;
        cmap[4*lsstMasks.length+3]= (byte) 0; //alpha=0, transparent

        return  new IndexColorModel(8, lsstMasks.length+1, cmap, 0, true);
    }





    private RenderedImage getImage(int width, int height) {


        // Create a buffered image in which to draw
        BufferedImage bufferedImage = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_ARGB);

        // Create a graphics contents on the buffered image
        Graphics2D g2d = bufferedImage.createGraphics();

        // Draw graphics
        g2d.setComposite(AlphaComposite.Clear);
        g2d.fillRect(0, 0, width, height);


        // Graphics context no longer needed so dispose it
        g2d.dispose();

        return bufferedImage;
    }


    private static void makeTransparant(BufferedImage img)
    {
        Graphics2D g = img.createGraphics();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR, 0.0f));
        g.fillRect(0, 0, img.getWidth(), img.getHeight());
        g.dispose();
    }

    private static BufferedImage imageToBufferedImage(Image image) {

        BufferedImage bufferedImage = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bufferedImage.createGraphics();
        g2.drawImage(image, 0, 0, null);
        g2.dispose();

        return bufferedImage;

    }

    private static Image makeColorTransparent(BufferedImage im, final Color color) {
        ImageFilter filter = new RGBImageFilter() {

            // the color we are looking for... Alpha bits are set to opaque
            public int markerRGB = color.getRGB() | 0xFF000000;

            public final int filterRGB(int x, int y, int rgb) {
                if ((rgb | 0xFF000000) == markerRGB) {
                    // Mark the alpha bits as zero - transparent
                    return 0x00FFFFFF & rgb;
                } else {
                    // nothing to do
                    return rgb;
                }
            }
        };

        ImageProducer ip = new FilteredImageSource(im.getSource(), filter);
        return Toolkit.getDefaultToolkit().createImage(ip);
    }

    private void constructImage(FitsRead fitsReadAry[])  {


        if (_imageType==ImageType.TYPE_8_BIT) {
            _raster = null;
            if (imageMasks!=null && imageMasks.length!=0){


               _bufferedImage = new BufferedImage(_width, _height, BufferedImage.TYPE_BYTE_INDEXED, _cm);
               fitsReadAry[0].doStretch(rangeValues, getDataArray(0), false, _x, _lastPixel, _y, _lastLine, imageMasks);
/*

                //test if the image has transparent pixels as intended
                File out1 = new File("/Users/zhang/lsstDev/testingData/transLZ.PNG");
                try {
                    ImageIO.write(_bufferedImage, "PNG", out1);
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
*/


   //another way to make transparent (using none transparent white color to start with and then convert to transparent white color)  (for cross check only)

   /*             int color = _bufferedImage.getRGB(0, 0);

                Image image = makeColorTransparent(_bufferedImage, new Color(color));

                BufferedImage transparent = imageToBufferedImage(image);
                _bufferedImage=transparent;

                File out = new File("/Users/zhang/lsstDev/testingData/trans.PNG");
                try {
                    ImageIO.write(transparent, "PNG", out);
                }
               catch (IOException e) {
                    e.printStackTrace();
                }

                IndexColorModel cm = (IndexColorModel) _bufferedImage.getColorModel();

                if ( _bufferedImage.getType()==BufferedImage.TYPE_4BYTE_ABGR){
                    System.out.println("support alpha");
                }

                if ( _bufferedImage.getColorModel().hasAlpha()){
                    System.out.println("support alpha");
                    int trans=_bufferedImage.getColorModel().getTransparency();
                    byte[] a = new byte[imageMasks.length+1];
                    ((IndexColorModel) _bufferedImage.getColorModel()).getAlphas(a);
                    for (int i=0; i<a.length; i++){
                        System.out.println("a="+a);
                    }
                }
               byte r = (byte) cm.getRed(1);
                byte g = (byte) cm.getGreen(1);
                byte b = (byte) cm.getBlue(1);
                byte a = (byte) cm.getAlpha(1);
*/               // Color plottedBGColor = new Color(r,g, b, a);
              //  Color bk = new Color(-1,-1, -1);

            }
            else {


            _bufferedImage = new BufferedImage(_width, _height,
                BufferedImage.TYPE_BYTE_INDEXED, _cm);
                fitsReadAry[0].doStretch(rangeValues, getDataArray(0), false, _x, _lastPixel, _y, _lastLine);


            }
        }

        else if (_imageType==ImageType.TYPE_24_BIT) {
            _bufferedImage= new BufferedImage(_width,_height,BufferedImage.TYPE_INT_RGB);

            for(int i=0; (i<fitsReadAry.length); i++) {
                byte array[]= getDataArray(i);
                if(fitsReadAry[i]!=null) {
                    fitsReadAry[i].doStretch(rangeValues, array,true, _x,_lastPixel, _y, _lastLine);
                }
                else {
                    for(int j=0; j<array.length; j++) array[j]= 0;
                }
            }
            _bufferedImage.setData(_raster);


        }
        else {
            Assert.tst(false, "image type must be TYPE_8_BIT or TYPE_24_BIT");
        }
        _imageOutOfDate=false;
        inUseCnt.decrementAndGet();

    }
}
