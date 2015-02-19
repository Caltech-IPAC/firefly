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
package net.zschech.gwt.comet.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.zschech.gwt.comet.client.impl.CometTransport;
import net.zschech.gwt.comet.server.impl.AsyncServlet;
import net.zschech.gwt.comet.server.impl.CometServletResponseImpl;
import net.zschech.gwt.comet.server.impl.CometSessionImpl;
import net.zschech.gwt.comet.server.impl.EventSourceCometServletResponse;
import net.zschech.gwt.comet.server.impl.HTTPRequestCometServletResponse;
import net.zschech.gwt.comet.server.impl.IEHTMLFileCometServletResponse;
import net.zschech.gwt.comet.server.impl.OperaEventSourceCometServletResponse;

import com.google.gwt.rpc.server.ClientOracle;
import com.google.gwt.rpc.server.HostedModeClientOracle;
import com.google.gwt.rpc.server.WebModeClientOracle;
import com.google.gwt.user.server.rpc.SerializationPolicy;

/**
 * This is the base class for application's Comet servlets. To process a Comet request override
 * {@link #doComet(CometServletResponse)} and send messages by calling {@link CometServletResponse#write(Serializable)}
 * or enqueue messages using {@link CometServletResponse#getSession()} and {@link CometSession#enqueue(Serializable)}.
 * 
 * @author Richard Zschech
 */
public class CometServlet extends HttpServlet {
	
	public static final String AUTO_CREATE_COMET_SESSION = "net.zschech.gwt.comet.server.auto.create.comet.session.on.comet.request";
	
	private static final long serialVersionUID = 820972291784919880L;
	
	private int heartbeat = 15 * 1000; // 15 seconds by default
	
	private transient AsyncServlet async;
	
	private boolean autoCreateCometSession;
	
	public void setHeartbeat(int heartbeat) {
		this.heartbeat = heartbeat;
	}
	
	public int getHeartbeat() {
		return heartbeat;
	}
	
	@Override
	public void init() throws ServletException {
		ServletConfig servletConfig = getServletConfig();
		String heartbeat = servletConfig.getInitParameter("heartbeat");
		if (heartbeat != null) {
			this.heartbeat = Integer.parseInt(heartbeat);
		}
		this.autoCreateCometSession = "true".equals(getServletConfig().getInitParameter(AUTO_CREATE_COMET_SESSION));
		async = AsyncServlet.initialize(getServletContext());
	}
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			int requestHeartbeat = getHeartbeat();
			String requestedHeartbeat = request.getParameter("heartbeat");
			if (requestedHeartbeat != null) {
				try {
					requestHeartbeat = Integer.parseInt(requestedHeartbeat);
					if (requestHeartbeat <= 0) {
						throw new IOException("invalid heartbeat parameter");
					}
				}
				catch (NumberFormatException e) {
					throw new IOException("invalid heartbeat parameter");
				}
			}
			
			ClientOracle clientOracle = getClientOracle(request);
			SerializationPolicy serializationPolicy = clientOracle == null ? createSerializationPolicy() : null;
			CometServletResponseImpl cometServletResponse = createCometServletResponse(request, response, serializationPolicy, clientOracle, requestHeartbeat);
			doCometImpl(cometServletResponse);
		}
		catch (IOException e) {
			CometServletResponseImpl cometServletResponse = createCometServletResponse(request, response, null, null, 0);
			cometServletResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
		}
	}
	
	private CometServletResponseImpl createCometServletResponse(HttpServletRequest request, HttpServletResponse response, SerializationPolicy serializationPolicy, ClientOracle clientOracle, int requestHeartbeat) {
		
		String accept = request.getHeader("Accept");
		String userAgent = request.getHeader("User-Agent");
		if ("text/event-stream".equals(accept)) {
			return new EventSourceCometServletResponse(request, response, serializationPolicy, clientOracle, this, async, requestHeartbeat);
		}
		else if ("application/comet".equals(accept)) {
			return new HTTPRequestCometServletResponse(request, response, serializationPolicy, clientOracle, this, async, requestHeartbeat);
		}
		else if (userAgent != null && userAgent.contains("Opera")) {
			return new OperaEventSourceCometServletResponse(request, response, serializationPolicy, clientOracle, this, async, requestHeartbeat);
		}
		else {
			return new IEHTMLFileCometServletResponse(request, response, serializationPolicy, clientOracle, this, async, requestHeartbeat);
		}
	}
	
	private void doCometImpl(CometServletResponseImpl response) throws IOException {
		try {
			// setup the request
			response.initiate();
			
			if (autoCreateCometSession) {
				response.getSession();
			}
			
			// call the application code
			doComet(response);
		}
		catch (IOException e) {
			log("Error calling doComet()", e);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		}
		catch (ServletException e) {
			log("Error calling doComet()", e);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		}
		
		// at this point the application may have spawned threads to process this response
		// so we have to be careful about concurrency from here on
		response.suspend();
	}
	
	/**
	 * Override this method to process a new comet request. All required information from the {@link HttpServletRequest}
	 * must be retrieved {@link CometServletResponse#getRequest()} in this method as it will not be available after this
	 * method returns and the request is suspended. This method may write data to the Comet response but should not
	 * block. Writing data from this method before the request is suspended can improve the efficiency because padding
	 * data may not be needed to cause the browser to start processing the stream.
	 * 
	 * @param cometResponse
	 * @throws ServletException
	 * @throws IOException
	 */
	protected void doComet(CometServletResponse cometResponse) throws ServletException, IOException {
	}
	
	/**
	 * Override this method to be notified of the Comet connection being terminated.
	 * 
	 * @param cometResponse
	 * @param serverInitiated
	 */
	public void cometTerminated(CometServletResponse cometResponse, boolean serverInitiated) {
	}
	
	/**
	 * Override this method to override the requested heartbeat. By default only requested heartbeats > this.heartbeat
	 * are allowed.
	 * 
	 * @param requestedHeartbeat
	 * @return
	 */
	protected int getHeartbeat(int requestedHeartbeat) {
		return requestedHeartbeat < heartbeat ? heartbeat : requestedHeartbeat;
	}
	
	protected SerializationPolicy createSerializationPolicy() {
		return new SerializationPolicy() {
			@Override
			public boolean shouldDeserializeFields(final Class<?> clazz) {
				throw new UnsupportedOperationException("shouldDeserializeFields");
			}
			
			@Override
			public boolean shouldSerializeFields(final Class<?> clazz) {
				return Object.class != clazz;
			}
			
			@Override
			public void validateDeserialize(final Class<?> clazz) {
				throw new UnsupportedOperationException("validateDeserialize");
			}
			
			@Override
			public void validateSerialize(final Class<?> clazz) {
			}
		};
	}
	
	private final Map<String, SoftReference<ClientOracle>> clientOracleCache = new HashMap<String, SoftReference<ClientOracle>>();
	
	protected ClientOracle getClientOracle(HttpServletRequest request) throws IOException {
		String permutationStrongName = request.getParameter(CometTransport.STRONG_NAME_PARAMETER);
		if (permutationStrongName == null) {
			return null;
		}
		
		ClientOracle toReturn;
		synchronized (clientOracleCache) {
			if (clientOracleCache.containsKey(permutationStrongName)) {
				toReturn = clientOracleCache.get(permutationStrongName).get();
				if (toReturn != null) {
					return toReturn;
				}
			}
			
			if ("HostedMode".equals(permutationStrongName)) {
				// if (!allowHostedModeConnections()) {
				// throw new SecurityException("Blocked hosted mode request");
				// }
				toReturn = new HostedModeClientOracle();
			}
			else {
				String moduleBase = request.getParameter(CometTransport.MODULE_BASE_PARAMETER);
				if (moduleBase == null) {
					return null;
				}
				
				String basePath = new URL(moduleBase).getPath();
				if (basePath == null) {
					throw new MalformedURLException("Blocked request without GWT base path parameter (XSRF attack?)");
				}
				
				String contextPath = getServletContext().getContextPath();
				if (!basePath.startsWith(contextPath)) {
					throw new MalformedURLException("Blocked request with invalid GWT base path parameter (XSRF attack?)");
				}
				basePath = basePath.substring(contextPath.length());
				
				InputStream in = findClientOracleData(basePath, permutationStrongName);
				
				toReturn = WebModeClientOracle.load(in);
			}
			clientOracleCache.put(permutationStrongName, new SoftReference<ClientOracle>(toReturn));
		}
		
		return toReturn;
	}
	
	protected static final String CLIENT_ORACLE_EXTENSION = ".gwt.rpc";
	
	protected InputStream findClientOracleData(String requestModuleBasePath, String permutationStrongName) throws IOException {
		String resourcePath = requestModuleBasePath + permutationStrongName + CLIENT_ORACLE_EXTENSION;
		InputStream in = getServletContext().getResourceAsStream(resourcePath);
		if (in == null) {
			throw new IOException("Could not find ClientOracle data for permutation " + permutationStrongName);
		}
		return in;
	}
	
	public static CometSession getCometSession(HttpSession httpSession) {
		return getCometSession(httpSession, new ConcurrentLinkedQueue<Serializable>());
	}
	
	public static CometSession getCometSession(HttpSession httpSession, Queue<Serializable> queue) {
		return getCometSession(httpSession, true, queue);
	}
	
	public static CometSession getCometSession(HttpSession httpSession, boolean create) {
		return getCometSession(httpSession, create, create ? new ConcurrentLinkedQueue<Serializable>() : null);
	}
	
	public static CometSession getCometSession(HttpSession httpSession, boolean create, Queue<Serializable> queue) {
		synchronized (httpSession) {
			CometSession session = (CometSession) httpSession.getAttribute(CometSession.HTTP_SESSION_KEY);
			if (session == null) {
				if (create) {
					session = new CometSessionImpl(httpSession, queue, AsyncServlet.initialize(httpSession.getServletContext()));
					httpSession.setAttribute(CometSession.HTTP_SESSION_KEY, session);
				}
			}
			return session;
		}
	}
}
