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
import net.zschech.gwt.comet.client.SerialMode;

import com.google.gwt.core.client.Duration;
import com.google.gwt.core.client.GWT;

/**
 * This is the base class for the comet implementations
 * 
 * @author Richard Zschech
 */
public abstract class CometTransport {
	
	public static final String MODULE_BASE_PARAMETER = "b";
	public static final String STRONG_NAME_PARAMETER = "p";
	
	protected CometClient client;
	protected CometListener listener;
	
	public void initiate(CometClient client, CometListener listener) {
		this.client = client;
		this.listener = listener;
	}
	
	public abstract void connect(int connectionCount);
	
	public abstract void disconnect();
	
	public String getUrl(int connectionCount) {
		StringBuilder url = new StringBuilder(client.getUrl());
		if (client.getSerializer() != null && client.getSerializer().getMode() == SerialMode.DE_RPC) {
			url.append(url.indexOf("?") > 0 ? '&' : '?');
			url.append(MODULE_BASE_PARAMETER).append('=').append(GWT.getModuleBaseURL());
			url.append('&').append(STRONG_NAME_PARAMETER).append('=').append(GWT.getPermutationStrongName());
		}
		
		url.append(url.indexOf("?") > 0 ? '&' : '?');
		url.append("t=").append(Integer.toString((int) (Duration.currentTimeMillis() % Integer.MAX_VALUE), Character.MAX_RADIX));
		url.append("&c=").append(Integer.toString(connectionCount, Character.MAX_RADIX));
		
		return url.toString();
	}
}
