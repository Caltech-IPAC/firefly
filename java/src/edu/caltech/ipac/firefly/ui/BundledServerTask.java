package edu.caltech.ipac.firefly.ui;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;

import java.util.ArrayList;
import java.util.List;

/**
 * Date: Apr 7, 2008
 *
 * @author loi
 * @version $Id: BundledServerTask.java,v 1.12 2010/09/14 17:58:07 loi Exp $
 */
public abstract class BundledServerTask {

    private ServerTask maskingImpl;
    private List<ServerTaskWrapper> tasks = new ArrayList<ServerTaskWrapper>();
    private transient List<ServerTask> inprogressTasks;
    private boolean isCancelled;
    private boolean isStopOnError = true;
    private boolean runSequentially = false;
    private int currentTaskIdx;

    public BundledServerTask() {
        this(null);
    }

    public BundledServerTask(Widget widget) {
        this(widget, true);
    }

    public BundledServerTask(Widget widget, boolean cancelable) {
        maskingImpl = new ServerTask(widget, null, cancelable){
                public void onSuccess(Object result) {}
                public void doTask(AsyncCallback passAlong) {}
                protected void onCancel() {
                    BundledServerTask.this.onCancel();
                }
        };
    }

    public List<ServerTask> getTasks() {
        ArrayList<ServerTask> list = new ArrayList<ServerTask>();
        for (ServerTaskWrapper tw : tasks) {
            list.add(tw.getTask());
        }
        return list;
    }

    public void addServerTask(ServerTask task) {
        tasks.add(new ServerTaskWrapper(task));
    }

    /**
     * sequentially run one task at a time based on the order it was added
     * the next task starts only when the previous one has completed.
     * this bundle stops on error.
     */
    public void start() {
        if(tasks.size() > 0) {
            runSequentially = true;
            isCancelled = false;
            inprogressTasks = new ArrayList<ServerTask>();
            currentTaskIdx = 0;
            tasks.get(0).start();
            handleMasking();
        }
    }

    /**
     * start all of the tasks at the same time.
     */
    public void startAll() {
        if(tasks.size() > 0) {
            runSequentially = false;
            isCancelled = false;
            inprogressTasks = new ArrayList<ServerTask>();
            for(ServerTaskWrapper tw : tasks) {
                inprogressTasks.add(tw.getTask());
                tw.start();
            }
            handleMasking();
        }
    }

    public String getMessage() {
        return getActiveTask() == null ? "" : getActiveTask().getMsg();
    }

    public boolean isStopOnError() {
        return isStopOnError;
    }

    /**
     * If true, it will cancel all of the other running tasks if any one of
     * its task fail.
     * @param stopOnError
     */
    public void setStopOnError(boolean stopOnError) {
        isStopOnError = stopOnError;
    }

    public abstract void finish();

    public int size() { return tasks.size(); }

    protected void onFinish() {
        if(!isCancelled) {
            finish();
        }
    }

    protected void onTaskComplete(ServerTask task, Object o) {}

    protected void taskCompleted(ServerTask task, Object o) {
        if (runSequentially) {
            currentTaskIdx++;
            if (currentTaskIdx < tasks.size()) {
                tasks.get(currentTaskIdx).start();
            } else {
                onFinish();
            }
        } else {
            for(ServerTaskWrapper tw : tasks) {
                if (!tw.getTask().isFinish()) {
                    return;
                }
            }
            // all tasks have completed
            onFinish();
        }
    }

    protected void onTaskFailure(ServerTask task, Throwable throwable) {
        if (isStopOnError()) {
            onCancel();
        }
    }

    protected void onCancel() {
        isCancelled = true;
        for(ServerTask t : inprogressTasks) {
            t.cancel();
        }
        inprogressTasks.clear();
    }

    private void handleMasking() {
        if(getActiveTask() != null) {
            maskingImpl.unMask();
            maskingImpl.setMsg(getActiveTask().getMsg());
            maskingImpl.mask();
        } else {
            maskingImpl.unMask();
        }
    }

    protected ServerTask getActiveTask() {
        if (inprogressTasks != null && inprogressTasks.size() > 0) {
            return inprogressTasks.get(0);
        }
        return null;
    }

//====================================================================
//
//====================================================================

    class ServerTaskWrapper implements AsyncCallback {
        private ServerTask task;

        public ServerTaskWrapper(ServerTask task) {
            this.task = task;
            task.setAutoMask(false);
        }

        public ServerTask getTask() {
            return task;
        }

        public void onFailure(Throwable throwable) {
            if(!isCancelled) {
                inprogressTasks.remove(task);
                onTaskFailure(task, throwable);
                handleMasking();
                task.doFailure(throwable);
            }
        }

        public void onSuccess(Object o) {
            if(!isCancelled) {
                try {
                    inprogressTasks.remove(task);
                    task.doSuccess(o);
                    onTaskComplete(task, o);
                    taskCompleted(task, o);
                } finally {
                    handleMasking();
                }
            }
        }

        void start() {
            task.start(this);
        }
    }

}
/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
* OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS,
* HOWEVER USED.
*
* IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE
* FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL
* OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO
* PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE
* ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
*
* RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE
* AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR
* ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE
* OF THE SOFTWARE.
*/
