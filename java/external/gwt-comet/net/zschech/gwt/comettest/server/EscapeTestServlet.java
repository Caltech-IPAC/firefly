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
package net.zschech.gwt.comettest.server;

import java.io.IOException;
import java.io.Serializable;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import net.zschech.gwt.comet.server.CometServlet;
import net.zschech.gwt.comet.server.CometServletResponse;
import net.zschech.gwt.comettest.client.CometTestEntryPoint;
import net.zschech.gwt.comettest.client.CometTestEntryPoint.TestData;

public class EscapeTestServlet extends CometServlet {
	
	@Override
	protected void doComet(CometServletResponse cometResponse) throws ServletException, IOException {
		HttpServletRequest request = cometResponse.getRequest();
		String mode = request.getParameter("mode");
		
		if ("string".equals(mode)) {
			cometResponse.write(CometTestEntryPoint.ESCAPE);
		}
		else {
			cometResponse.write(new TestData(0, CometTestEntryPoint.ESCAPE));
			cometResponse.write((Serializable) null);
		}
		cometResponse.terminate();
	}
}
