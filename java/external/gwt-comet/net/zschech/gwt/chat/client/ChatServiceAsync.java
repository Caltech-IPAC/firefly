package net.zschech.gwt.chat.client;

import net.zschech.gwt.chat.client.StatusUpdate.Status;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface ChatServiceAsync {
	public void getUsername(AsyncCallback<String> callback);
	
	public void login(String username, AsyncCallback<Void> callback);
	
	public void logout(String username, AsyncCallback<Void> callback);
	
	public void send(String message, AsyncCallback<Void> callback);
	
	public void setStatus(Status status, AsyncCallback<Void> callback);
}
