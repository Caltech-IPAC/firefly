/*
 * Copyright 2009 Richard Zschech.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package net.zschech.gwt.comet.server;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A Comet response provides methods for sending messages using the associated HTTP response. It also provides methods
 * for setting up Comet sessions.
 * 
 * @author Richard Zschech
 */
public interface CometServletResponse {
	
	/**
	 * The HTTP request attached to this Comet response
	 * 
	 * @return the HTTP request
	 */
	public HttpServletRequest getRequest();
	
	/**
	 * The HTTP response attached to this Comet response
	 * 
	 * @return the HTTP response
	 */
	public HttpServletResponse getResponse();
	
	/**
	 * Returns the current Comet session associated with this request, or if the request does not have a Comet session,
	 * creates one.
	 * 
	 * @return the Comet session associated with this request
	 */
	public CometSession getSession();
	
	/**
	 * Returns the current Comet session associated with this request or, if there is no current Comet session and
	 * create is true, returns a new Comet session.
	 * 
	 * @param create
	 *            true to create a new session for this request if necessary; false to return null if there's no current
	 *            session
	 * @return the HttpSession associated with this request or null if create is false and the request has no valid
	 *         session
	 */
	public CometSession getSession(boolean create);
	
	/**
	 * Write a single message to the associated HTTP response.
	 * 
	 * @param message
	 * @throws IOException
	 */
	public void write(Serializable message) throws IOException;
	
	/**
	 * Write a single message to the associated HTTP response. Flush the HTTP output stream if flush is true.
	 * 
	 * @param message
	 * @param flush
	 * @throws IOException
	 */
	public void write(Serializable message, boolean flush) throws IOException;
	
	/**
	 * Write a list of message to the associated HTTP response. This method may be more optimal to the single message
	 * version.
	 * 
	 * @param messages
	 * @throws IOException
	 */
	public void write(List<? extends Serializable> messages) throws IOException;
	
	/**
	 * Write a list of message to the associated HTTP response. This method may be more optimal to the single message
	 * version. Flush the HTTP output stream if flush is true.
	 * 
	 * @param messages
	 * @param flush
	 * @throws IOException
	 */
	public void write(List<? extends Serializable> messages, boolean flush) throws IOException;
	
	/**
	 * Write a heartbeat message to the associated HTTP response.
	 * 
	 * @throws IOException
	 */
	public void heartbeat() throws IOException;
	
	/**
	 * Write a terminate message to the associated HTTP response and close the HTTP output stream/
	 * 
	 * @throws IOException
	 */
	public void terminate() throws IOException;
	
	/**
	 * Test if this Comet response has been terminated by calling the {@link #terminate()} method or terminated from the
	 * HTTP client disconnecting.
	 * 
	 * @return if this Comet response has been terminated
	 */
	public boolean isTerminated();
	
	/**
	 * @return the heartbeat interval in milliseconds for this Comet response.
	 */
	public int getHeartbeat();
	
	/**
	 * Send an error before the response is sent.
	 * @param statusCode
	 * @throws IOException
	 */
	public void sendError(int statusCode) throws IOException;
	
	/**
	 * Send an error before the response is sent.
	 * @param statusCode
	 * @param message
	 * @throws IOException
	 */
	public void sendError(int statusCode, String message) throws IOException;
}
