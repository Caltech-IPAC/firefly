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
