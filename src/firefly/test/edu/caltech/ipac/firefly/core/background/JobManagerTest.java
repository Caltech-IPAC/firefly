/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.core.background;

import edu.caltech.ipac.TestCategory;
import edu.caltech.ipac.firefly.ConfigTest;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.SrvParam;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.AppProperties;
import org.apache.logging.log4j.Level;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Date;
import java.util.HashMap;

/**
 * @author loi
 * @version $Id: IpacTableParser.java,v 1.18 2011/12/08 19:34:02 loi Exp $
 */
public class JobManagerTest extends ConfigTest {

    static Logger.LoggerImpl logger = Logger.getLogger();

    @BeforeClass
    public static void setUp() {
        // needed when dealing with code running in a server's context, ie  SearchProcessor, RequestOwner, etc.
        setupServerContext(null);
        Logger.setLogLevel(Level.DEBUG);
    }

    @Category({TestCategory.Perf.class})
    @Test
    public void testRunAs() throws Exception {
        // set so JobManager does not wait for results
        AppProperties.setProperty("job.wait.complete", "0");

        /*
            This test submits 20 PACKAGE jobs.  Each one sleeps for 2 seconds, then print completed status.
            Because we set JobManager to not wait for results, you will see all 20 job submitted immediately.
            Since PACKAGE queue is set at 10 max, you will see the first 10 finishes around 2 seconds later,
            follow by the next 10 another 2 seconds after.
            Confirm that the key and ranAs matches.
         */


        for(int i =0; i < 20; i++) {
            ServerContext.getRequestOwner().setWsConnInfo(String.valueOf(i), String.valueOf(i));
            logger.debug(String.format("%2$tH:%2$tM:%2$tS:  Job %d submitted", i, new Date()));
            JobManager.submit(new SleepJob(Job.Type.PACKAGE, ServerContext.getRequestOwner().getEventConnID()));
        }
        logger.debug("All jobs submitted.");
        Thread.currentThread().join(5000);      // wait long enough to see all the jobs processed before terminating.
    }


    private static class SleepJob extends ServCmdJob {
        Job.Type type;
        String key;
        public SleepJob(Job.Type type, String key) {
            this.type = type;
            this.key = key;
            setParams(new SrvParam(new HashMap<>()));
        }

        public Type getType() {
            return type;
        }

        public String doCommand(SrvParam params) throws Exception {
            Thread.sleep(2000);
            logger.debug(String.format("%5$tH:%5$tM:%5$tS:  key: %s    ranAs: %s-%s  Thread[%s]",key,
                    ServerContext.getRequestOwner().getEventConnID(), ServerContext.getRequestOwner().getEventChannel(), Thread.currentThread().getName(),  new Date()));
            return "done";
        }

    }

}
