package edu.caltech.ipac.firefly.tools;

import edu.caltech.ipac.firefly.server.util.Logger;
import nom.tam.fits.*;
import nom.tam.util.Cursor;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.PropertyConfigurator;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

/**
 * Utility class for command line tool to output FITS header from local file or URL.
 *
 * Run with Java, you need to build firefly first, then run:
 * java -cp build/firefly/war/WEB-INF/lib/firefly.jar:build/firefly/war/WEB-INF/lib/* edu.caltech.ipac.firefly.tools.FitsHeaderTool [file or URL]
 *
 */
public class FitsHeaderTool {

    private static final Logger.LoggerImpl LOGGER = Logger.getLogger();
    private static File tempFile;

    public static void main(String[] args) throws FitsException, IOException {

        initLog4j();

        if (args.length != 1) {
            usage();
        }
        //String url ="https://irsa.ipac.caltech.edu/data/SOFIA/FIFI-LS/OC5I/20170726_F422/proc/p4647/data/g11/F0422_FI_IFS_70050813_RED_RP0_100696.fits";
        try {
            URL uri = new URL(args[0]);
            if (uri.getProtocol().startsWith("http")) {
                File.createTempFile("temp", ".fits");
                tempFile.deleteOnExit();
                FileUtils.copyURLToFile(uri, tempFile);
            }
        } catch (MalformedURLException e) {
            tempFile = new File(args[0]);
        }
        if (!tempFile.exists()) {
            LOGGER.info("Your input " + args[0]);
            usage();
        }

        Fits fits1 = new Fits(tempFile);
        BasicHDU[] HDUs = fits1.read();
        int i = 0;
        for (BasicHDU hdu : HDUs) {
            LOGGER.info("# HDU " + i + " in " + args[0]);
            i++;
            Header header = hdu.getHeader();
            Cursor<String, HeaderCard> iterator = header.iterator();
            while (iterator.hasNext()) {
                HeaderCard card = iterator.next();
                LOGGER.info(card.toString());

            }
        }
    }

    private static void initLog4j() {
        Properties prop = new Properties();
        prop.setProperty("log4j.appender.terminal", "org.apache.log4j.ConsoleAppender");
        prop.setProperty("log4j.appender.terminal.layout", "org.apache.log4j.PatternLayout");
        //prop.setProperty("log4j.appender.terminal.layout.ConversionPattern","%d{MM/dd HH:mm:ss} %5p - %.2000m [%t] (%c{2}#%M:%L)%n");
        prop.setProperty("log4j.logger.edu.caltech.ipac", "INFO, terminal");
//        prop.setProperty("log4j.additivity.edu.caltech.ipac","true");
//        PropertyConfigurator.configure("/tmp/log4j.properties");
        PropertyConfigurator.configure(prop);
    }

    static void usage() {
        System.out.println("usage java " + FitsHeaderTool.class.getCanonicalName() + " FITS file or url");
        System.exit(1);
    }

}
