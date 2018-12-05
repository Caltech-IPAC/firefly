/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.visualize.plot.plotdata;
/**
 * User: roby
 * Date: 7/17/18
 * Time: 10:24 AM
 */


import edu.caltech.ipac.visualize.plot.ImagePt;
import edu.caltech.ipac.visualize.plot.PixelValueException;
import nom.tam.fits.BinaryTable;
import nom.tam.fits.BinaryTableHDU;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;

import java.io.IOException;
import java.util.zip.DataFormatException;

/**
 * @author Trey Roby
 */
public class Wavelength {


    private final static String  IMAGETYPE = "2D-IMAGE";

    private final Header header;
    private final BinaryTableHDU tableHDU;
    

    public Wavelength(Header header, BinaryTableHDU tableHDU) {
        this.header= header;
        this.tableHDU= tableHDU;
    }

    /**
     * This method will calculate wavelength at any given image point.  The calculation is based on
     * the paper "Representations of spectral coordinates in FITS", by E. W. Greisen1,M.R.Calabretta2, F.G.Valdes3,
     * and S. L. Allen.
     *
     * Current, the Linear, Log and Non-linear algorithms based on the WCS parameters/keywords in the headers
     * are implemented.  The TAB look up is also implemented.  For algorithm details, please read this paper.
     * The TAB algorithm is very complicated.  Since we only have one coordinate (the wavelength), our case is
     * less complicated.
     *
     * @param ipt: image point
     * @return
     * @throws PixelValueException
     * @throws FitsException
     * @throws IOException
     * @throws DataFormatException
     */
    public double getWaveLength(ImagePt ipt) throws PixelValueException, FitsException, IOException, DataFormatException {


        double[] pixelCoords= Wavelength.getPixelCoords(ipt, header);

        int N = header.getIntValue("WCSAXES", -1);
        if (N==-1) {
            N = header.getIntValue("NAXIS", -1);
            if (N==-1){
                throw new  DataFormatException("Dimension value is not available, Please set either NAXIS or WCSAXES in the header ");
            }
        }
        String type = header.getStringValue("CTYPE3");
        String fitsType = type!=null ? type : IMAGETYPE;

        double lamda = Wavelength.calculateWavelength(header, tableHDU, pixelCoords, fitsType);



        //vacuum wavelength
        if (fitsType.startsWith("WAVE")){
            return lamda;
        }
        else if( fitsType.startsWith("AWAV")) { //calculate the air wavelength and then convert to vacuum wavelength
            double n_lamda = 1 + Math.pow(10, -6) * ( 287.6155 + 1.62887/Math.pow(lamda, 2) + 0.01360/Math.pow(lamda, 4) );
            return lamda/n_lamda;
        }
        else {
            throw new  DataFormatException("This "+ fitsType + " is not supported");
        }
    }




    /**
     *
     * The algorithm is based on the fact that the spectral data is stored in the naxis3.  The naxis1, ctype1:ra
     * naxis2, ctype2: dec, naxis3, ctype3 : wavelength
     * Thus, the k value referenced in the paper is 3 here
     *
     *  omega  = x_3 = s_3* Sum [ m3_j * (p_j - r_j) ]
     *
     *  lamda = lamda_r + omega
     *
     *  lamda_r : is the reference value,  given by CRVAL3
     *
     * @param pixCoords
     * @return
     * @throws PixelValueException
     */
    private static double getWaveLengthLinear(double[] pixCoords ,int N,  double[] r_j, double[] pc_3j, double s_3,  double lamda_r) throws PixelValueException {

        return lamda_r + getOmega(pixCoords,  N, r_j, pc_3j, s_3);
    }

    /**
     *
     * The algorithm is based on the fact that the spectral data is stored in the naxis3.  The naxis1, ctype1:ra
     * naxis2, ctype2: dec, naxis3, ctype3 : wavelength
     * Thus, the k value referenced in the paper is 3 here
     *
     *  omega  = x_3 = s_3* Sum [ m3_j * (p_j - r_j) ]
     *
     *  lamda = lamda_r * exp (omega/s_3)
     *
     *  lamda_r : is the reference value,  given by CRVAL3
     *
     * @param pixCoords
     * @return
     * @throws PixelValueException
     */
     private static double getWaveLengthLog(double[] pixCoords,int N,  double[] r_j, double[] pc_3j, double s_3,  double lamda_r) throws PixelValueException {


        double omega= getOmega(pixCoords,  N, r_j, pc_3j, s_3);
        return lamda_r* Math.exp(omega/lamda_r);

    }

    /**
     *
     * The algorithm is based on the fact that the spectral data is stored in the naxis3.  The naxis1, ctype1:ra
     * naxis2, ctype2: dec, naxis3, ctype3 : wavelength
     * Thus, the k value referenced in the paper is 3 here
     *
     *  omega  = x_3 = s_3* Sum [ m3_j * (p_j - r_j) ]
     *
     *  lamda = lamda_r * exp (omega/s_3)
     *
     *  lamda_r : is the reference value,  given by CRVAL3
     *
     * @param pixCoords
     * @return
     * @throws PixelValueException
     */
    private static double getWaveLengthNonLinear(Header header, double[] pixCoords, int N, String algorithmCode, double[] r_j, double[] pc_3j, double s_3, double lamda_r) throws PixelValueException {

        double lamda=Double.NaN;
        double omega = getOmega(pixCoords,  N, r_j, pc_3j, s_3);


        switch (algorithmCode){
            case "F2W":
                lamda =lamda_r *  lamda_r/(lamda_r - omega);
                break;
            case "V2W":
                double lamda_0 = header.getDoubleValue("RESTWAV", 0);
                double b_lamda = ( Math.pow(lamda_r, 4) - Math.pow(lamda_0, 4) + 4 *Math.pow(lamda_0, 2)*lamda_r*omega )/
                        Math.pow((Math.pow(lamda_0, 2) + Math.pow(lamda_r, 2) ), 2);
                lamda = lamda_0 - Math.sqrt( (1 + b_lamda)/(1-b_lamda) );
                break;
        }
        return lamda;

    }

    private static int isSorted(double[] intArray) {

        int end = intArray.length-1;
        int counterAsc = 0;
        int counterDesc = 0;

        for (int i = 0; i < end; i++) {
            if(intArray[i] < intArray[i+1]){
                counterAsc++;
            }
            else if(intArray[i] > intArray[i+1]){
                counterDesc++;
            }
        }
        if(counterDesc==0){
            return 1;
        }
        else if (counterAsc==0){
            return -1;
        }
        else return 0;
    }

    private static int searchIndex(double[] indexVec, double psi) throws DataFormatException {
        /*Scan the indexing vector, (Ψ1, Ψ2,...), sequen-tially starting from the ﬁrst element, Ψ1,
          until a successive pair of index values is found that encompass ψm (i.e. such that Ψk ≤ ψm ≤ Ψk+1
          for monotonically increasing index values or Ψk ≥ ψm ≥ Ψk+1 for monotonically decreasing index values
          for some k). Then, when Ψk  Ψk+1, interpolate linearly on the indices
         */

        int sort = isSorted(indexVec); //1:ascending, -1: descending, 0: not sorted
        if (sort==0){
            throw new DataFormatException("The vector index array has to be either ascending or descending");
        }


        for (int i=1; i<indexVec.length; i++){
            if (sort==1 && indexVec[i-1]<=psi  && psi<=indexVec[i]){
                return i;
            }
            if (sort==-1 && indexVec[i-1]>=psi && psi>=indexVec[i]){
                return i;

            }
        }
        return -1;

    }

    private static double calculateGamma_m(double[] indexVec, double psi, int idx) throws DataFormatException {
        /*Scan the indexing vector, (Ψ1, Ψ2,...), sequen-tially starting from the ﬁrst element, Ψ1,
          until a successive pair of index values is found that encompass ψm (i.e. such that Ψk ≤ ψm ≤ Ψk+1
          for monotonically increasing index values or Ψk ≥ ψm ≥ Ψk+1 for monotonically decreasing index values
          for some k). Then, when Ψk  Ψk+1, interpolate linearly on the indices
         */


        if (idx!=-1 && indexVec[idx-1]!=indexVec[idx]){
            // Υm = k + (ψm − Ψk) / (Ψk+1− Ψk)
            return  idx + (psi-indexVec[idx-1])/(indexVec[idx]-indexVec[idx-1]);
        }
        else {
            throw new DataFormatException("No index found in the index array, gamma is undefined, so is coordinate");
        }

    }

    private static double[] convertNumbericDataToDouble(Object obj){


        if (obj instanceof float[] ){
            float[] fData = (float[]) obj;
            double[] data = new double[fData.length];
            for (int i=0; i<fData.length; i++){
                data[i] = new Double(fData[i]).doubleValue();
            }
            return data;
        }
        else if (obj instanceof double[]){
            return (double[]) obj;
        }
        else if (obj instanceof int[] ){
            int[] iData = (int[]) obj;
            double[] data = new double[iData.length];
            for (int i=0; i<iData.length; i++){
                data[i] = new Double(iData[i]).doubleValue();
            }
            return data;
        }
        else if (obj instanceof long[] ){
            long[] lData = (long[]) obj;
            double[] data = new double[lData.length];
            for (int i=0; i<lData.length; i++){
                data[i] = new Double(lData[i]).doubleValue();
            }
            return data;
        }
        else if (obj instanceof short[] ) {
            short[] sData = (short[]) obj;
            double[] data = new double[sData.length];
            for (int i=0; i<sData.length; i++){
                data[i] = new Double(sData[i]).doubleValue();
            }
            return data;
        }
        return null;
    }

    /**
     * The Table look up table contains only one row, one or two columns.  The coordinates column and an optional index
     * vector column.
     * The data stored as an array in each cell.  The array size in coordinate and index can be different.
     *
     * Using FitsTableReader, the array in the cell is flatten and the result table is the multi-row with single value
     * at each cell.  The length of rows equals to the maximum array length between two columns. There fore the
     * table processed is two column and muliti-rows table.
     *
     *
     * @param columnName
     * @return
     */
    private static double[] getArrayDataFromTable(BinaryTableHDU tableHDU, String columnName)  throws DataFormatException, FitsException {


        if (tableHDU!=null) {
            //use binaryTable
            BinaryTable table  = tableHDU.getData();
            int nCols = table.getNCols();
            for (int i=0; i<nCols; i++){
                if (tableHDU.getColumnName(i).equalsIgnoreCase(columnName)){
                    Object obj = table.getFlattenedColumn(i);
                    return convertNumbericDataToDouble(obj);
                }

            }
        }
        else {
            throw new  DataFormatException("The Tab Lookup table is not provided");

        }
        return null;
    }

    /**
     *
     * The pre-requirement is that the FITS has three axies, ra (1), dec(2) and wavelength (3).
     * Therefore the keyword for i referenced in the paper is 3
     * @param pixCoords: is the pixel coordinates
     * @param N : is the dimensionality of the WCS representation given by NAXIS or WCSAXES
     * @param r_j: is the pixel coordinate of the reference point given by CRPIXJ where j=1... N, where N=3 in our case
     * @param s_3: is a scaling factor given by CDELT3
     * @param lamda_r : is the reference value,  given by CRVAL3
     * @return
     * @throws PixelValueException
     */
    public static double getWaveLengthTable(BinaryTableHDU bHDU, double[] pixCoords, int N, double[] r_j, double s_3,  double lamda_r, Header header) throws PixelValueException, FitsException, IOException, DataFormatException {



        double[] pc_3j = new double[N];

        //The indexing vectors exist, the CDEL
        String key =  header.containsKey("CDELT3")?"PC3":"CD3";
        for (int i = 0; i <N; i++) { //add default since the FITS file created by the python omiting some PCi_j if they are 0.
            pc_3j[i]=header.getDoubleValue(key +"_"+i+1, 0);
        }
        double omega = getOmega(pixCoords,  N, r_j, pc_3j, s_3);

        double cdelt_3 = header.getDoubleValue("CDELT3");
        double psi_m = header.containsKey("CDELT3")?lamda_r + cdelt_3* omega:lamda_r + omega;

        //read the cell coordinate from the FITS table and save to two one dimensional arrays
        double [] coordData = getArrayDataFromTable(bHDU,"COORDS");

        double[] indexData=null;
        if (header.containsKey("PS3_2")) {
            indexData =  getArrayDataFromTable(bHDU,"INDEX");

        }

        if (indexData!=null) {


            int psiIndex = searchIndex(indexData, psi_m);
            if (psiIndex==-1) {
                throw new DataFormatException("No index found in the index array, gamma is undefined, so is coordinate");
            }
            double gamma_m = calculateGamma_m(indexData, psi_m, psiIndex);


            return  coordData[psiIndex] + (gamma_m - psiIndex) * (coordData[psiIndex+1] - coordData[psiIndex]);
        }
        else {
            return coordData[ (int) psi_m];
        }
    }

    /**
     *
     * The algorithm is based on the fact that the spectral data is stored in the naxis3.  The naxis1, ctype1:ra
     * naxis2, ctype2: dec, naxis3, ctype3 : wavelength
     * Thus, the k value referenced in the paper is 3 here
     *                      N
     *  omega  = x_3 = s_3* ∑ [ m3_j * (p_j - r_j) ]
     *                      j=1
     *
     *  N: is the dimensionality of the WCS representation given by NAXIS or WCSAXES
     *  p_j: is the pixel coordinates
     *  r_j: is the pixel coordinate of the reference point given by CRPIXJ where j=1... N, where N=3 in our case
     *  s_3: is a scaling factor given by CDELT3
     *  m3_j: is a linear transformation matrix given either by PC3_j or CD3_j
     *
     *
     * @param pixCoords
     * @param N
     * @param r_j
     * @param pc_3j
     * @param s_3
     * @return
     */
    private static double getOmega(double[] pixCoords, int N,  double[] r_j, double[] pc_3j, double s_3){
        double omega =0.0;
        for (int i=0; i<N; i++){
            omega += s_3 * pc_3j[i] * (pixCoords[i]-r_j[i]);
        }
        return omega;
    }

    public static double calculateWavelength(Header header, BinaryTableHDU tableHDU, double[] pixelCoords, String fitsType)
            throws PixelValueException, FitsException, IOException, DataFormatException {

        String algorithm =getAlgorithm(fitsType);
        int N = header.getIntValue("WCSAXES", -1);
        if (N==-1) {
            N = header.getIntValue("NAXIS", -1);
            if (N==-1){
                throw new  DataFormatException("Dimension value is not avaiable, Please set either NAXIS or WCSAXES in the header ");
            }
        }

        double[] r_j = new double[N];
        double[] pc_3j = new double[N];
        //The pi_j can be either CDi_j or PCi_j, so we have to test both
        for (int i = 0; i < N; i++) {
            r_j[i] = header.getDoubleValue("CRPIX" + (i+1), Double.NaN);

            //matrix mij can be either PCij or CDij
            pc_3j[i]=header.getDoubleValue("PC3" +"_"+ (i+1), Double.NaN);
            if (Double.isNaN(pc_3j[i])) {
                pc_3j[i]=header.getDoubleValue("CD3" +"_"+ (i+1), Double.NaN);
            }

            if (Double.isNaN(r_j[i])){
                throw new DataFormatException("CRPIXka  is not defined");
            }
            if ( Double.isNaN(pc_3j[i]) ){
                throw new DataFormatException("Either PC3_i or CD3_i has to be defined");
            }

            if (algorithm.equalsIgnoreCase("LOG")){
                //the values in CRPIXk and CDi_j (PC_i_j) are log based on 10 rather than natural log, so a factor is needed.
                r_j[i] *=Math.log(10);
                pc_3j[i] *=Math.log(10);
            }
        }

        double s_3 = header.getDoubleValue("CDELT3", 0);
        double lamda_r = header.getDoubleValue("CRVAL3");

        if (algorithm==null) return Double.NaN;
        double lamda=Double.NaN;

        switch (algorithm.toUpperCase()){
            case "LINEAR":
                lamda = getWaveLengthLinear(pixelCoords,N, r_j, pc_3j, s_3, lamda_r);
                break;
            case "LOG":
                lamda =getWaveLengthLog(pixelCoords,N, r_j, pc_3j, s_3, lamda_r);
                break;
            case "F2W":
            case "V2W":
                lamda =getWaveLengthNonLinear(header,pixelCoords,N, algorithm,r_j, pc_3j, s_3, lamda_r);
                break;
            case "TAB":
                lamda = getWaveLengthTable(tableHDU, pixelCoords,  N,  r_j,  s_3,   lamda_r, header) ;
                break;
        }
        return lamda;

    }

    /**
     * This method will return the algorithm specified in the FITS header.
     * If the algorithm is TAB, the header has to contain the keyword "EXTNAME".
     * The fitsType = header.getStringValue("CTYPE3"), will tell what algorithm it is.
     * The value of "CTYPE3" is WAVE-AAA, the AAA means algorithm.  If the fitsType only has
     * "WAVEE', it is linear.
     * @return
     */
    private static String getAlgorithm(String fitsType){

        String[] sArray = fitsType.split("-");
        if (sArray.length==0) return null;

        //the length has to be larger than 1
        if (sArray.length==1){
            return "Linear";

        }
        else if (sArray[1].trim().equalsIgnoreCase("LOG")){
            return "Log";
        }
        else {
            return sArray[1];
        }
    }

    /**
     *
     * The algorithm is based on the fact that the spectral data is stored in the naxis3.  The naxis1, ctype1:ra
     * naxis2, ctype2: dec, naxis3, ctype3 : wavelength
     * Thus, the k value referenced in the paper is 3 here
     *
     *                      N
     *  omega  = x_3 = s_3* ∑ [ m3_j * (p_j - r_j) ]
     *                      j=1
     *
     *  lamda = lamda_r + omega
     *
     *  lamda_r : is the reference value,  given by CRVAL3
     *
     * @param ipt
     * @return
     * @throws PixelValueException
     */
    public static double[] getPixelCoords(ImagePt ipt, Header header) throws PixelValueException {

        int p0 = (int) Math.round(ipt.getX() - 0.5); //x
        int p1 = (int) Math.round(ipt.getY() - 0.5); //y
        int p2 = header.getIntValue("SPOT_PL", 0); //z  default is 0

        int naxis1 = header.getIntValue("NAXIS1");
        int naxis2 = header.getIntValue("NAXIS2");


        if ( (p0 < 0) || (p0 >= naxis1) || (p1 < 0) || (p1 >= naxis2) /*|| (p2 < 0) || (p2 >= imageHeader.naxis3) */ ) {
            throw new PixelValueException("location " + p0 + " "+ p1 + " " +p2 + " not on the image");
        }
        double[] p_j={p0, p1, p2};
        return p_j;
    }
}
