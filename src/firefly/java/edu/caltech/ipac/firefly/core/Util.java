/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.core;

import edu.caltech.ipac.firefly.server.util.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static edu.caltech.ipac.util.StringUtils.isEmpty;

/**
 * Date: 11/19/24
 *
 * @author loi
 * @version : $
 */
public class Util {
    private static final Logger.LoggerImpl logger = Logger.getLogger();


    /**
     * Serializes a Java object into a Base64-encoded string.
     * <p>
     * This method converts the given object into a byte stream,
     * encodes the byte stream into a Base64 string, and returns the result.
     * The object must implement {@link java.io.Serializable} for this method to work.
     * </p>
     * @param obj the object to serialize; must implement {@link java.io.Serializable}
     * @return a Base64-encoded string representing the serialized object, or null
     */
    public static String serialize(Object obj) {
        if (obj == null) return null;
        try {
            ByteArrayOutputStream bstream = new ByteArrayOutputStream();
            ObjectOutputStream ostream = new ObjectOutputStream(bstream);
            ostream.writeObject(obj);
            ostream.flush();
            byte[] bytes =  bstream.toByteArray();
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            logger.warn(e);
            return null;
        }
    }

    /**
     * Deserializes a Base64-encoded string into a Java object.
     * <p>
     * This method decodes the provided Base64 string into a byte stream,
     * then reconstructs the original object using Java's object serialization mechanism.
     * </p>
     * @param base64 the Base64-encoded string representing the serialized object
     * @return the deserialized Java object, or null.
     */
    public static Object deserialize(String base64) throws Exception {
            if (base64 == null) return null;
            byte[] bytes = Base64.getDecoder().decode(base64);
            ByteArrayInputStream bstream = new ByteArrayInputStream(bytes);
            ObjectInputStream ostream = new ObjectInputStream(bstream);
            return ostream.readObject();
    }


//====================================================================
//  Functional Helpers
//====================================================================

    /**
     * A function that throws exception
     * @param <T>  the return type of this function
     */
    @FunctionalInterface
    public interface FuncWithEx<T> {
        T get() throws Exception;
    }

    /**
     * A callable that throws exception
     */
    @FunctionalInterface
    public interface CallWithEx {
        void run() throws Exception;
    }

    /**
     * A function with parameter that throws exception
     */
    @FunctionalInterface
    public interface FuncParamWithEx<P, T> {
        T apply(P p) throws Exception;
    }

    public static class Try<T> {
        private final T val;
        private final Exception ex;

        Try(T val, Exception ex) {
            this.val = val;
            this.ex = ex;
        }

        /**
         * Get the value if no exception was thrown
         * @return the value if no exception was thrown, or null
         */
        public T get() {
            return ex == null ? val : null;
        }

        /**
         * Get the value if no exception was thrown, otherwise return the default value
         * @param defVal the default value to return if an exception was thrown
         * @return the value if no exception was thrown, or the default value
         */
        public T getOrElse(T defVal) {
            return ex == null ? val : defVal;
        }

        /**
         * Get the value if no exception was thrown
         * @param onError the consumer to call if an exception was thrown
         *                The exception is passed to the consumer
         * @return the value if no exception was thrown, or null
         */
        public T getOrElse(Consumer<Exception> onError) {
            if (ex == null) return val;
            onError.accept(ex);
            return null;
        }

        /**
         * Execute the given function and parameter and return a Try object that can be used to
         * get the result or handle the exception.
         * @param func the function to call
         * @param param the parameter to pass to the function
         * @return a Try object that can be used to chain operations
         */
        public static <P, T> Try<T> it(FuncParamWithEx<P,T>  func, P param) {
            try {
                return new Try<>(func.apply(param), null);
            } catch (Exception e) {
                return new Try<>(null, e);
            }
        }

        /**
         * Execute the given function and return a Try object that can be used to
         * get the result or handle the exception.
         * This function does not take any parameters nor return a value.
         * @param func the function to call
         * @return a Try object that can be used to chain operations
         */
        public static <T> Try<T> it(CallWithEx func) {
            try {
                func.run();
                return new Try<>(null, null);
            } catch (Exception e) {
                return new Try<>(null, e);
            }
        }

        /**
         * Execute the given function and return a Try object that can be used to
         * get the result or handle the exception.
         * @param func the function to call
         * @return a Try object that can be used to chain operations
         */
        public static <T> Try<T> it(FuncWithEx<T> func) {
            try {
                return new Try<>(func.get(), null);
            } catch (Exception e) {
                return new Try<>(null, e);
            }
        }

        /**
         * Execute the given function until it passes test, then return a
         * Try object that can be used to get the result or handle the exception.
         * @param func  the function to execute
         * @param test  a function that takes the result as a parameter and returns true if it acceptable
         * @param tries the number of times to try
         * @return Try results
         */
        public static <T> Try<T> until(FuncWithEx<T> func, Predicate<T> test, int tries) {
            for (int i = 0; i < tries; i++) {
                var res = it(func).get();
                if (res != null && test.test(res)) return new Try<>(res, null);
            }
            return new Try<>(null, new IndexOutOfBoundsException("Exceeded max tries"));
        }
    }

    /**
     * A lightweight optional wrapper that allows conditional execution of functions
     * based on the input value.
     * @see edu.caltech.ipac.firefly.core.UtilTest for sample usage
     * @param <T> The type of the value contained in this Opt instance.
     */
    public static class Opt<T> {
        private final T val;
        private Predicate<T> test;

        private Opt(T val, Predicate<T> test) {
            this.val = val;
            this.test = test;
        }

        /**
         * Applies the given function if the value passes the predicate check and returns a new Opt instance for chaining.
         * @param func The function to apply.
         * @param <R>  The return type of the function.
         * @return A new Opt containing the function result if the value is valid, otherwise an empty Opt.
         */
        public <R> Opt<R> then(Function<T, R> func) {
            if (test.test(val)) {
                return  new Opt<>(func.apply(val), (v)->true);
            } else {
                return new Opt<>(null, (v)->false);
            }
        }

        /**
         * Applies the given function if the value does not pass the predicate check and returns a new Opt instance for chaining.
         * @param func The function to apply.
         * @param <R>  The return type of the function.
         * @return A new Opt containing the function result if the value is invalid, otherwise an empty Opt.
         */
        public <R> Opt<R> orElse(Function<T, R> func) {
            if (!test.test(val)) {
                return  new Opt<>(func.apply(val), (v)->true);
            } else {
                return (Opt<R>)this;
            }
        }

        /**
         * Returns an Opt containing the default value if the value does not pass the predicate check for chaining
         * @param defVal The default value.
         * @param <R>    The type of the default value.
         * @return A new Opt containing the default value if the original value is invalid, otherwise returns this instance.
         */
        public <R> Opt<R> orElse(R defVal) {
            if (!test.test(val)) {
                return  new Opt<>(defVal, (v)->true);
            } else {
                return (Opt<R>)this;
            }
        }

        /**
         * Call the given function if the value is valid.
         * @param func The function to call.
         */
        public void apply(Consumer<T> func) {
            if (test.test(val)) {
                func.accept(val);
            }
        }

        /**
         * @return Returns the value if valid, otherwise returns null.
         */
        public T get() {
            if (test.test(val)) {
                return val;
            } else {
                return null;
            }
        }

        /**
         * @param defVal The default value to return if the value is invalid.
         * @return Returns the value if valid, otherwise returns the specified default value.
         */
        public T get(T defVal) {
            if (test.test(val)) {
                return val;
            } else {
                return defVal;
            }
        }

        /**
         * Applies the given function to the value if valid and returns the result.
         * @param func The function to apply.
         * @param <R>  The return type of the function.
         * @return The function result or null if the value is invalid.
         */
        public <R> R get(Function<T, R> func) {
            if (test.test(val)) {
                return  func.apply(val);
            } else {
                return null;
            }
        }

        /**
         * Creates an Opt instance if the provided value is not empty (null or empty-string).
         * @param val The value to check.
         * @param <T> The type of the value.
         * @return A new Opt instance if the value is not empty, otherwise an empty Opt.
         */
        public static <T> Opt<T> ifNotEmpty(T val) {
            return new Opt<>(val, (v) -> !isEmpty(v));
        }

        /**
         * Creates an Opt instance if the provided value is not null.
         * @param val The value to check.
         * @param <T> The type of the value.
         * @return A new Opt instance if the value is not null, otherwise an empty Opt.
         */
        public static <T> Opt<T> ifNotNull(T val) {
            return new Opt<>(val, Objects::nonNull);
        }

        /**
         * Creates an Opt instance from a value returned by a function.
         * @param val The function providing the value.
         * @param <T> The type of the value.
         * @return A new Opt instance if the function returns a non-null value, otherwise an empty Opt.
         */
        public static <T> Opt<T> ifNotNull(Supplier<T> val) {
            try {
                return new Opt<>(val.get(), Objects::nonNull);
            } catch (Exception e) {
                return new Opt<>(null, (v)->false);
            }
        }
    }

    public static class SynchronizedAccess {
        private final ConcurrentHashMap<String, ReentrantLock> activeRequests = new ConcurrentHashMap<>();

        @FunctionalInterface
        public interface LockHandle {
            void unlock();
        }

        /**
         * Acquires a lock associated with the given ID. If the lock does not already exist, it is created.
         *
         * @param id the identifier for the lock
         * @return a {@code Runnable} that, when executed, releases the lock and removes it from the active requests
         */
        public LockHandle lock(String id) {
            ReentrantLock lock = activeRequests.computeIfAbsent(id, k -> new ReentrantLock());
            Logger.getLogger().trace("waiting %s: %s\n".formatted(id, lock));
            lock.lock();
            Logger.getLogger().trace("got lock %s: %s\n".formatted(id, lock));
            return () -> {
                try {
                    lock.unlock();              // Ensure lock is released even if an exception occurs
                } finally {
                    if (!lock.isLocked()) activeRequests.remove(id);  // Remove the lock from activeRequests if no threads are using it
                    Logger.getLogger().trace("unlock %s: %s\n".formatted(id, lock));
                }
            };
        }
    }
}
