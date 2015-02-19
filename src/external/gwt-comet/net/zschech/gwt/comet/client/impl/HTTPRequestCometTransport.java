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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.regexp.shared.SplitResult;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.gwt.user.client.rpc.StatusCodeException;
import com.google.gwt.xhr.client.ReadyStateChangeHandler;
import com.google.gwt.xhr.client.XMLHttpRequest;

/**
 * This class uses a XmlHttpRequest and onreadystatechange events to process stream events.
 * 
 * The main issue with this implementation is that GWT does not generate RECEIVING events from its XMLHTTPRequest. The
 * implementation of XMLHTTPRequest included in this package overrides that behaviour.
 * 
 * Another issues is that the memory required for the XMLHTTPRequest's responseText constantly grows so the server
 * occasionally disconnects the client who then reestablishes the connection.
 * 
 * The protocol for this transport is a '\n' separated transport messages. The different types of transport message are
 * identified by the first character in the line as follows:
 * 
 * ! A connection message followed by the heartbeat duration as an integer
 * 
 * ? A clean server disconnection message
 * 
 * # A heartbeat message
 * 
 * * A padding message to cause the browser to start processing the stream
 * 
 * ] A string message that needs unescaping
 * 
 * | A string message that does not need unescaping
 * 
 * [ A GWT serialized object
 * 
 * R, r or f A GWT deRPC object
 * 
 * string messages are escaped for '\\' and '\n' characters as '\n' is the message separator.
 * 
 * GWT serialized object messages are escaped by GWT so do not need to be escaped by the transport
 * 
 * @author Richard Zschech
 */
public class HTTPRequestCometTransport extends RawDataCometTransport {
	
	private static final String SEPARATOR = "\n";
	private static RegExp separator;
	
	static {
		Event.addNativePreviewHandler(new NativePreviewHandler() {
			@Override
			public void onPreviewNativeEvent(NativePreviewEvent e) {
				if (e.getTypeInt() == Event.getTypeInt(KeyDownEvent.getType().getName())) {
					NativeEvent nativeEvent = e.getNativeEvent();
					if (nativeEvent.getKeyCode() == KeyCodes.KEY_ESCAPE) {
						nativeEvent.preventDefault();
					}
				}
			}
		});
		separator = RegExp.compile(SEPARATOR);
	}
	
	private XMLHttpRequest xmlHttpRequest;
	private int read;
	
	@Override
	public void connect(int connectionCount) {
		super.connect(connectionCount);
		read = 0;
		
		xmlHttpRequest = XMLHttpRequest.create();
		try {
			xmlHttpRequest.open("GET", getUrl(connectionCount));
			xmlHttpRequest.setRequestHeader("Accept", "application/comet");
			xmlHttpRequest.setOnReadyStateChange(new ReadyStateChangeHandler() {
				@Override
				public void onReadyStateChange(XMLHttpRequest request) {
					if (!disconnecting) {
						switch (request.getReadyState()) {
						case XMLHttpRequest.LOADING:
							onReceiving(request.getStatus(), request.getResponseText());
							break;
						case XMLHttpRequest.DONE:
							onLoaded(request.getStatus(), request.getResponseText());
							break;
						}
					}
				}
			});
			xmlHttpRequest.send();
		}
		catch (JavaScriptException e) {
			xmlHttpRequest = null;
			listener.onError(new RequestException(e.getMessage()), false);
		}
	}
	
	@Override
	public void disconnect() {
		super.disconnect();
		if (xmlHttpRequest != null) {
			xmlHttpRequest.clearOnReadyStateChange();
			xmlHttpRequest.abort();
			xmlHttpRequest = null;
		}
	}
	
	private void onLoaded(int statusCode, String responseText) {
		xmlHttpRequest.clearOnReadyStateChange();
		xmlHttpRequest = null;
		onReceiving(statusCode, responseText, false);
	}
	
	private void onReceiving(int statusCode, String responseText) {
		onReceiving(statusCode, responseText, true);
	}
	
	private void onReceiving(int statusCode, String responseText, boolean connected) {
		if (statusCode != Response.SC_OK) {
			if (!connected) {
				super.disconnect();
				listener.onError(new StatusCodeException(statusCode, responseText), connected);
			}
		}
		else {
			int index = responseText.lastIndexOf(SEPARATOR);
			if (index > read) {
				List<Serializable> messages = new ArrayList<Serializable>();
				
				SplitResult data = separator.split(responseText.substring(read, index), index);
				int length = data.length();
				for (int i = 0; i < length; i++) {
					if (disconnecting) {
						return;
					}
					
					String message = data.get(i);
					if (!message.isEmpty()) {
						parse(message, messages);
					}
				}
				read = index + 1;
				if (!messages.isEmpty()) {
					listener.onMessage(messages);
				}
			}
			
			if (!connected) {
				super.disconnected();
			}
		}
	}
}
