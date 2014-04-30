package net.zschech.gwt.chat.client;

import java.io.Serializable;

public class ChatMessage implements Serializable {

	private static final long serialVersionUID = -1741682874903010139L;
	
	private String username;
	private String message;
	
	public String getUsername() {
		return username;
	}
	
	public void setUsername(String username) {
		this.username = username;
	}
	
	public String getMessage() {
		return message;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}
}
