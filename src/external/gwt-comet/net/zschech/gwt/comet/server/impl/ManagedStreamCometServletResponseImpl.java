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

public abstract class ManagedStreamCometServletResponseImpl extends CometServletResponseImpl {
	
	private final int paddingRequired;
	private final long length;
	
	private CountOutputStream countOutputStream;
	
	public ManagedStreamCometServletResponseImpl(HttpServletRequest request, HttpServletResponse response, SerializationPolicy serializationPolicy, ClientOracle clientOracle, CometServlet servlet, AsyncServlet async, int heartbeat) {
		super(request, response, serializationPolicy, clientOracle, servlet, async, heartbeat);
		
		String paddingParameter = getRequest().getParameter("padding");
		if (paddingParameter != null) {
			paddingRequired = Integer.parseInt(paddingParameter);
		}
		else {
			paddingRequired = getPaddingRequired();
		}
		
		String lengthParameter = getRequest().getParameter("length");
		if (lengthParameter != null) {
			length = Integer.parseInt(lengthParameter);
		}
		else {
			length = 0;
		}
	}
	
	protected OutputStream setupCountOutputStream(OutputStream outputStream) {
		countOutputStream = new CountOutputStream(outputStream);
		return countOutputStream;
	}
	
	@Override
	protected void doSuspend() throws IOException {
		if (paddingRequired != 0 && countOutputStream != null) {
			
			int written = getCount();
			CometSessionImpl session = getSessionImpl();
			if (session != null) {
				while (written < paddingRequired && !session.isEmpty()) {
					session.writeQueue(this, false);
					written = getCount();
				}
			}
			
			if (paddingRequired > written) {
				CharSequence paddingData = getPadding(paddingRequired - written);
				if (paddingData != null) {
					writer.append(paddingData);
				}
			}
		}
	}
	
	private int getCount() throws IOException {
		countOutputStream.setIgnoreFlush(true);
		writer.flush();
		countOutputStream.setIgnoreFlush(false);
		return countOutputStream.getCount();
	}
	
	@Override
	public synchronized void write(List<? extends Serializable> messages, boolean flush) throws IOException {
		super.write(messages, flush);
		checkLength();
	}
	
	@Override
	public synchronized void heartbeat() throws IOException {
		super.heartbeat();
		checkLength();
	}
	
	private void checkLength() throws IOException {
		if (countOutputStream != null) {
			int count = getCount();
			CometSessionImpl session = getSessionImpl();
			if (session == null) {
				if (length != 0) {
					if (count > length) {
						terminate();
					}
				}
				else if (isOverTerminateLength(count)) {
					terminate();
				}
			}
			else {
				if (length != 0) {
					if (!session.setRefresh() && count > length) {
						doRefresh();
					}
					else if (count > length) {
						terminate();
					}
				}
				else {
					if (session.isAndSetOverRefreshLength(count)) {
						doRefresh();
					}
					else if (session.isOverTerminateLength(count)) {
						terminate();
					}
				}
			}
		}
	}
	
	protected void appendMessageHeader() throws IOException {
	}
	
	protected void appendMessageTrailer() throws IOException {
	}
	
	protected abstract void doRefresh() throws IOException;
	
	protected abstract int getPaddingRequired();
	
	protected abstract CharSequence getPadding(int padding);
	
	protected abstract boolean isOverTerminateLength(int written);
}
