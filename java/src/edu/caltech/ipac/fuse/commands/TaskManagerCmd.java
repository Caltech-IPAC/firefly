package edu.caltech.ipac.fuse.commands;

import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.core.RequestCmd;
import edu.caltech.ipac.firefly.core.task.TaskManager;
import edu.caltech.ipac.firefly.data.Request;

/**
 * Date: 7/3/14
 *
 * @author loi
 * @version $Id: $
 */
public class TaskManagerCmd extends RequestCmd {
    public static final String COMMAND = "TaskManager";

    private TaskManager taskManager;

    public TaskManagerCmd() {
        super(COMMAND);
    }

    protected void doExecute(Request req, AsyncCallback<String> callback) {
        if (taskManager == null) {
            taskManager = new TaskManager();
        }
        taskManager.show();
    }
}
