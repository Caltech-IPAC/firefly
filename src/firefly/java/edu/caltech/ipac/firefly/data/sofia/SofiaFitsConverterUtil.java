package edu.caltech.ipac.firefly.data.sofia;

import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataObject;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.URLDownload;
import edu.caltech.ipac.visualize.plot.ImageHeader;
import edu.caltech.ipac.visualize.plot.plotdata.FitsReadUtil;
import nom.tam.fits.*;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;


public class SofiaFitsConverterUtil {



    /**
     * This method is not use for now.  In the future, if the specific WAVE-TAB with known HDU number, using this method,
     * the extname can be obtained.
     *
     * @param HDUs
     * @param extname
     * @return
     */
    private static BasicHDU getSpecificHDU(BasicHDU[] HDUs, String extname)  {

        for (int i=0; i<HDUs.length; i++){
            Header header = HDUs[i].getHeader();
            if (header.containsKey("EXTNAME")) {
                String extName = header.getStringValue("EXTNAME").trim();
                if (extName.equalsIgnoreCase(extname)) {
                    return HDUs[i];
                }
            }
            continue;
        }
        return null;
    }
    /**
     * In this FIFI-LS case, the index array is not specified. Thus, PS3_2 is not needed to be defined. Thus,the index is
     * a list of the integers from 1 to k1 which is the dimension of the coordinate array.
     *
     * PV3_1 is the EXTVER, the default is 1. The PV3_2 is the EXTLEVEL, default to 1.
     * PV3_3 is 3 here.  It is not needed since we don't handle multi-dimension FITs.
     *
     * So we only need to define P3_0, PS3_1 and CTYPE3.
     */
    private static void updateImageHDU(ImageHDU hdu, String waveTableName) throws HeaderCardException {
        Header header = hdu.getHeader();
        //PS3_0 is the EXTNAME in the BinaryTableHDU
        header.addLine(new HeaderCard( "PS3_0", waveTableName, "WAVE-TAB binary table name (added by Sofia)"));
        //PS3_1 is the column name for the coordinate array
        header.addLine(new HeaderCard( "PS3_1", "wavelength", "WAVE-TAB Coordinate (added by Sofia)"));
        header.addLine(new HeaderCard( "PS3_3", 1, "axis number for the Coordinate (added by Sofia)"));
        //If the wavelength is depending on the axes, the PC3_j (j=1, 2, 3) have to be defined.  By default, PC3_3 is 1.
        if (!header.containsKey("PC3_1")) {
            header.addLine(new HeaderCard("PC3_1", 1, "PC3_j matrix (added by Sofia)"));
        }
        if (!header.containsKey("PC3_2")) {
            header.addLine(new HeaderCard("PC3_2", 0, "PC3_j matrix  (added by Sofia)"));
        }

        header.updateLine("CTYPE3", new HeaderCard( "CTYPE3","WAVE-TAB", "WAVE-TAB (added by Sofia)"));
        String unit = !header.containsKey("CUNIT3")? "micron": header.getStringValue("CUNIT3");
        header.updateLine("CUNIT3", new HeaderCard( "CUNIT3",unit, "wavelength unit (maybe added by Sofia)"));

        //E. W. Greisen and M. R. Calabretta: Representations of world coordinates in FITS,
        // WCSAXES: The default value is the larger of NAXIS and the largest index of these keywords found in the
        //FITS header. Since the CTYPE3 is the WAVE-TAB, the larger is 3. Thus, we define it as a value of 3.
        header.updateLine("WCSAXES", new HeaderCard( "WCSAXES",3, "Add the missing wcsaxes keyword (added by Sofia)"));

        header.resetOriginalSize();

    }




    private static  BinaryTableHDU convertWaveTabHDU(BasicHDU hdu) throws FitsException {

        double[] coords = FitsReadUtil.getImageHDUDataInDoubleArray(hdu);
        Header header = hdu.getHeader();

        Object[][] cols = new Object[1][];
        cols[0]=new Object[]{coords};

        BinaryTable tbl = BinaryTableHDU.encapsulate(cols);
        Header hdr = BinaryTableHDU.manufactureHeader(tbl);
        hdr.addValue("TTYPE1", "wavelength", "number of table fields (added by Sofia)");
        hdr.addValue("EXTNAME", header.getStringValue("EXTNAME"), "wavelength table name from the original image header (added by Sofia)");

        return new BinaryTableHDU(hdr, tbl);
    }

    /**
     * This method is to convert the URL which has the WAVE-TAB information to a standard WAVE-TAB FITS file.
     * @param url
     * @return
     * @throws FitsException
     * @throws IOException
     * @throws FailedRequestException
     */
    public static  Fits convertToWaveTabFits(URL url) throws FitsException, IOException, FailedRequestException {
        //upload url to a FITs file
        File tempFile = File.createTempFile("fitsFile","fits");
        URLDownload.getDataToFile(url, tempFile);
        Fits fits = new Fits ( tempFile);
        tempFile.deleteOnExit();
        return convertToWaveTabFits(fits);

    }

    /**
     * This method is used for the case when the wavelength is stored in a different extname other than "WAVELENGTH".
     * @param inFits
     * @param wavelengthExtName
     * @return
     * @throws FitsException
     * @throws IOException
     */
    public static  Fits convertToWaveTabFits(Fits inFits, String wavelengthExtName) throws FitsException, IOException {

        if ( wavelengthExtName==null || wavelengthExtName.isEmpty()) return null;

        BasicHDU[] HDUs = inFits.read();

        Fits outFits = new Fits();
        BinaryTableHDU bhdu=null;
        for (int i=0; i<HDUs.length; i++){
            Header header = HDUs[i].getHeader();

            if (header.containsKey("EXTNAME")) {
                String extName = header.getStringValue("EXTNAME").toUpperCase();
                if (extName.equalsIgnoreCase(wavelengthExtName)){
                    bhdu = convertWaveTabHDU(HDUs[i]);
                    outFits.addHDU(bhdu);
                }
                else {
                    switch (extName) {
                        case "FLUX":
                            ImageHDU imageHDU = (ImageHDU) HDUs[i];
                            updateImageHDU(imageHDU,wavelengthExtName);
                            outFits.addHDU(imageHDU);
                            break;
                        default:
                            outFits.addHDU(HDUs[i]);
                            break;
                    }
                }
            }
            else { //0 header
                outFits.addHDU(HDUs[i]);
            }


        }
        outFits.close();
        return outFits;

    }


    /**
     * If the FITS is a wave-tab: 1. CTYPEi=WAVE-TAB or Lambda; 2: there is a HDU that has an EXTNAME="WAVELENGTH"
     * @param HDUs
     * @return
     */
    private static boolean isWaveTabFits(BasicHDU[] HDUs){
        //check the CTYPE
        boolean hasCTYPEDefined=false;
        for (int i=0; i<HDUs.length; i++){
            Header header = HDUs[i].getHeader();
            if (hasCTYPEDefined) break;
            for (int j=1; j<=3; j++){
                String ctype = "CTYPE"+j;
                if (!header.containsKey(ctype)) continue;
                if (header.getStringValue(ctype).equalsIgnoreCase("WAVE-TAB") ||
                        header.getStringValue("CTYPE"+j).equalsIgnoreCase("LAMBDA") ){
                    hasCTYPEDefined=true;
                    break;
                }
            }
        }

        //check if there is a HDU which has an EXTNAME="WAVELENGTH"
        if (!hasCTYPEDefined) return false;

        for (int i=0; i<HDUs.length; i++) {
            Header header = HDUs[i].getHeader();
            if (!header.containsKey("EXTNAME")) continue;
            String extName = header.getStringValue("EXTNAME").trim();
            if (extName.equalsIgnoreCase("WAVELENGTH")){
                return true;
            }
        }
        return false;
    }
    /**
     * This only works when the wavelength HDU has the EXTNAME="WAVELENGTH".  There is no other way to identify
     * which ImageHDU is the wavelength HDU.  Using this keyword is the only way to identify it.  If the wavelength
     * HDU has other EXTNAME, it will not work.
     *
     * To make it generic, we can pass the EXTNAME as a parameter. In such case, use the convertWaveTabFits(fits, extname)
     * method above.
     * @param inFits
     * @return
     * @throws FitsException
     * @throws IOException
     */
    public static  Fits convertToWaveTabFits(Fits inFits) throws FitsException, IOException {
        BasicHDU[] HDUs = inFits.read();
        if (!isWaveTabFits(HDUs)) {
            throw new IllegalArgumentException("This is not a WAVE-TAB FITS, skipping analysis");
        }
        Fits outFits = new Fits();
        BinaryTableHDU bhdu;
        for (int i=0; i<HDUs.length; i++){
            Header header = HDUs[i].getHeader();
            if (header.containsKey("EXTNAME")) {
                String extName = header.getStringValue("EXTNAME").toUpperCase();
                switch (extName) {
                    case "FLUX":
                        ImageHDU imageHDU = (ImageHDU) HDUs[i];
                        updateImageHDU(imageHDU, "WAVELENGTH");
                        outFits.addHDU(imageHDU);
                        break;
                    case "WAVELENGTH":
                        bhdu = convertWaveTabHDU(HDUs[i]);
                        outFits.addHDU(bhdu);
                        break;
                    default:
                        outFits.addHDU(HDUs[i]);
                        break;
                }
            }
            else {
                outFits.addHDU(HDUs[i]);
            }
        }

        outFits.close();
        return outFits;
    }

    /**
     *  This method is convert the FIFI-LS FITs from the URL to standard WAVE-TAB file
     * @param url
     * @param outputFile
     * @throws IOException
     * @throws FitsException
     * @throws FailedRequestException
     */
    public static void doConvertFits(URL url, String outputFile) throws IOException, FitsException, FailedRequestException {
        Fits fits = convertToWaveTabFits(url);
        writeFitsFile(fits, new FileOutputStream(outputFile));

    }
    /**
     * This method converts the input FITS to a standard WAVE-TAB FITS
     * @param inputFile
     * @param outputFile
     * @throws IOException
     * @throws FitsException
     */
    public static void doConvertFits(String inputFile, String outputFile) throws IOException, FitsException {
        Fits fits = convertToWaveTabFits(new Fits(inputFile));
        writeFitsFile(fits, new FileOutputStream(outputFile));

    }
    public static void writeFitsFile(Fits outFits, OutputStream stream) throws FitsException {

        outFits.write(new DataOutputStream(stream));

    }

    /**
     * Each FITs file may define the FREQ axis differently.  This method is to find which naxis_ia is the FREQ axis.
     * @param header
     * @return
     */
    private static int  getFrequencyAxis(Header header){
        int naxis = header.getIntValue("NAXIS", -1);
        if (naxis>1){
            for (int i=1; i<=naxis; i++){

                String ctype = header.getStringValue("CTYPE"+ Integer.toString(i));
                if (ctype.equalsIgnoreCase("FREQ")){
                    return i;
                }

            }
        }
        return -1;
    }

    /**
     * The transformation formula is
     *
     *  velocity = C * [restFrequency - frequency) / restFrequency]
     *  where C is the speed of light.
     *  Reference :
     *  Table 4 in "E. W. Greisen et al.: Representations of spectral coordinates in FITS"
     * @param frequency
     * @param header
     * @return
     */
    private static double[] getVelocityArray(double[] frequency, Header header){
        double restFreq = header.getDoubleValue("RESTFREQ");
        double SPEED_LIGHT = 300000; //using unit km, 3*10^8 meter
        double[] velocity = new double[ frequency.length];
        for (int i=0; i<frequency.length; i++){
            velocity[i] = SPEED_LIGHT * (restFreq  - frequency[i])/restFreq;
        }
        return velocity;
    }

    /**
     *  Reference: "E. W. Greisen et al.: Representations of spectral coordinates in FITS"
     *  frequency = restFrequency + cdet * ( pixel - crpix)
     *  where the pc_ij = 0 when i!=j, thus, the frequency is independent of the other axes
     * @param header
     * @param freqAxis
     * @return
     */
    private static double[] getFrequencyArray(Header header, int freqAxis){

        int arrayLength =  header.getIntValue("NAXIS"+ freqAxis);
        double restFreq = header.getDoubleValue("RESTFREQ");
        double cdelt = header.getDoubleValue("CDELT"+freqAxis);
        double crpix = header.getDoubleValue("CRPIX"+freqAxis);

        double[] frequency = new double[arrayLength];
        for (int i=0; i<arrayLength; i++){
            frequency[i] = (restFreq +  (i+1 - crpix )*cdelt)/1.E12;
        }

        return frequency;

    }


    /**
     * This method is to create a DataGroup based on the FREQ definition in the HDU.  It first validates the HDU to see
     * if it has the FREQ defined in the HDU header.
     * The SofiaSpectraModel is used.  The SofiaSpectraModel defines the instrument "GREAT" and the data types that will
     * be used in the DataGroup to be created.
     * 
     * @param hdu
     * @param fileName
     * @return
     * @throws FitsException
     */
    public static DataGroup makeDataGroupFromHDU(BasicHDU hdu, String fileName) throws FitsException {

        if (!isValidFrequencyHDU(hdu)) return null;
        SofiaSpectraModel spectraModel =  new SofiaSpectraModel(SofiaSpectraModel.SpectraInstrument.GREAT);


        Header header = hdu.getHeader();

        double[] flux = FitsReadUtil.getImageHDUDataInDoubleArray(hdu);

        if( header.containsKey("BSCALE")  &&  header.containsKey("BZERO")){
            flux = FitsReadUtil.getPhysicalDataDouble(flux,  new ImageHeader(header) );
        }


        int freqAxis =  getFrequencyAxis(header);
        double[] frequency = getFrequencyArray(header, freqAxis);

        String fluxUnit= hdu.getBUnit();
        spectraModel.setUnits(VOSpectraModel.SPECTRA_FIELDS.FLUX, fluxUnit);

        String freqUnit = header.getStringValue("CUNIT"+freqAxis);

        //since there is nothing in the HDU, hard code this for now
        freqUnit= freqUnit!=null?freqUnit:"THz";
        spectraModel.setUnits(VOSpectraModel.SPECTRA_FIELDS.FREQUENCY,freqUnit );

        double[] velocity=  getVelocityArray(frequency, header);
        String velocityUnit = "km/s";
        spectraModel.setUnits(VOSpectraModel.SPECTRA_FIELDS.VELOCITY, velocityUnit);

        ArrayList<DataType>  dataTypes = new ArrayList<DataType>(spectraModel.getMeta().values());

        DataGroup dataGroup = new DataGroup(fileName, dataTypes);
        DataObject row = new DataObject(dataGroup);

        for (int i=0; i<frequency.length; i++){
            row.setDataElement(dataTypes.get(0), frequency[i] );
            row.setDataElement(dataTypes.get(1), velocity[i] );
            row.setDataElement(dataTypes.get(2), flux[i] );

            dataGroup.add(row);
        }
       return dataGroup;
    }


    /**
     * Currently this implementation only support the GREAT instrument with the following requirementn
     *
     *    It has a CTYPE  = FREQU
     *    naxis2 = 1
     *    naxis3 = 1
     *    naxis4 (if exists)=1
     *    The frequency is independent of other axis
     * @param hdu
     * @return
     */
    private static boolean isValidFrequencyHDU(BasicHDU hdu) {

         Header header = hdu.getHeader();
         int freqAxis =  getFrequencyAxis(header);
         if (freqAxis==-1) return false;

         int naxis = header.getIntValue("NAXIS");
         for (int i=1; i<=naxis; i++){
             int naxisVal= header.getIntValue(("NAXIS"+i));
             if (i!=freqAxis && naxisVal!=1){
                 return false;
             }
         }
         return true;
    }

    public static void main(String[] args) throws FitsException, IOException,  FailedRequestException {
        //test file input
        String inputPath = "/Users/zhang/IRSA_Dev/testingData/sofiaWaveTab/";///Users/zhang/lsstDev/testingData/sofiaWaveTab/
        String inputFileName ="F0316_FI_IFS_04015210_BLU_WGR_700012-700013.fits";
        String inputFile = inputPath + inputFileName;
        String outputFileName = "FIFI-LS-WaveTab3.fits";
        String outputFile =inputPath +outputFileName;

        doConvertFits(inputFile,outputFile);


        Fits fits = new Fits(outputFile);
        BasicHDU[] HDUs = fits.read();
        if (HDUs == null || HDUs.length==0) throw new FitsException("Bad format in FITS file");

        //test URL
       /* String url= "https://irsadev.ipac.caltech.edu:443/data/SOFIA/FIFI-LS/OC4F/20160706_F317/proc/p4074/data/g22/F0317_FI_IFS_04004936_RED_WGR_200748-200749.fits";
        outputFile = inputPath + "URL-FIFI-LS-WaveTab1.fits";
        SofiaUtils.doConvertFits(new URL(url),outputFile);
        fits = new Fits(outputFile);
        HDUs = fits.read();
        if (HDUs == null || HDUs.length==0) throw new FitsException("Bad format in FITS file");*/

    }

}
