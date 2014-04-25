package net.zschech.gwt.comettest.server;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import net.zschech.gwt.comet.server.CometServlet;
import net.zschech.gwt.comet.server.CometServletResponse;
import net.zschech.gwt.comet.server.CometSession;
import net.zschech.gwt.comettest.client.CometTestEntryPoint.TestData;

public class MessagingTestServlet extends CometServlet {
	
	@Override
	protected void doComet(final CometServletResponse cometResponse) throws ServletException, IOException {
		HttpServletRequest request = cometResponse.getRequest();
		final int connectionCount = Integer.parseInt(request.getParameter("c"), Character.MAX_RADIX);
		final boolean session = "true".equals(request.getParameter("session"));
		final int count = Integer.parseInt(request.getParameter("count"));
		final int batch = Integer.parseInt(request.getParameter("batch"));
		final int delay = Integer.parseInt(request.getParameter("delay"));
		final boolean string = "string".equals(request.getParameter("mode"));
		final CometSession cometSession = cometResponse.getSession(false);
		final boolean order = request.getRequestURI().endsWith("order");
		
		if (session && cometSession == null) {
			cometResponse.terminate();
			return;
		}
		
		if (session && connectionCount != 1) {
			return;
		}
		
		new Thread() {
			public void run() {
				try {
					if (cometSession == null) {
						for (int i = 0; i < count; i++) {
							if (!cometResponse.isTerminated()) {
								
								if (batch > 1) {
									List<Serializable> messages = new ArrayList<Serializable>(batch);
									for (int b = 0; b < batch; b++) {
										messages.add(getMessage(string, order, i * batch + b));
									}
									synchronized (cometResponse) {
										if (!cometResponse.isTerminated()) {
											cometResponse.write(messages);
										}
									}
								}
								else {
									synchronized (cometResponse) {
										if (!cometResponse.isTerminated()) {
											cometResponse.write(getMessage(string, order, i));
										}
									}
								}
								
								if (delay > 0) {
									try {
										sleep(delay);
									}
									catch (InterruptedException e) {
										throw new InterruptedIOException();
									}
								}
							}
						}
						cometResponse.terminate();
					}
					else {
						for (int i = 0; i < count; i++) {
							for (int b = 0; b < batch; b++) {
								cometSession.enqueue(getMessage(string, order, i * batch + b));
							}
							
							if (delay > 0) {
								try {
									sleep(delay);
								}
								catch (InterruptedException e) {
									throw new InterruptedIOException();
								}
							}
						}
						// there is no proper way to wait for the queue to drain :-(
						while (cometSession.isValid() && !cometSession.getQueue().isEmpty()) {
							try {
								sleep(1);
							}
							catch (InterruptedException e) {
								throw new InterruptedIOException();
							}
						}
						cometSession.invalidate();
					}
				}
				catch (IOException e) {
					log("Error writing data", e);
				}
			}
		}.start();
	}
	
	protected Serializable getMessage(boolean string, boolean order, int i) {
		double data = order ? i : System.currentTimeMillis();
		return string ? String.valueOf(data) : new TestData(data, null);
	}
}
