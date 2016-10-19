package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.astro.IpacTableException;
import edu.caltech.ipac.astro.IpacTableReader;
import edu.caltech.ipac.astro.IpacTableWriter;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.packagedata.FileInfo;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupReader;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupWriter;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataObjectUtil;
import edu.caltech.ipac.util.DataType;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
@SearchProcessorImpl(id = "HistogramProcessor")
/**
 * Created by zhang on 10/16/15.
 * This class is a Histogram processor.  Its input is TableServerRequest where it contains the needed parameters to calculate
 * the histogram.
 * It works for two algorithm, fixed bin size algorithm and variable bin size algorithm.  If the bin size is available
 * from the input parameter, it will calculate the fixed bin size histogram.  If the bin size is not available, it will
 * calculate variable bin size histogram.
 */
public class HistogramProcessor extends IpacTablePartProcessor {
    private static final String SEARCH_REQUEST = "searchRequest";
    private static DataType[] columns = new DataType[]{
        new DataType("numInBin", Integer.class),
        new DataType("binMin", Double.class),
        new DataType("binMax", Double.class)
    };
    static {
        // set default precision to 9 significant digits
        columns[1].getFormatInfo().setDataFormat("%.9g");
        columns[2].getFormatInfo().setDataFormat("%.9g");
    }
    private final String FIXED_SIZE_ALGORITHM = "fixedSizeBins";
    private final String NUMBER_BINS = "numBins";
    private final String COLUMN = "columnExpression";

    private final String MIN = "min";
    private final String MAX = "max";
    // private final String ALGORITHM = "algorithm";
    private final String FALSEPOSTIVERATE = "falsePositiveRate";
    private final String PRESERVE_EMPTY_BIN="preserveEmptyBins";
    private String algorithm = null;// FIXED_SIZE_ALGORITHM;
    private int numBins;
    private double min = Double.NaN;
    private double max = Double.NaN;
    private String columnExpression;
    private double falsePostiveRate = 0.05;
    private boolean showEmptyBin=false;


    protected File loadDataFile(TableServerRequest request) throws IOException, DataAccessException {

        String searchRequestJson = request.getParam(SEARCH_REQUEST);
        if (searchRequestJson == null) {
            throw new DataAccessException("Unable to get histogram: " + SEARCH_REQUEST + " is missing");
        }
        JSONObject searchRequestJSON = (JSONObject) JSONValue.parse(request.getParam(SEARCH_REQUEST));
        String searchId = (String) searchRequestJSON.get(ServerParams.ID);
        if (searchId == null) {
            throw new DataAccessException("Unable to get histogram: " + SEARCH_REQUEST + " must contain " + ServerParams.ID);
        }
        TableServerRequest sReq = new TableServerRequest(searchId);

        String value;
        //Assign all the rest parameters (except ID) from the request to the TableServerRequest object
        for (Object param : searchRequestJSON.keySet()) {
            String name = (String) param;
            if (!name.equalsIgnoreCase(ServerParams.ID)) {
                value = searchRequestJSON.get(param).toString();
                sReq.setTrueParam(name, value);
            }
        }

        FileInfo fi = new SearchManager().getFileInfo(sReq);
        if (fi == null) {
            throw new DataAccessException("Unable to get file location info");
        }
        if (fi.getInternalFilename() == null) {
            throw new DataAccessException("File not available");
        }
        if (!fi.hasAccess()) {
            throw new SecurityException("Access is not permitted.");
        }
        getParameters(request);
        DataGroup sourceDataGroup = DataGroupReader.readAnyFormat(new File(fi.getInternalFilename()));
        double[] columnData = getColumnData(sourceDataGroup);
        DataGroup histogramDataGroup = createHistogramTable(columnData);
        histogramDataGroup.addAttribute("column", columnExpression);
        histogramDataGroup.addAttribute("searchRequest", sReq.toString());
        File histogramFile = createFile(request);
        DataGroupWriter.write(histogramFile, histogramDataGroup, 0);
        return histogramFile;
    }

    private void getParameters(TableServerRequest tableServerRequest) {
        //get all the required parameters
        List<Param> params = tableServerRequest.getParams();
        for (Param p: params.toArray(new Param[0])){
            String name = p.getName();
            String value = p.getValue();
            if (name.equalsIgnoreCase(COLUMN)) {
                //columnName = (String) value;
                columnExpression = (String) value;
            } else if (name.equalsIgnoreCase(MIN)) {
                min =  Double.parseDouble(value);

            } else if (name.equalsIgnoreCase(MAX)) {
                max =  Double.parseDouble(value);
            } else if (name.equalsIgnoreCase(NUMBER_BINS)) {
                numBins =  Integer.parseInt(value);
                if (numBins>0) algorithm = FIXED_SIZE_ALGORITHM;
            } else if (name.equalsIgnoreCase(FALSEPOSTIVERATE)) {
                falsePostiveRate =  Double.parseDouble(value);
            }
            else if (name.equalsIgnoreCase(PRESERVE_EMPTY_BIN) ){
                showEmptyBin= Boolean.parseBoolean(value);
            }
        }

    }


    /**
     * This method is changed to public to be able to run the test case in the test tree
     * This method adds the three data arrays and the DataTypes into a DataGroup (IpacTable).
     *
     * @param columnData
     * @return
     */
    public DataGroup createHistogramTable(double[] columnData) throws DataAccessException {

        DataType[] tblcolumns = columns;
        DataGroup HistogramTable;

        if (columnData.length > 0) {
            //calculate three arrays, numInBin, binMix and binMax
            Object[] obj;
            if (algorithm != null && algorithm.equalsIgnoreCase(FIXED_SIZE_ALGORITHM)) {
                obj = calculateFixedBinSizeDataArray(columnData);
            } else {
                obj = calculateVariableBinSizeDataArray(columnData);

            }

            int[] numPointsInBin = (int[]) obj[0];
            double[] binMin = (double[]) obj[1];
            double[] binMax = (double[]) obj[2];
            int nPoints = numPointsInBin.length;

            //add each row to the DataGroup
            HistogramTable = new DataGroup("histogramTable", tblcolumns);
            for (int i = 0; i < nPoints; i++) {
                DataObject row = new DataObject(HistogramTable);
                row.setDataElement(tblcolumns[0], numPointsInBin[i]);
                row.setDataElement(tblcolumns[1], binMin[i]);
                row.setDataElement(tblcolumns[2], binMax[i]);
                HistogramTable.add(row);
            }
        } else {
            HistogramTable = new DataGroup("histogramTable", tblcolumns);
        }
        return HistogramTable;
    }

    /**
     * Calculate the numInBin, binMin and binMax arrays
     *
     * @param columnData
     * @return
     */
    private Object[] calculateFixedBinSizeDataArray(double[] columnData) {
        //sort the data in ascending order, thus, index 0 has the minimum and the last index has the maximum
        Arrays.sort(columnData);

        if (Double.isNaN(min)) {
            min = columnData[0];
        }
        if (Double.isNaN(max)) {
            max = columnData[columnData.length - 1];
        }


        double binSize = (max-min)/numBins;

       // double delta =( max -min)/100*numBins;
        int[] numPointsInBin = new int[numBins];
        double[] binMin = new double[numBins];

        double[] binMax = new double[numBins];


        for (int i = 0; i < columnData.length; i++) {
            if (columnData[i] >= min && columnData[i] < max) {
                 int iBin = (int) ((columnData[i] - min) / binSize);
                 numPointsInBin[iBin]++;

            }
            else if (columnData[i]  == max) { //put the last data in the last bin
                numPointsInBin[numBins-1]++;
            }

          }
        for (int i=0; i<numBins; i++){
            binMin[i]=min+i*binSize;
            binMax[i]=binMin[i]+binSize;
        }



       if (showEmptyBin){

           Object[] obj = {numPointsInBin, binMin, binMax};
           return obj;

       }
       else {
          return filterEmptyBins(numPointsInBin, binMin, binMax);

       }
    }

    private int[]  getSelection(int[] inArray, ArrayList<Integer> list ){

        int[] outArray=new int[list.size()];
        int count=0;
        for (int i=0; i<inArray.length; i++){
            if (isInSelection(i, list)){
                outArray[count]=inArray[i];
                count++;
            }

        }
        return outArray;
    }

    private double[]  getSelection(double[] inArray, ArrayList<Integer> list ){

        double[] outArray=new double[list.size()];
        int count=0;
        for (int i=0; i<inArray.length; i++){
            if (isInSelection(i, list)){
                outArray[count]=inArray[i];
                count++;
            }

        }
        return outArray;
    }

    private boolean isInSelection( int idx, ArrayList<Integer> list){
        for (int j=0; j<list.size(); j++){
              if (idx==list.get(j).intValue()){
                 return true;
            }
        }
        return false;
    }

    private double[] getColumnData(DataGroup dg) {
        List<DataObject> objList = dg.values();
        int nRow = objList.size();
        DataType[] dataTypes=dg.getDataDefinitions();
        DataObjectUtil.DoubleValueGetter dGetter = new DataObjectUtil.DoubleValueGetter(dataTypes, columnExpression);

        double[] data = new double[nRow];
        for (int i = 0; i < nRow; i++) {
            DataObject row = objList.get(i);
            data[i] = dGetter.getValue(row);
        }
        return Arrays.stream(data).filter(d -> !Double.isNaN(d)).toArray();
    }

    private static String[] getInputFilePath(String inputFileName) {
        String[] dirs = inputFileName.split("/");
        String name = dirs[dirs.length - 1];
        String path = inputFileName.substring(0, inputFileName.length() - name.length());
        String[] ret = {path, name};
        return ret;
    }

    /**
     * This method calculate the variable bins based on the paper:
     * http://iopscience.iop.org/article/10.1088/0004-637X/764/2/167/pdf;jsessionid=22827FAAA086B2A127E88C517E0E8DD3.c1.iopscience.cld.iop.org
     * http://arxiv.org/pdf/1304.2818.pdf
     * and the python implementation: https://github.com/fedhere/fedsastroutils/blob/master/bayesianblocks_fbb.py
     * There are a few implementations. Each of them is a little different.  I based the two above and
     * modified it to take the special cases where there is no fitness value found.
     *
     * @return
     */


    private Object[] calculateVariableBinSizeDataArray(double[] columnData) throws DataAccessException {

        //sort the data in ascending order, thus, index 0 has the minimum and the last index has the maximum
        Arrays.sort(columnData);


        //get the variable bins
        double[] bins = getBins(columnData);


        int nBin = bins.length;
        int[] numPointsInBin = new int[nBin];
        double[] binMin = new double[nBin];
        if (Double.isNaN(min)) {
            min = columnData[0];
        }
        if (Double.isNaN(max)) {
            max = columnData[columnData.length - 1];
        }
     //fill all entries to the maximum, thus, all data values will be smaller than it
       // Arrays.fill(binMin, Double.MAX_VALUE);
        double[] binMax = new double[nBin];
        //fill all entries to the minimum thus, all data values will be larger than it
       // Arrays.fill(binMax, -Double.MAX_VALUE);

        if (nBin==1){  //only one bin

            double delta =( max -min)/100*nBin;

            for (int i = 0; i < columnData.length; i++) {
                if (columnData[i] >= min && columnData[i] <= max) {
                    numPointsInBin[0]++;
                }
            }
            binMin[0]=min;
            binMax[0]=bins[0];
            if (binMin[0]==binMax[0]){
                binMin[0]=min-delta;
                binMax[0]=max+delta;
            }
        }
        else {
            for (int ibin = 0; ibin < nBin; ibin++) {
                for (int i = 0; i < columnData.length; i++) {
                    if (columnData[i] >= min && columnData[i] <= max) {

                        if (ibin == 0 && columnData[i] < bins[ibin] || //left bin edge
                                ibin >= 1 && ibin < nBin - 1 && columnData[i] >= bins[ibin - 1] && columnData[i] < bins[ibin] || //the middle bins
                                ibin == nBin - 1 && columnData[i] >= bins[ibin - 1] && columnData[i] <= bins[ibin])  //the right edge bin
                        {
                            numPointsInBin[ibin]++;
                        }

                    }

                }
            }

            binMin[0] = min;
            //assign the left edge to the binMin
            for (int i = 1; i < nBin; i++) {
                binMin[i] = bins[i - 1];
              }

            //assign the right edge to the binMax
            for (int i = 0; i < nBin; i++) {
                binMax[i] = bins[i];
            }


        }

        if (showEmptyBin){

            Object[] obj = {numPointsInBin, binMin, binMax};
            return obj;

        }
        else {
           return filterEmptyBins(numPointsInBin, binMin, binMax);

        }


    }
     private  Object[] filterEmptyBins(int[] numPointsInBin, double[] binMin, double[] binMax){
         //filter out the  entries which has the empty bins
         ArrayList<Integer> idx = new ArrayList<Integer>();
         for (int i = 0; i < numPointsInBin.length; i++) {
             if (numPointsInBin[i] == 0) continue;
             idx.add(i);
         }
         Object[] obj = {getSelection(numPointsInBin, idx), getSelection(binMin, idx), getSelection(binMax, idx)};

         return obj;
     }

    /**
     * Change to public in order to run unit test
     * This method calculates the variable bins
     *
     * @param columnData - input, double array
     * @return
     * @throws DataAccessException
     */
    public double[] getBins(double[] columnData) throws DataAccessException {

        int n = columnData.length;
        //create a length=n+1 array of edges
        double[] edges = getEdges(columnData);

        double[] blockLength = new double[edges.length];
        for (int i = 0; i < edges.length; i++) {
            blockLength[i] = columnData[n - 1] - edges[i];
        }

        //this is the definition from the paper in 2013:http://arxiv.org/pdf/1304.2818.pdf
        double ncpPrior = 4.0 - Math.log(falsePostiveRate / (0.0136 * Math.pow(n, 0.478)));
        //-----------------------------------------------------------------
        // Start with first data cell; add one cell at each iteration
        //-----------------------------------------------------------------
        // arrays needed for the iteration
        double[] nnVec = new double[n];
        //fill this array by 1.0
        Arrays.fill(nnVec, 1.0);
        double[] best = new double[n];
        Arrays.fill(best, 0);
        int[] last = new int[n];
        Arrays.fill(last, 0);
        for (int k = 0; k < n; k++) {
            //Compute the argLog and count of the final bin for all possible
            // locations of the K^th changepoint
            double[] width = getWidth(blockLength, k);
            double[] comsumCountVec = getCumulativeSum(nnVec, k);
            //evaluate fitness function for these possibilities
            double[] fitnessVec = calculateFitnessFunction(comsumCountVec, width, best, ncpPrior);
            //find the max of the fitness: this is the K^th changepoint
            int iMax = getIndexOfTheMaxValue(fitnessVec);

            last[k] = iMax;
            if (iMax == -1) continue;
            best[k] = fitnessVec[iMax];

        }

        //-----------------------------------------------------------------
        // Recover changepoints by iteratively peeling off the last block
        //-----------------------------------------------------------------
        ArrayList<Integer> changePointList = new ArrayList<Integer>();
        int ind = n;

        for (int icp = n - 1; icp > -1; icp--) {
            if (ind == -1) continue;
            changePointList.add(ind);
            if (ind == 0) break;
            ind = last[ind - 1];
        }


        int[] newChangePoint = reverseArray(changePointList);

        ArrayList<Double> sData = new ArrayList<Double>();
        for (int i = 0; i < edges.length; i++) {
            for (int j = 0; j < newChangePoint.length; j++) {
                if (i == newChangePoint[j]) {
                    sData.add(edges[newChangePoint[j]]);
                    break;
                }
            }
        }
        int nBin = sData.size();
        double[] bins = new double[nBin];
        for (int i = 0; i < nBin; i++) {
            bins[i] = sData.get(i).doubleValue();
        }
        return bins;
    }

    /**
     * return -1 if there is no valid data values in the array
     *
     * @param inArray
     * @return
     */
    private int getIndexOfTheMaxValue(double[] inArray) {
        double max = -Double.MAX_VALUE; //using the minimum double value
        int maxIdx = -1;  //no valid data
        if (inArray.length >= 1) {
            for (int i = 0; i < inArray.length; i++) {
                if (Double.isFinite(inArray[i]) && inArray[i] > max) {
                    max = inArray[i];
                    maxIdx = i;
                }
            }
        }
        return maxIdx;
    }


    /**
     * This method evaluate the fitness function for these possibilities
     *
     * @param countVec
     * @param width
     * @return
     */
    private double[] calculateFitnessFunction(double[] countVec, double[] width, double[] best, double ncpPrior) throws DataAccessException {


        double[] fitnessVec = new double[countVec.length];
        for (int i = 0; i < countVec.length; i++) {
            if (Double.isFinite(width[i]) && width[i] > 0) {
                fitnessVec[i] = countVec[i] * (Math.log(countVec[i]) - Math.log(width[i]));
            } else {
                fitnessVec[i] = Double.NaN;
            }
        }

        for (int i = 0; i < fitnessVec.length; i++) {
            fitnessVec[i] -= ncpPrior;
        }
        for (int i = 1; i < fitnessVec.length; i++) {
            fitnessVec[i] += best[i - 1];
        }

        return fitnessVec;
    }

    /**
     * variable bin methods
     *
     * @param data
     * @return
     */
    private double[] getEdges(double[] data) throws DataAccessException {

        //sort the data
        Arrays.sort(data);
        int n = data.length;
        // create length=(N + 1) array of cell edges
        double[] a1 = Arrays.copyOfRange(data, 0, 1);
        double[] a2 = Arrays.copyOfRange(data, 1, n);
        double[] a3 = Arrays.copyOfRange(data, 0, n - 1);
        double[] a4 = Arrays.copyOfRange(data, n - 1, n);

        double[] a2Plusa3 = new double[a2.length];
        for (int i = 0; i < a2.length; i++) {
            a2Plusa3[i] = a2[i] + a3[i];
        }
        double[] multiply = multiply(a2Plusa3, 0.5);
        double[] edges = concatenate(concatenate(a1, multiply), a4);

        return edges;

    }

    private double[] getWidth(double[] inArray, int k) {
        double[] width = new double[k + 1];
        for (int i = 0; i < k + 1; i++) {

            width[i] = inArray[i] - inArray[k + 1];
            if (width[i] <= 0) {
                width[i] = Double.NaN;
            }

        }
        return width;
    }

    /**
     * This method reverse the array's order
     *
     * @param inArray
     * @return
     */
    private double[] reverseArray(double[] inArray) {
        double[] outArray = new double[inArray.length];
        for (int i = 0; i < inArray.length; i++) {
            outArray[i] = inArray[inArray.length - 1 - i];
        }
        return outArray;
    }

    /**
     * This method reverse the ArrayList and then return a double array
     *
     * @param inArrayList
     * @return
     */
    private int[] reverseArray(ArrayList<Integer> inArrayList) {
        int len = inArrayList.size();
        int[] outArray = new int[len];
        for (int i = 0; i < len; i++) {
            outArray[i] = inArrayList.get(len - 1 - i).intValue();
        }
        return outArray;
    }

    /**
     * This method calculates the cumulative sum
     *
     * @param nnVec
     * @param k
     * @return
     */
    private double[] getCumulativeSum(double[] nnVec, int k) {
        double[] nnCumVec = new double[k + 1];
        double[] pArray = Arrays.copyOfRange(nnVec, 0, k + 1);
        double[] rArray = reverseArray(pArray);
        double sum = 0.0;
        for (int i = 0; i < k + 1; i++) {
            sum += rArray[i];
            nnCumVec[i] = sum;
        }

        return reverseArray(nnCumVec);


    }

    /**
     * This method concatenate two double arrays
     *
     * @param a
     * @param b
     * @return
     * @throws DataAccessException
     */
    private double[] concatenate(double[] a, double[] b) throws DataAccessException {
        if (a == null && b != null) return b;
        if (b == null && a != null) return a;
        if (a == null && b == null) {
            throw new DataAccessException("can not concatenate two null data arrays ");
        }
        double[] r = new double[a.length + b.length];
        System.arraycopy(a, 0, r, 0, a.length);
        System.arraycopy(b, 0, r, a.length, b.length);
        return r;

    }

    /**
     * This is needed for unit test
     *
     * @param nBin: integer
     */
    public void setBinNumber(int nBin) {
        numBins=nBin;
        algorithm = FIXED_SIZE_ALGORITHM;
    }

    /**
     * This method multiplies an array by a number
     *
     * @param array
     * @param a
     * @return
     */
    private double[] multiply(double[] array, double a) {
        double[] newArray = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            newArray[i] = a * array[i];
        }
        return newArray;
    }

    public static void main(String args[]) throws IOException, DataAccessException {

        if (args.length > 0) {
            String path = getInputFilePath(args[0])[0];
            String inFileName = getInputFilePath(args[0])[1];
            if (args.length > 0) {
                try {
                    File inFile = new File(args[0]);
                    DataGroup dg = IpacTableReader.readIpacTable(inFile, null, false, "inputTable");

                    HistogramProcessor hp = new HistogramProcessor();
                    hp.columnExpression = "f_y";

                    if (columns == null) {
                        throw new DataAccessException(hp.columnExpression + " is not found in the input table");
                    }
                    double[] columnData = hp.getColumnData(dg);

                    DataGroup outDg = hp.createHistogramTable(columnData);
                    String outFileName = path + "output_" + inFileName;
                    File outFile = new File(outFileName);
                    IpacTableWriter.save(outFile, outDg);

                } catch (IpacTableException e) {
                    e.printStackTrace();
                }
            }
        }
    }


}
