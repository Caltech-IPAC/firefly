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
package net.zschech.gwt.comettest.server;

import java.io.IOException;
import java.io.InterruptedIOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import net.zschech.gwt.comet.server.CometServlet;
import net.zschech.gwt.comet.server.CometServletResponse;
import net.zschech.gwt.comet.server.CometSession;

public class ConnectionTestServlet extends CometServlet {
	
	@Override
	protected void doComet(final CometServletResponse cometResponse) throws ServletException, IOException {
		final CometSession cometSession = cometResponse.getSession(false);
		
		HttpServletRequest request = cometResponse.getRequest();
		final int delay = Integer.parseInt(request.getParameter("delay"));
		new Thread() {
			public void run() {
				try {
					try {
						synchronized (cometResponse) {
							cometResponse.wait(delay);
						}
					}
					catch (InterruptedException e) {
						throw new InterruptedIOException();
					}
					
					if (cometSession != null) {
						cometSession.invalidate();
					}
					else {
						if (!cometResponse.isTerminated()) {
							log("Sending terminate");
							cometResponse.terminate();
						}
					}
				}
				catch (IOException e) {
					log("Error writing data", e);
				}
			}
		}.start();
	}
	
	@Override
	public void cometTerminated(CometServletResponse cometResponse, boolean serverInitiated) {
		log("Comet terminated by " + (serverInitiated ? "server" : "client"));
		synchronized (cometResponse) {
			cometResponse.notifyAll();
		}
	}
}
