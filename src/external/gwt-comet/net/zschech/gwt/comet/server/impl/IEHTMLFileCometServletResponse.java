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
import java.io.OutputStream;
import java.io.Serializable;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.zschech.gwt.comet.server.CometServlet;

import com.google.gwt.rpc.server.ClientOracle;
import com.google.gwt.user.server.rpc.SerializationPolicy;

/**
 * The CometServletResponse for the {@link IEHTMLFileCometTransport}
 * 
 * @author Richard Zschech
 */
public class IEHTMLFileCometServletResponse extends ManagedStreamCometServletResponseImpl {
	
	// IE requires padding to start processing the page.
	private static final int PADDING_REQUIRED = 256;
	
	private static final String HEAD = "<html><body onload='parent.d()'><script>";
	private static final String MID = "parent.c(";
	private static final String TAIL = ");var m=parent.m;var h=parent.h;</script>";
	
	private static final String PADDING_STRING;
	static {
		// the required padding minus the length of the heading
		int capacity = PADDING_REQUIRED - HEAD.length() - MID.length() - TAIL.length();
		char[] padding = new char[capacity];
		for (int i = 0; i < capacity; i++) {
			padding[i] = ' ';
		}
		PADDING_STRING = new String(padding);
	}
	
	private int clientMemory;
	
	public IEHTMLFileCometServletResponse(HttpServletRequest request, HttpServletResponse response, SerializationPolicy serializationPolicy, ClientOracle clientOracle, CometServlet servlet, AsyncServlet async, int heartbeat) {
		super(request, response, serializationPolicy, clientOracle, servlet, async, heartbeat);
	}
	
	@Override
	protected void setupHeaders(HttpServletResponse response) {
		super.setupHeaders(response);
		response.setContentType("text/html");
		response.setCharacterEncoding("UTF-8");
	}
	
	@Override
	protected OutputStream getOutputStream(OutputStream outputStream) {
		return setupCountOutputStream(outputStream);
	}
	
	@Override
	protected int getPaddingRequired() {
		return PADDING_REQUIRED;
	}
	
	@Override
	protected void doInitiate(int heartbeat) throws IOException {
		writer.append(HEAD);
		String domain = getRequest().getParameter("d");
		if (domain != null) {
			writer.append("document.domain='");
			writer.append(domain);
			writer.append("';");
		}
		writer.append(MID);
		writer.append(Integer.toString(heartbeat));
		writer.append(TAIL);
	}
	
	@Override
	protected CharSequence getPadding(int padding) {
		if (padding > PADDING_STRING.length()) {
			StringBuilder result = new StringBuilder(padding);
			for (int i = 0; i < padding; i++) {
				result.append(' ');
			}
			return result;
		}
		else {
			return PADDING_STRING.substring(0, padding);
		}
	}
	
	@Override
	protected void doSendError(int statusCode, String message) throws IOException {
		writer.append("<html><script>parent.e(").append(Integer.toString(statusCode));
		if (message != null) {
			writer.append(",'").append(escapeString(message)).append('\'');
		}
		writer.append(")</script></html>");
	}
	
	@Override
	protected void doWrite(List<? extends Serializable> messages) throws IOException {
		clientMemory *= 2;
		writer.append("<script>m(");
		boolean first = true;
		for (Serializable message : messages) {
			CharSequence string;
			if (message instanceof CharSequence) {
				string = "]" + escapeString((CharSequence) message);
			}
			else {
				string = escapeObject(serialize(message));
			}
			if (first) {
				first = false;
			}
			else {
				writer.append(',');
			}
			writer.append('\'').append(string).append('\'');
			clientMemory += string.length() + 1;
		}
		writer.append(")</script>");
	}
	
	@Override
	protected void doHeartbeat() throws IOException {
		writer.append("<script>h();</script>");
	}
	
	@Override
	protected void doTerminate() throws IOException {
		writer.append("<script>parent.t();</script>");
	}
	
	@Override
	protected void doRefresh() throws IOException {
		writer.append("<script>parent.r();</script>");
	}
	
	@Override
	protected boolean isOverTerminateLength(int written) {
//		if(System.currentTimeMillis() - start < 1000) {
//			return false;
//		}
		return clientMemory > 1024 * 1024;
//			return written > 4 * 1024 * 1024;
	}
	
	private CharSequence escapeString(CharSequence string) {
		int length = string.length();
		int i = 0;
		loop: while (i < length) {
			char ch = string.charAt(i);
			switch (ch) {
			case '\'':
			case '\\':
			case '/':
			case '\b':
			case '\f':
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
			case '\'':
				str.append("\\\'");
				break;
			case '\\':
				str.append("\\\\");
				break;
			case '/':
				str.append("\\/");
				break;
			case '\b':
				str.append("\\b");
				break;
			case '\f':
				str.append("\\f");
				break;
			case '\n':
				str.append("\\n");
				break;
			case '\r':
				str.append("\\r");
				break;
			case '\t':
				str.append("\\t");
				break;
			default:
				str.append(ch);
			}
			i++;
		}
		return str;
	}
	
	private CharSequence escapeObject(CharSequence string) {
		int length = string.length();
		int i = 0;
		loop: while (i < length) {
			char ch = string.charAt(i);
			switch (ch) {
			case '\'':
			case '\\':
			case '/':
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
			case '\'':
				str.append("\\\'");
				break;
			case '\\':
				str.append("\\\\");
				break;
			case '/':
				str.append("\\/");
				break;
			default:
				str.append(ch);
			}
			i++;
		}
		return str;
	}
}
