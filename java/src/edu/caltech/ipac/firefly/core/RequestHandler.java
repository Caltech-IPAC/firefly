package edu.caltech.ipac.firefly.core;

import com.google.gwt.event.logical.shared.ValueChangeHandler;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.ui.StatefulWidget;

/**
 * This class is responsible for handling back/forward buttons as well as the bookmark feature.
 * In doing so, it serves as the controller of this application.  The application flow is based
 * on a command and a request.  A command dictate where the request should be forward to.
 * A request contains all of the information necessary to process it.
 * In order to be generic, it defers the format of the URL to its implementing class.
 */
public interface RequestHandler extends ValueChangeHandler<String> {

    enum Context {PAGE, INCL_SEARCH}

    public void processRequest(Request req);

    public Request parse(String str);

    public void registerComponent(String name, StatefulWidget b);
    public void registerComponent(String name, Context context, StatefulWidget b);
    public void moveToRequestState(Request req);
    public String getStateInfo(Context context);

    public Request getCurrentRequest();
    Request getCurrentSearchRequest();
    SearchDescResolver getSearchDescResolver();
}