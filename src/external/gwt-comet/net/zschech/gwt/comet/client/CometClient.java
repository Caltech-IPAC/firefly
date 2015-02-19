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
package net.zschech.gwt.comet.client;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import net.zschech.gwt.comet.client.impl.CometTransport;
import net.zschech.gwt.comet.client.impl.EventSourceCometTransport;
import net.zschech.gwt.eventsource.client.EventSource;

import com.google.gwt.core.client.Duration;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Timer;

/**
 * This class is the Comet client. It will connect to the given url and notify the given {@link CometListener} of comet
 * events. To receive GWT serialized objects supply a {@link CometSerializer} method to parse the messages.
 * 
 * The sequence of events are as follows: The application calls {@link CometClient#start()}.
 * {@link CometListener#onConnected(int)} gets called when the connection is established.
 * {@link CometListener#onMessage(List)} gets called when messages are received from the server.
 * {@link CometListener#onDisconnected()} gets called when the connection is disconnected this includes connection
 * refreshes. {@link CometListener#onError(Throwable, boolean)} gets called if there is an error with the connection.
 * 
 * The Comet client will attempt to maintain to connection when disconnections occur until the application calls
 * {@link CometClient#stop()}.
 * 
 * The server sends heart beat messages to ensure the connection is maintained and that disconnections can be detected
 * in all cases.
 * 
 * @author Richard Zschech
 */
public class CometClient {
	
	private enum RefreshState {
		CONNECTING, PRIMARY_DISCONNECTED, REFRESH_CONNECTED
	}
	
	private final String url;
	private final CometSerializer serializer;
	private final CometListener listener;
	private CometClientTransportWrapper primaryTransport;
	private CometClientTransportWrapper refreshTransport;
	
	private boolean running;
	private RefreshState refreshState;
	private List<Object> refreshQueue;
	
	private static final Object REFRESH = new Object();
	private static final Object DISCONNECT = new Object();
	
	private int connectionCount;
	
	private int connectionTimeout = 10000;
	private int reconnectionTimout = 1000;
	
	public CometClient(String url, CometListener listener) {
		this(url, null, listener);
	}
	
	public CometClient(String url, CometSerializer serializer, CometListener listener) {
		this.url = url;
		this.serializer = serializer;
		this.listener = listener;
		
		primaryTransport = new CometClientTransportWrapper();
	}
	
	public String getUrl() {
		return url;
	}
	
	public CometSerializer getSerializer() {
		return serializer;
	}
	
	public CometListener getListener() {
		return listener;
	}
	
	public void setConnectionTimeout(int connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}
	
	public int getConnectionTimeout() {
		return connectionTimeout;
	}
	
	public void setReconnectionTimout(int reconnectionTimout) {
		this.reconnectionTimout = reconnectionTimout;
	}
	
	public int getReconnectionTimout() {
		return reconnectionTimout;
	}
	
	public boolean isRunning() {
		return running;
	}
	
	public void start() {
		if (!running) {
			running = true;
			doConnect();
		}
	}
	
	public void stop() {
		if (running) {
			running = false;
			doDisconnect();
		}
	}
	
	private void doConnect() {
		primaryTransport.connect();
	}
	
	private void doDisconnect() {
		refreshState = null;
		primaryTransport.disconnect();
		if (refreshTransport != null) {
			refreshTransport.disconnect();
		}
	}
	
	private void doOnConnected(int heartbeat, CometClientTransportWrapper transport) {
		if (refreshState != null) {
			if (transport == refreshTransport) {
				if (refreshState == RefreshState.PRIMARY_DISCONNECTED) {
					doneRefresh();
				}
				else if (refreshState == RefreshState.CONNECTING) {
					refreshState = RefreshState.REFRESH_CONNECTED;
				}
				else {
					assert false;
				}
			}
			else {
				assert false;
			}
		}
		else {
			listener.onConnected(heartbeat);
		}
	}
	
	private void doOnDisconnected(CometClientTransportWrapper transport) {
		if (refreshState != null) {
			if (transport == primaryTransport) {
				if (refreshState == RefreshState.REFRESH_CONNECTED) {
					doneRefresh();
				}
				else if (refreshState == RefreshState.CONNECTING) {
					refreshState = RefreshState.PRIMARY_DISCONNECTED;
				}
				else {
					assert false;
				}
			}
			else {
				// the refresh transport has disconnected before the primary disconnected
				refreshEnqueue(DISCONNECT);
			}
		}
		else {
			listener.onDisconnected();
			
			if (running) {
				doConnect();
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private void doneRefresh() {
		refreshState = null;
		CometClientTransportWrapper temp = primaryTransport;
		primaryTransport = refreshTransport;
		refreshTransport = temp;
		
		if (refreshQueue != null) {
			for (Object object : refreshQueue) {
				if (object == REFRESH) {
					doOnRefresh(primaryTransport);
				}
				else if (object == DISCONNECT) {
					doOnDisconnected(primaryTransport);
				}
				else {
					doOnMessage((List<? extends Serializable>) object, primaryTransport);
				}
			}
			refreshQueue.clear();
		}
	}
	
	private void doOnHeartbeat(CometClientTransportWrapper transport) {
		if (transport == primaryTransport) {
			listener.onHeartbeat();
		}
	}
	
	private void doOnRefresh(CometClientTransportWrapper transport) {
		if (refreshState == null && transport == primaryTransport) {
			refreshState = RefreshState.CONNECTING;
			
			if (refreshTransport == null) {
				refreshTransport = new CometClientTransportWrapper();
			}
			refreshTransport.connect();
			
			listener.onRefresh();
		}
		else if (transport == refreshTransport) {
			refreshEnqueue(REFRESH);
		}
		else {
			assert false;
		}
	}
	
	private void refreshEnqueue(Object message) {
		if (refreshQueue == null) {
			refreshQueue = new ArrayList<Object>();
		}
		refreshQueue.add(message);
	}
	
	private void doOnError(Throwable exception, boolean connected, CometClientTransportWrapper transport) {
		if (connected) {
			doDisconnect();
		}
		
		listener.onError(exception, connected);
		
		if (running) {
			primaryTransport.reconnectionTimer.schedule(reconnectionTimout);
		}
	}
	
	private void doOnMessage(List<? extends Serializable> messages, CometClientTransportWrapper transport) {
		if (transport == primaryTransport) {
			listener.onMessage(messages);
		}
		else {
			refreshEnqueue(messages);
		}
	}
	
	private class CometClientTransportWrapper implements CometListener {
		
		private final CometTransport transport;
		
		private final Timer connectionTimer = createConnectionTimer();
		private final Timer reconnectionTimer = createReconnectionTimer();
		private final Timer heartbeatTimer = createHeartbeatTimer();
		
		private int heartbeatTimeout;
		private double lastReceivedTime;
		
		public CometClientTransportWrapper() {
			if (EventSource.isSupported()) {
				transport = new EventSourceCometTransport();
			}
			else {
				transport = GWT.create(CometTransport.class);
			}
			transport.initiate(CometClient.this, this);
		}
		
		public void connect() {
			connectionTimer.schedule(connectionTimeout);
			transport.connect(++connectionCount);
		}
		
		public void disconnect() {
			cancelTimers();
			transport.disconnect();
		}
		
		@Override
		public void onConnected(int heartbeat) {
			heartbeatTimeout = heartbeat + connectionTimeout;
			lastReceivedTime = Duration.currentTimeMillis();
			
			cancelTimers();
			heartbeatTimer.schedule(heartbeatTimeout);
			
			doOnConnected(heartbeat, this);
		}
		
		@Override
		public void onDisconnected() {
			cancelTimers();
			doOnDisconnected(this);
		}
		
		@Override
		public void onError(Throwable exception, boolean connected) {
			cancelTimers();
			doOnError(exception, connected, this);
		}
		
		@Override
		public void onHeartbeat() {
			lastReceivedTime = Duration.currentTimeMillis();
			doOnHeartbeat(this);
		}
		
		@Override
		public void onRefresh() {
			lastReceivedTime = Duration.currentTimeMillis();
			doOnRefresh(this);
		}
		
		@Override
		public void onMessage(List<? extends Serializable> messages) {
			lastReceivedTime = Duration.currentTimeMillis();
			doOnMessage(messages, this);
		}
		
		private void cancelTimers() {
			connectionTimer.cancel();
			reconnectionTimer.cancel();
			heartbeatTimer.cancel();
		}
		
		private Timer createConnectionTimer() {
			return new Timer() {
				@Override
				public void run() {
					doDisconnect();
					doOnError(new CometTimeoutException(url, connectionTimeout), false, CometClientTransportWrapper.this);
				}
			};
		}
		
		private Timer createHeartbeatTimer() {
			return new Timer() {
				@Override
				public void run() {
					double currentTimeMillis = Duration.currentTimeMillis();
					double difference = currentTimeMillis - lastReceivedTime;
					if (difference >= heartbeatTimeout) {
						doDisconnect();
						doOnError(new CometException("Heartbeat failed"), false, CometClientTransportWrapper.this);
					}
					else {
						// we have received a message since the timer was
						// schedule so reschedule it.
						schedule(heartbeatTimeout - (int) difference);
					}
				}
			};
		}
		
		private Timer createReconnectionTimer() {
			return new Timer() {
				@Override
				public void run() {
					if (running) {
						doConnect();
					}
				}
			};
		}
	}
}
