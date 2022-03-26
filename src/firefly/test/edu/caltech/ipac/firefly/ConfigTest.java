package edu.caltech.ipac.firefly;

import edu.caltech.ipac.firefly.server.RequestAgent;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.ws.WsCredentials;
import edu.caltech.ipac.util.AppProperties;
import org.apache.logging.log4j.Level;
import org.junit.BeforeClass;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.UUID;

/**
 * Should load the logger and app properties to apply to any test runnner
 * For logger, using "test" alias name in unit test and change test properties to be used in {@link AppProperties}
 * Use the class to extend you test case and make use of particular log level
 *
 * @author ejoliet
 */
public class ConfigTest {

    public static String TEST_PROP_FILE = "./config/test/app-test.prop";
    public static String WS_USER_ID = AppProperties.getProperty("workspace.user","test@ipac.caltech.edu");

    /**
     * Use the logger in the test case that would extends this class.
     */
    public static final Logger.LoggerImpl LOG = Logger.getLogger("test");

    /**
     * Run before each test class get instantiated
     */
    @BeforeClass
    public static void initializeTest() {

        Logger.setLogLevel(Level.OFF);
        // Turn off logging initially.
        // use Logger.setLogLevel() anywhere else to adjust logging level.

        try {
            loadProperties();
        } catch (IOException e) {
            System.err.println(TEST_PROP_FILE + " not found");
        }
    }

    /**
     * Load properties from test config file app-test.config to overwrite the app.config (OPS)
     *
     * @throws IOException
     */
    public static void loadProperties() throws IOException {
        // Load and overwrite test properties
        Properties props = System.getProperties();
        AppProperties.loadClassPropertiesFromFileToPdb(new File(TEST_PROP_FILE), props);
    }

    /**
     * Only return loggedin user because tests
     * @return
     */
    public static WsCredentials getWsCredentials() {
        if(AppProperties.getProperty("workspace.pass")!=null){
            return new WsCredentials(WS_USER_ID, AppProperties.getProperty("workspace.pass"));
        }else{
            return null;
        }
    }

    public static void load(String propFile) throws IOException {
        // If there are no file properties, no need to add anything
        Properties props = System.getProperties();

        final String propDirs[] = AppProperties.getArrayProperties("firefly.propdirs", ":", "config");

        if (propDirs != null) {
            // If the file exist locally:
            for (final String dir : propDirs) {

                final File f = new File(dir, propFile);

                if (f.exists()) {
                    props.load(new FileInputStream(f));
                }
            }
        } else {
            // If the file does not exist locally, then it will be looked after
            // in the jar archive.
            final URL configUrl = new File(propFile).toURI().toURL();

            if (configUrl == null) {
                throw new IOException("Cannot find property file " + propFile);
            }
            props.load(configUrl.openStream());
        }
    }

    public static void setupServerContext(RequestAgent requestAgent) {
        String contextPath = System.getenv("contextPath");
        String contextName = System.getenv("contextName");
        String webappConfigPath = System.getenv("webappConfigPath");

        AppProperties.setProperty("CacheManager.disabled", "true");
        AppProperties.setProperty("work.directory", Paths.get("build").toAbsolutePath().toString());

        contextPath = contextPath == null ? "/firefly" : contextPath;
        contextName = contextName == null ? "firefly" : contextName;
        webappConfigPath = webappConfigPath == null ? Paths.get("config/test/").toAbsolutePath().toString() : webappConfigPath;

        requestAgent = requestAgent == null ? new RequestAgent(null, "localhost", "/test", "localhost:8080/", "127.0. 0.1", UUID.randomUUID().toString(), contextPath): requestAgent;

        ServerContext.getRequestOwner().setRequestAgent(requestAgent);
        ServerContext.init(contextPath, contextName, webappConfigPath);
    }

}
