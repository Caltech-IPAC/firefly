package edu.caltech.ipac.firefly.server.query;
import edu.caltech.ipac.astro.IpacTableException;
import edu.caltech.ipac.firefly.util.FileLoader;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.*;
import nom.tam.fits.FitsException;
import org.junit.*;
import org.junit.Assert;

import java.io.IOException;
import java.util.List;

/**
 * Created by zhang on 10/29/15.
 *  10/19/16
 *  DM-8028
 *    Use teh UnitTestUtility to load file
 */
public class StatisticsProcessorTest {

    private static StatisticsProcessor sp;
    private static DataGroup inDg;
    private DataGroup outDg;
    private String fileName = "ipacTableTestFile.tbl";

    @Before
    public void setup()  {

         sp = new StatisticsProcessor();
         inDg = FileLoader.loadIpacTable(StatisticsProcessor.class, fileName );

    }

    @After
    /**
     * Release the memories
     */
    public void tearDown() {
        sp=null;
        inDg=null;
        outDg=null;
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

        outDg = sp.createTableStatistic(inDg);
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
            obj = objList.get(i).getDataElement(inColumns[4]);
            max[i]= ((Double) obj).doubleValue();
            obj = objList.get(i).getDataElement(inColumns[5]);
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

        Assert.assertArrayEquals(expectedMin, min, 10e-5);
        Assert.assertArrayEquals(expectedMax, max, 10e-5);
        Assert.assertArrayEquals(expectedNumPoints, numPoints);


    }
/*
    public static void main(String args[]) throws IpacTableException, IOException, DataAccessException, FitsException, ClassNotFoundException {
        String fileName = "ipacTableTestFile.tbl";
        StatisticsProcessorTest myTest = new StatisticsProcessorTest();
        sp = new StatisticsProcessor();
        inDg = FileLoader.loadIpacTable(StatisticsProcessor.class, fileName );
        myTest.testCreateStatisticTable();
        myTest.testStatisticDataArray();
    }*/

}
