package edu.caltech.ipac.firefly.core;

import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.ui.PopupUtil;

/**
 * Date: Sep 14, 2010
 *
 * @author loi
 * @version $Id: BaseCallback.java,v 1.3 2010/09/28 21:35:09 loi Exp $
 */
public abstract class BaseCallback<T> implements AsyncCallback<T> {

    public void onFailure(Throwable caught) {
        PopupUtil.showSevereError(caught);
        doFinally();
    }

    public void onSuccess(T result) {
        try {
            doSuccess(result);
        } catch(Exception e) {
            PopupUtil.showSevereError(e);
        }finally {
            doFinally();
        }
    }

    public void doFinally() {}

    public abstract void doSuccess(T result);
}
