/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.util;

import edu.caltech.ipac.firefly.ConfigTest;
import org.junit.Assert;
import org.junit.Test;

import java.util.regex.Pattern;

import static edu.caltech.ipac.util.StringUtils.*;

/**
 * Date: 8/5/22
 *
 * @author loi
 * @version : $
 */
public class StringUtilsTest extends ConfigTest {


    @Test
    public void groupMatchFunc() {

        // extract values from a string
        String regex = ".*from (\\w+) where (\\w+=\\w+).*";

        String[] matches = groupMatch(regex, "select * from table1 where a=123");
        Assert.assertNotNull(matches);
        Assert.assertEquals("table1", matches[0]);
        Assert.assertEquals("a=123", matches[1]);

        // test no-match case
        matches = groupMatch(regex, "no variable defined");
        Assert.assertNull(matches);

        // CASE_INSENSITIVE test using pattern
        matches = groupMatch(Pattern.compile(regex, Pattern.CASE_INSENSITIVE), "SELECT * FROM table1 WHERE a=123\"");
        Assert.assertNotNull(matches);
        Assert.assertEquals("table1", matches[0]);
        Assert.assertEquals("a=123", matches[1]);
    }

    @Test
    public void groupFindFunc() {

        // find all key=value pairs in a string
        String regex = "\\w+=\\w+";

        String[] results = groupFind(regex, "find abc=123 in a string");
        Assert.assertNotNull(results);
        Assert.assertEquals("abc=123", results[0]);

        // test no-match case
        results = groupFind(regex, "no key value pair in string");
        Assert.assertNull(results);

        // multiple occurrences using Pattern
        results = groupFind(Pattern.compile(regex), "abc=123 and another xyz=999");
        Assert.assertNotNull(results);
        Assert.assertEquals("abc=123", results[0]);
        Assert.assertEquals("xyz=999", results[1]);
    }

    // @Kartikeya Puri, please add a few more test here..
    // i.e getInt, getLong, getDouble, getFloat from edu.caltech.ipac.util.StringUtils
    @Test
    public void getIntFunc() {
        String num = "273";

        int result = getInt(num);

        Assert.assertNotNull(result);
        Assert.assertEquals(273, result);

        result = getInt("not a number");
        Assert.assertNotNull(result);
        Assert.assertEquals(Integer.MIN_VALUE, result);

        num = "21474836478"; //max int = 2147483647, so this should cause NumberFormatException and return Integer.MIN_VALUE
        result = getInt(num);

        Assert.assertNotNull(result);
        System.out.println(result);
        Assert.assertEquals(Integer.MIN_VALUE, result);

        //test with sending null, floating point numbers etc. should all return Integer.MIN_VALUE
    }

    @Test
    public void getLongFunc() {
        String num = "273";

        long result = getLong(num);

        Assert.assertNotNull(result);
        Assert.assertEquals(273, result);

        result = getLong("not a number");
        Assert.assertNotNull(result);
        Assert.assertEquals(Long.MIN_VALUE, result);

        result = getLong("2958583");
        Assert.assertNotNull(result);
        Assert.assertEquals(2958583, result);

        result = getLong(null);
        Assert.assertNotNull(result);
        Assert.assertEquals(Long.MIN_VALUE, result);

        result = getLong("6.54"); //float
        Assert.assertNotNull(result);
        Assert.assertEquals(Long.MIN_VALUE, result);

        System.out.println(result);
    }

    @Test
    public void getDoubleFunc() {
        String num = "273";

        double result = getDouble(num);

        Assert.assertNotNull(result);
        //what's delta in floating point numbers? 
        Assert.assertEquals(273.0, result, 0);

        result = getDouble("not a number");
        Assert.assertNotNull(result);
        Assert.assertEquals(Double.NaN, result, 0);

        result = getDouble("273");
        Assert.assertNotNull(result);
        Assert.assertEquals(273.0, result, 0);

        System.out.println(result);
    }
}
