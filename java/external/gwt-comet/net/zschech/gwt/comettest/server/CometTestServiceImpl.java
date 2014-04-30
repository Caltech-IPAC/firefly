package net.zschech.gwt.comettest.server;

import javax.servlet.http.HttpSession;

import net.zschech.gwt.comet.server.CometServlet;
import net.zschech.gwt.comet.server.CometSession;
import net.zschech.gwt.comettest.client.CometTestService;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public class CometTestServiceImpl extends RemoteServiceServlet implements CometTestService {
	
	@Override
	public boolean createSession() {
		HttpSession httpSession = getThreadLocalRequest().getSession();
		CometSession cometSession = CometServlet.getCometSession(httpSession, false);
		if (cometSession == null) {
			CometServlet.getCometSession(httpSession);
			return true;
		}
		return false;
	}
	
	@Override
	public boolean invalidateSession() {
		HttpSession httpSession = getThreadLocalRequest().getSession(false);
		if (httpSession == null) {
			return false;
		}
		
		CometSession cometSession = CometServlet.getCometSession(httpSession);
		if (cometSession == null) {
			return false;
		}
		cometSession.invalidate();
		return true;
	}
}
