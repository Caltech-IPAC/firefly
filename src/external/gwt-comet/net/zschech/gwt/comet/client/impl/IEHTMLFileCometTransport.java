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

import net.zschech.gwt.comet.client.CometClient;
import net.zschech.gwt.comet.client.CometException;
import net.zschech.gwt.comet.client.CometListener;
import net.zschech.gwt.comet.client.CometSerializer;

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.BodyElement;
import com.google.gwt.dom.client.IFrameElement;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.StatusCodeException;

/**
 * This class uses IE's ActiveX "htmlfile" with an embedded iframe to stream events.
 * http://cometdaily.com/2007/11/18/ie-activexhtmlfile-transport-part-ii/
 * 
 * The main issue with this implementation is that we can't detect initial connection errors. A connection timer is
 * setup to detect this.
 * 
 * Another issues is that the memory required for the iframe constantly grows so the server occasionally disconnects the
 * client who then reestablishes the connection with an empty iframe. To alleviate the issue the client removes script
 * tags as the messages in them have been processed.
 * 
 * The protocol for this transport is a stream of <script> tags with function calls to this transports callbacks as
 * follows:
 * 
 * c(heartbeat) A connection message with the heartbeat duration as an integer
 * 
 * e(error) An error message with the error code
 * 
 * h() A heartbeat message
 * 
 * r() A refresh message
 * 
 * m(string...) string and gwt serialized object messages
 * 
 * string and gwt serialized object messages are Java Script escaped
 * 
 * @author Richard Zschech
 */
public class IEHTMLFileCometTransport extends CometTransport {
	
	private String domain;
	private IFrameElement iframe;
	private BodyElement body;
	private boolean connected;
	private boolean expectingDisconnection;
	
	@Override
	public void initiate(CometClient client, CometListener listener) {
		super.initiate(client, listener);
		domain = getDomain(getDocumentDomain(), client.getUrl());
		
		StringBuilder html = new StringBuilder("<html>");
		if (domain != null) {
			html.append("<script>document.domain='").append(domain).append("'</script>");
		}
		html.append("<iframe src=''></iframe></html>");
		
		iframe = createIFrame(this, html.toString());
	}
	
	@Override
	public void connect(int connectionCount) {
		expectingDisconnection = false;
		String url = getUrl(connectionCount);
		if (domain != null) {
			url += "&d=" + domain;
		}
		iframe.setSrc(url);
	}
	
	@Override
	public void disconnect() {
		// TODO this does not seem to close the connection immediately.
		expectingDisconnection = true;
		iframe.setSrc("");
		if (connected) {
			onDisconnected();
		}
	}
	
	private native IFrameElement createIFrame(IEHTMLFileCometTransport client, String html) /*-{
		var htmlfile = new ActiveXObject("htmlfile");
		htmlfile.open();
		htmlfile.write(html);
		htmlfile.close();
		
		htmlfile.parentWindow.m = $entry(function(message) {
			client.@net.zschech.gwt.comet.client.impl.IEHTMLFileCometTransport::onMessages(Lcom/google/gwt/core/client/JsArrayString;)(arguments);
		});
		htmlfile.parentWindow.c = $entry(function(heartbeat) {
			client.@net.zschech.gwt.comet.client.impl.IEHTMLFileCometTransport::onConnected(I)(heartbeat);
		});
		htmlfile.parentWindow.d = $entry(function() {
			client.@net.zschech.gwt.comet.client.impl.IEHTMLFileCometTransport::onDisconnected()();
		});
		htmlfile.parentWindow.e = $entry(function(statusCode, message) {
			client.@net.zschech.gwt.comet.client.impl.IEHTMLFileCometTransport::onError(ILjava/lang/String;)(statusCode, message);
		});
		htmlfile.parentWindow.h = $entry(function() {
			client.@net.zschech.gwt.comet.client.impl.IEHTMLFileCometTransport::onHeartbeat()();
		});
		htmlfile.parentWindow.r = $entry(function() {
			client.@net.zschech.gwt.comet.client.impl.IEHTMLFileCometTransport::onRefresh()();
		});
		// no $entry() because no user code is reachable
		htmlfile.parentWindow.t = function() {
			client.@net.zschech.gwt.comet.client.impl.IEHTMLFileCometTransport::onTerminate()();
		};
		
		return htmlfile.documentElement.getElementsByTagName("iframe").item(0);
	}-*/;
	
	private native String getDocumentDomain() /*-{
		return $doc.domain;
	}-*/;
	
	private native String getDomain(String documentDomain, String url) /*-{
 		var urlParts = /(^https?:)?(\/\/(([^:\/\?#]+)(:(\d+))?))?([^\?#]*)/.exec(url);
        var urlDomain = urlParts[4];
        
        if (!urlDomain || documentDomain == urlDomain) {
        	return null;
        }
        
        var documentDomainParts = documentDomain.split('.');
        var urlDomainParts = urlDomain.split('.');
        
        var d = documentDomainParts.length - 1;
        var u = urlDomainParts.length - 1;
        var resultDomainParts = [];
        
        while (d >= 0 && u >= 0 && documentDomainParts[d] == urlDomainParts[u]) {
        		resultDomainParts.push(urlDomainParts[u]);
        		d--;
        		u--;
        }
        return resultDomainParts.reverse().join('.')
	}-*/;
	
	private void collect() {
		NodeList<Node> childNodes = body.getChildNodes();
		if (childNodes.getLength() > 1) {
			body.removeChild(childNodes.getItem(0));
		}
	}
	
	private void onMessages(JsArrayString arguments) {
		collect();
		int length = arguments.length();
		List<Serializable> messages = new ArrayList<Serializable>(length);
		for (int i = 0; i < length; i++) {
			String message = arguments.get(i);
			switch (message.charAt(0)) {
			case ']':
				messages.add(message.substring(1));
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
				listener.onError(new CometException("Invalid message received: " + message), true);
			}
		}
		
		listener.onMessage(messages);
	}
	
	private void onConnected(int heartbeat) {
		connected = true;
		body = iframe.getContentDocument().getBody();
		collect();
		listener.onConnected(heartbeat);
	}
	
	private void onDisconnected() {
		connected = false;
		body = null;
		if (expectingDisconnection) {
			listener.onDisconnected();
		}
		else {
			listener.onError(new CometException("Unexpected disconnection"), false);
		}
	}
	
	private void onError(int statusCode, String message) {
		listener.onError(new StatusCodeException(statusCode, message), false);
	}
	
	private void onHeartbeat() {
		listener.onHeartbeat();
	}
	
	private void onRefresh() {
		listener.onRefresh();
	}
	
	private void onTerminate() {
		expectingDisconnection = true;
	}
}
