/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.table;

import edu.caltech.ipac.firefly.ConfigTest;
import org.junit.BeforeClass;
import org.junit.Test;
import java.util.Arrays;

import static edu.caltech.ipac.table.TableUtil.strToStringAry;
import static org.junit.Assert.*;

/**
 * General tests for table related functions
 */
public class TableUtilTest extends ConfigTest {

    @BeforeClass
    public static void setUp() {
        // needed by test testGetSelectedData because it's dealing with code running in a server's context, ie  SearchProcessor, RequestOwner, etc.
        setupServerContext(null);
    }

    @Test
    public void validCases() {
        // no arraysize specified, so should return a single string
        Object out = strToStringAry("Hello", new int[]{}, false);
        assertEquals("Hello", out);

        // fixed length
        out = strToStringAry("ABC", new int[]{3}, false);
        assertEquals("ABC", out);

        // arraysize[0]=5; content has trailing spaces that should be trimmed when trimPad=true
        out = strToStringAry("ABC  ", new int[]{5}, true);
        assertEquals("ABC", out);

        // arraysize="*" => one string of any length, so "ANY LENGTH OK" is valid
        out = strToStringAry("ANY LENGTH OK", new int[]{-1}, false);
        assertEquals("ANY LENGTH OK", out);

        // arraysize="3x2" => two strings, each length 3: ["ABC","DEF"]
        out = strToStringAry("ABCDEF", new int[]{3,2}, false);
        Object[] arr = (Object[]) out;
        assertEquals(2, arr.length);
        assertArrayEquals(new String[]{"ABC","DEF"}, Arrays.stream(arr).toArray(Object[]::new));

        // arraysize="2x2x3" => 2 groups, each with 3 strings of length 2
        String data = "AB" + "CD" + "EF" + "GH" + "IJ" + "KL"; // 6 strings * 2 chars
        out = strToStringAry(data, new int[]{2,2,3}, false);

        Object[] g = (Object[]) out;      // size 2
        assertEquals(2, g.length);
        Object[] g0 = (Object[]) g[0];    // size 3
        Object[] g1 = (Object[]) g[1];    // size 3

        assertArrayEquals(new String[]{"AB","CD","EF"}, Arrays.stream(g0).toArray(Object[]::new));
        assertArrayEquals(new String[]{"GH","IJ","KL"}, Arrays.stream(g1).toArray(Object[]::new));
    }

    @Test
    public void lastDimVarLength() {
        // arraysize="3x2x*" => [3,2,-1]; total strings = len/3 = 4; fixedProd (excluding last) = 2
        // => last dim resolves to 2 → [[AAA, BBB], [CCC, DDD]]
        String data = "AAA" + "BBB" + "CCC" + "DDD";
        Object out = strToStringAry(data, new int[]{3,2,-1}, true);

        Object[] outer = (Object[]) out;
        assertEquals(2, outer.length);
        Object[] a0 = (Object[]) outer[0];
        Object[] a1 = (Object[]) outer[1];
        assertArrayEquals(new String[]{"AAA","BBB"}, Arrays.stream(a0).toArray(Object[]::new));
        assertArrayEquals(new String[]{"CCC","DDD"}, Arrays.stream(a1).toArray(Object[]::new));
    }

    @Test
    public void withTrim() {
        // Each leaf string is length 4; trailing spaces should be trimmed with trimPad=true
        String data = "AB  " + "CD  ";
        Object out = strToStringAry(data, new int[]{4,2}, true);
        Object[] arr = (Object[]) out;
        assertArrayEquals(new String[]{"AB","CD"}, Arrays.stream(arr).toArray(Object[]::new));
    }

    // ---------- Error cases ----------

    private void expectError(Runnable action) {
        try {
            action.run();
            fail("Expected IllegalArgumentException");
        } catch (Exception ignored) {}
    }

    @Test
    public void errorCases() {
        // wrong length for fixed size
        expectError(() -> strToStringAry("AB", new int[]{3}, false));

        // first dim 0 invalid
        expectError(() -> strToStringAry("ABC", new int[]{0,2}, false));

        // first dim negative invalid in multi-dim
        expectError(() -> strToStringAry("ABC", new int[]{-2,2}, false));

        // nonLast_negativeDim  *** we need to support this
        expectError(() -> strToStringAry("ABCDEF", new int[]{3,-1,2}, false));

        // lastDim_zero_invalid() {
        expectError(() -> strToStringAry("ABCDEF", new int[]{3,0}, false));

        // arraysize="3x2" expects total length multiple of 3
        expectError(() -> strToStringAry("ABCDE", new int[]{3,2}, false));

        // arraysize="2x3" expects 3 strings of len 2 => total 6 chars, here OK,
        // but change dims to force mismatch with data
        expectError(() -> strToStringAry("ABCDEF", new int[]{2,4}, false)); // expects 4 strings, but got 3
    }

    @Test
    public void nullOrEmptyReturnsNull() {
        assertNull(strToStringAry(null, new int[]{3}, false));
        assertNull(strToStringAry("", new int[]{3}, false));
    }
}
