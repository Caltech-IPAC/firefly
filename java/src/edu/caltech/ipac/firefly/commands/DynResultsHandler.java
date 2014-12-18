package edu.caltech.ipac.firefly.commands;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.dyn.xstream.SearchTypeTag;
import edu.caltech.ipac.firefly.ui.Form;
import edu.caltech.ipac.firefly.ui.table.EventHub;
import edu.caltech.ipac.firefly.ui.table.builder.PrimaryTableUILoader;

import java.util.List;

/**
 * Date: Sep 17, 2014
 *
 * @author loi
 * @version $Id: EventWorker.java,v 1.6 2012/02/27 18:43:20 roby Exp $
 */
public interface DynResultsHandler {

    Widget processRequest(final Request inputReq, AsyncCallback<String> callback, EventHub hub, Form form, PrimaryTableUILoader loader, SearchTypeTag searchTypeTag);
}
