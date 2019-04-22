/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.messaging;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Date: 2019-03-15
 *
 * @author loi
 * @version $Id: $
 */
public class JsonHelperTest {

    @Test
    public void testSimpleCases(){

        assertEquals("Simple string message", "\"hello world\"", new JsonHelper("hello world").toJson());

        // setting a string at a particular index
        // this will create a sparse list with the string value at the given index
        JsonHelper jh = new JsonHelper().setValue("hello world", "2");
        assertEquals("[null,null,\"hello world\"]", jh.toJson());

        // using integer key in an object
        jh = new JsonHelper(new HashMap<>())                    // making root a Map.  Any path given to a map is treated as a string key.
                .setValue("hello world", "2");
        assertEquals("{\"2\":\"hello world\"}", jh.toJson());

        try {
            // setting a string at index 2 of b when b contains a string.
            jh = new JsonHelper().setValue("hello world", "b");
            jh.setValue("exception", "b", "2");
            fail("Expect to see IndexOutOfBoundsException");
        } catch (Exception e) {}


        // using alpha character key in array, should fail
        try {
            new JsonHelper(new ArrayList<>())
                    .setValue("hello world", "a");
            fail("Expect to see IndexOutOfBoundsException");
        } catch (Exception e) {}
    }

    @Test
    public void testComplexObject(){

        /*  tests below show multiple ways of building this json object
        {
            a: [ null, "index_1", { b: "hello world" } ],
            b: "b_branch"
        }
        */
        String resultingJson = "{\"a\":[null,\"index_1\",{\"b\":\"hello world\"}],\"b\":\"b_branch\"}";

        // building up a complex object using path setters

        // setting an object with key/val b:'hello world' into 'a' at index 2.
        JsonHelper c1 = new JsonHelper().setValue("hello world", "a", "2", "b");
        assertEquals("{\"a\":[null,null,{\"b\":\"hello world\"}]}", c1.toJson());

        // setting a string 'index_1' into 'a' at index 1.
        c1.setValue("index_1", "a", "1");
        assertEquals("{\"a\":[null,\"index_1\",{\"b\":\"hello world\"}]}", c1.toJson());

        // setting a string 'b_branch' into 'b'.
        c1.setValue("b_branch", "b");
        assertEquals(resultingJson, c1.toJson());

        // doing the same thing as above using complex objects.
        // the resulting json should still be the same as the one above
        HashMap<String, String> aAtIdx2 = new HashMap<>();
        aAtIdx2.put("b", "hello world");
        JsonHelper c2 = new JsonHelper()
                .setValue(Arrays.asList(null, "index_1", aAtIdx2), "a")
                .setValue("b_branch", "b");
        assertEquals(resultingJson, c2.toJson());
    }

}
