package edu.caltech.ipac.firefly.server.query;
import com.google.gwt.thirdparty.guava.common.annotations.VisibleForTesting;
import edu.caltech.ipac.astro.IpacTableException;
import edu.caltech.ipac.astro.IpacTableReader;
import edu.caltech.ipac.firefly.server.query.StatisticsProcessor;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.*;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created by zhang on 10/29/15.
 */
public class StatisticsProcessorTest {
    private static String filename = "./ipacTableTestFile.tbl";
    private StatisticsProcessor sp;
    private DataGroup inDg;
    private DataGroup outDg;
    File inFile;
    @BeforeClass
    public void setup() throws IpacTableException, IOException, DataAccessException{

        sp = new StatisticsProcessor();

        inFile = new File(filename);
        inDg = IpacTableReader.readIpacTable(inFile, null, false, "inputTable" );


    }

    @Test
    public void testCreateStatisticTable(){

        try {
            outDg = sp.createTableStatistic(inDg);
            Assert.assertNotNull(outDg);
        }
        catch (Exception ex){
            ex.printStackTrace();
        }

    }

    @Test
    public void testStatisticDataArray(){
        List<DataObject> objList= outDg.values();
        DataType[] inColumns = outDg.getDataDefinitions();

        //Validate the values for each column
        int nRow = objList.size();
        String[] colNames = new String[nRow];
        String[] unitNames = new String[nRow];
        double[] min=new double[nRow];
        double[] max = new double[nRow];
        int[] numPoints = new int[nRow];
        Object obj;
        for (int i =0; i<nRow; i++){
            obj = objList.get(i).getDataElement(inColumns[0]);
            colNames[i]=(String) obj;
            obj = objList.get(i).getDataElement(inColumns[2]);
            unitNames[i]=(String) obj;
            obj = objList.get(i).getDataElement(inColumns[3]);
            min[i]=((Double) obj).doubleValue();
            obj = objList.get(i).getDataElement(inColumns[2]);
            max[i]= ((Double) obj).doubleValue();
            obj = objList.get(i).getDataElement(inColumns[2]);
            numPoints[i]=((Integer) obj).intValue();

        }

        //check to see if all the out DataGroup has the right columns
        Assert.assertEquals(inColumns.length, 6);
        String[] expectedColumnNames={"id", "f_x", "f_y", "i_x", "i_y", "peakValue"};
        String[] expectedUnits={ "", "pixels", "pixels", "pixels", "pixels", "dn"};
        double[] expectedMin ={1, 3, 4, 3, 4, 10.1368};
        double[] expectedMax ={19588, 2044, 1485,  2044, 1485, 9833.3799};
        int[] expectedNumPoints ={1152, 1152,1152, 1152, 1152, 1152};

        Assert.assertArrayEquals(expectedColumnNames, colNames);
        Assert.assertArrayEquals(expectedUnits, unitNames );

        Assert.assertArrayEquals(expectedMin, min, 10e-10);
        Assert.assertArrayEquals(expectedMax, max, 10e-10);
        Assert.assertArrayEquals(expectedNumPoints, numPoints);


    }
    public static void main(String args[]) throws IpacTableException, IOException, DataAccessException {
        StatisticsProcessorTest myTest = new StatisticsProcessorTest();
        myTest.setup();
        myTest.testCreateStatisticTable();
        myTest.testStatisticDataArray();



    }
}
