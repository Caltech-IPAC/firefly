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

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import net.zschech.gwt.comet.server.impl.AsyncServlet;

/**
 * This ServletContextListener initializes and shuts down comet related resources associated with an ApplicationContext.
 * With out this listener the comet related resources will be initialized when first used and will not be shut down
 * cleanly.
 * 
 * Configure it in your web.xml as follows:
 * 
 * <pre>
 * <listener>
 *   <listener-class>net.zschech.gwt.comet.server.CometServletContextListener</listener-class>
 * </listener>
 * </pre>
 * 
 * @author Richard Zschech
 */
public class CometServletContextListener implements ServletContextListener {
	
	@Override
	public void contextInitialized(ServletContextEvent e) {
		AsyncServlet.initialize(e.getServletContext());
	}
	
	@Override
	public void contextDestroyed(ServletContextEvent e) {
		AsyncServlet.destroy(e.getServletContext());
	}
}
