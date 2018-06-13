package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.astro.IpacTableException;
import edu.caltech.ipac.astro.IpacTableReader;
import edu.caltech.ipac.astro.IpacTableWriter;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupPart;
import edu.caltech.ipac.util.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static edu.caltech.ipac.firefly.data.TableServerRequest.INCL_COLUMNS;


/**
 * Created by zhang on 10/16/15.
 * This class is a Histogram processor.  Its input is TableServerRequest where it contains the needed parameters to calculate
 * the histogram.
 * It works for two algorithm, fixed bin size algorithm and variable bin size algorithm.  If the bin size is available
 * from the input parameter, it will calculate the fixed bin size histogram.  If the bin size is not available, it will
 * calculate variable bin size histogram.
 *
 * By default it keeps empty bins if any.  This could be controlled by a parameter, reserveEmptyBins in the UI if needed.
 *
 * CHANGE HISTORY:
 *
 * 5/23/17
 * IRSA-371 Changed the showEmptyBin to true.  It was false previously.
 *
 */
@SearchProcessorImpl(id = "HistogramProcessor")
public class HistogramProcessor extends IpacTablePartProcessor {

    private static final String SEARCH_REQUEST = "searchRequest";
    private static DataType[] columns = new DataType[]{
        new DataType("numInBin", Long.class),
        new DataType("binMin", Double.class),
        new DataType("binMax", Double.class)
    };
    static {
        // set default precision to 14 significant digits
        columns[1].setFormat("%.14e");
        columns[2].setFormat("%.14e");
    }
    private final String FIXED_SIZE_ALGORITHM = "fixedSizeBins";
    private final String FIXED_BIN_SIZE_SELECTION="fixedBinSizeSelection";
    private final String BIN_SIZE = "binSize";
    // column expressions should be handled by search request
    private final String COLUMN = "columnName";
    private final String SORTED_COL_DATA = "sortedColData";
    private final String MIN = "min";
    private final String MAX = "max";
    // private final String ALGORITHM = "algorithm";
    private final String FALSEPOSTIVERATE = "falsePositiveRate";
    private final String PRESERVE_EMPTY_BIN="preserveEmptyBins";
    private String algorithm = null;// FIXED_SIZE_ALGORITHM;
    private double binWidth=0.0;
    private String binSelection=null;
    private String binSize;
    private String columnName;
    private boolean sortedColData = false;
    private double falsePostiveRate = 0.05;

    //change to protected so that they can be set by unit test class
    protected boolean showEmptyBin= true;
    protected int numBins=0;
    protected double min = Double.NaN;
    protected double max = Double.NaN;


    private static String[] getInputFilePath(String inputFileName) {
        String[] dirs = inputFileName.split("/");
        String name = dirs[dirs.length - 1];
        String path = inputFileName.substring(0, inputFileName.length() - name.length());
        return new String[]{path, name};
    }

    public static void main(String args[]) throws IOException, DataAccessException {

        if (args.length > 0) {
            String path = getInputFilePath(args[0])[0];
            String inFileName = getInputFilePath(args[0])[1];
            if (args.length > 0) {
                try {
                    File inFile = new File(args[0]);
                    DataGroup dg = IpacTableReader.readIpacTable(inFile, null, "inputTable");

                    HistogramProcessor hp = new HistogramProcessor();
                    hp.columnName = "f_y";

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

    protected File loadDataFile(TableServerRequest request) throws IOException, DataAccessException {

        String searchRequestJson = request.getParam(SEARCH_REQUEST);
        if (searchRequestJson == null) {
            throw new DataAccessException("Unable to get histogram: " + SEARCH_REQUEST + " is missing");
        }

        TableServerRequest sReq = QueryUtil.convertToServerRequest(searchRequestJson);

        if (sReq.getRequestId() == null) {
            throw new DataAccessException("Unable to get histogram: " + SEARCH_REQUEST + " must contain " + ServerParams.ID);
        }
        getParameters(request);

        // get the relevant data
        sReq.setPageSize(Integer.MAX_VALUE);

        if (!sortedColData) {
            sReq.setSortInfo(null);
            sReq.removeParam(INCL_COLUMNS);
        }

        DataGroupPart sourceData = new SearchManager().getDataGroup(sReq);
        if (sourceData == null) {
            throw new DataAccessException("Unable to get source data");
        } else if (sourceData.getErrorMsg() != null) {
            throw new DataAccessException(sourceData.getErrorMsg());
        }
        DataGroup sourceDataGroup = sourceData.getData();
        double[] columnData = getColumnData(sourceDataGroup);
        DataGroup histogramDataGroup = createHistogramTable(columnData);
        histogramDataGroup.addAttribute("searchRequest", sReq.toString());

        File histogramFile = createFile(request);
        IpacTableWriter.save(histogramFile, histogramDataGroup);
        return histogramFile;
    }

    private void getParameters(TableServerRequest tableServerRequest) {
        //get all the required parameters
        List<Param> params = tableServerRequest.getParams();
        for (Param p: params.toArray(new Param[params.size()])){
            String name = p.getName();

            String value = p.getValue();
            if (name==null || value==null ) continue;
            if (name.equalsIgnoreCase(COLUMN)) {
                columnName = value;
            } else if (name.equalsIgnoreCase(MIN)) {
                min =  Double.parseDouble(value);

            } else if (name.equalsIgnoreCase(MAX)) {
                max =  Double.parseDouble(value);
            } else if (name.equalsIgnoreCase(FIXED_BIN_SIZE_SELECTION)) {
                binSelection = value;
               // numBins =  Integer.parseInt(value);
              //  if (numBins>0) algorithm = FIXED_SIZE_ALGORITHM;
            } else if (name.equalsIgnoreCase(BIN_SIZE)) {
                binSize  =  value;
            } else if (name.equalsIgnoreCase(FALSEPOSTIVERATE)) {
                falsePostiveRate =  Double.parseDouble(value);
            } else if (name.equalsIgnoreCase(SORTED_COL_DATA)) {
                // table request returns a single column with the sorted data
                sortedColData = Boolean.parseBoolean(value);
            }
            /*05/23/17
              The UI does not have this parameter specified and passed.
              The default now changed to show all the bins.  The code
              leave as is in case UI may need this field in the future.
            */
            else if (name.equalsIgnoreCase(PRESERVE_EMPTY_BIN) ){
                showEmptyBin= Boolean.parseBoolean(value);
            }
        }

        if (binSelection!=null ){
            if (binSelection.equalsIgnoreCase("numBins")) {

               numBins = Integer.parseInt(binSize);
           }
            else if (binSelection.equalsIgnoreCase("binWidth") ) {
            binWidth = Double.parseDouble(binSize);

           }
           if (numBins>0 || binWidth>0.0 ){
               algorithm = FIXED_SIZE_ALGORITHM;
           }
        }
    }

    /**
     * This method is changed to public to be able to run the test case in the test tree
     * This method adds the three data arrays and the DataTypes into a DataGroup (IpacTable).
     *
     * @param columnData - an array of doubles
     * @return DataGroup
     */
    public DataGroup createHistogramTable(double[] columnData) throws DataAccessException {

        DataType[] tblcolumns = columns;
        DataGroup HistogramTable;

        if (columnData.length > 0) {
            if (!sortedColData) {
                //sort the data in ascending order, thus, index 0 has the minimum and the last index has the maximum
                Arrays.sort(columnData);
            }

            //calculate three arrays, numInBin, binMix and binMax
            Object[] obj;
            if (algorithm != null && algorithm.equalsIgnoreCase(FIXED_SIZE_ALGORITHM)) {
                obj = calculateFixedBinSizeDataArray(columnData);
            } else {
                obj = calculateVariableBinSizeDataArray(columnData);

            }

            long[] numPointsInBin = (long[]) obj[0];
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
     * @param columnData a sorted array of doubles
     * @return an array of 3 arrays: numInBin[], min[], max[]
     */
    private Object[] calculateFixedBinSizeDataArray(double[] columnData) {

        if (Double.isNaN(min)) {
            min = columnData[0];
        }
        if (Double.isNaN(max)) {
            max = columnData[columnData.length - 1];
        }


        double binSize =numBins>0? (max-min)/numBins:binWidth;

        int nBins = numBins>0? numBins : (int) Math.ceil((max-min)/binSize);

        long[] numPointsInBin = new long[nBins];
        double[] binMin = new double[nBins];

        double[] binMax = new double[nBins];

        int iBin;
        for (int i = 0; i < columnData.length; i++) {
            if (columnData[i] >= min && columnData[i] < max) {
                iBin = (int) ((columnData[i] - min) / binSize);
                numPointsInBin[iBin]++;

            }
            else if (columnData[i]  == max) { //put the last data in the last bin
                numPointsInBin[nBins - 1]++;
            }

        }
        for (int i=0; i<nBins; i++){
            binMin[i]=min+i*binSize;
            binMax[i]=binMin[i]+binSize;
        }



       if (showEmptyBin){
           return new Object[]{numPointsInBin, binMin, binMax};
       }
       else {
          return filterEmptyBins(numPointsInBin, binMin, binMax);

       }
    }

    private long[]  getSelection(long[] inArray, ArrayList<Integer> list ){

        long[] outArray=new long[list.size()];
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
        for (Integer el : list) {
            if (idx == el) {
                return true;
            }
        }
        return false;
    }

    private double[] getColumnData(DataGroup dg) throws DataAccessException {
        List<DataObject> objList = dg.values();
        int nRow = objList.size();
        DataType[] dataTypes=dg.getDataDefinitions();
        DataObjectUtil.DoubleValueGetter dGetter = new DataObjectUtil.DoubleValueGetter(dataTypes, columnName);
        if (!dGetter.isValid()) {
            throw new DataAccessException("Invalid column: "+ columnName);
        }

        double[] data = new double[nRow];
        for (int i = 0; i < nRow; i++) {
            DataObject row = objList.get(i);
            data[i] = dGetter.getValue(row);
        }
        return Arrays.stream(data).filter(d -> !Double.isNaN(d)).toArray();
    }

    /**
     * This method calculate the variable bins based on the paper:
     * http://iopscience.iop.org/article/10.1088/0004-637X/764/2/167/pdf;jsessionid=22827FAAA086B2A127E88C517E0E8DD3.c1.iopscience.cld.iop.org
     * http://arxiv.org/pdf/1304.2818.pdf
     * and the python implementation: https://github.com/fedhere/fedsastroutils/blob/master/bayesianblocks_fbb.py
     * There are a few implementations. Each of them is a little different.  I based the two above and
     * modified it to take the special cases where there is no fitness value found.
     *
     * @return an array of 3 arrays: numInBin[], min[], max[]
     */


    private Object[] calculateVariableBinSizeDataArray(double[] columnData) throws DataAccessException {

        //get the variable bins
        double[] bins = getBins(columnData);


        int nBin = bins.length;
        long[] numPointsInBin = new long[nBin];
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
            System.arraycopy(bins, 0, binMin, 1, nBin-1);

            //assign the right edge to the binMax
            System.arraycopy(bins, 0, binMax, 0, nBin);

        }

        if (showEmptyBin){
            return new Object[]{numPointsInBin, binMin, binMax};
        }
        else {
            return filterEmptyBins(numPointsInBin, binMin, binMax);

        }
    }

     private  Object[] filterEmptyBins(long[] numPointsInBin, double[] binMin, double[] binMax){
         //filter out the  entries which has the empty bins
         ArrayList<Integer> idx = new ArrayList<>();
         for (int i = 0; i < numPointsInBin.length; i++) {
             if (numPointsInBin[i] == 0) continue;
             idx.add(i);
         }
         return new Object[]{getSelection(numPointsInBin, idx), getSelection(binMin, idx), getSelection(binMax, idx)};
     }

    /**
     * Change to public in order to run unit test
     * This method calculates the variable bins
     *
     * @param columnData - input, double array
     * @return an array of doubles
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
        // Recover change points by iteratively peeling off the last block
        //-----------------------------------------------------------------
        ArrayList<Integer> changePointList = new ArrayList<>();
        int ind = n;

        for (int icp = n - 1; icp > -1; icp--) {
            if (ind == -1) continue;
            changePointList.add(ind);
            if (ind == 0) break;
            ind = last[ind - 1];
        }


        int[] newChangePoint = reverseArray(changePointList);

        ArrayList<Double> sData = new ArrayList<>();
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
            bins[i] = sData.get(i);
        }
        return bins;
    }

    /**
     * return -1 if there is no valid data values in the array
     *
     * @param inArray input array
     * @return index of the max value or -1
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
     * @param data a sorted array of doubles
     * @return edges
     */
    private double[] getEdges(double[] data) throws DataAccessException {

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
        return concatenate(concatenate(a1, multiply), a4);
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
     * @param inArray input array
     * @return reversed array
     */
    private double[] reverseArray(double[] inArray) {
        double[] outArray = new double[inArray.length];
        for (int i = 0; i < inArray.length; i++) {
            outArray[i] = inArray[inArray.length - 1 - i];
        }
        return outArray;
    }

    /**
     * This method reverse the ArrayList and then return a int array
     *
     * @param inArrayList  input
     * @return reversed list as an array
     */
    private int[] reverseArray(ArrayList<Integer> inArrayList) {
        int len = inArrayList.size();
        int[] outArray = new int[len];
        for (int i = 0; i < len; i++) {
            outArray[i] = inArrayList.get(len - 1 - i);
        }
        return outArray;
    }

    /**
     * This method calculates the cumulative sum
     *
     * @param nnVec nnVec
     * @param k k
     * @return cumulative sum
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
     * @param a first array
     * @param b second array
     * @return concatenated array
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
     * @param array an array
     * @param a multiplier
     * @return result array
     */
    private double[] multiply(double[] array, double a) {
        double[] newArray = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            newArray[i] = a * array[i];
        }
        return newArray;
    }


}
