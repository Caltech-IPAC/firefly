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
package net.zschech.gwt.comet.client.impl;

import net.zschech.gwt.comet.client.CometClient;
import net.zschech.gwt.comet.client.CometException;
import net.zschech.gwt.comet.client.CometListener;
import net.zschech.gwt.eventsource.client.ErrorHandler;
import net.zschech.gwt.eventsource.client.EventSource;
import net.zschech.gwt.eventsource.client.MessageEvent;
import net.zschech.gwt.eventsource.client.MessageHandler;
import net.zschech.gwt.eventsource.client.OpenHandler;

/**
 * @author Richard Zschech
 */
public class EventSourceCometTransport extends RawDataCometTransport {
	
	private EventSource eventSource;
	private EventSourceHandler handler;
	private boolean connected;
	
	@Override
	public void initiate(CometClient client, CometListener listener) {
		super.initiate(client, listener);
	}
	
	@Override
	public void connect(int connectionCount) {
		super.connect(connectionCount);
		handler = new EventSourceHandler();
		eventSource = EventSource.create(getUrl(connectionCount));
		eventSource.setOnOpen(handler);
		eventSource.setOnError(handler);
		eventSource.setOnMessage(handler);
	}
	
	@Override
	public void disconnect() {
		super.disconnect();
		if (eventSource != null) {
			eventSource.close();
			eventSource = null;
			connected = false;
		}
	}
	
	public class EventSourceHandler implements OpenHandler, ErrorHandler, MessageHandler {
		
		@Override
		public void onOpen(EventSource eventSource) {
		}
		
		@Override
		public void onError(EventSource eventSource) {
			if (expectingDisconnection) {
				if (connected) {
					disconnected();
				}
			}
			else {
				listener.onError(new CometException("EventSource error"), connected);
			}
		}
		
		@Override
		public void onMessage(EventSource eventSource, MessageEvent event) {
			connected = true;
			parse(event.getData());
		}
	}
}
