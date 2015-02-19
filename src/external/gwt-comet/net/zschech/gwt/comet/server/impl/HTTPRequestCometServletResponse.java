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

import net.zschech.gwt.comet.client.impl.HTTPRequestCometTransport;
import net.zschech.gwt.comet.server.CometServlet;

import com.google.gwt.rpc.server.ClientOracle;
import com.google.gwt.user.server.rpc.SerializationPolicy;

/**
 * The CometServletResponse for the {@link HTTPRequestCometTransport}
 * 
 * @author Richard Zschech
 */
public class HTTPRequestCometServletResponse extends RawDataCometServletResponse {
	
	private int clientMemory;
	
	public HTTPRequestCometServletResponse(HttpServletRequest request, HttpServletResponse response, SerializationPolicy serializationPolicy, ClientOracle clientOracle, CometServlet servlet, AsyncServlet async, int heartbeat) {
		super(request, response, serializationPolicy, clientOracle, servlet, async, heartbeat);
	}
	
	@Override
	protected void setupHeaders(HttpServletResponse response) {
		super.setupHeaders(response);
		response.setContentType("application/comet");
		response.setCharacterEncoding("UTF-8");
		
		String origin = getRequest().getHeader("Origin");
		if (origin != null) {
			response.setHeader("Access-Control-Allow-Origin", origin);
		}
	}
	
	@Override
	protected OutputStream getOutputStream(OutputStream outputStream) {
		return setupCountOutputStream(outputStream);
	}
	
	@Override
	protected void doWrite(List<? extends Serializable> messages) throws IOException {
		clientMemory *= 2;
		super.doWrite(messages);
	}
	
	@Override
	protected void appendMessage(CharSequence string) throws IOException {
		clientMemory += string.length() + 1;
		super.appendMessage(string);
	}
	
	@Override
	protected boolean isOverTerminateLength(int written) {
		// if (chrome) {
		// Chrome seems to have a problem with lots of small messages consuming lots of memory.
		// I'm guessing for each readyState = 3 event it copies the responseText from its IO system to its
		// JavaScript
		// engine and does not clean up all the events until the HTTP request is finished.
		return clientMemory > 1024 * 1024;
		// }
		// else {
		// return false;//written > 2 * 1024 * 1024;
		// }
	}
}
