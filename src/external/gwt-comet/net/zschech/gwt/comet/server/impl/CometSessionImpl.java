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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.http.HttpSession;

import net.zschech.gwt.comet.server.CometSession;

public class CometSessionImpl implements CometSession {
	
	private static final int INITIAL_WINDOW_SIZE = 1024 * 2;
	private static final int MIN_WINDOW_SIZE = 1024;
	private static final int MAX_WINDOW_SIZE = 1024 * 1024;
	private static final int IE_MAX_WINDOW_SIZE = 256 * 1024;
	private static final int WINDOW_SIZE_MULTIPLIER = 2;
	private static final double TERMINATE_LENGTH_MULTIPLIER = 1.1;
	private static final int REFRESH_LATENCY_CUTOFF = 1000;
	
	private static final long SESSION_KEEP_ALIVE_BUFFER = 10000;
	
	private final HttpSession httpSession;
	private final Queue<Serializable> queue;
	private final AsyncServlet async;
	private final AtomicBoolean valid;
	private final AtomicReference<CometServletResponseImpl> response;
	
	private final AtomicBoolean refreshing;
	private volatile long refreshSentTime;
	private volatile int windowSize = INITIAL_WINDOW_SIZE;
	
	private volatile long lastAccessedTime;
	
	public CometSessionImpl(HttpSession httpSession, Queue<Serializable> queue, AsyncServlet async) {
		this.httpSession = httpSession;
		this.queue = queue;
		this.async = async;
		this.valid = new AtomicBoolean(true);
		this.response = new AtomicReference<CometServletResponseImpl>();
		this.refreshing = new AtomicBoolean(false);
	}
	
	private void ensureValid() {
		if (!valid.get()) {
			throw new IllegalStateException("CometSession has been invalidated");
		}
	}
	
	@Override
	public HttpSession getHttpSession() {
		ensureValid();
		return httpSession;
	}
	
	@Override
	public void enqueue(Serializable message) {
		ensureValid();
		queue.add(message);
		async.enqueued(this);
	}
	
	@Override
	public void enqueued() {
		ensureValid();
		async.enqueued(this);
	}
	
	@Override
	public Queue<? extends Serializable> getQueue() {
		return queue;
	}
	
	@Override
	public void invalidate() {
		if (valid.compareAndSet(true, false)) {
			async.invalidate(this);
			try {
				httpSession.removeAttribute(HTTP_SESSION_KEY);
			}
			catch (IllegalStateException e) {
				// HttpSession already invalidated
			}
			
			CometServletResponseImpl prevResponse = response.getAndSet(null);
			if (prevResponse != null) {
				prevResponse.tryTerminate();
			}
		}
	}
	
	@Override
	public boolean isValid() {
		return valid.get();
	}
	
	boolean isEmpty() {
		return isValid() && queue.isEmpty();
	}
	
	CometServletResponseImpl setResponse(CometServletResponseImpl response) {
		refreshing.set(false);
		
		if (refreshSentTime != 0) {
			long currentTime = System.currentTimeMillis();
			long refreshTime = currentTime - refreshSentTime;
			
			if (refreshTime > REFRESH_LATENCY_CUTOFF) {
				windowSize = Math.max(windowSize / WINDOW_SIZE_MULTIPLIER, MIN_WINDOW_SIZE);
			}
			else {
				windowSize = Math.min(windowSize * WINDOW_SIZE_MULTIPLIER, response instanceof IEHTMLFileCometServletResponse ? IE_MAX_WINDOW_SIZE : MAX_WINDOW_SIZE);
			}
		}
		
		return this.response.getAndSet(response);
	}
	
	boolean clearResponse(CometServletResponseImpl response) {
		return this.response.compareAndSet(response, null);
	}
	
	CometServletResponseImpl getResponse() {
		return response.get();
	}
	
	boolean setRefresh() {
		boolean result = refreshing.compareAndSet(false, true);
		if (result) {
			refreshSentTime = System.currentTimeMillis();
		}
		return result;
	}
	
	boolean isAndSetOverRefreshLength(int count) {
		return count > windowSize && setRefresh();
	}
	
	boolean isOverTerminateLength(int count) {
		return count > windowSize * TERMINATE_LENGTH_MULTIPLIER;
	}
	
	/**
	 * @param flushIfEmpty
	 *            flush if the queue is empty
	 * @throws IOException
	 */
	void writeQueue(CometServletResponseImpl response, boolean flushIfEmpty) throws IOException {
		assert Thread.holdsLock(response);
		
		int batchSize = 10;
		List<Serializable> messages = new ArrayList<Serializable>(batchSize);
		
		Serializable message = queue.remove();
		messages.add(message);
		for (int i = 0; i < batchSize - 1; i++) {
			message = queue.poll();
			if (message == null) {
				break;
			}
			messages.add(message);
		}
		
		response.write(messages, flushIfEmpty && queue.isEmpty());
	}
	
	long getKeepAliveScheduleTime() throws IllegalStateException {
		int maxInactiveInterval = httpSession.getMaxInactiveInterval();
		if (maxInactiveInterval < 0) {
			return Long.MAX_VALUE;
		}
		long lastAccessedTime = Math.max(this.lastAccessedTime, httpSession.getLastAccessedTime());
		return (maxInactiveInterval * 1000) - (System.currentTimeMillis() - lastAccessedTime) - SESSION_KEEP_ALIVE_BUFFER;
	}
	
	void setLastAccessedTime() {
		this.lastAccessedTime = System.currentTimeMillis();
	}
	
	long getLastAccessedTime() {
		return lastAccessedTime;
	}
}
