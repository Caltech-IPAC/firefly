package edu.caltech.ipac.firefly.data.packagedata;


import edu.caltech.ipac.firefly.core.background.BackgroundState;
import edu.caltech.ipac.firefly.core.background.DefaultBackgroundPart;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.TableServerRequest;

public class SearchBundle extends DefaultBackgroundPart {

    private Request clientRequest;
    private TableServerRequest serverRequest;

    private SearchBundle() {
    }

    public SearchBundle(TableServerRequest serverRequest, Request clientRequest, BackgroundState state) {
        super(state);
        this.clientRequest = clientRequest;
        this.serverRequest = serverRequest;
    }

    public Request getClientRequest() {
        return clientRequest;
    }

    public TableServerRequest getServerRequest() {
        return serverRequest;
    }
}

