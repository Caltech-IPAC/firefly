package net.zschech.gwt.chat.server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.servlet.http.HttpSession;

import net.zschech.gwt.chat.client.ChatException;
import net.zschech.gwt.chat.client.ChatMessage;
import net.zschech.gwt.chat.client.ChatService;
import net.zschech.gwt.chat.client.StatusUpdate;
import net.zschech.gwt.chat.client.StatusUpdate.Status;
import net.zschech.gwt.comet.server.CometServlet;
import net.zschech.gwt.comet.server.CometSession;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

/**
 * A simple implementation of {@link ChatService} which maintains all its state in memory.
 * 
 * @author Richard Zschech
 */
public class ChatServiceImpl extends RemoteServiceServlet implements ChatService {
	
	/**
	 * A mapping of user names to CometSessions used for routing messages.
	 */
	private ConcurrentMap<String, CometSession> users = new ConcurrentHashMap<String, CometSession>();
	
	@Override
	public String getUsername() throws ChatException {
		// check if there is a HTTP session setup.
		HttpSession httpSession = getThreadLocalRequest().getSession(false);
		if (httpSession == null) {
			return null;
		}
		
		// return the user name
		return (String) httpSession.getAttribute("username");
	}
	
	/**
	 * @see net.zschech.gwt.chat.client.ChatService#login(java.lang.String)
	 */
	@Override
	public void login(String username) throws ChatException {
		// Get or create the HTTP session for the browser
		HttpSession httpSession = getThreadLocalRequest().getSession();
		// Get or create the Comet session for the browser
		CometSession cometSession = CometServlet.getCometSession(httpSession);
		// Remember the user name for the
		httpSession.setAttribute("username", username);
		
		// setup the mapping of user names to CometSessions
		if (users.putIfAbsent(username, cometSession) != null) {
			// some one else has already logged in with this user name 
			httpSession.invalidate();
			throw new ChatException("User: " + username + " already logged in");
		}
	}
	
	/**
	 * @see net.zschech.gwt.chat.client.ChatService#logout(java.lang.String)
	 */
	@Override
	public void logout(String username) throws ChatException {
		// check if there is a HTTP session setup.
		HttpSession httpSession = getThreadLocalRequest().getSession(false);
		if (httpSession == null) {
			throw new ChatException("User: " + username + " is not logged in: no http session");
		}
		
		// check if there is a Comet session setup. In a larger application the HTTP session may have been
		// setup via other means.
		CometSession cometSession = CometServlet.getCometSession(httpSession, false);
		if (cometSession == null) {
			throw new ChatException("User: " + username + " is not logged in: no comet session");
		}
		
		// check the user name parameter matches the HTTP sessions user name
		if (!username.equals(httpSession.getAttribute("username"))) {
			throw new ChatException("User: " + username + " is not logged in on this session");
		}
		
		// remove the mapping of user name to CometSession
		users.remove(username, cometSession);
		httpSession.invalidate();
	}
	
	/**
	 * @see net.zschech.gwt.chat.client.ChatService#send(java.lang.String)
	 */
	@Override
	public void send(String message) throws ChatException {
		// check if there is a HTTP session setup.
		HttpSession httpSession = getThreadLocalRequest().getSession(false);
		if (httpSession == null) {
			throw new ChatException("not logged in: no http session");
		}
		
		// get the user name for the HTTP session.
		String username = (String) httpSession.getAttribute("username");
		if (username == null) {
			throw new ChatException("not logged in: no http session username");
		}
		
		// create the chat message
		ChatMessage chatMessage = new ChatMessage();
		chatMessage.setUsername(username);
		chatMessage.setMessage(message);
		
		for (Map.Entry<String, CometSession> entry : users.entrySet()) {
			entry.getValue().enqueue(chatMessage);
		}
	}

	@Override
	public void setStatus(Status status) throws ChatException {
		// check if there is a HTTP session setup.
		HttpSession httpSession = getThreadLocalRequest().getSession(false);
		if (httpSession == null) {
			throw new ChatException("not logged in: no http session");
		}
		
		// get the user name for the HTTP session.
		String username = (String) httpSession.getAttribute("username");
		if (username == null) {
			throw new ChatException("not logged in: no http session username");
		}
		
		// create the chat message
		StatusUpdate statusUpdate = new StatusUpdate();
		statusUpdate.setUsername(username);
		statusUpdate.setStatus(status);
		
		for (Map.Entry<String, CometSession> entry : users.entrySet()) {
			entry.getValue().enqueue(statusUpdate);
		}
	}
}