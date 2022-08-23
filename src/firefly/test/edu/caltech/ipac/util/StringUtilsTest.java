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

    @Test
    public void getIntFunc() {
        String num = "273";

        int result = getInt(num);
        Assert.assertNotNull(result);
        Assert.assertEquals(273, result);
        Assert.assertNotEquals(273.0, result);

        result = getInt("not a number"); //not a valid number, should return Integer.MIN_VALUE
        Assert.assertNotNull(result);
        Assert.assertEquals(Integer.MIN_VALUE, result);

        //max int = 2147483647, so this should cause NumberFormatException and return Integer.MIN_VALUE
        result = getInt("21474836478");
        Assert.assertNotNull(result);
        Assert.assertEquals(Integer.MIN_VALUE, result);

        result = getInt("-3147483648"); //min int is = -2,147,483,648, so this should also return Integer.MIN_VALUE
        Assert.assertNotNull(result);
        Assert.assertEquals(Integer.MIN_VALUE, result);

        result = getInt(null);
        Assert.assertNotNull(result);
        Assert.assertEquals(Integer.MIN_VALUE, result);

        result = getInt("21.56"); //floating point -> NumberFormatException, should return Integer.MIN_VALUE
        Assert.assertNotNull(result);
        Assert.assertEquals(Integer.MIN_VALUE, result);
    }

    @Test
    public void getLongFunc() {
        String num = "273";

        long result = getLong(num);
        Assert.assertNotNull(result);
        Assert.assertEquals(273, result);

        result = getLong("not a number"); //non-number, so this should return Long.MIN_VALUE
        Assert.assertNotNull(result);
        Assert.assertEquals(Long.MIN_VALUE, result);

        result = getLong("2958583");
        Assert.assertNotNull(result);
        Assert.assertEquals(2958583, result);

        result = getLong(null);
        Assert.assertNotNull(result);
        Assert.assertEquals(Long.MIN_VALUE, result);

        result = getLong("6.54"); //floating point number, so this should return Long.MIN_VALUE
        Assert.assertNotNull(result);
        Assert.assertEquals(Long.MIN_VALUE, result);

        //Long.MAX_VALUE = 9223372036854775807
        result = getLong("9223372036854775807");
        Assert.assertNotNull(result);
        Assert.assertEquals(Long.MAX_VALUE, result);

        //This is > Long.MAX_VALUE so this should return Long.MIN_VALUE (NumberFormatException)
        result = getLong("9223372036854775807999");
        Assert.assertNotNull(result);
        Assert.assertEquals(Long.MIN_VALUE, result);
    }

    @Test
    public void getDoubleFunc() {
        String num = "273";

        double result = getDouble(num);
        Assert.assertNotNull(result);
        Assert.assertEquals(273.0, result, 0);

        result = getDouble("not a number"); //non-number, so getDouble will return Double.NaN
        Assert.assertNotNull(result);
        Assert.assertEquals(Double.NaN, result, 0);

        result = getDouble("273.1425");
        Assert.assertNotNull(result);
        Assert.assertEquals(273.0, result, 0.5); //testing delta, this should return true aas 273.1425 is within 0.5 of 273
        Assert.assertNotEquals(272.0, result, 1); //outside of delta range

        result = getDouble("1.7976931348623157E308"); //Double.MAX_VALUE
        Assert.assertNotNull(result);
        Assert.assertEquals(1.7976931348623157E308, result, 0);

        result = getDouble("4.9E-324"); //Double.MIN_VALUE
        Assert.assertNotNull(result);
        Assert.assertEquals(4.9E-324, result, 0);

        result = getDouble(null); //null, should return Double.NaN
        Assert.assertNotNull(result);
        Assert.assertEquals(Double.NaN, result, 0);
    }

    @Test
    public void getFloatFunc() {
        String num = "273";

        double result = getFloat(num);
        Assert.assertNotNull(result);
        Assert.assertEquals(273.0, result, 0);

        result = getFloat("not a number"); //non-number, so getFloat will return Float.NaN
        Assert.assertNotNull(result);
        Assert.assertEquals(Float.NaN, result, 0);

        result = getFloat("273.1425");
        Assert.assertNotNull(result);
        Assert.assertEquals(273.0, result, 0.5); //testing delta, this should return true aas 273.1425 is within 0.5 of 273

        result = getFloat("1.4E-45"); //Float.MIN_VALUE
        Assert.assertNotNull(result);
        Assert.assertEquals(1.401298464324817E-45, result, 0);

        result = getFloat(null); //null, should return Double.NaN
        Assert.assertNotNull(result);
        Assert.assertEquals(Float.NaN, result, 0);
    }
}
