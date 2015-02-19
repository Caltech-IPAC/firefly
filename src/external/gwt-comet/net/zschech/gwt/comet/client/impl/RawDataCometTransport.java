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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import net.zschech.gwt.comet.client.CometException;
import net.zschech.gwt.comet.client.CometSerializer;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.StatusCodeException;

public abstract class RawDataCometTransport extends CometTransport {
	
	protected boolean expectingDisconnection;
	protected boolean disconnecting;
	
	@Override
	public void connect(int connectionCount) {
		disconnecting = false;
		expectingDisconnection = false;
	}
	
	@Override
	public void disconnect() {
		disconnecting = true;
		expectingDisconnection = true;
	}
	
	protected void disconnected() {
		if (expectingDisconnection) {
			listener.onDisconnected();
		}
		else {
			listener.onError(new CometException("Unexpected disconnection"), false);
		}
	}
	
	protected void parse(String message) {
		List<Serializable> messages = new ArrayList<Serializable>(1);
		parse(message, messages);
		if (!messages.isEmpty()) {
			listener.onMessage(messages);
		}
	}
	
	protected void parse(String message, List<Serializable> messages) {
		if (expectingDisconnection) {
			listener.onError(new CometException("Expecting disconnection but received message: " + message), true);
		}
		else if (message.isEmpty()) {
			listener.onError(new CometException("Invalid empty message received"), true);
		}
		else {
			char c = message.charAt(0);
			switch (c) {
			case '!':
				String heartbeatParameter = message.substring(1);
				try {
					listener.onConnected(Integer.parseInt(heartbeatParameter));
				}
				catch (NumberFormatException e) {
					listener.onError(new CometException("Unexpected heartbeat parameter: " + heartbeatParameter), true);
				}
				break;
			case '?':
				// clean disconnection
				expectingDisconnection = true;
				break;
			case '#':
				listener.onHeartbeat();
				break;
			case '@':
				listener.onRefresh();
				break;
			case '*':
				// ignore padding
				break;
			case '|':
				messages.add(message.substring(1));
				break;
			case ']':
				messages.add(unescape(message.substring(1)));
				break;
			case '[':
			case 'R':
			case 'r':
			case 'f':
				CometSerializer serializer = client.getSerializer();
				if (serializer == null) {
					listener.onError(new SerializationException("Can not deserialize message with no serializer: " + message), true);
				}
				else {
					try {
						messages.add(serializer.parse(message));
					}
					catch (SerializationException e) {
						listener.onError(e, true);
					}
				}
				break;
			default:
				if (c >= '0' && c <= '9') {
					// error codes
					expectingDisconnection = true;
					try {
						int statusCode;
						String statusMessage;
						int index = message.indexOf(' ');
						if (index == -1) {
							statusCode = Integer.parseInt(message);
							statusMessage = null;
						}
						else {
							statusCode = Integer.parseInt(message.substring(0, index));
							statusMessage = unescape(message.substring(index + 1));
						}
						listener.onError(new StatusCodeException(statusCode, statusMessage), false);
					}
					catch (NumberFormatException e) {
						listener.onError(new CometException("Unexpected status code: " + message), false);
					}
					break;
				}
				else {
					listener.onError(new CometException("Invalid message received: " + message), true);
				}
			}
		}
	}
	
	private String unescape(String string) {
		return string.replace("\\n", "\n").replace("\\r", "\r").replace("\\\\", "\\");
	}
}
