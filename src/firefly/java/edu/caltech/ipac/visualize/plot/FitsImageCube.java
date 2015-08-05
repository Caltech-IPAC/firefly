package edu.caltech.ipac.visualize.plot;

import edu.caltech.ipac.astro.IpacTableWriter;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import nom.tam.fits.*;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Yi Mei on 7/23/15.
 * FitsImageCube handles FITS image cube files. Save the data in a map: the keys are extension names, the values are FitsRead[].
 * Provide a getter to return the map.
 * Provide a getter to return a FitsRead when the fits extension and the 3rd WCS index are chosen.
 * Provide a getter to return a data group when the fits extension and an image point are chosen. The data group contains naxis3 data objects.
 *      Each data object is composed of the 3rd wcs value and the image data value at that image point.
 */
public class FitsImageCube {

    //private variables:
    private Map <String, FitsRead[]> fitsReadMap;
    private Map <String, DataType[]> dataTypeMap;
    private String[] extNames;
    private Fits fits;


    //Constructor(s):
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
        boolean hasExtension = HDUs.length > 1 ? true : false; // what's this for?

        fitsReadMap = new HashMap<String, FitsRead[]>();
        dataTypeMap = new HashMap<String, DataType[]>();
        extNames = new String[HDUs.length];

        for (int j = 0; j < HDUs.length; j++){
            Header header = HDUs[j].getHeader();
            if (header == null)
                throw new FitsException("The" + j + "th HDU missing header in FITS file.");
            int naxis = header.getIntValue("NAXIS", -1);
            int naxis3 = header.getIntValue("NAXIS3", -1);

            if (!(HDUs[j] instanceof ImageHDU) || naxis <3 || naxis3 < 1) {
                continue;   //ignore non-image and non-cube extensions
            }

            String extName;
            if (header.getStringValue("EXTNAME") != null){
                extName = header.getStringValue("EXTNAME");
            }
            else{
                extName = "The " + String.valueOf(j) + "th extension";
            }
            extNames[j] = extName;

            String wcs3Name;
            if (header.getStringValue("CTYPE3") != null){
                wcs3Name = header.getStringValue("CTYPE3");
            }
            else {
                wcs3Name = "WCS3";
            }

            FitsRead[] fitsReadAry = FitsRead.createFitsReadArray(fits, HDUs[j]);
            if (fitsReadAry != null) {
                // Make the fitsReadMap:
                fitsReadMap.put(extName, fitsReadAry);
                // Make the dataTypeMap: <key = extName, value = dataTypeAry>
                dataTypeMap = getDataTypeMap(extName, wcs3Name, header, dataTypeMap);
            }
        }
        if (fitsReadMap.size() == 0){
            throw new FitsException("The FITS has no image cubes.");
        }
    }


    /**
     * Build the data type map for one HDU
     * @param header
     * @param dataTypeMap
     * @return
     */

    private Map<String, DataType[]> getDataTypeMap(String extName, String wcs3Name, Header header, Map<String, DataType[]> dataTypeMap) {

        //If the input dataTypeMap exists, keep adding elements to it. Otherwise make a new one.
        dataTypeMap = dataTypeMap.size() == 0 ? new HashMap<String, DataType[]>() : dataTypeMap;

        // Only two dataTypes: the first one is for the 3rd WCS; the second one is for the flux.
        DataType[] dataTypeAry = new DataType[2];

        dataTypeAry[0] = new DataType(wcs3Name, null);
        dataTypeAry[0].setDataType(Double.class);
        dataTypeAry[0].setUnits(header.getStringValue("CUNIT3"));
        dataTypeAry[0].setKeyName(header.getStringValue("CTYPE3"));

        dataTypeAry[1] = new DataType(extName, null);
        dataTypeAry[1].setDataType(Double.class);
        dataTypeAry[1].setUnits(header.getStringValue("BUNIT"));
        dataTypeAry[1].setKeyName(extName);

        dataTypeMap.put(extName, dataTypeAry);

        return dataTypeMap;
    }

    /**
     *
     * @return
     */
    public String[] getExtNames(){
        return extNames;
    }

    public Object[] getMapKeys(){
        return fitsReadMap.keySet().toArray();
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
     * At a given map key (fits extension) and an image point, return a data group:
     *     Each dataObj contains the 3rd dimension value (wavelength, frequency or time in WCS) and
     *     the image data (image flux or error or coverage).
     *
     * @param mapKey
     * @param imagePt: image point
     * @return dataGroup
     */
    public DataGroup getDataGroup(String mapKey, ImagePt imagePt)
        throws PixelValueException, FitsException {

        // Get the dataTypeAry for this extension:
        DataType[] dataTypeAry = dataTypeMap.get(mapKey);

        // Initialize the dataGroup:
        DataGroup dataGroup = new DataGroup(mapKey, dataTypeAry);

        // Get the fitsReadAry for this extension:
        FitsRead[] fitsReadAry = fitsReadMap.get(mapKey);
        int numOfFitsReads = fitsReadAry.length;

        double wcs3Min = fitsReadAry[0].getHeader().getFloatValue("CRVAL3", -1);
        double wcs3Delt = fitsReadAry[0].getHeader().getFloatValue("CDELT3", -1);

        if ( (wcs3Min == Double.NaN) || (wcs3Delt == Double.NaN) ){
            throw new FitsException("The FITS doesn't have WCS3 information.");
        }

        for (int z = 0; z < numOfFitsReads; z ++) {
            // For each image, set the dataObj: the 3rd WCS (wavelength or frequency or time) and the data value (eg. flux).

            FitsRead fitsRead = fitsReadAry[z];
            Header header = fitsRead.getHeader();

            DataObject dataObj = new DataObject(dataGroup);

            // Get the value of the 3rd WCS (eg. wavelength) at zth image:
            wcs3Min = header.getFloatValue("CRVAL3", -1);
            wcs3Delt = header.getFloatValue("CDELT3", -1);
            double wcs3Val = wcs3Min + wcs3Delt * z;

            // Set the 3rd WCS value at the zth image to the first value of the dataObj:
            dataObj.setDataElement(dataTypeAry[0], wcs3Val);

            // Get the image value at the zth image and the imagePt:
            double imgVal = fitsRead.getFlux(imagePt);

            // Set the flux at the zth image and the imagePt to the second value of the dataObj:
            dataObj.setDataElement(dataTypeAry[1], imgVal);

            // Add the dataobj to the dataGroup:
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

    public static void main(String[] args)
    {
        if (args.length != 2)
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
        }
    }
}
