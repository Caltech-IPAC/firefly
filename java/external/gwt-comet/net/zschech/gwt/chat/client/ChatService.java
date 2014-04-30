package net.zschech.gwt.chat.client;

import net.zschech.gwt.chat.client.StatusUpdate.Status;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * This is the interface for the chat communicating with the chat server.
 * 
 * @author Richard Zschech
 */
@RemoteServiceRelativePath("chat")
public interface ChatService extends RemoteService {
	
	/**
	 * Gets the currently logged on user name.
	 * 
	 * @return
	 * @throws ChatException
	 */
	public String getUsername() throws ChatException;
	
	/**
	 * Login and setup a CometSession on the chat server.
	 * 
	 * @param username
	 * @throws ChatException
	 */
	public void login(String username) throws ChatException;
	
	/**
	 * Logout and destroy the CometSession on the chat server.
	 * 
	 * @param username
	 * @throws ChatException
	 */
	public void logout(String username) throws ChatException;
	
	/**
	 * Send a message to all users on the chat server.
	 * 
	 * @param message
	 * @throws ChatException
	 */
	public void send(String message) throws ChatException;
	
	/**
	 * Send a status update message to all users on the chat server.
	 * 
	 * @param status
	 * @throws ChatException
	 */
	public void setStatus(Status status) throws ChatException;
}
