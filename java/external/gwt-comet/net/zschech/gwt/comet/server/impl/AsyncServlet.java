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
package net.zschech.gwt.comet.server.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.concurrent.ScheduledFuture;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public abstract class AsyncServlet {
	
	public static final String SERVLET_CONTEXT_KEY = AsyncServlet.class.getName();
	
	public static AsyncServlet initialize(ServletContext context) {
		synchronized (context) {
			AsyncServlet async = (AsyncServlet) context.getAttribute(SERVLET_CONTEXT_KEY);
			if (async == null) {
				String serverInfo = context.getServerInfo();
				String server = context.getInitParameter(SERVLET_CONTEXT_KEY);
				if (server == null) {
					if (serverInfo.startsWith("jetty-6") || serverInfo.startsWith("jetty/6")) {
						// e.g. jetty-6.1.x
						server = "Jetty6";
					}
					else if (serverInfo.startsWith("jetty/7")) {
						server = "Jetty7";
					}
					else if (serverInfo.startsWith("Apache Tomcat/5.5.")) {
						// e.g. Apache Tomcat/5.5.26
						server = "Catalina55";
					}
					else if (serverInfo.startsWith("Apache Tomcat/6.")) {
						// e.g. Apache Tomcat/6.0.18
						server = "Catalina60";
					}
					else if (serverInfo.startsWith("Grizzly/")) {
						server = "Grizzly";
					}
					else if (serverInfo.startsWith("GlassFish ")) {
						server = "GlassFish";
					}
					else if (serverInfo.startsWith("Google App Engine/")) {
						server = "GAE";
					}
				}
				
				if (server != null) {
					context.log("Creating " + server + " async servlet handler for server " + serverInfo);
					try {
						async = (AsyncServlet) Class.forName("net.zschech.gwt.comet.server.impl." + server + "AsyncServlet").newInstance();
					}
					catch (Throwable e) {
						context.log("Error creating " + server + " async servlet handler for server " + serverInfo + ". Falling back to default blocking async servlet handler.", e);
						async = new BlockingAsyncServlet();
					}
				}
				else {
					context.log("Creating blocking async servlet handler for server " + serverInfo);
					async = new BlockingAsyncServlet();
				}
				
				try {
					try {
						async.init(context);
					}
					catch (Throwable e) {
						context.log("Error initiating " + server + " async servlet handler for server " + serverInfo + ". Falling back to default blocking async servlet handler.", e);
						context.log("Creating blocking async servlet handler for server " + serverInfo);
						async = new BlockingAsyncServlet();
						async.init(context);
					}
					context.setAttribute(SERVLET_CONTEXT_KEY, async);
				}
				catch (ServletException e) {
					throw new Error("Error setting up async servlet");
				}
			}
			return async;
		}
	}
	
	public static void destroy(ServletContext context) {
		synchronized (context) {
			AsyncServlet async = (AsyncServlet) context.getAttribute(SERVLET_CONTEXT_KEY);
			if (async != null) {
				async.shutdown();
			}
		}
	}
	
	private ServletContext context;
	
	/**
	 * Override for web-server specific initialisation  
	 * @throws ServletException  
	 */
	protected void init(ServletContext context) throws ServletException {
		this.context = context;
	}
	
	/**
	 * Override for web-server specific shutdown  
	 */
	protected void shutdown() {
	}
	
	/**
	 * @return the servlet context associated with this AsyncServlet
	 */
	protected ServletContext getServletContext() {
		return context;
	}
	
	/**
	 * Log a message to the servlet context
	 * @see ServletContext#log(String)
	 * @param message
	 */
	protected void log(String message) {
		context.log(message);
	}
	
	/**
	 * Log a message to the servlet context
	 * @see ServletContext#log(String, Throwable)
	 * @param message
	 * @param throwable
	 */
	protected void log(String message, Throwable throwable) {
		context.log(message, throwable);
	}
	
	/**
	 * Gets a web-server specific wrapper for the servlet response output stream.
	 * @param outputStream
	 * @return the web-server specific wrapper 
	 */
	public OutputStream getOutputStream(OutputStream outputStream) {
		return outputStream;
	}
	
	public abstract Object suspend(CometServletResponseImpl response, CometSessionImpl session, HttpServletRequest request) throws IOException;
	
	public abstract void terminate(CometServletResponseImpl response, CometSessionImpl session, boolean serverInitiated, Object suspendInfo);
	
	public abstract void invalidate(CometSessionImpl session);
	
	public abstract void enqueued(CometSessionImpl session);

	/**
	 * web-server specific implementation of updating the access time of the HTTP session
	 * @param httpSession
	 * @return true if the access time was updated successfully
	 */
	protected boolean access(HttpSession httpSession) {
		return false;
	}
	
	/**
	 * web-server specific implementation of scheduling a heartbeat
	 * @param response
	 * @param session
	 * @return null if no scheduling is required
	 */
	public ScheduledFuture<?> scheduleHeartbeat(CometServletResponseImpl response, CometSessionImpl session) {
		return null;
	}
	
	/**
	 * web-server specific implementation of scheduling a session keep alive
	 * @param response
	 * @param session
	 * @return null if no scheduling is required
	 */
	public ScheduledFuture<?> scheduleSessionKeepAlive(CometServletResponseImpl response, CometSessionImpl session) {
		return null;
	}
	
	protected Object get(String path, Object object) {
		try {
			for (String property : path.split("\\.")) {
				Class<?> c = object.getClass();
				while (true) {
					try {
						Field field = c.getDeclaredField(property);
						field.setAccessible(true);
						object = field.get(object);
						break;
					}
					catch (NoSuchFieldException e) {
						c = c.getSuperclass();
						if (c == null) {
							throw e;
						}
					}
				}
			}
			return object;
		}
		catch (Exception e) {
			log("Error accessing underlying objects " + path + " from " + object.getClass().getCanonicalName(), e);
			return null;
		}
	}
}
