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

import static edu.caltech.ipac.firefly.core.Util.Opt.ifNotNull;
import static org.junit.Assert.*;
import static edu.caltech.ipac.firefly.core.Util.*;

public class UtilTest extends ConfigTest {

    @Test
    public void arrayOfBoolean() throws Exception {
        Object in = new Boolean[]{true, false, true};
        String s = serialize(in);
        Object d = deserialize(s);
        if (d instanceof Boolean[] v) {
            Assert.assertArrayEquals(v, (Boolean[]) in);
        } else Assert.fail("Deserialized type Boolean mismatch");
    }

    @Test
    public void arrayOfDouble() throws Exception {
        Object in = new Double[]{1.0, 2.0, 3.0};
        String s = serialize(in);
        Object d = deserialize(s);
        if (d instanceof Double[] v) {
            Assert.assertArrayEquals(v, (Double[]) in);
        } else Assert.fail("Deserialized type Double mismatch");
    }

    @Test
    public void arrayOfInt() throws Exception {
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
    public void deserializeValidString() throws Exception {
        String original = "testString";
        String serialized = serialize(original);
        Object deserialized = deserialize(serialized);
        assertEquals(original, deserialized);
    }

    @Test
    public void deserializeInvalidString() throws Exception {
        try {
            deserialize("invalidBase64");
            fail("Should have thrown an exception");
        } catch (Exception ignored) {}
    }

    @Test
    public void deserializeNullString() throws Exception {
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

    @Test
    public void optTestGet() {
        int a = ifNotNull(12).get();                // because input is not null, it returns the value
        assertEquals(12, a);

        int a1 = ifNotNull(12).get(v -> v + 1);    // because input is not null, it calls the function with it and return the result.
        assertEquals(13, a1);

        String b1 = ifNotNull(123456)
                .get((v) -> String.valueOf(v).substring(3));     // it can also transform the value and return a different type
        assertEquals("456", b1);
    }

    @Test
    public void optTestApply() {
        // similar to get(), but it does not return a value
        AtomicInteger a = new AtomicInteger(0);
        ifNotNull(12).apply(a::set);                // because input is not null, apply is called
        assertEquals(12, a.get());

        AtomicInteger a1 = new AtomicInteger(0);
        ifNotNull(null).apply(v -> a1.set(-1));    // because input is not null, it skips apply
        assertEquals(0, a1.get());
    }

    @Test
    public void optTestException() {
        int a1 = ifNotNull(() -> Integer.parseInt("1"))
                        .orElse(-1)      // skip
                        .get(v -> v + 1);       // because input function returns 1, it calls the function with it and return the result.
        assertEquals(2, a1);

        int a2 = ifNotNull(() -> Integer.parseInt("xxx"))
                .orElse(-1)           // because input function throws an exception, it returns the default value
                .get();
        assertEquals(-1, a2);
    }

    @Test
    public void optTestChain() {

        Integer a2 = ifNotNull((Integer)null)
                        .then(v -> v + 1)       // skip because input is null
                        .get();                 // if input is null, it should return null without calling the function
        assertNull(a2);

        int a3 = ifNotNull((Integer)null)
                        .then(v -> v + 1)       // skip because input is null
                        .orElse(v -> -1)        // execute this instead
                        .get();                 // return the result
        assertEquals(-1, a3);

        String b = ifNotNull(123456)
                        .then((v) -> String.valueOf(v).substring(3))      // convert int to String
                        .orElse(v -> "null")                                        // skip
                        .get();                                                     // return the result
        assertEquals("456", b);

        var c = Opt.ifNotNull("test")
                        .orElse(String::toUpperCase)   // skip
                        .get();
        assertEquals("test", c);              // since the value was valid, orElse does not apply

        var c1 = Opt.ifNotNull(null)
                .orElse("default")
                .get();
        assertEquals("default", c1);
    }



}