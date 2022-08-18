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

        //System.out.println(result);
    }
}
