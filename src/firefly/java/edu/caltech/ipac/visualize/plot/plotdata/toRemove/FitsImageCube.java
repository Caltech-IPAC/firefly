/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.visualize.plot.plotdata.toRemove;

import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataObject;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.visualize.plot.ImagePt;
import edu.caltech.ipac.visualize.plot.PixelValueException;
import edu.caltech.ipac.visualize.plot.plotdata.FitsRead;
import edu.caltech.ipac.visualize.plot.plotdata.FitsReadFactory;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCardException;
import nom.tam.fits.ImageHDU;
import nom.tam.util.BufferedDataOutputStream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.DataFormatException;

/**
 * Updated - 3/10/2021 - We are not using this file, but I am going to keep it around for awhile because is might
 * have code we want to reference or port to JS
 *
 *
 * Created by Yi Mei on 7/23/15.
 * FitsImageCube handles FITS image cube files. Save the data in a map: the keys are extension names, the values are FitsRead[].
 * Provide a getter to return the map.
 * Provide a getter to return a FitsRead when the fits extension and the 3rd WCS index are chosen.
 * Provide a getter to return a data group when the fits extension and an image point are chosen. The data group contains naxis3 data objects.
 *      Each data object is composed of the 3rd wcs value and the image data value at that image point.
 *
 * 5/30/18
 * LZ IRSA-1899
 *
 * Refactored the FitsImageCube class
 * Add the wavelength calculation features
 *
 */
public class FitsImageCube {

    //private variables:
    private Map <String, FitsRead[]> fitsReadMap = new HashMap<>();
    private Map <String, DataType[]> dataTypeMap = new HashMap<>();
    private Fits fits;


    /**
     * Input a fits, for each extension hdu call FitsRead.createFitsReadArray(fits, hdu) to get FitsRead[]. Save in fitsReadMap: < key = extName, value = FitsRead[]>.
     * For each extension,
     * @param fits
     * @throws FitsException
     */
    public FitsImageCube (Fits fits)
        throws FitsException {

        this.fits = fits;

        BasicHDU[] HDUs = fits.read();
        if (HDUs == null) {
            throw new FitsException("Bad format in FITS file. The FITS file doesn't have any HDUs.");
        }

        /* Go through each HDU in the FITS and process it if it is an valid image HDU.  The HDU can be
         compressed image HDU
        */

        for (int j = 0; j < HDUs.length; j++){

            validateHDU(HDUs[j], j);
            Header header = HDUs[j].getHeader();
            if ( !isImageCube(HDUs[j]) ) continue;
            String extName = header.containsKey("EXTNAME")?header.getStringValue("EXTNAME"):
                    "The " + String.valueOf(j) + "th extension";
            String wcs3Name = header.containsKey("CTYPE3")?header.getStringValue("CTYPE3"): "WCS3";

            //If the FITs file contains the WCS TAB lookup, the PS3_0 should be the same as the EXTNAME defined
            //in the TAB table header
            String ps3_0Name = header.getStringValue("PS3_0");
            BasicHDU[] hduPair = getHduPair(HDUs, wcs3Name, ps3_0Name,header, j);
            FitsRead[] fitsReadAry = FitsReadFactory.createFitsReadArray(hduPair, false);
            if (fitsReadAry != null) {
                // Make the fitsReadMap:
                fitsReadMap.put(extName, fitsReadAry);
                // Make the dataTypeMap: <key = extName, value = dataTypeAry>
                dataTypeMap.put(extName,getDataTypes(extName,  wcs3Name, header) );
            }

        }

        if (fitsReadMap.size() == 0){
            throw new FitsException("The FITS has no image cubes.");
        }
    }

    private void validateHDU(BasicHDU hdu, int j) throws FitsException{
        if (hdu.getHeader() == null ) {
            throw new FitsException("The" + j + "th HDU missing header in FITS file.");
        }

        if (hdu.getData().getSize()==0) {
            throw new FitsException("This HDU does not contain any data");
        }
    }
    private BasicHDU[] getHduPair (BasicHDU[] HDUs,String wcs3Name, String ps3_0Name, Header header, int j) throws HeaderCardException {

//        //Wavelength calculation needs the pixel's coordinates.
//        if (wcs3Name.toUpperCase().startsWith("WAVE") || wcs3Name.toUpperCase().startsWith("AWAV")){
//
//            //go through the HDUs array to find the BinaryTableHDU that matches the ps3_0Name
//            BasicHDU tableHdu = FitsReadUtil.getBinaryTableHdu(HDUs, ps3_0Name);
//            if (tableHdu!=null){
//                BasicHDU[] hduArr = {HDUs[j], tableHdu};
//                return hduArr;
//            }
//        }
//        BasicHDU[] hduArr = {HDUs[j]};
//        return hduArr;
        return null;
    }

    private boolean isImageCube(BasicHDU hdu)  {
        int naxis = hdu.getHeader().getIntValue("NAXIS", -1);
        int naxis3 = hdu.getHeader().getIntValue("NAXIS3", -1);
        if (! (hdu instanceof ImageHDU) || naxis!=3 || naxis3==-1) {
            return false;
        }
        return true;

    }


    /**
     * There are only two data types, one is the WCS and the other is flux of the image
     * If the WCS keywords in the header indicate that the Wavelength needs to be calculated,
     * the column name will be wavelength and the value should be calculated.  If the WCS keywords
     * indicate it is a FITS with the wavelength calculated, the column name will be wcs3Name
     * @param extName
     * @param wcs3Name
     * @param header
     * @return
     */
    private  DataType[] getDataTypes(String extName, String wcs3Name, Header header) {

        DataType[] dataTypes = new DataType[2];

        if (wcs3Name.toUpperCase().startsWith("WAVE") || wcs3Name.toUpperCase().startsWith("AWAV")){
            dataTypes[0] = new DataType("wavelength", Double.class);
            dataTypes[0].setUnits(header.getStringValue("CUNIT3"));

        }
        else {
            dataTypes[0] = new DataType(wcs3Name, Double.class);
            dataTypes[0].setUnits(header.getStringValue("CUNIT3"));

        }

        dataTypes[1] = new DataType(extName, Double.class);
        dataTypes[1].setUnits(header.getStringValue("BUNIT"));

        return dataTypes;
    }


    public String[] getMapKeys(){
        return fitsReadMap.keySet().toArray(new String[0]);
    }

    /**
     *
     * @return fitsReadMap
     */
    public Map<String, FitsRead[]> getFitsReadMap(){
        return fitsReadMap;
    }

    /**
     * Return a single FitsRead when the extension name and the 3rd index of the cube are given.
     * @param extName: extension name
     * @param z: The3rd index of the cube
     * @return: A single FitsRead of the image in that extension at zth index.
     */

    public FitsRead getFitsRead(String extName, int z) {
        return fitsReadMap.get(extName)[z];
    }
    /**
     *
     * @param dg
     * @param dataTypes
     * @param fitsRead
     * @param imagePt
     * @param pixPosition
     * @return
     * @throws DataFormatException
     * @throws FitsException
     * @throws PixelValueException
     * @throws IOException
     */
    private DataObject getDataRow (DataGroup dg, DataType[] dataTypes, FitsRead fitsRead, ImagePt imagePt, int pixPosition) throws DataFormatException, FitsException, PixelValueException, IOException {

        // ----------------
        // note - this was commented out because fits read is no longer including a table HDU
        // this will be handled differently in the future
        // ----------------
        //
//        DataObject dataObject = new DataObject(dg);
//        Header header = fitsRead.getHeader();
//
//        Wavelength wl= new Wavelength(fitsRead.getHeader(), fitsRead.getTableHDU());
//        for (int i=0; i<dataTypes.length; i++){
//
//            if (i==0 ) {
//                if (dataTypes[i].getKeyName().equalsIgnoreCase("wavelength")) {
//                    //Use WCS keywords information in the header to calculate the wavelength
//                    double waveLengthVal = wl.getWaveLength(imagePt);
//
//                    // Set the image data value at the zth image and the imagePt to the second value of the dataObj:
//                    dataObject.setDataElement(dataTypes[i], waveLengthVal);
//                } else {
//                    // Get the value of the 3rd WCS (eg. wavelength) at zth image:
//                    double wcs3Min = header.getDoubleValue("CRVAL3", Double.NaN);
//                    double wcs3Delt = header.getDoubleValue("CDELT3", Double.NaN);
//                    double wcs3Val;
//                    if (!Double.isNaN(wcs3Min) && !Double.isNaN(wcs3Delt)) {
//                        wcs3Val = wcs3Min + wcs3Delt * pixPosition;
//                    } else {
//                        // if not enough wcs3 information, use the index:
//                        wcs3Val = pixPosition;
//                    }
//
//                    // Set the 3rd WCS value at the zth image to the first value of the dataObj:
//                    dataObject.setDataElement(dataTypes[i], wcs3Val);
//                }
//            }
//            else {
//                // Get the image value at the zth image and the imagePt:
//                double imgVal = fitsRead.getFlux(imagePt);
//
//                // Set the image data value at the zth image and the imagePt to the second value of the dataObj:
//                dataObject.setDataElement(dataTypes[i], imgVal);
//            }
//        }
//        return dataObject;
        return null;
    }


    /**
     * At a given map key (fits extension) and an image point, return a data group:
     *     Each dataObj contains the 3rd dimension value (wavelength, frequency or time in WCS) and
     *     the image data (image flux or error or coverage).
     *
     * @param mapKey
     * @param imagePt: image point
     * @return dataGroup
     */
    public DataGroup getDataGroup(String mapKey, ImagePt imagePt)
            throws PixelValueException, FitsException, IOException, DataFormatException {

        // Get the dataTypeAry for this extension:
        DataType[] dataTypeAry = dataTypeMap.get(mapKey);

        // Initialize the dataGroup:
        DataGroup dataGroup = new DataGroup(mapKey, dataTypeAry);

        // Get the fitsReadAry for this extension:
        FitsRead[] fitsReadAry = fitsReadMap.get(mapKey);
        int numOfFitsReads = fitsReadAry.length;

        for (int z = 0; z < numOfFitsReads; z ++) {
            // For each image, set the dataObj: the 3rd WCS (wavelength or frequency or time) and the data value (eg. flux).
            FitsRead fitsRead = fitsReadAry[z];
            DataObject dataObj = getDataRow(dataGroup, dataTypeAry, fitsRead,imagePt, z);
            dataGroup.add(dataObj);
        }
        return dataGroup;
    }

    public Fits getFits() {
        return (fits);
    }

    static void usage()
    {
        System.out.println("usage java edu.caltech.ipac.astro.FITSTableReader <fits_filename> <ipac_filename>");
        System.exit(1);
    }



    public static void main(String[] args) throws DataFormatException, FitsException, IOException, PixelValueException {

        //make a new FITs for from cube1.fits to test wavelength
        Fits inFits = new Fits(args[0]);

        //create a output FITS file for unit test (cube1LinearDg.fits)
       /* FitsImageCube fic = new FitsImageCube(inFits);
        String[] keys =fic.getMapKeys();
        DataGroup dg = fic.getDataGroup(keys[0], new ImagePt(0, 0));
        IpacTableToFITS ipac_to_fits = new IpacTableToFITS();
        Fits retFits =  ipac_to_fits.convertToFITS(dg);
        retFits.write(new File(args[1]));*/



        //create a testing file for wavelength calculation using  linear algorithm
        BasicHDU[] hdus = inFits.read();
        hdus[0].getHeader().addValue("CTYPE3", "WAVE", "Linear wavelength algorithm");
        hdus[0].getHeader().addValue("PC3_1", "0.1", "Transformation matrix element");
        hdus[0].getHeader().addValue("PC3_2", "0.2", "Transformation matrix element");
        hdus[0].getHeader().addValue("PC3_3", "0.3", "Transformation matrix element");
        hdus[0].getHeader().addValue("WCSAXES", "3", "Dimension");

        Fits fits = new Fits();
        fits.addHDU(hdus[0]);
        FileOutputStream fo = new FileOutputStream(new File(args[1]));
        BufferedDataOutputStream o = new BufferedDataOutputStream(fo);
        fits.write(o);

        /*if (args.length != 2)
        {
            usage();
        }

        String fits_filename = args[0];
        String ipac_filename = args[1];

        try {

            Fits fits = new Fits(fits_filename);
            //FitsImageCube fitsImageCube = new FitsImageCube(fits);
            FitsImageCube fitsImageCube = FitsRead.createFitsImageCube(fits);
            Fits fits1 = fitsImageCube.getFits();
            System.out.println("fits = " + fits1.toString());
            //valid pixel:
            ImagePt imagePt = new ImagePt(28,28);
            //Null value:
            //ImagePt imagePt = new ImagePt(20,20);
            Object[] keys = fitsImageCube.getMapKeys();
            DataGroup dg = fitsImageCube.getDataGroup((String)keys[0], imagePt);
            File output_file = new File(ipac_filename);
            IpacTableWriter.save(output_file, dg);
        }
        catch (FitsException fe)
        {
            System.out.println("got FitsException: " + fe.getMessage());
            fe.printStackTrace();
        }
        catch (PixelValueException pve)
        {
            System.out.println("got TableFormatException: " + pve.getMessage());
            pve.printStackTrace();
        }
        catch (IOException ioe)
        {
            System.out.println("got IOException: " + ioe.getMessage());
            ioe.printStackTrace();
        }*/
    }
}
