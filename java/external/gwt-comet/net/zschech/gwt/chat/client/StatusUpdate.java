package net.zschech.gwt.chat.client;

import java.io.Serializable;

public class StatusUpdate implements Serializable {
	
	public enum Status {
		ONLINE, BUSY, AWAY, OFFLINE
	}
	
	private static final long serialVersionUID = -1741682874903010139L;
	
	private String username;
	private Status status;
	
	public String getUsername() {
		return username;
	}
	
	public void setUsername(String username) {
		this.username = username;
	}
	
	public Status getStatus() {
		return status;
	}
	
	public void setStatus(Status status) {
		this.status = status;
	}
}
