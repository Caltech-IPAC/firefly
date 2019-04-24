/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/**
 * [April-23-19]
 *
 * Author: L. Z.
 * This is the unit test for IrsaLightCurveHandler.  It tests the TableServerRequest and the HttpServiceInput created
 * by the PeridogramRequest is correct.
 *
 * This class is extended from the base class LightCurveTestCommon that is shared with LightCurveProcessorTest.  The unit
 * test is done through verifying the known parameters that should not be changed after they are passed to the IrsaLightCurveHandler.
 *
 * When the PeriodogramRequest passes to the IrsaLightCurveHandler, it creates two important object, one is the TableRequest
 * and the other is HttpInput.  The TableRequest is used to save the input to a table that is passed to the Periodogram API
 * to calculate the Periodogram.  The HttpInput is the input parameters to post on the Periodogram API.
 *
 * This unit tests test two public methods, getHttpInput and getTableServerRequest.  It tests three kind inputs, a table,
 * a filter table and a URL.
 *
 *
 */
package edu.caltech.ipac.firefly.server.query.lc;

import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.network.HttpServiceInput;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import java.util.*;


/**
 * Test IRSA LC API
 */
public class IrsaLightCurveHandlerTest extends LightCurveTestCommon {
    private IrsaLightCurveHandler irsaLc = new IrsaLightCurveHandler();

    @BeforeClass
    public static void setUp() {
        setupServerContext(null);
    }


    @After
    /**
     * Release the memories
     */
    public void tearDown() {
        irsaLc=null;
    }


    @Test
    public void testGetTableServerRequest(){
        testGetTableServerRequestFromUI();
        testGetTableServerRequestGator();
        tesGetTableServerRequestWithAFilterInTable();
    }

    @Test
    public void testGetHttpInput(){
        testHttpInputFromUI();
        testHttpInputFromGator();
    }



    /**
     * This is to test the PeriodogramRequest has a table filter.
     */
    private void tesGetTableServerRequestWithAFilterInTable(){
        HashMap  expectedParams= getExpectedParmsForUI();
        TableServerRequest request = irsaLc.getTableServerRequest(periodogramAPIRequestHasFilterForUI);
        ArrayList<Param> calParamList = (ArrayList)request.getParams();

        //validate parameters
        validateTableServerRequestParams(expectedParams, calParamList);
        Assert.assertTrue("The filters in Calculated Periogogram is not the same as what is in expected data",
                request.getFilters().get(0).equalsIgnoreCase( "BJD>3270"));
    }

    /**
     * This is to test the input from API (UI)
     * @return
     */
    private void testGetTableServerRequestFromUI(){
        HashMap  expectedParams= getExpectedParmsForUI();

        TableServerRequest request = irsaLc.getTableServerRequest(periodogramAPIRequestForUI);
        ArrayList<Param> calParamList = (ArrayList)request.getParams();

        //validate parameters
        validateTableServerRequestParams(expectedParams, calParamList);

    }

    /**
     * This is to test the input from Gator (URL).
     */
    private void testGetTableServerRequestGator(){

        HashMap expectedParams = new HashMap(reqParamsCommon);
        expectedParams.put("uploadFileName", uploadFileInURL);
        expectedParams.put("sortInfo",  "ASC,\"mjd\"");
        expectedParams.put("source", url);

        TableServerRequest request = irsaLc.getTableServerRequest(periodogramAPIRequestForURL);
        ArrayList<Param> calParamList = (ArrayList)request.getParams();

        //validate parameters
        validateTableServerRequestParams(expectedParams, calParamList);

    }


    private void testHttpInputFromUI(){

        HttpServiceInput httpInput = irsaLc.getHttpInput(periodogramAPIRequestForUI);
        //validate parameters
        Map<String, String> calHttpInputParams = httpInput.getParams();
        validateParams(calHttpInputParams, expectedHttpInputParamsForUI);

        //validate file, note that the file is the same as the input since the file is not saved yet.
        Assert.assertTrue(httpInput.getFiles().toString().contains(periodogramAPIRequestForUI.getLcSource()));

    }


    private void testHttpInputFromGator()  {

        HttpServiceInput httpInputForPeriodogram = irsaLc.getHttpInput(periodogramAPIRequestForURL);
        //validate parameters

        Map<String, String> calHttpInputParams = httpInputForPeriodogram.getParams();
        validateParams(calHttpInputParams, expectedHttpInputParamsForURL);

        //validate file, note that the file is the same as the input since the file is not saved yet.
        Assert.assertTrue(httpInputForPeriodogram.getFiles().toString().contains(periodogramAPIRequestForURL.getLcSource()));
    }
    private HashMap getExpectedParmsForUI(){
        HashMap expectedParams = new HashMap(reqParamsCommon);
        expectedParams.put("uploadFileName", uploadFileName);
        expectedParams.put("sortInfo",  "ASC,\"BJD\"");
        expectedParams.put("source", inputTbl.getAbsoluteFile().toString());
        return  expectedParams;
    }

    private void validateParams(Map<String, String> calParams,  Map<String, String>  expectParams){
        String[] keys =  expectParams.keySet().toArray(new String[0]);
        for (int i=0; i<keys.length; i++){
            if (calParams.containsKey(keys[i]) && expectParams.containsKey(keys[i]) ){
                Assert.assertTrue("The paramemter: " + calParams.get(keys[i]) + " , is not the same as" +
                        "expected", calParams.get(keys[i]).equalsIgnoreCase(expectParams.get(keys[i])));

            }
        }
    }
    private void validateTableServerRequestParams( HashMap expectedParams, ArrayList<Param> calParamList){

        Map calParams = new HashMap<>();
        for (int i=0; i<calParamList.size(); i++){
            calParams.put(calParamList.get(i).getName(), calParamList.get(i).getValue());
        }

        //validate parameters
        validateParams(calParams, expectedParams);
    }


}

