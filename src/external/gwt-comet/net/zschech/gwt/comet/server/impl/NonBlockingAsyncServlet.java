/*
 * Copyright 2010 Richard Zschech.
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

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

/**
 * A non-blocking (does not block HTTP request threads) implementation for AsyncServlet.
 * 
 * Requires a scheduler for sending heart beats and keeping sessions alive.
 * 
 * @author Richard Zschech
 */
public abstract class NonBlockingAsyncServlet extends AsyncServlet {
	
	private ScheduledExecutorService scheduledExecutor;
	
	@Override
	protected void init(ServletContext context) throws ServletException {
		super.init(context);
		
		scheduledExecutor = new RemoveOnCancelScheduledThreadPoolExecutor(1, new ThreadFactory() {
			@Override
			public Thread newThread(Runnable runnable) {
				String name = getServletContext().getServletContextName();
				if (name == null || name.isEmpty()) {
					name = getServletContext().getContextPath();
				}
				return new Thread(runnable, "gwt-comet " + name);
			}
		});
	}
	
	@Override
	protected void shutdown() {
		scheduledExecutor.shutdown();
	}
	
	protected ScheduledExecutorService getScheduledExecutor() {
		return scheduledExecutor;
	}

	@Override
	public ScheduledFuture<?> scheduleHeartbeat(final CometServletResponseImpl response, CometSessionImpl session) {
		assert Thread.holdsLock(response);
		return scheduledExecutor.schedule(new Runnable() {
			@Override
			public void run() {
				response.tryHeartbeat();
			}
		}, response.getHeartbeat(), TimeUnit.MILLISECONDS);
	}
	
	@Override
	public ScheduledFuture<?> scheduleSessionKeepAlive(final CometServletResponseImpl response, final CometSessionImpl session) {
		assert Thread.holdsLock(response);
		try {
			long keepAliveTime = session.getKeepAliveScheduleTime();
			if (keepAliveTime == Long.MAX_VALUE) {
				return null;
			}
			else {
				if (keepAliveTime <= 0) {
					if (!access(session.getHttpSession())) {
						response.tryTerminate();							
						return null;
					}
				}
				
				return scheduledExecutor.schedule(new Runnable() {
					@Override
					public void run() {
						if (!access(session.getHttpSession())) {
							response.tryTerminate();							
						}
						session.setLastAccessedTime();
						response.scheduleSessionKeepAlive();
					}
				}, keepAliveTime, TimeUnit.MILLISECONDS);
			}
		}
		catch (IllegalStateException e) {
			// the session has been invalidated
			response.tryTerminate();
			return null;
		}
	}
}
