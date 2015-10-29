package edu.caltech.ipac.firefly.server.query;
import com.google.gwt.thirdparty.guava.common.annotations.VisibleForTesting;
import org.junit.After;
import org.junit.Test;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
/**
 * Created by zhang on 10/29/15.
 */
public class StatisticsProcessorTest {
    private static String filename = "./ipacTable.tbl";
    private StatisticsProcessor sp;
    private DataGroup outDg;
    @BeforeClass
    public void setup(){
        File inFile = new File(filename);
        sp = new StatisticsProcessor();

    }
    pubic void testCreateStatisticTable(){


        outDg = sp.createTableStatistic(inFile);
        Assert.assertNotNul(outDg);

    }

    @Test
    public testStatisticDataArray(){
        List<DataObject> dgjList= outDg.values();
        DataType[] inColumns = outDg.getDataDefinitions();

        //check to see if all the out DataGroup has the right column names
        Assert.assetEqual(inColumns[0].getKeyName(), "name");


        //Validate the values for each column
        //TOODO 
    }
}
