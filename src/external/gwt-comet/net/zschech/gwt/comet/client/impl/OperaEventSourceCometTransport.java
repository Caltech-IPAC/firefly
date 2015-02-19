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
package net.zschech.gwt.comet.client.impl;

import net.zschech.gwt.comet.client.CometClient;
import net.zschech.gwt.comet.client.CometListener;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;

/**
 * This class uses Opera's event-source element to stream events.<br/>
 * http://my.opera.com/WebApplications/blog/show.dml/438711
 * 
 * The main issue with Opera's implementation is that we can't detect connection events. To support this three event
 * listeners are setup: one "s" for string messages, one "o" for the GWT serialized object messages, and the other "c"
 * for connection events. The server sends the event "c" as soon as the connection is established and "d" when the
 * connection is terminated. A connection timer is setup to detect initial connection errors. To detect subsequent
 * connection failure it also sends a heart beat events "h" when no messages have been sent for a specified heart beat
 * interval.
 * 
 * @author Richard Zschech
 */
public class OperaEventSourceCometTransport extends RawDataCometTransport {
	
	private Element eventSource;
	private boolean connected;
	
	@Override
	public void initiate(CometClient client, CometListener listener) {
		super.initiate(client, listener);
	}
	
	@Override
	public void connect(int connectionCount) {
		eventSource = createEventSource(this);
		DOM.setElementAttribute(eventSource, "src", getUrl(connectionCount));
	}
	
	@Override
	public void disconnect() {
		if (eventSource != null) {
			DOM.setElementAttribute(eventSource, "src", "");
			eventSource = null;
			connected = false;
		}
	}
	
	private native Element createEventSource(OperaEventSourceCometTransport client) /*-{
		var eventSource = document.createElement("event-source");

		var eventHandler = $entry(function(event) {
			client.@net.zschech.gwt.comet.client.impl.OperaEventSourceCometTransport::onEvent(Ljava/lang/String;)(event.data);
		});

		eventSource.addEventListener("e", eventHandler, false);

		return eventSource;
	}-*/;
	
	private void onEvent(String message) {
		connected = true;
		parse(message);
		if (expectingDisconnection) {
			disconnect();
			disconnected();
		}
	}
}
