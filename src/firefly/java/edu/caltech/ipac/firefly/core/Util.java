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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Predicate;

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
    public static Object deserialize(String base64) {
        try {
            if (base64 == null) return null;
            byte[] bytes = Base64.getDecoder().decode(base64);
            ByteArrayInputStream bstream = new ByteArrayInputStream(bytes);
            ObjectInputStream ostream = new ObjectInputStream(bstream);
            return ostream.readObject();
        } catch (Exception e) {
            logger.warn(e);
            return null;
        }
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

        public T getOrElse(T defVal) {
            return ex == null ? val : defVal;
        }

        public T getOrElse(Consumer<Exception> onError) {
            if (ex == null) return val;
            onError.accept(ex);
            return null;
        }

        public static <P, T> Try<T> it(FuncParamWithEx<P,T>  func, P param) {
            try {
                return new Try<>(func.apply(param), null);
            } catch (Exception e) {
                return new Try<>(null, e);
            }
        }

        public static <T> Try<T> it(CallWithEx func) {
            try {
                func.run();
                return new Try<>(null, null);
            } catch (Exception e) {
                return new Try<>(null, e);
            }
        }

        public static <T> Try<T> it(FuncWithEx<T> func) {
            try {
                return new Try<>(func.get(), null);
            } catch (Exception e) {
                return new Try<>(null, e);
            }
        }

        /**
         * Execute the given function until it passes test, then return the result
         * @param func  the function to execute
         * @param test  test the returned value
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
