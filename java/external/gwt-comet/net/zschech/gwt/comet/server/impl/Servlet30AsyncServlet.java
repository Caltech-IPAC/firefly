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

import java.io.IOException;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.http.HttpServletRequest;

public class Servlet30AsyncServlet extends NonBlockingAsyncServlet {
	
	@Override
	public Object suspend(final CometServletResponseImpl response, CometSessionImpl session, HttpServletRequest request) throws IOException {
		assert Thread.holdsLock(response);
		assert session == null || !Thread.holdsLock(session);
		response.flush();
		AsyncContext asyncContext = request.startAsync();
		asyncContext.addListener(new AsyncListener() {
			
			public void onStartAsync(AsyncEvent e) throws IOException {
			}
			
			public void onComplete(AsyncEvent e) throws IOException {
			}
			
			public void onTimeout(AsyncEvent e) throws IOException {
				onError(e);
			}
			
			public void onError(AsyncEvent e) throws IOException {
				synchronized (response) {
					if (!response.isTerminated()) {
						response.setTerminated(false);
					}
				}
			}
		});
		
		asyncContext.setTimeout(Long.MAX_VALUE);
		write(response, session);
		return asyncContext;
	}
	
	@Override
	public void terminate(CometServletResponseImpl response, CometSessionImpl session, boolean serverInitiated, Object suspendInfo) {
		assert Thread.holdsLock(response);
		assert session == null || !Thread.holdsLock(session);
		
		if (serverInitiated && suspendInfo != null) {
			AsyncContext asyncContext = (AsyncContext) suspendInfo;
			asyncContext.complete();
		}
	}
	
	@Override
	public void enqueued(CometSessionImpl session) {
		final CometServletResponseImpl response = session.getResponse();
		if (response != null) {
			synchronized (response) {
				write(response, session);
			}
		}
	}
	
	@Override
	public void invalidate(CometSessionImpl session) {
		final CometServletResponseImpl response = session.getResponse();
		if (response != null) {
			response.tryTerminate();
		}
	}
	
	private void write(final CometServletResponseImpl response, final CometSessionImpl session) {
		assert Thread.holdsLock(response);
		if (!session.isEmpty()) {
			final AsyncContext asyncContext = (AsyncContext) response.getSuspendInfo();
			asyncContext.start(new Runnable() {
				@Override
				public void run() {
					synchronized (response) {
						try {
							session.writeQueue(response, true);
						}
						catch (IOException e) {
							log("Error writing session messages");
						}
						
						if (!session.isEmpty()) {
							asyncContext.start(this);
						}
					}
				}
			});
		}
	}
}
