/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/**
 * [April-23-19]
 * Author: L.Z.
 * This is an integration test since there is so many dependencies in LightCurveProcesor, thus, a unit test is not a right
 * choice.
 * Input data:
 *   There are two types of input data, one is a table and the other is a URL.  The table input has two kinds, one is none
 *   filtered table and the other is filtered table.   Both tables are  stored in firefly_test_data under the same package name.
 *      212027909.tbl
 *      212027909_filteredBJDLargeThan3270.tbl
 *
 *   The URL is taken from the original integration test written by Emmanuel:
 *      url = "http://web.ipac.caltech.edu/staff/ejoliet/demo/AllWISE-MEP-m82-2targets-10arsecs.tbl";
 *
 * Expected data:
 *   The expected data is stored in firefly_test_data under the same package name.
 *     periodogramResult_212027909.voTbl
 *     periodogramResult_212027909_filteredBJDLargeThan3270.voTbl
 *
 *    The expected data was created by running the api directly or through curl
 *      curl -F upload=@/work/azhang_9027/pubspace/04/70/08a53fa0f79695e0f726aeef96f5/input.tbl \
 *      -F alg=ls \
 *      -F x=xCol \
 *      -F y=yCol \
 *      -F peaks=50 \
 *       https://irsadev.ipac.caltech.edu:9028/cgi-bin/periodogram/nph-periodogram_api -o outputFile
 *
 *     The default peaks is 50.
 *
 * Purpose of the integration tests:
 *   The integration test is testing the module from beginning to the end.  The beginning is getting the input and
 *   the ending is return a result.  Here, we compare the calculated results with the expected results that were created
 *   by using the same input directly through the API.
 *
 *   If the test failed, it tells either the API that calculated periodogram is changed, or the codes that call this API
 *   is changed.
 *
 *  This class is extended from the base class LightCurveTestCommon which is shared by this class and the unit
 *  test class: IrsaLightCurveHandler.
 */
package edu.caltech.ipac.firefly.server.query.lc;

import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.util.FileLoader;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataObject;
import edu.caltech.ipac.table.DataType;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;;
import java.io.File;
import java.util.List;


/**
 * Test IRSA LC API
 */
public class LightCurveProcessorTest extends LightCurveTestCommon  {


    private LightCurveProcessor lcProcessor = new LightCurveProcessor();
    private String resultFileName = "periodogramResult_212027909.voTbl";
    private File resultFile= FileLoader.resolveFile(LightCurveProcessorTest.class, resultFileName);
    private String withFilterResultFileName = "periodogramResult_212027909_filteredBJDLargeThan3270.voTbl";
    private File withFilterResultFile= FileLoader.resolveFile(LightCurveProcessorTest.class, withFilterResultFileName);
    @BeforeClass
    public static void setUp() {

        setupServerContext(null);

    }
    @After
    /**
     * Release the memories
     */
    public void tearDown() {
        lcProcessor=null;
    }


    @Test
    public void testLightCurveProcessorFromUI() throws DataAccessException {

        testGetPeriodogram(periodogramAPIRequestForUI ,resultFile);
        testGetPeaks(periodogramAPIRequestForUI,resultFile);
    }

    @Test
    public void testLightCurveProcessorHasFilterFromUI() throws DataAccessException {

        testGetPeriodogram( periodogramAPIRequestHasFilterForUI, withFilterResultFile);
        testGetPeaks( periodogramAPIRequestHasFilterForUI, withFilterResultFile);
    }

    private void testGetPeriodogram(PeriodogramAPIRequest  req, File resultFile) throws DataAccessException {

        DataGroup expectedData = getExpectedData(resultFile, LightCurveHandler.RESULT_TABLES_IDX.PERIODOGRAM);

        req.setParam("table_name", LightCurveHandler.RESULT_TABLES_IDX.PERIODOGRAM.name());

        DataGroup calDataGroup = lcProcessor.fetchDataGroup(req);

        valiateDataGroup(expectedData, calDataGroup);
    }


    private void testGetPeaks(PeriodogramAPIRequest  req, File resultFile) throws DataAccessException {

        DataGroup expectedData = getExpectedData(resultFile, LightCurveHandler.RESULT_TABLES_IDX.PEAKS);

        req.setParam("table_name", LightCurveHandler.RESULT_TABLES_IDX.PEAKS.name());

        DataGroup calDataGroup = lcProcessor.fetchDataGroup(req);

        valiateDataGroup(expectedData, calDataGroup);

}


    private DataGroup getExpectedData(File file, LightCurveHandler.RESULT_TABLES_IDX result){
        return extractTblFrom(file, result);
    }

    private void validateDataTypes(DataType[] expColumns, DataType[] calColumns ){

        String message = " ,is not the same as expected";
        for (int i=0; i<expColumns.length; i++){
            Assert.assertTrue("The calculated: " + expColumns[i].getKeyName() + message,
                    expColumns[i].getKeyName().equalsIgnoreCase(calColumns[i].getKeyName()));
            Assert.assertTrue("The calculated: " + expColumns[i].getDataType() + message,
                    expColumns[i].getDataType().equals(calColumns[i].getDataType()));
            Assert.assertTrue("The calculated: " + expColumns[i].getUnits() + message,
                    expColumns[i].getUnits().equals(calColumns[i].getUnits()));

        }
    }
    private void validateDataObject( List<DataObject> expDGList,  List<DataObject> calDGList){
        for (int i=0; i<expDGList.size(); i++){
           Assert.assertArrayEquals("The calculated data is not the same as expected data",
                   expDGList.get(i).getData(), calDGList.get(i).getData());
        }
    }
    private void valiateDataGroup(DataGroup calDG, DataGroup expDG){
        DataType[] expColumns = expDG.getDataDefinitions();
        List<DataObject> expDGList = expDG.values();

        DataType[] calColumns = calDG.getDataDefinitions();
        List<DataObject> calDGList = calDG.values();
        Assert.assertTrue("The number of columns are not the same", expColumns.length == calColumns.length);

        validateDataTypes(expColumns, calColumns);
        validateDataObject(expDGList,calDGList);
    }
}

