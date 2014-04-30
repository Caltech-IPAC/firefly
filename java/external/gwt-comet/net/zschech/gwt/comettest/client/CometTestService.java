package net.zschech.gwt.comettest.client;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("cometTestService")
public interface CometTestService extends RemoteService {
	public boolean createSession();
	public boolean invalidateSession();
}
