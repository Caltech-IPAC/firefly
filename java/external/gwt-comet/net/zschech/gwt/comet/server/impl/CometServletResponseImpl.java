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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.ServletResponse;
import javax.servlet.ServletResponseWrapper;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.zschech.gwt.comet.server.CometServlet;
import net.zschech.gwt.comet.server.CometServletResponse;
import net.zschech.gwt.comet.server.CometSession;
import net.zschech.gwt.comet.server.deflate.DeflaterOutputStream;

import com.google.gwt.rpc.server.ClientOracle;
import com.google.gwt.rpc.server.RPC;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.server.rpc.SerializationPolicy;
import com.google.gwt.user.server.rpc.impl.ServerSerializationStreamWriter;

public abstract class CometServletResponseImpl implements CometServletResponse {
	
	private final HttpServletRequest request;
	private final HttpServletResponse response;
	private CometSessionImpl session;
	
	private final SerializationPolicy serializationPolicy;
	private final ClientOracle clientOracle;
	private final CometServlet servlet;
	private final AsyncServlet async;
	private final int heartbeat;
	
	private OutputStream asyncOutputStream;
	protected Writer writer;
	
	private boolean terminated;
	private boolean suspended;
	private AtomicBoolean processing = new AtomicBoolean();
	
	private Object suspendInfo;
	private volatile long lastWriteTime;
	private ScheduledFuture<?> heartbeatFuture;
	private ScheduledFuture<?> sessionKeepAliveFuture;
	
	protected CometServletResponseImpl(HttpServletRequest request, HttpServletResponse response, SerializationPolicy serializationPolicy, ClientOracle clientOracle, CometServlet servlet, AsyncServlet async, int heartbeat) {
		this.request = request;
		this.response = response;
		this.serializationPolicy = serializationPolicy;
		this.clientOracle = clientOracle;
		this.servlet = servlet;
		this.async = async;
		this.heartbeat = heartbeat;
	}
	
	@Override
	public int getHeartbeat() {
		return heartbeat;
	}
	
	@Override
	public synchronized boolean isTerminated() {
		return terminated;
	}
	
	protected OutputStream getOutputStream(OutputStream outputStream) {
		return outputStream;
	}
	
	protected OutputStream getAsyncOutputStream() {
		return asyncOutputStream;
	}
	
	protected boolean isDeRPC() {
		return clientOracle != null;
	}
	
	@Override
	public HttpServletRequest getRequest() {
		return request;
	}
	
	@Override
	public HttpServletResponse getResponse() {
		return response;
	}
	
	protected HttpServletResponse getUnwrappedResponse() {
		ServletResponse result = response;
		while (result instanceof ServletResponseWrapper) {
			result = ((ServletResponseWrapper) result).getResponse();
		}
		return (HttpServletResponse) result;
	}
	
	CometSessionImpl getSessionImpl() {
		return session;
	}
	
	@Override
	public CometSession getSession() {
		return getSession(true);
	}
	
	@Override
	public synchronized CometSession getSession(boolean create) {
		if (suspended) {
			throw new IllegalStateException("CometSession can not be accessed after the CometServletResponse has been suspended.");
		}
		if (session != null) {
			return session;
		}
		HttpSession httpSession = getRequest().getSession(create);
		if (httpSession == null) {
			return null;
		}
		
		session = (CometSessionImpl) CometServlet.getCometSession(httpSession, create);
		if (create) {
			session.setLastAccessedTime();
			scheduleSessionKeepAlive();
			session.setResponse(this);
		}
		return session;
	}
	
	boolean hasSession() {
		assert Thread.holdsLock(this);
		return session != null;
	}
	
	synchronized void scheduleSessionKeepAlive() {
		if (sessionKeepAliveFuture != null) {
			sessionKeepAliveFuture.cancel(false);
		}
		sessionKeepAliveFuture = async.scheduleSessionKeepAlive(this, session);
	}
	
	void scheduleHeartbeat() {
		assert Thread.holdsLock(this);
		lastWriteTime = System.currentTimeMillis();
		if (heartbeatFuture != null) {
			heartbeatFuture.cancel(false);
		}
		heartbeatFuture = async.scheduleHeartbeat(this, session);
	}
	
	@Override
	public void sendError(int statusCode) throws IOException {
		sendError(statusCode, null);
	}
	
	@Override
	public synchronized void sendError(int statusCode, String message) throws IOException {
		if (suspended) {
			throw new IllegalStateException("sendError can not be accessed after the CometServletResponse has been suspended.");
		}
		try {
			response.reset();
			setupHeaders(response);
			
			OutputStream outputStream = response.getOutputStream();
			writer = new OutputStreamWriter(outputStream, "UTF-8");
			
			doSendError(statusCode, message);
		}
		catch (IllegalStateException e) {
			servlet.log("Error resetting response to send error: " + e.getMessage());
		}
		finally {
			setTerminated(true);
		}
	}
	
	public synchronized void initiate() throws IOException {
		setupHeaders(response);
		
		OutputStream outputStream = response.getOutputStream();
		asyncOutputStream = outputStream = async.getOutputStream(outputStream);
		
		String acceptEncoding = request.getHeader("Accept-Encoding");
		if (acceptEncoding != null && acceptEncoding.contains("deflate")) {
			response.setHeader("Content-Encoding", "deflate");
			outputStream = new DeflaterOutputStream(outputStream);
		}
		
		writer = new OutputStreamWriter(getOutputStream(outputStream), "UTF-8");
		
		scheduleHeartbeat();
		getSession(false);
		if (session != null) {
			session.setLastAccessedTime();
			scheduleSessionKeepAlive();
			
			// This must be as the last step of initialise because after this
			// response is set in the session
			// it must be fully setup as it can be immediately terminated by the
			// next response
			CometServletResponseImpl prevResponse = session.setResponse(this);
			if (prevResponse != null) {
				prevResponse.tryTerminate();
			}
		}
		
		doInitiate(heartbeat);
	}
	
	protected void setupHeaders(HttpServletResponse response) {
		response.setHeader("Cache-Control", "no-cache");
	}
	
	public void suspend() {
		try {
			CometSessionImpl s;
			synchronized (this) {
				if (terminated) {
					return;
				}
				
				doSuspend();
				
				s = session;
				boolean flush;
				if (s == null) {
					flush = true;
				}
				else {
					flush = s.isEmpty();
				}
				
				if (flush) {
					flush();
				}
				
				suspended = true;
				
				if (!(async instanceof BlockingAsyncServlet)) {
					suspendInfo = async.suspend(this, s, request);
				}
			}
			
			if (async instanceof BlockingAsyncServlet) {
				async.suspend(this, s, request);
			}
		}
		catch (IOException e) {
			servlet.log("Error suspending response", e);
			synchronized (this) {
				suspended = false;
				setTerminated(false);
			}
		}
	}
	
	synchronized Object getSuspendInfo() {
		return suspendInfo;
	}
	
	@Override
	public synchronized void terminate() throws IOException {
		if (!terminated) {
			try {
				doTerminate();
				flush();
			}
			finally {
				setTerminated(true);
			}
		}
	}
	
	void tryTerminate() {
		try {
			terminate();
		}
		catch (IOException e) {
			servlet.log("Error terminating response", e);
		}
	}
	
	@Override
	public void write(Serializable message) throws IOException {
		write(Collections.singletonList(message), true);
	}
	
	@Override
	public void write(Serializable message, boolean flush) throws IOException {
		write(Collections.singletonList(message), flush);
	}
	
	@Override
	public void write(List<? extends Serializable> messages) throws IOException {
		write(messages, true);
	}
	
	@Override
	public synchronized void write(List<? extends Serializable> messages, boolean flush) throws IOException {
		if (terminated) {
			throw new IOException("CometServletResponse terminated");
		}
		try {
			doWrite(messages);
			if (flush) {
				flush();
			}
			scheduleHeartbeat();
		}
		catch (IOException e) {
			setTerminated(false);
			throw e;
		}
	}
	
	@Override
	public synchronized void heartbeat() throws IOException {
		if (!terminated) {
			try {
				doHeartbeat();
				flush();
				scheduleHeartbeat();
			}
			catch (IOException e) {
				setTerminated(false);
				throw e;
			}
		}
	}
	
	void tryHeartbeat() {
		try {
			heartbeat();
		}
		catch (IOException e) {
			servlet.log("Error sending heartbeat", e);
		}
	}
	
	void flush() throws IOException {
		assert Thread.holdsLock(this);
		writer.flush();
	}
	
	void setTerminated(boolean serverInitiated) {
		assert Thread.holdsLock(this);
		
		terminated = true;
		if (heartbeatFuture != null) {
			heartbeatFuture.cancel(false);
			heartbeatFuture = null;
		}
		
		if (serverInitiated) {
			try {
				writer.close();
			}
			catch (IOException e) {
				servlet.log("Error closing connection", e);
			}
		}
		
		if (session != null) {
			session.clearResponse(this);
			if (sessionKeepAliveFuture != null) {
				sessionKeepAliveFuture.cancel(false);
			}
		}
		
		if (suspended) {
			async.terminate(this, session, serverInitiated, suspendInfo);
		}
		
		servlet.cometTerminated(this, serverInitiated);
	}
	
	long getHeartbeatScheduleTime() throws IllegalStateException {
		return heartbeat - (System.currentTimeMillis() - lastWriteTime);
	}
	
	protected abstract void doInitiate(int heartbeat) throws IOException;
	
	protected abstract void doSendError(int statusCode, String message) throws IOException;
	
	protected abstract void doSuspend() throws IOException;
	
	protected abstract void doWrite(List<? extends Serializable> messages) throws IOException;
	
	protected abstract void doHeartbeat() throws IOException;
	
	protected abstract void doTerminate() throws IOException;
	
	protected String serialize(Serializable message) throws NotSerializableException, UnsupportedEncodingException {
		try {
			if (clientOracle == null) {
				ServerSerializationStreamWriter streamWriter = new ServerSerializationStreamWriter(serializationPolicy);
				streamWriter.prepareToWrite();
				streamWriter.writeObject(message);
				return streamWriter.toString();
			}
			else {
				ByteArrayOutputStream result = new ByteArrayOutputStream();
				RPC.streamResponseForSuccess(clientOracle, result, message);
				return new String(result.toByteArray(), "UTF-8");
			}
		}
		catch (SerializationException e) {
			throw new NotSerializableException("Unable to serialize object, message: " + e.getMessage());
		}
	}
	
	boolean setProcessing(boolean processing) {
		return this.processing.compareAndSet(!processing, processing);
	}
}
