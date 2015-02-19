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

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

/**
 * This HttpSessionListener invalidates CometSessions when their associated HttpSessions are destroyed. Also it can
 * create CometSession when a HttpSession is created if you configure the context parameter
 * "net.zschech.gwt.comet.server.auto.create.comet.session" to "true".
 * 
 * Configure it in your web.xml as follows:
 * 
 * <pre>
 * <listener>
 *   <listener-class>net.zschech.gwt.comet.server.CometHttpSessionListener</listener-class>
 * </listener>
 * <context-param>
 *   <param-name>net.zschech.gwt.comet.server.auto.create.comet.session.with.http.session</param-name>
 *   <param-value>true</param-value>
 * </context-param>
 * </pre>
 * 
 * @author Richard Zschech
 */
public class CometHttpSessionListener implements HttpSessionListener {
	
	public static final String AUTO_CREATE_COMET_SESSION = "net.zschech.gwt.comet.server.auto.create.comet.session.with.http.session";
	
	@Override
	public void sessionCreated(HttpSessionEvent e) {
		HttpSession httpSession = e.getSession();
		String autoCreate = httpSession.getServletContext().getInitParameter(AUTO_CREATE_COMET_SESSION);
		if ("true".equals(autoCreate)) {
			CometServlet.getCometSession(httpSession);
		}
	}
	
	@Override
	public void sessionDestroyed(HttpSessionEvent e) {
		HttpSession httpSession = e.getSession();
		CometSession cometSession = CometServlet.getCometSession(httpSession, false);
		if (cometSession != null) {
			cometSession.invalidate();
		}
	}
}
