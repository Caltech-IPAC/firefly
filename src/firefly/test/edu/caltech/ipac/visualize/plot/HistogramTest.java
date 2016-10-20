package edu.caltech.ipac.visualize.plot;

import edu.caltech.ipac.firefly.util.FileLoader;
import nom.tam.fits.FitsException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.Before;

import java.io.*;

/**
 * Created by zhang on 10/4/16.
 */
public class HistogramTest {

     private static String fileName = "f3.fits";

    /*
     * 12 bit pixel image has 4096 colors
     *
     */
    private float[] inData = null;
    private static int HISTSIZ2 = 4096;    /* full size of hist array */
    private Histogram hist;
    private double dataMin=Double.MAX_VALUE;
    private double dataMax=Double.MIN_VALUE;
    private double delta = 1.010E-10;
    double hbinSize;
    private int len = 5000;
    private float[] data = new float[len];
    private static int[] histArray = new int[HISTSIZ2 + 1];
    private static  double[] expectedTblArray;


    @Before
    /**
     * An one dimensional array is created and it is used to run the unit test for Histogram's public methods
     */
    public void setUp() throws FitsException {


       float c = 1000.0f;
       for (int i=0; i<len; i++){
            data[i]= (float) (c*Math.abs(Math.sin(i*Math.PI/180.0)) );
        }
        dataMin = 0.0;
        dataMax = 1000.0;
        hbinSize = (dataMax - dataMin)/HISTSIZ2;
        histArray = buildHistArray(data, 0, hbinSize);

        expectedTblArray = getExpectedTblArray( histArray, dataMin, hbinSize);
        hist = new Histogram(data, dataMin, dataMax);

    }
    @After
    /**
     * Release the memories
     */
    public void tearDown() {
        inData=null;
        histArray=null;
        data=null;
    }



    @Test
    /**
     * A method was added to calculate the expected value differently here and then compare with the value in Histogram.
     */
    public void testGet_sigma(){

        boolean round_up= true;
        double rangeValue = FitsRead.getDefaultRangeValues().getUpperValue();
        double expectedSima = getExpectedSigma( histArray,rangeValue , round_up, hbinSize,  dataMin);
        double sigma = hist.get_sigma( rangeValue , round_up);
        Assert.assertEquals(expectedSima,sigma,delta);

        round_up= false;
        expectedSima = getExpectedSigma( histArray, rangeValue , round_up, hbinSize,  dataMin);
        sigma = hist.get_sigma( rangeValue , round_up);
        Assert.assertEquals(expectedSima,sigma,delta);
        System.out.println("test get_sigma is pass") ;
    }

    @Test
    /**
     * Calculated the tbl array differently here and then compare with the Histogram's result
     */
    public void testGetTblArray(){

        double[] tbl = hist.getTblArray();
        Assert.assertArrayEquals(expectedTblArray, tbl, delta);
        System.out.println("test getTbl array is pass") ;

    }

    /**
     * This method calculate the expected value
     * @param histArray
     * @param sigma_value
     * @param round_up
     * @param histBinsize
     * @param histMin
     * @return
     */
    private double getExpectedSigma(int[] histArray, double sigma_value, boolean round_up,double histBinsize, double histMin){
        double lev16 = getExpectedPctValue(histArray,16, round_up, histBinsize,histMin);
        double lev50 = getExpectedPctValue(histArray,50, round_up, histBinsize,histMin);
        double lev84 = getExpectedPctValue(histArray,84,round_up, histBinsize,histMin);
        double sigma = (lev84 - lev16) / 2;
        return (lev50 + sigma_value * sigma);
    }

    /**
     * This method calculates the expected tbl array
     * @param histArray
     * @param histMin
     * @param histBinsize
     * @return
     */

   private double[] getExpectedTblArray(int[] histArray, double histMin, double histBinsize) {

        double[] tbl = new double[256];

        int goodpix = 0;
        for (int hist_index = 0; hist_index < HISTSIZ2; hist_index++)
            goodpix +=histArray[hist_index];
        double goodpix_255 = goodpix / 255.0;

        int tblindex = 1;
        tbl[tblindex] = histMin;
        double next_goal = goodpix_255;
        int hist_index = 0;
        int accum = 0;
        while (hist_index < HISTSIZ2 && tblindex < 255) {
            if (accum >= next_goal) {
                tbl[tblindex++] = (hist_index * histBinsize + histMin);
                next_goal += goodpix_255;
            } else {
                accum += histArray[hist_index++];
            }
        }
        while (tblindex < 255)
            tbl[tblindex++] = hist_index * histBinsize + histMin;
        tbl[255] = Double.MAX_VALUE;
        return tbl;
    }

    @Test
    public  void testGet_pct(){

        double ra_value=0.5;
        double pct = hist.get_pct(ra_value, true);

        double expectedPct = getExpectedPctValue(histArray, ra_value, true, hbinSize,dataMin);

        Assert.assertEquals(expectedPct, pct,delta );

        pct = hist.get_pct(ra_value, Boolean.FALSE);

        expectedPct = getExpectedPctValue(histArray, ra_value, Boolean.FALSE, hbinSize,dataMin);

        Assert.assertEquals(expectedPct, pct,delta );
        System.out.println("test get_pct is pass") ;

    }

    /**
     * This method calculates the expected pct value
     * @param histArray
     * @param ra_value
     * @param round_up
     * @param histBinsize
     * @param histMin
     * @return
     */
    private double getExpectedPctValue(int[] histArray, double ra_value, boolean round_up, double histBinsize, double histMin ){
        int goodpix = 0;
        for (int i = 0; i < HISTSIZ2; i++)
            goodpix += histArray[i];

        int goal = (int) (goodpix * (ra_value) / 100);
        int sum=0;
        int count=0;
        for (int i=0; i<histArray.length; i++){
            sum +=histArray[i];
            if (sum>=goal){
                count=i;
                break;
            }
        }
        if (round_up)
            return ((count + 1.0) * histBinsize + histMin);
        else
            return ((count) * histBinsize + histMin);

    }

    /**
     * Build the hist array to be used for calculating expected values
     * @param data
     * @param histMin
     * @param histBinsize
     * @return
     */
    private static int[] buildHistArray(float[] data, double histMin, double histBinsize){

        int [] histArray = new int[HISTSIZ2 + 1];
        for (int k = 0; k < data.length; k++) {

            if (!Double.isNaN(data[k])) {

                int i = (int) ((data[k] - histMin) / histBinsize);
                if (i < 0 || i > HISTSIZ2) {
                    continue;
                } else {
                    histArray[i]++;
                }

            }
        }
        return histArray;

    }
    private double[] jsonArray1dToDouble1d(JSONArray jsonArray1d){
        double[] double1d = new double[jsonArray1d.size()];
        for (int i=0; i<jsonArray1d.size(); i++){
            double1d[i]=(double)jsonArray1d.get(i);
        }
        return double1d;
    }

    /**
     * This method validate the end - to - end test results.
     * @param hist
     * @param jsonObject
     */
    private void validate(Histogram hist, JSONObject jsonObject ){
        double rangeValue = FitsRead.getDefaultRangeValues().getUpperValue();
        Assert.assertEquals(hist.get_pct(rangeValue, true), jsonObject.get("pct"));
        Assert.assertEquals(hist.get_sigma(rangeValue, true), jsonObject.get("sigma"));

        JSONArray jArray=  (JSONArray) jsonObject.get("tbl");
        double[] expectedTbl = jsonArray1dToDouble1d(jArray);
        Assert.assertArrayEquals(hist.getTblArray(),expectedTbl, delta );
        System.out.println("Histogram end to end test is pass") ;

    }
    @Test
    /**
     * This method is an end to end test.  It takes a FITS file, f3.fits and creates an instance of Histogram.
     * Compare its results with the ones stored in the f3HistogramResult.json file.  The results stored in
     * f3HistogramResult.json was generated by the same procedures at the time when HistogramTest class is added.
     * It ensures that any future changes will not introduce problems.
     */
    public void testEndToEnd() throws FitsException, IOException, ParseException, ClassNotFoundException {

        String inFitsName =FileLoader.getDataPath(HistogramTest.class)+fileName;
        FitsRead fitsRead =FileLoader.loadFitsRead(HistogramTest.class, fileName);
        ImageHeader imageHeader = fitsRead.getImageHeader();
        inData = fitsRead.getDataFloat();

        float[] float1d = new float[inData.length];
        //get the raw data
        for (int i=0; i<inData.length; i++){
            float1d[i]=(inData[i] -(float) imageHeader.bzero)/( (float) imageHeader.bscale);
        }
        hist = new Histogram(float1d, (imageHeader.datamin - imageHeader.bzero) / imageHeader.bscale,
                (imageHeader.datamax - imageHeader.bzero) / imageHeader.bscale);


        String jsonFileName = inFitsName.substring(0, inFitsName.length()-5 ) + "HistogramResult.json";

        JSONParser parser = new JSONParser();
        Object obj = parser.parse(new FileReader(jsonFileName));
        JSONObject jsonObject = (JSONObject) obj;
        validate(hist, jsonObject);

    }

    /**
     * This method to convert one or two dimensional object/double array to JSONArray
     * @param  dArray
     * @return
     */
    private static JSONArray doubleArrayToJsonList(double[] dArray) {
        JSONArray list1d = new JSONArray();
         for (int j = 0; j < dArray.length; j++) {
             list1d.add(dArray[j]);
         }
         return list1d;

    }

    /**
     * Save the Histogram result to a file in json formata
     * @param hist
     * @param outJsonFile
     */
    private static void saveToJson(Histogram hist, String outJsonFile){
        double rangeValue = FitsRead.getDefaultRangeValues().getUpperValue();
        double pct = hist.get_pct(rangeValue, true);
        double sigma = hist.get_sigma(rangeValue,true);
        double[] tbl = hist.getTblArray();
        JSONArray jsonArray = doubleArrayToJsonList(tbl);
        JSONObject obj = new JSONObject();
        obj.put("pct", pct);
        obj.put("sigma",sigma);
        obj.put("tbl", jsonArray);
        try {
            FileWriter file = new FileWriter(outJsonFile);
            file.write(obj.toJSONString());
            file.flush();
            file.close();

        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    /**
     * This main program does not need to re-run.  It ran only once to create the json file.
     * Use main to generate the Histogram output and store in json format.  This is end to end test.  The json file only needs
     * to generated once.  It will be used for all future testing.  It ensures that the future changes will not affect the
     * results.  If the assertion fails, it means the changes introduce the bugs.
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {


        String inFitsName =FileLoader.getDataPath(HistogramTest.class)+fileName;
        FitsRead fitsRead =FileLoader.loadFitsRead(HistogramTest.class, fileName);
        ImageHeader imageHeader = fitsRead.getImageHeader();
        float[] inData = fitsRead.getDataFloat();

        float[] float1d = new float[inData.length];
        //get the raw data
        for (int i=0; i<inData.length; i++){
            float1d[i]=(inData[i] -(float) imageHeader.bzero)/( (float) imageHeader.bscale);
        }

        Histogram  hist = new Histogram(float1d, (imageHeader.datamin - imageHeader.bzero) / imageHeader.bscale,
                (imageHeader.datamax - imageHeader.bzero) / imageHeader.bscale);

        String outJsonFile = inFitsName.substring(0, inFitsName.length()-5 ) + "HistogramResult.json";

        saveToJson(hist, outJsonFile);
    }
}
