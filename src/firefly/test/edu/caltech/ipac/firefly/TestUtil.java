package edu.caltech.ipac.firefly;

import edu.caltech.ipac.firefly.server.RequestAgent;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.ws.WsCredentials;
import edu.caltech.ipac.util.AppProperties;
import org.junit.BeforeClass;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Supplier;

public class TestUtil {

    private static final String TEST_DATA_ROOT = "firefly_test_data/";
    private static final Logger.LoggerImpl LOG = Logger.getLogger("test");


    /**
     * Log the memory usage of this run.
     * @param run the function to run
     */
    public static void logMemUsage(Supplier<String> run) {
        ManagementFactory.getMemoryMXBean().gc();
        long before = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
        LOG.trace(String.format("Memory before=%6.2fMB", before/1024.0/1024.0));

        long start = System.currentTimeMillis();
        String desc = run.get();
        long elapsed = System.currentTimeMillis() - start;
        long peak = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed() - before;

        ManagementFactory.getMemoryMXBean().gc();
        long after = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
        LOG.trace(String.format("Memory after=%6.2fMB",  after/1024.0/1024.0));
        LOG.info(String.format("%s:  Memory Usage peak=%6.2fMB  final:%6.2fMB  elapsed:%4.2fsecs", desc, peak/1024.0/1024.0, (after-before)/1024.0/1024.0, elapsed/1000.0));
    }


    /**
     * @return the root directory of firefly test data
     */
    public static File getDataFile() {
        Path root = Paths.get("").toAbsolutePath().getParent().resolve(TEST_DATA_ROOT);
        return root.toFile();
    }

    /**
     * @param cls the class used to resolve the data directory
     * @return the test data directory for the given class.  This directory is parallel to the class's package relative to firefly test data
     * i.e  edu.caltech.ipac.firefly.core.FileAnalysisTest -> edu/caltech/ipac/firefly/core/
     */
    public static File getDataFile(Class cls) {
        File root = getDataFile();
        if (cls == null) return root;
        String relPath = cls.getPackageName().replaceAll("\\.", "/");

        return new File(root, relPath);
    }

    /**
     * @param cls the class used to resolve the data directory
     * @param path a relative path or file name
     * @return the file or directory for the given cls and path
     */
    public static File getDataFile(Class cls, String path) {
        return new File(getDataFile(cls), path);
    }

    /**
     * @param path
     * @return a File represented by the given path relative to firefly test data root
     */
    public static File getDataFile(String path) {
        return new File(getDataFile(), path);
    }


}
