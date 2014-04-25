package net.zschech.gwt.comettest.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface CometTestServiceAsync {
	public void createSession(AsyncCallback<Boolean> callback);
	public void invalidateSession(AsyncCallback<Boolean> callback);
}
