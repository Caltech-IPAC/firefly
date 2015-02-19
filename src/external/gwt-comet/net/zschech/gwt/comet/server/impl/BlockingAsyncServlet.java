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

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

/**
 * This AsyncServlet implementation blocks the HTTP request processing thread.
 * 
 * It does not generate notifications for client disconnection and therefore must wait for a heartbeat send attempt to
 * detect client disconnection :-(
 * 
 * @author Richard Zschech
 */
public class BlockingAsyncServlet extends AsyncServlet {
	
	@Override
	public void init(ServletContext context) throws ServletException {
		super.init(context);
	}
	
	@Override
	public Object suspend(CometServletResponseImpl response, CometSessionImpl session, HttpServletRequest request) throws IOException {
		assert !Thread.holdsLock(response);
		
		if (session == null) {
			try {
				synchronized (response) {
					while (!response.isTerminated()) {
						long heartBeatTime = response.getHeartbeatScheduleTime();
						if (heartBeatTime <= 0) {
							try {
								response.heartbeat();
							}
							catch (IOException e) {
								log("Error sending heartbeat", e);
								return null;
							}
							heartBeatTime = response.getHeartbeatScheduleTime();
						}
						response.wait(heartBeatTime);
					}
				}
			}
			catch (InterruptedException e) {
				log("Interrupted waiting for messages", e);
				response.tryTerminate();
			}
		}
		else {
			assert !Thread.holdsLock(session);
			
			try {
				try {
					synchronized (response) {
						response.setProcessing(true);
						
						while (session.isValid() && !response.isTerminated()) {
							long sessionKeepAliveTime = session.getKeepAliveScheduleTime();
							if (sessionKeepAliveTime <= 0) {
								if (access(session.getHttpSession())) {
									session.setLastAccessedTime();
									sessionKeepAliveTime = session.getKeepAliveScheduleTime();
								}
								else {
									response.tryTerminate();
									break;
								}
							}
							
							if (session.isEmpty()) {
								long heartBeatTime = response.getHeartbeatScheduleTime();
								if (heartBeatTime <= 0) {
									try {
										response.heartbeat();
									}
									catch (IOException e) {
										log("Error sending heartbeat", e);
										break;
									}
									heartBeatTime = response.getHeartbeat();
								}
								
								response.setProcessing(false);
								response.wait(Math.min(sessionKeepAliveTime, heartBeatTime));
								response.setProcessing(true);
							}
							else {
								session.writeQueue(response, true);
							}
						}
					}
				}
				catch (InterruptedException e) {
					log("Interrupted waiting for messages", e);
					response.tryTerminate();
				}
			}
			catch (IOException e) {
				log("Error writing messages", e);
			}
			
			if (!session.isValid() && !response.isTerminated()) {
				response.tryTerminate();
			}
		}
		return null;
	}
	
	@Override
	public void terminate(CometServletResponseImpl response, final CometSessionImpl session, boolean serverInitiated, Object suspendInfo) {
		assert Thread.holdsLock(response);
		response.notifyAll();
	}
	
	@Override
	public void invalidate(CometSessionImpl session) {
		CometServletResponseImpl response = session.getResponse();
		if (response != null) {
			synchronized (response) {
				response.notifyAll();
			}
		}
	}
	
	@Override
	public void enqueued(CometSessionImpl session) {
		CometServletResponseImpl response = session.getResponse();
		if (response != null) {
			if (response.setProcessing(true)) {
				synchronized (response) {
					response.notifyAll();
				}
			}
		}
	}
}
