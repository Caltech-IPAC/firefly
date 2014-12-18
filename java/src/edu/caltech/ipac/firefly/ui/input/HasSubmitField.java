package edu.caltech.ipac.firefly.ui.input;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * User: roby
 * Date: Aug 16, 2010
 * Time: 3:05:13 PM
 */
public interface HasSubmitField {
    public void submit(AsyncCallback<String> callback);
}
