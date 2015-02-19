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
import java.io.Serializable;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.zschech.gwt.comet.client.impl.HTTPRequestCometTransport;
import net.zschech.gwt.comet.server.CometServlet;

import com.google.gwt.rpc.server.ClientOracle;
import com.google.gwt.user.server.rpc.SerializationPolicy;

/**
 * The CometServletResponse for the {@link HTTPRequestCometTransport}
 * 
 * @author Richard Zschech
 */
public abstract class RawDataCometServletResponse extends ManagedStreamCometServletResponseImpl {
	
	private static final int MAX_PADDING_REQUIRED = 256;
	private static final String PADDING_STRING;
	static {
		char[] padding = new char[MAX_PADDING_REQUIRED];
		for (int i = 0; i < padding.length - 1; i++) {
			padding[i] = '*';
		}
		padding[padding.length - 1] = '\n';
		PADDING_STRING = new String(padding);
	}
	
	public RawDataCometServletResponse(HttpServletRequest request, HttpServletResponse response, SerializationPolicy serializationPolicy, ClientOracle clientOracle, CometServlet servlet, AsyncServlet async, int heartbeat) {
		super(request, response, serializationPolicy, clientOracle, servlet, async, heartbeat);
	}
	
	@Override
	protected void appendMessageTrailer() throws IOException {
		writer.append('\n');
	}
	
	@Override
	protected int getPaddingRequired() {
		return 0;
	}
	
	@Override
	protected CharSequence getPadding(int padding) {
		if (padding > PADDING_STRING.length()) {
			StringBuilder result = new StringBuilder(padding);
			for (int i = 0; i < padding - 2; i++) {
				result.append('*');
			}
			return result;
		}
		else {
			return PADDING_STRING.substring(padding);
		}
	}

	@Override
	protected void doInitiate(int heartbeat) throws IOException {
		// send connection event to client
		appendMessageHeader();
		writer.append('!').append(String.valueOf(heartbeat));
		appendMessageTrailer();
	}
	
	@Override
	protected void doSendError(int statusCode, String message) throws IOException {
		appendMessageHeader();
		writer.append(String.valueOf(statusCode));
		if (message != null) {
			writer.append(' ').append(escape(message));
		}
		appendMessageTrailer();
	}
	
	@Override
	protected void doWrite(List<? extends Serializable> messages) throws IOException {
		for (Serializable message : messages) {
			CharSequence string;
			appendMessageHeader();
			if (message instanceof CharSequence) {
				string = escape((CharSequence) message);
				if (string == message) {
					writer.append('|');
				}
				else {
					writer.append(']');
				}
			}
			else {
				string = serialize(message);
			}
			appendMessage(string);
			appendMessageTrailer();
		}
	}
	
	protected void appendMessage(CharSequence string) throws IOException {
		writer.append(string);
	}
	
	@Override
	protected void doHeartbeat() throws IOException {
		appendMessageHeader();
		writer.append('#');
		appendMessageTrailer();
	}
	
	@Override
	protected void doTerminate() throws IOException {
		appendMessageHeader();
		writer.append('?');
		appendMessageTrailer();
	}
	
	@Override
	protected void doRefresh() throws IOException {
		appendMessageHeader();
		writer.append('@');
		appendMessageTrailer();
	}
	
	@Override
	protected boolean isOverTerminateLength(int written) {
		return false;
	}
	
	private CharSequence escape(CharSequence string) {
		int length = string.length();
		int i = 0;
		loop: while (i < length) {
			char ch = string.charAt(i);
			switch (ch) {
			case '\\':
			case '\n':
			case '\r':
				break loop;
			}
			i++;
		}
		
		if (i == length) {
			return string;
		}
		
		StringBuilder str = new StringBuilder(string.length() * 2);
		str.append(string, 0, i);
		while (i < length) {
			char ch = string.charAt(i);
			switch (ch) {
			case '\\':
				str.append("\\\\");
				break;
			case '\n':
				str.append("\\n");
				break;
			case '\r':
				str.append("\\r");
				break;
			default:
				str.append(ch);
			}
			i++;
		}
		return str;
	}
}
