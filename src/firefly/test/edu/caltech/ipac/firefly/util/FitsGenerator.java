package edu.caltech.ipac.firefly.util;

import nom.tam.fits.*;

import java.io.IOException;
import java.util.Random;

/**
 * Created by zhang on 2/1/17.
 * This unititty class is added to produce FITS file for unit test when the FITS file is needed.
 *
 */
public class FitsGenerator {
    private static int NAXIS1=492;
    private static int NAXIS2=504;
    private  static  final  String[] defaultCards ={
            "SIMPLE  =                    T / STANDARD FITS FORMAT                           ",
            "BITPIX  =                   -32 / IEEE single precision floating point          ",
            "NAXIS   =                    2 / NUMBER OF AXES                                 ",
           // "NAXIS1  =                  492 / # SAMPLES PER LINE (FASTEST VARY NDEX)         ",
           // "NAXIS2  =                  504 / # LINES OF DATA IN IMAGE FILE                  ",
            "NAXIS3  =                    1 / # WAVELENGTHS                                  ",
            "BSCALE  =         3.080017E+00 / TRUE=TAPE*BSCALE+BZERO                         ",
            "BZERO   =                  0.0 /                                                ",
            "BUNIT   = 'JY/SR   '           /  INTENSITY                                     ",
            "BLANK   =          -2000000000 / TAPE VALUE FOR EMPTY PIXEL                     ",
            "CRVAL1  =               336.00 / RA AT ORIGIN (DEGREES)                         ",
            "CRPIX1  =                246.0 / SAMPLE AXIS ORIGIN (PIXEL)                     ",
            "CTYPE1  = 'RA---TAN'           /  DECREASES IN VALUE AS SAMPLE INDEX            ",
            "COMMENT INCREASES  (GNOMONIC PROJECTION)                                        ",
            "CDELT1  =        -3.333333E-02 / COORD VALUE INCREMENT DEG/PIXEL                ",
            "COMMENT AT ORIGIN ON SAMPLE AXIS                                                ",
            "CRVAL2  =                60.00 / DEC AT ORIGIN (DEGREES)                        ",
            "CRPIX2  =                 252.0 / LINE AXIS ORIGIN (PIXEL)                       ",
            "CTYPE2  = 'DEC--TAN'           /  DECREASES IN VALUE AS LINE INDEX              ",
            "COMMENT INCREASES  (GNOMONIC PROJECTION)                                        ",
            "CDELT2  =        -3.333333E-02 / COORD VALUE INCREMENT DEG/PIXEL                ",
            "COMMENT AT ORIGIN ON LINE AXIS                                                  ",
            "CRVAL3  =              2.5E-05 / WAVELENGTH IN METERS                           ",
            "CRPIX3  =                   1.0                                                  ",
            "CTYPE3  = 'LAMBDA  '                                                            ",
            "CDELT3  =                   0.0                                                  ",
            "DATAMAX =         3.080017E+09 / JY/SR  (TRUE VALUE)                            ",
            "DATAMIN =         2.939840E+07 / JY/SR  (TRUE VALUE)                            ",
            "EPOCH   =                1950.0 / EME50                                          ",
            "DATE-MAP= '01/11/84'           /  MAP RELEASE DATE (DD/MM/YY)                   ",
            "DATE    = '10/09/84'           /  DATE THIS TAPE WRITTEN (DD/MM/YY)             ",
            "ORIGIN  = 'JPL-IRAS'           /  INSTITUTION                                   ",
            "TELESCOP= 'IRAS    '                                                            ",
            "INSTRUME= 'SKYPLATE'           /  IRAS SKY PLATE                                ",
            "OBJECT  = 'PL026 H3'           /  PLATE NUMBER / HCON                           ",
            "PROJTYPE= 'GNOMONIC'           /  PROJECTION TYPE                               ",
            "COMMENT MINSOP = 428; MAXSOP = 476                                              ",
            "COMMENT LOGTAG = VSFLOG( 7.8)                                                   ",
            "COMMENT GEOMTAG = GEOM( 7.5)                                                    ",
            "COMMENT                                                                         ",
            "COMMENT PROJECTION FORMULAE:                                                    ",
            "COMMENT FORWARD FORMULA; RA0 AND DEC0 ARE THE PLATE CENTER                      ",
            "COMMENT R2D = 45.0 / ATAN(1.0)                                                    ",
            "COMMENT PIX = 30.0                                                               ",
            "COMMENT A = COS(DEC) * COS(RA0 - RA)                                            ",
            "COMMENT F = PIX * R2D / (SIN(DEC0) * SIN(DEC) + A * COS(DEC0))                  ",
            "COMMENT SAMPLE = -F * COS(DEC) * SIN(RA-RA0)                                    ",
            "COMMENT XLINE = -F * (COS(DEC0) * SIN(DEC) - A * SIN(DEC0))                     ",
            "COMMENT                                                                         ",
            "COMMENT INVERSE FORMULA; REQUIRES ARCSINE                                       ",
            "COMMENT X = -SAMPLE / (PIX * R2D)                                               ",
            "COMMENT Y = -LINE / (PIX * R2D)                                                 ",
            "COMMENT DELTA = ATAN(SQRT(X*X + Y*Y))                                           ",
            "COMMENT BETA = ATAN2(Y,X)                                                       ",
            "COMMENT DEC = ASIN(SIN(DELTA)*SIN(BETA)*COS(DEC0)+COS(DELTA)*SIN(DEC0))         ",
            "COMMENT RA = RA0 + ASIN(SIN(DELTA) * COS(BETA) / COS(DEC))                      ",
            "COMMENT REFERENCES:                                                             ",
            "COMMENT IRAS SDAS SOFTWARE INTERFACE SPECIFICATION(SIS)  #623-94/NO. SF05       ",
            "COMMENT ASTRON. ASTROPHYS. SUPPL. SER. 44,(1981) 363-370 (RE:FITS)              ",
            "COMMENT RECONCILIATION OF FITS PARMS W/ SIS SF05 PARMS:                         ",
            "COMMENT NAXIS1  = (ES - SS + 1); NAXIS2  = (EL - SL + 1);                       ",
            "COMMENT CRPIX1 = (1 - SS);       CRPIX2 = (1 - SL)                              ",
            "END                                                                             "
    };


    /**
     * This method create a FITS file using the provided header card and random generated data
     * @return
     * @throws FitsException
     * @throws ClassNotFoundException
     * @throws IOException
     */
    public static Fits getSimulateFits() throws FitsException, ClassNotFoundException, IOException {


        //use the default naxis1 and naxis2
        return getSimulateFits(NAXIS1, NAXIS2);
    }

    /**
     * This method uses the default header cards but reset the naxis1 and naxis2 values.
     * @param naxis1
     * @param naxis2
     * @return
     * @throws FitsException
     * @throws ClassNotFoundException
     * @throws IOException
     */
    public static Fits getSimulateFits(int naxis1, int naxis2) throws FitsException, ClassNotFoundException, IOException {

        Fits outFits = new Fits();

        //create a header using the default cards
        Header header = new Header(defaultCards);

        //update the naxisn
        header.setNaxis(1, naxis1);
        header.setNaxis(2, naxis2);
        ImageData imageData = getImaegData(naxis1,naxis2);
        BasicHDU newHDU = new ImageHDU(header,imageData);
        outFits.addHDU(newHDU);

        return outFits;
    }

   /* public static Fits getSimulateFits(int naxis1, int naxis2, int naxis3) throws FitsException, ClassNotFoundException, IOException {

        Fits outFits = new Fits();

        //create a header using the default cards
        Header header = new Header(defaultCards);

        //update the naxisn
        header.setNaxis(1, naxis1);
        header.setNaxis(2, naxis2);
        header.setNaxis(3, naxis3);

        ImageData imageData = getImaegData(naxis1,naxis2);
        BasicHDU newHDU = new ImageHDU(header,imageData);// d);
        outFits.addHDU(newHDU);

        return outFits;
    }

    public static Fits getSimulateFits(int naxis1, int naxis2, int naxis3, int naxis4) throws FitsException, ClassNotFoundException, IOException {

        Fits outFits = new Fits();

        //create a header using the default cards
        Header header = new Header(defaultCards);

        //update the naxisn
        header.setNaxis(1, naxis1);
        header.setNaxis(2, naxis2);
        header.setNaxis(3, naxis3);
        header.setNaxis(4, naxis4);

        ImageData imageData = getImaegData(naxis1,naxis2);
        BasicHDU newHDU = new ImageHDU(header,imageData);// d);
        outFits.addHDU(newHDU);

        return outFits;
    }*/


    private  static ImageData getImaegData(int naxis1, int naxis2){
        float[][] data= new float[naxis2][naxis1];
        Random rand = new Random(1000);
        for (int i=0; i<naxis2; i++){
            for (int j=0; j<naxis1; j++){
                data[i][j]=1000f*rand.nextFloat();
            }
        }

        return new ImageData(data);
    }
    /**
     * This method uses specified data array as the data and the dimensions as naxis1 and naxis2
     * @param data
     * @return
     * @throws FitsException
     * @throws ClassNotFoundException
     * @throws IOException
     */
    public static Fits getSimulateFits( Object data) throws FitsException, ClassNotFoundException, IOException {

        Fits outFits = new Fits();

        //create a header using the default cards
        Header header = new Header(defaultCards);
        if (data instanceof float[][]) {
            float[][] f2d=(float[][]) data;
            //update the naxisn to the
            header.setNaxis(1, f2d[0].length);
            header.setNaxis(2, f2d.length);

        }
        else if (data instanceof float[][][] ) {
            float[][][] f3d= (float[][][]) data;
            header.setNaxes(3);
            header.setNaxis(3, 1);
            header.setNaxis(1, f3d[0][0].length);
            header.setNaxis(2, f3d[0].length);

        }
        else if (data instanceof  float[][][][]){
            float[][][][] f4d=(float[][][][]) data;
            header.setNaxes(4);
            header.setNaxis(3, 1);
            header.setNaxis(4, 1);
            header.setNaxis(1, f4d[0][0][0].length);
            header.setNaxis(2, f4d[0][0].length);
        }
        ImageData imageData =new ImageData(data);
        BasicHDU newHDU = new ImageHDU(header,imageData);// d);
        outFits.addHDU(newHDU);

        return outFits;
    }

}
