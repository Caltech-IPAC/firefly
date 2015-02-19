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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;

/**
 * An extension of {@link BlockingAsyncServlet} for Jetty.
 * 
 * This extension improves on the default session keep alive strategy, refreshing the connection just before the session
 * expires, by updating the session managers last access time when ever sending data down the Comet connection
 * 
 * @author Richard Zschech
 */
public class Jetty7AsyncServlet extends BlockingAsyncServlet {
	
	private Object sessionManager;
	private Method accessMethod;
	
	@Override
	public void init(ServletContext context) throws ServletException {
		super.init(context);
		try {
			sessionManager = get("this$0._sessionHandler._sessionManager", context);
			if (sessionManager == null) {
				throw new ServletException("Error getting session manager");
			}
			accessMethod = sessionManager.getClass().getMethod("access", HttpSession.class, Boolean.TYPE);
		}
		catch (SecurityException e) {
			throw new ServletException(e);
		}
		catch (NoSuchMethodException e) {
			throw new ServletException(e);
		}
	}
	
	@Override
	protected boolean access(HttpSession httpSession) {
		try {
			accessMethod.invoke(sessionManager, httpSession, false);
			return true;
		}
		catch (IllegalArgumentException e) {
			log("Error updating session last access time", e);
			return false;
		}
		catch (IllegalAccessException e) {
			log("Error updating session last access time", e);
			return false;
		}
		catch (InvocationTargetException e) {
			log("Error updating session last access time", e);
			return false;
		}
	}
}
