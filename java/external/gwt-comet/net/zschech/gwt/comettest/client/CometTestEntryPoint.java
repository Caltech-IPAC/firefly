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
package net.zschech.gwt.comettest.client;

import java.io.Serializable;
import java.util.List;

import net.zschech.gwt.comet.client.CometClient;
import net.zschech.gwt.comet.client.CometListener;
import net.zschech.gwt.comet.client.CometSerializer;
import net.zschech.gwt.comet.client.SerialMode;
import net.zschech.gwt.comet.client.SerialTypes;

import com.google.gwt.core.client.Duration;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.StatusCodeException;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.ScrollPanel;

public class CometTestEntryPoint implements EntryPoint {
	
	private CometTestServiceAsync cometTestService;
	private CometTest cometTest;
	
	private HTML messages;
	private ScrollPanel scrollPanel;
	private CometTest[][] tests;
	private int allX;
	private int allY;
	
	@Override
	public void onModuleLoad() {
		GWT.setUncaughtExceptionHandler(new GWT.UncaughtExceptionHandler() {
			@Override
			public void onUncaughtException(Throwable e) {
				output("uncaught " + string(e), "red");
				e.printStackTrace();
			}
		});
		
		cometTestService = GWT.create(CometTestService.class);
		
		messages = new HTML();
		scrollPanel = new ScrollPanel();
		scrollPanel.setHeight("250px");
		scrollPanel.add(messages);
		
		RootPanel.get().add(scrollPanel);

		tests = new CometTest[][] {{
			new ReconnectionTest(true),
			new ReconnectionTest(false),
		}, {
			new ConnectionTest(true),
			new ConnectionTest(false),
		}, {
			new ErrorTest(),
		}, {
			new EscapeTest(null),
			new EscapeTest(SerialMode.RPC),
			new EscapeTest(SerialMode.DE_RPC),
		}, {
			new ThroughputTest(true, true, null),
			new ThroughputTest(true, true, SerialMode.RPC),
			new ThroughputTest(true, true, SerialMode.DE_RPC),
		}, {
			new ThroughputTest(true, false, null),
			new ThroughputTest(true, false, SerialMode.RPC),
			new ThroughputTest(true, false, SerialMode.DE_RPC),
		}, {
			new ThroughputTest(false, false, null),
			new ThroughputTest(false, false, SerialMode.RPC),
			new ThroughputTest(false, false, SerialMode.DE_RPC),
		}, {
			new LatencyTest(true, true, null),
			new LatencyTest(true, true, SerialMode.RPC),
			new LatencyTest(true, true, SerialMode.DE_RPC),
		}, {
			new LatencyTest(true, false, null),
			new LatencyTest(true, false, SerialMode.RPC),
			new LatencyTest(true, false, SerialMode.DE_RPC),
		}, {
			new LatencyTest(false, false, null),
			new LatencyTest(false, false, SerialMode.RPC),
			new LatencyTest(false, false, SerialMode.DE_RPC),
		}, {
			new OrderTest(true, true, null),
			new OrderTest(true, true, SerialMode.RPC),
			new OrderTest(true, true, SerialMode.DE_RPC),
		}, {
			new OrderTest(true, false, null),
			new OrderTest(true, false, SerialMode.RPC),
			new OrderTest(true, false, SerialMode.DE_RPC),
		}, {
			new OrderTest(false, false, null),
			new OrderTest(false, false, SerialMode.RPC),
			new OrderTest(false, false, SerialMode.DE_RPC),
		}, {
			new PaddingTest()
		}, {
			new SlowBrowserTest()
		}};
		
		FlowPanel controls = new FlowPanel();
		controls.add(new Button("stop", new ClickHandler() {
			@Override
			public void onClick(ClickEvent arg0) {
				if (cometTest != null) {
					cometTest.stop();
					cometTest = null;
				}
				allX = -1;
			}
		}));
		controls.add(new Button("clear", new ClickHandler() {
			@Override
			public void onClick(ClickEvent arg0) {
				messages.setHTML("");
			}
		}));
		controls.add(new Button("all", new ClickHandler() {
			@Override
			public void onClick(ClickEvent e) {
				runAll();
			}
		}));
		RootPanel.get().add(controls);
		
		for (CometTest[] typeTests : tests) {
			controls = new FlowPanel();
			for (CometTest t : typeTests) {
				final CometTest test = t;
				controls.add(new Button(test.name, new ClickHandler() {
					@Override
					public void onClick(ClickEvent e) {
						if (cometTest != null) {
							cometTest.stop();
						}
						allX = -1;
						cometTest = test;
						test.start();
					}
				}));
			}
			RootPanel.get().add(controls);
		}
	}
	
	private void runAll() {
		allX = 0;
		allY = 0;
		tests[allX][allY].start();
	}
	
	private void runNext() {
		if (allX != -1) {
			allY++;
			if (allY >= tests[allX].length) {
				allX++;
				allY = 0;
				if (allX >= tests.length) {
					output("All done!", "lime");
					allX = -1;
					return;
				}
			}
			tests[allX][allY].start();
		}
	}
	
	public static final char ESCAPE_START = 32;
	public static final char ESCAPE_END = 127;
	public static final String ESCAPE;
	static {
		StringBuilder result = new StringBuilder();
		result.append(' '); // event source discards prefixed spaces
		for (char i = ESCAPE_START; i <= ESCAPE_END; i++) {
			result.append(i);
		}
		result.append("\n\r\r\n\\/\n\t");
		result.append("')</script>");
		result.append(' ');
		ESCAPE = result.toString();
	}
	
	@SerialTypes(mode = SerialMode.RPC, value = { TestData.class })
	public static abstract class RPCTestCometSerializer extends CometSerializer {
	}
	
	@SerialTypes(mode = SerialMode.DE_RPC, value = { TestData.class })
	public static abstract class DeRPCTestCometSerializer extends CometSerializer {
	}
	
	public static class TestData implements Serializable {
		private static final long serialVersionUID = 2554091659231006755L;
		public double d;
		public String s;
		
		public TestData() {
		}
		
		public TestData(double d, String s) {
			this.d = d;
			this.s = s;
		}
	}
	
	abstract class CometTest implements CometListener {
		
		final String name;
		final boolean session;
		
		CometClient cometClient;
		
		double startTime;
		double stopTime;
		double connectedTime;
		int connectedCount;
		double disconnectedTime;
		int disconnectedCount;
		int errorCount;
		int heartbeatCount;
		int refreshCount;
		int messageCount;
		int messagesCount;
		
		String failure;
		boolean pass;
		
		CometTest(String name, boolean session) {
			this.name = name + " session=" + session;
			this.session = session;
		}
		
		abstract void start();
		
		void start(String url) {
			start(url, (CometSerializer) null);
		}
		
		void start(String url, SerialMode mode) {
			final CometSerializer serializer;
			if (mode == null) {
				url = url + (url.contains("?") ? "&" : "?") + "mode=string";
				serializer = null;
			}
			else if (mode == SerialMode.RPC) {
				serializer = GWT.create(RPCTestCometSerializer.class);
			}
			else {
				serializer = GWT.create(DeRPCTestCometSerializer.class);
			}
			start(url + (url.contains("?") ? "&" : "?") + "session=" + session, serializer);
		}
		
		private void start(final String url, final CometSerializer serializer) {
			reset();
			cometTestService.invalidateSession(new AsyncCallback<Boolean>() {
				@Override
				public void onSuccess(Boolean result) {
					if (session) {
						cometTestService.createSession(new AsyncCallback<Boolean>() {
							@Override
							public void onSuccess(Boolean result) {
								startTime = Duration.currentTimeMillis();
								output("start " + name, "black");
								doStart(url, serializer);
							}
							
							@Override
							public void onFailure(Throwable error) {
								output("create session failure " + string(error), "red");
							}
						});
					}
					else {
						startTime = Duration.currentTimeMillis();
						output("start " + name, "black");
						doStart(url, serializer);
					}
				}
				
				@Override
				public void onFailure(Throwable error) {
					output("invalidate session failure " + string(error), "red");
				}
			});
		}
		
		void reset() {
			startTime = 0;
			stopTime = 0;
			connectedTime = 0;
			connectedCount = 0;
			disconnectedTime = 0;
			disconnectedCount = 0;
			errorCount = 0;
			heartbeatCount = 0;
			refreshCount = 0;
			messageCount = 0;
			messagesCount = 0;
			
			pass = false;
			failure = null;
		}
		
		void doStart(String url, CometSerializer serializer) {
			cometClient = new CometClient(url, serializer, this);
			cometClient.start();
		}
		
		void stop() {
			doStop();
			cometTest = null;
			stopTime = Duration.currentTimeMillis();
			output("stop " + name + " " + (stopTime - startTime) + "ms", "black");
			if (pass && failure == null) {
				output("pass!", "lime");
			}
			else {
				output("fail  :\n" + (failure == null ? "unknown" : failure), "red");
			}
			runNext();
		}

		protected void doStop() {
			if (cometClient != null) {
				cometClient.stop();
				cometClient = null;
			}
		}
		
		void outputStats() {
			output("count     : " + messageCount, "black");
			output("rate      : " + messageCount / (disconnectedTime - connectedTime) * 1000 + "/s", "black");
			output("batch size: " + (double) messageCount / (double) messagesCount, "black");
		}
		
		@Override
		public void onConnected(int heartbeat) {
			connectedTime = Duration.currentTimeMillis();
			connectedCount++;
			output("connected " + connectedCount + " " + (connectedTime - startTime) + "ms heartbeat: " + heartbeat, "silver");
			assertTrue("connected once", connectedCount == 1);
		}
		
		@Override
		public void onDisconnected() {
			disconnectedTime = Duration.currentTimeMillis();
			disconnectedCount++;
			output("disconnected " + disconnectedCount + " " + (disconnectedTime - connectedTime) + "ms", "silver");
			assertTrue("disconnected once", disconnectedCount == 1);
			stop();
		}
		
		@Override
		public void onError(Throwable exception, boolean connected) {
			double errorTime = Duration.currentTimeMillis();
			errorCount++;
			output("error " + errorCount + " " + (errorTime - startTime) + "ms " + connected + " " + exception, "red");
			fail(exception.toString());
			stop();
		}
		
		@Override
		public void onHeartbeat() {
			double heartbeatTime = Duration.currentTimeMillis();
			heartbeatCount++;
			output("heartbeat " + heartbeatCount + " " + (heartbeatTime - connectedTime) + "ms", "silver");
		}
		
		@Override
		public void onRefresh() {
			double refreshTime = Duration.currentTimeMillis();
			refreshCount++;
			output("refresh " + refreshCount + " " + (refreshTime - connectedTime) + "ms", "silver");
		}
		
		@Override
		public void onMessage(List<? extends Serializable> messages) {
			messagesCount++;
			messageCount += messages.size();
		}
		
		void assertTrue(String message, boolean b) {
			if (!b) {
				fail(message);
			}
			else {
				pass = true;
			}
		}
		
		void assertEquals(String message, Object expected, Object actual) {
			if (!expected.equals(actual)) {
				fail(message + " expected " + expected + " actual " + actual);
			}
			else {
				pass = true;
			}
		}
		
		void pass() {
			pass = true;
		}
		
		void fail(String message) {
			if (failure == null) {
				failure = message;
			}
			else {
				failure += "\n" + message;
			}
		}
	}
	
	class ConnectionTest extends CometTest {
		
		private final int connectionTime = 120 * 1000;
		
		ConnectionTest(boolean session) {
			super("heartbeat and session keep alive", session);
		}
		
		@Override
		void start() {
			String url = GWT.getModuleBaseURL() + "connection?delay=" + connectionTime;
			super.start(url);
		}
		
		@Override
		void stop() {
			assertTrue("connection time", disconnectedTime - connectedTime >= connectionTime - 100);
			outputStats();
			super.stop();
		}
	}
	
	class ReconnectionTest extends CometTest {
		
		private final int connectionTime = 120 * 1000;
		
		ReconnectionTest(boolean session) {
			super("reconnection", session);
		}
		
		@Override
		void start() {
			String url = GWT.getModuleBaseURL() + "connection?delay=" + connectionTime;
			super.start(url);
		}
		
		@Override
		public void onConnected(int heartbeat) {
			connectedTime = Duration.currentTimeMillis();
			connectedCount++;
			if (connectedCount > 1) {
				pass();
				stop();
			}
			else {
				output("connected " + connectedCount + " " + (connectedTime - startTime) + "ms heartbeat: " + heartbeat, "silver");
				output("stop your server now!", "blue");
			}
		}
		
		@Override
		public void onError(Throwable exception, boolean connected) {
			double errorTime = Duration.currentTimeMillis();
			errorCount++;
			output("error " + errorCount + " " + (errorTime - startTime) + "ms " + connected + " " + exception, "silver");
			output("start your server now!", "blue");
		}
	}
	
	class ErrorTest extends CometTest {
		
		ErrorTest() {
			super("error", false);
		}
		
		@Override
		void start() {
			String url = GWT.getModuleBaseURL() + "error";
			super.start(url);
		}
		
		@Override
		public void onError(Throwable exception, boolean connected) {
			double errorTime = Duration.currentTimeMillis();
			errorCount++;
			output("error " + errorCount + " " + (errorTime - startTime) + "ms " + connected + " " + exception, "lime");
			assertTrue("status code exception", exception instanceof StatusCodeException);
			if (exception instanceof StatusCodeException) {
				assertEquals("status code", 417, ((StatusCodeException) exception).getStatusCode());
				assertEquals("status message", "Oh Noes!", ((StatusCodeException) exception).getEncodedResponse());
			}
			stop();
		}
	}
	
	class EscapeTest extends CometTest {
		
		private final SerialMode mode;
		
		EscapeTest(SerialMode mode) {
			super("escape mode=" + mode, false);
			this.mode = mode;
		}
		
		@Override
		void start() {
			String url = GWT.getModuleBaseURL() + "escape";
			
			super.start(url, mode);
		}
		
		@Override
		public void onMessage(List<? extends Serializable> messages) {
			super.onMessage(messages);
			pass();
			for (Serializable m : messages) {
				String type;
				String message;
				if (m instanceof TestData) {
					type = "gwt serialized object";
					message = ((TestData) m).s;
				}
				else if (m instanceof String) {
					type = "string";
					message = (String) m;
				}
				else if (m == null) {
					continue;
				}
				else {
					fail("unexpected object " + m.getClass() + " " + m);
					continue;
				}
				
				if (ESCAPE.length() != message.length()) {
					fail(type + " expected message length " + ESCAPE.length() + " acutal " + message.length());
				}
				else {
					for (int i = 0; i < ESCAPE.length(); i++) {
						char expected = ESCAPE.charAt(i);
						char actual = message.charAt(i);
						if (expected != actual) {
							fail(type + " expected character " + expected + " 0x" + Integer.toHexString(expected) + " actual " + actual + " 0x" + Integer.toHexString(actual));
						}
					}
				}
			}
		}
	}
	
	abstract class MessagingTest extends CometTest {
		
		private final String url;
		private final boolean refresh;
		private final SerialMode mode;
		
		private final int count;
		private final int batch;
		private final int delay;
		
		MessagingTest(String name, boolean session, boolean refresh, SerialMode mode, int count, int batch, int delay) {
			super(name + " refresh=" + refresh + " mode=" + mode, session);
			this.url = name;
			this.refresh = refresh;
			this.mode = mode;
			this.count = count;
			this.batch = batch;
			this.delay = delay;
		}
		
		@Override
		void start() {
			String url = GWT.getModuleBaseURL() + this.url + "?count=" + count + "&batch=" + batch;
			if (mode == null) {
				url += "&mode=string";
			}
			if (!refresh) {
				url += "&length=" + (count * batch * 10000);
			}
			
			url += "&delay=" + delay;
			
			super.start(url, mode);
		}
		
		@Override
		void stop() {
			assertTrue("count", count * batch == messageCount);
			outputStats();
			super.stop();
		}
	}
	
	class ThroughputTest extends MessagingTest {
		
		ThroughputTest(boolean session, boolean refresh, SerialMode mode) {
			super("throughput", session, refresh, mode, 1000, 10, 0);
		}
	}
	
	class LatencyTest extends MessagingTest {
		
		private double latency;
		private double min = Double.MAX_VALUE;
		private double max = Double.MIN_VALUE;
		
		LatencyTest(boolean session, boolean refresh, SerialMode mode) {
			super("latency", session, refresh, mode, 1000, 1, 10);
		}
		
		@Override
		void reset() {
			super.reset();
			latency = 0;
			min = Double.MAX_VALUE;
			max = Double.MIN_VALUE;
		}
		
		@Override
		public void onMessage(List<? extends Serializable> messages) {
			super.onMessage(messages);
			double now = Duration.currentTimeMillis();
			for (Serializable m : messages) {
				double message;
				if (m instanceof TestData) {
					message = ((TestData) m).d;
				}
				else if (m instanceof String) {
					message = Double.parseDouble((String) m);
				}
				else {
					continue;
				}
				double messageLatency = now - message;
				latency += messageLatency;
				if (messageLatency < min) {
					min = messageLatency;
				}
				if (messageLatency > max) {
					max = messageLatency;
				}
				if (messageLatency > 250) {
					output("latency " + messageLatency, "red");
				}
			}
		}
		
		@Override
		void outputStats() {
			super.outputStats();
			output("latency   : " + latency / messageCount + "ms", "black");
			output("min       : " + min + "ms", "black");
			output("max       : " + max + "ms", "black");
		}
	}
	
	class OrderTest extends MessagingTest {
		
		OrderTest(boolean session, boolean refresh, SerialMode mode) {
			super("order", session, refresh, mode, 1000, 1, 0);
		}
		
		@Override
		public void onMessage(List<? extends Serializable> messages) {
			int count = messageCount;
			super.onMessage(messages);
			for (Serializable m : messages) {
				double message;
				if (m instanceof TestData) {
					message = ((TestData) m).d;
				}
				else if (m instanceof String) {
					message = Double.parseDouble((String) m);
				}
				else {
					continue;
				}
				
				assertTrue("expected count " + count + " actual " + message, count == message);
				count++;
			}
		}
	}
	
	class PaddingTest extends CometTest {
		
		int min;
		int max;
		int padding;
		
		PaddingTest() {
			super("padding", false);
		}

		@Override
		void start() {
			doTest(0, 8 * 1024);
		}
		
		private void doTest(int min, int max) {
			if (min == max) {
				output("padding required: " + min, "lime");
				pass();
				stop();
			}
			else {
				this.min = min;
				this.max = max;
				this.padding = (min + max) / 2;
				output("padding test: " + min + " " + max + " " + padding, "silver");
				String url = GWT.getModuleBaseURL() + "connection?delay=60000&padding=" + padding;
				
				doStart(url, null);
			}
		}
		
		@Override
		public void onConnected(int heartbeat) {
			output("connected", "silver");
			doStop();
			doTest(min, padding - 1);
		}
		
		@Override
		public void onDisconnected() {
			output("disconnected", "silver");
		}
		
		@Override
		public void onError(Throwable exception, boolean connected) {
			output("error " + exception.toString(), "silver");
			doStop();
			doTest(padding + 1, max);
		}
	}
	
	class SlowBrowserTest extends MessagingTest {
		
		SlowBrowserTest() {
			super("slowbrowser", true, true, null, 12000, 1, 0);
		}
		
		@Override
		public void onMessage(List<? extends Serializable> messages) {
			super.onMessage(messages);
			int waitTime = messages.size() * 10;
			double time = Duration.currentTimeMillis() + waitTime;
			while (Duration.currentTimeMillis() < time) {
			}
			output("waited " + waitTime + "ms " + messages.size() + "messages", "black");
		}
	}
	
	private static String string(Throwable exception) {
		StringBuilder result = new StringBuilder(exception.toString());
		exception = exception.getCause();
		while (exception != null) {
			result.append("\n").append(exception.toString());
			exception = exception.getCause();
		}
		return result.toString();
	}
	
	public void output(String text, String color) {
		DivElement div = Document.get().createDivElement();
		div.setInnerText(text);
		div.setAttribute("style", "font-family:monospace;white-space:pre;color:" + color);
		messages.getElement().appendChild(div);
		scrollPanel.scrollToBottom();
	}
}
