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

import java.io.Serializable;
import java.util.Queue;

import javax.servlet.http.HttpSession;

/**
 * A Comet session encapsulates a queue of messages to be delivered to a comet client. The Comet session is attached to
 * the HTTP session as an attribute with the {@link #HTTP_SESSION_KEY}.
 * 
 * @author Richard Zschech
 */
public interface CometSession {
	
	/**
	 * The key for the HttpSession to look up the CometSession attribute 
	 */
	public static final String HTTP_SESSION_KEY = "net.zschech.gwt.comet.server.CometSession";
	
	/**
	 * @return the associated HTTP session
	 * @exception IllegalStateException
	 *                if this method is called on an invalidated session
	 */
	public HttpSession getHttpSession() throws IllegalStateException;
	
	/**
	 * Enqueues a message. This is equivalent to:
	 * 
	 * <code>
	 *  session.getQueue().add(message);
	 *  session.enqueued();
	 * </code>
	 * 
	 * @param message
	 * @exception IllegalStateException
	 *                if this method is called on an invalidated session
	 */
	public void enqueue(Serializable message) throws IllegalStateException;
	
	/**
	 * Call to notify the comet session that a message has been enqueued by other means than
	 * {@link CometSession#enqueue(Serializable)}.
	 * 
	 * @exception IllegalStateException
	 *                if this method is called on an invalidated session
	 */
	public void enqueued() throws IllegalStateException;
	
	/**
	 * @return the message queue
	 * @exception IllegalStateException
	 *                if this method is called on an invalidated session
	 */
	public Queue<? extends Serializable> getQueue() throws IllegalStateException;
	
	/**
	 * Invalidates the comet session
	 */
	public void invalidate();
	
	/**
	 * @return if the comet session is valid
	 */
	public boolean isValid();
}
