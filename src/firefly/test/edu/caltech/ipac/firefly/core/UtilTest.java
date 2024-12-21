/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.core;

/**
 * Date: 12/20/24
 *
 * @author loi
 * @version : $
 */
import edu.caltech.ipac.firefly.ConfigTest;
import edu.caltech.ipac.firefly.server.util.Logger;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.Assert.*;
import static edu.caltech.ipac.firefly.core.Util.*;

public class UtilTest extends ConfigTest {

    @Test
    public void arrayOfBoolean() {
        Object in = new Boolean[]{true, false, true};
        String s = serialize(in);
        Object d = deserialize(s);
        if (d instanceof Boolean[] v) {
            Assert.assertArrayEquals(v, (Boolean[]) in);
        } else Assert.fail("Deserialized type Boolean mismatch");
    }

    @Test
    public void arrayOfDouble() {
        Object in = new Double[]{1.0, 2.0, 3.0};
        String s = serialize(in);
        Object d = deserialize(s);
        if (d instanceof Double[] v) {
            Assert.assertArrayEquals(v, (Double[]) in);
        } else Assert.fail("Deserialized type Double mismatch");
    }

    @Test
    public void arrayOfInt() {
        Object in = new Integer[]{1, 2, 3};
        String s = serialize(in);
        Object d = deserialize(s);
        if (d instanceof Integer[] v) {
            Assert.assertArrayEquals(v, (Integer[])in);
        } else Assert.fail("Deserialized type Integer mismatch");
    }

    @Test
    public void serializeValidObject() {
        String original = "testString";
        String serialized = serialize(original);
        assertNotNull(serialized);
    }

    @Test
    public void serializeNullObject() {
        String serialized = serialize(null);
        assertNull(serialized);
    }

    @Test
    public void deserializeValidString() {
        String original = "testString";
        String serialized = serialize(original);
        Object deserialized = deserialize(serialized);
        assertEquals(original, deserialized);
    }

    @Test
    public void deserializeInvalidString() {
        Object deserialized = deserialize("invalidBase64");
        assertNull(deserialized);
    }

    @Test
    public void deserializeNullString() {
        Object deserialized = deserialize(null);
        assertNull(deserialized);
    }

    @Test
    public void tryItFuncWithExSuccess() {
        assertEquals("success", Try.it(() -> "success").getOrElse("default"));
    }

    @Test
    public void tryItFuncWithExFailure() {
        Try<String> result = Try.it(() -> { throw new Exception("failure"); });
        assertEquals("default", result.getOrElse("default"));
    }

    @Test
    public void tryItCallWithExSuccess() {
        Try<Void> result = Try.it(() -> {});
        assertNull(result.get());
    }

    @Test
    public void tryItCallWithExFailure() {
        Try<Void> result = Try.it(() -> { throw new Exception("failure"); });
        assertNull(result.getOrElse((e) -> Logger.getLogger().trace("error")));
    }

    @Test
    public void tryItFuncParamWithExSuccess() {
        Try<String> result = Try.it((param) -> param, "success");
        assertEquals("success", result.getOrElse("default"));
    }

    @Test
    public void tryItFuncParamWithExFailure() {
        Try<String> result = Try.it((param) -> { throw new Exception("failure"); }, "param");
        assertEquals("default", result.getOrElse("default"));
    }

    @Test
    public void tryUntilSuccess() {
        AtomicInteger count = new AtomicInteger();
        Try<Integer> result = Try.until(count::getAndIncrement, c -> c == 3, 5);
        assertEquals(3, result.get().intValue());
    }

    @Test
    public void testSynchronizedAccess() throws InterruptedException {
        ArrayList<Long> even = new ArrayList<>();
        ArrayList<Long> odd = new ArrayList<>();

        var locker = new Util.SynchronizedAccess();
        Function<String,Long> setResults = (q) -> {
            long start = System.currentTimeMillis();
            var locked = locker.lock(q);
            try {
                Thread.sleep(1_000);
            } catch (InterruptedException e) {}
            finally {
                locked.unlock();
            }
            return System.currentTimeMillis() - start;
        };

        int ntimes = 10;
        var p = Executors.newFixedThreadPool(ntimes);		// when all threads start at the same time, all be blocked.
        for(int i = 0; i < ntimes; i++) {
            long a = i % 2;
            p.submit(() -> {
                if (a == 0 ) {
                    even.add(Math.round(setResults.apply("even")/1000.0));
                } else {
                    odd.add(Math.round(setResults.apply("odd")/1000.0));
                }
            });
        }
        p.shutdown();
        if (!p.awaitTermination(10, TimeUnit.SECONDS)) {
            System.out.println("Not all tasks completed in time.");
        }
//		even.forEach(System.out::println);
//		odd.forEach(System.out::println);

        assertEquals(ntimes/2, (long) Collections.max(even));
        assertEquals(1L, (long)Collections.min(even));
        assertEquals(ntimes/2, (long)Collections.max(odd));
        assertEquals(1L, (long)Collections.min(odd));
    }


}