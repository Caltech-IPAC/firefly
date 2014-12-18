package edu.caltech.ipac.firefly.util;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Date: Sep 23, 2009
 *
 * @author loi
 * @version $Id: AsyncCallbackGroup.java,v 1.5 2010/08/12 19:04:09 loi Exp $
 */
public class AsyncCallbackGroup {
    private AsyncCallback callback;
    private List<AsyncCallback> stack = new ArrayList<AsyncCallback>();
    private String errMsg;

    public AsyncCallbackGroup(AsyncCallback callback) {
        this.callback = callback;
    }

    public AsyncCallback newCallback() {
        return newCallback(null, null);
    }

    /**
     * create a new wrapper callback to work with this group.  If the given callback is not null, the events will
     * be piped to it.
     * @param wrappedCallback
     * @return
     */
    public <T> AsyncCallback<T> newCallback(final AsyncCallback<T> wrappedCallback) {
        AsyncCallback<T> cb = new AsyncCallback<T>(){
            public void onFailure(Throwable caught) {
                errMsg += caught.getMessage();
                if (wrappedCallback != null) {
                    wrappedCallback.onFailure(caught);
                }
                onComplete(this);
            }

            public void onSuccess(T result) {
                if (wrappedCallback != null) {
                    wrappedCallback.onSuccess(result);
                }
                onComplete(this);
            }
        };
        stack.add(cb);
        return cb;
    }

    public AsyncCallback newCallback(final Command onSuccess, final Command onFailure) {
        AsyncCallback cb = new AsyncCallback(){
            public void onFailure(Throwable caught) {
                if (onFailure != null) {
                    onFailure.execute();
                }
            }
            public void onSuccess(Object result) {
                if (onSuccess != null) {
                    onSuccess.execute();
                }
            }
        };
        return newCallback(cb);
    }

    public int getStackSize() {
        return stack.size();
    }

    protected void onComplete(AsyncCallback callback) {
        stack.remove(callback);
        if (stack.size() == 0) {
            if (this.callback != null) {
                if (StringUtils.isEmpty(errMsg)) {
                    this.callback.onSuccess(null);
                } else {
                    this.callback.onFailure(new RuntimeException(errMsg));
                }
            }
        }
    }

}
