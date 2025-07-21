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

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

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


    @Category({TestCategory.Perf.class})
    @Test
    public void loadRedisDB() throws Exception {
        /*
          Redis benchmark results:
          Redis is configured to use both AOF and RDB persistence.
          Therefore, it will use rough 3 times the memory in disk space.  2 for the AOF and 1 for the RDB.
          1m jobs requires 600M RAM
            - 11s to get all jobs
            - 1.9s to get user with 100k jobs
            - 1s to get user with 1k or fewer jobs
          500k jobs requires 270M RAM
            - 4.7s to get all jobs
            - 1.1s to get user with 100k jobs
            - 450ms to get user with 1k or fewer jobs
          100k jobs requires 64M
            - 1s to get all jobs
            - 100ms to get user with 1k or fewer jobs
         */


        // set so JobManager does not wait for results
        AppProperties.setProperty("job.wait.complete", "0");
        Logger.setLogLevel(Level.INFO);

        ServerContext.getRequestOwner().setWsConnInfo("test", "test");
        for(int i =0; i < 100_000; i++) {
            JobManager.submit(new SleepJob(Job.Type.PACKAGE, ServerContext.getRequestOwner().getEventConnID()));
        }

        ServerContext.getRequestOwner().setUserKey("59bac3e4-6dc7-4b74-a83d-a35414629999");      // load 9999 with 100k; extreme
        for(int i =0; i < 100_000; i++) {
            JobManager.submit(new SleepJob(Job.Type.PACKAGE, ServerContext.getRequestOwner().getEventConnID()));
        }

        ServerContext.getRequestOwner().setUserKey("59bac3e4-6dc7-4b74-a83d-a35414621000");      // load 1000 with 1k; rare
        for(int i =0; i < 1000; i++) {
            JobManager.submit(new SleepJob(Job.Type.PACKAGE, ServerContext.getRequestOwner().getEventConnID()));
        }

        ServerContext.getRequestOwner().setUserKey("59bac3e4-6dc7-4b74-a83d-a35414620100");      // load 0100 with 100; normal
        for(int i =0; i < 100; i++) {
            JobManager.submit(new SleepJob(Job.Type.PACKAGE, ServerContext.getRequestOwner().getEventConnID()));
        }

        ServerContext.getRequestOwner().setUserKey("59bac3e4-6dc7-4b74-a83d-a35414620010");      // load 0010 with 10; less common
        for(int i =0; i < 10; i++) {
            JobManager.submit(new SleepJob(Job.Type.PACKAGE, ServerContext.getRequestOwner().getEventConnID()));
        }
    }

    @Category({TestCategory.Perf.class})
    @Test
    public void loadTest() throws Exception {
        /*
          Performance results
            Cache keys count: 101,104
            All Jobs: 1,165ms with 101,104 jobs
            User Jobs: 936ms with 99,994 jobs       # confirmed only 99,994 jobs for 9999; not sure why 6 missing
            User Jobs: 94ms with 1,000 jobs
            User Jobs: 84ms with 100 jobs
            User Jobs: 92ms with 10 jobs
         */

        // set so JobManager does not wait for results
        AppProperties.setProperty("job.wait.complete", "0");
        Logger.setLogLevel(Level.INFO);
        List<String> keys = JobManager.getAllJobKeys();
        System.out.printf("Cache keys count: %,d %n", keys.size());

        Instant start = Instant.now();
        int aCount = JobManager.getAllJobs().size();
        System.out.printf("All Jobs: %,dms with %,d jobs %n", Duration.between(start, Instant.now()).toMillis(), aCount);

        ServerContext.getRequestOwner().setUserKey("59bac3e4-6dc7-4b74-a83d-a35414629999");      // get jobs for 9999 with 100k jobs
        start = Instant.now();
        int count = JobManager.getUserJobs().size();
        System.out.printf("User Jobs: %,dms with %,d jobs %n", Duration.between(start, Instant.now()).toMillis(), count);

        ServerContext.getRequestOwner().setUserKey("59bac3e4-6dc7-4b74-a83d-a35414621000");      // get jobs for 1000 with 1k jobs
        start = Instant.now();
        count = JobManager.getUserJobs().size();
        System.out.printf("User Jobs: %,dms with %,d jobs %n", Duration.between(start, Instant.now()).toMillis(), count);

        ServerContext.getRequestOwner().setUserKey("59bac3e4-6dc7-4b74-a83d-a35414620100");      // get jobs for 0100 with 100 jobs
        start = Instant.now();
        count = JobManager.getUserJobs().size();
        System.out.printf("User Jobs: %,dms with %,d jobs %n", Duration.between(start, Instant.now()).toMillis(), count);

        ServerContext.getRequestOwner().setUserKey("59bac3e4-6dc7-4b74-a83d-a35414620010");      // get jobs for 0010 with 10 jobs
        start = Instant.now();
        count = JobManager.getUserJobs().size();
        System.out.printf("User Jobs: %,dms with %,d jobs %n", Duration.between(start, Instant.now()).toMillis(), count);

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
