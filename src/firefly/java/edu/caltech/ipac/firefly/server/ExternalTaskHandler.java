package edu.caltech.ipac.firefly.server;

import edu.caltech.ipac.firefly.ui.creator.CommonParams;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 *
 * @author tatianag
 */
public interface ExternalTaskHandler {

    String LAUNCHER = CommonParams.LAUNCHER;
    String TASK = CommonParams.TASK;
    String TASK_PARAMS = CommonParams.TASK_PARAMS;
    List<String> ALL_PARAMS = Arrays.asList(LAUNCHER,TASK,TASK_PARAMS);

    /**
     * ExternalTaskLauncher will invoke this method only once, before the execution of this task.
     * This method will be invoked first, before any other methods in this interface.
     * @param launcher
     */
    void setup(ExternalTaskLauncher launcher, Map<String,String> env) throws InterruptedException;

    /**
     * ExternalTaskLauncher will invoke this method only once, after the execution stops.
     * @param status the exit status of the process.
     */
    void finish(int status);

    /**
     * Handle external task's output stream
     * <p>
     * Throw InterruptedException to stop the running process.
     * @param is InputStream that must be read
     * @throws InterruptedException
     */
    void handleOut(InputStream is) throws InterruptedException;

    /**
     * Handle the external task's error stream
     * <p>
     * Throw InterruptedException to stop the running process.
     * @param is InputStream
     * @throws InterruptedException
     */
    void handleError(InputStream is) throws InterruptedException;

    /**
     * This method should return a false under normal circumstances.
     * If you want to make the ExternalTaskLauncher exit without executing the subprocess
     * then return true
     * @return boolean true is you want to abort, false if you want to continue
     *        execution
     */
    boolean abortExecution();

}
