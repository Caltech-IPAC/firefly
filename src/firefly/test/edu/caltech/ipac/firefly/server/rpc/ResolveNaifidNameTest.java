package edu.caltech.ipac.firefly.server.rpc;

import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.server.SrvParam;
import org.junit.Assert;
import org.junit.Test;
import java.util.HashMap;
import java.util.Map;


public class ResolveNaifidNameTest {

    @Test
    public void testDoCommand() throws Exception {
      //initialize ResolveNaifidName (the class under test):
      ResolveServerCommands.ResolveNaifidName naifidResolver= new ResolveServerCommands.ResolveNaifidName();

      //test values:
      String testNaifidName = "Jupiter";
      String testNaifidValue= "599";

      try {
          SrvParam sp = getServParams(testNaifidName.toLowerCase());
          String result = naifidResolver.doCommand(sp);

          Assert.assertTrue(result.contains(testNaifidName));
          Assert.assertTrue(result.contains(testNaifidValue));
      }catch (Exception e){
          throw new Exception("Could not resolve object: "+testNaifidName);
      }
    }


    SrvParam getServParams(String objName){
        //initialize & populate ServerParams with test value(s):
        SrvParam sp = new SrvParam(new HashMap<>());
        Map<String, String> testParamMap = new HashMap<>();
        testParamMap.put(ServerParams.OBJ_NAME,objName);
        sp.addParams(testParamMap);
        return sp;
    }
}
