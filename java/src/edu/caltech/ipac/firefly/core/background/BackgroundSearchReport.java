package edu.caltech.ipac.firefly.core.background;

import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.packagedata.SearchBundle;

import java.util.Arrays;


public class BackgroundSearchReport extends BackgroundReport {

    private String filePath;

    private BackgroundSearchReport() {}

    public BackgroundSearchReport(String packageID,
                                     BackgroundState state, Request clientRequest,
                                     TableServerRequest request) {
        super(packageID, Arrays.asList((BackgroundPart)new SearchBundle(request, clientRequest,state)), state);
    }

    public SearchBundle getSearchBundle() {
        SearchBundle retval= null;
        if (getState()==BackgroundState.SUCCESS) {
            if (getPartCount() > 0) {
                retval= (SearchBundle)get(0);
            }
        }
        return retval;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public boolean hasFileKey() {
        return filePath != null;
    }


    @Override
    public String getFileKey() {
        return filePath;
    }

    public TableServerRequest getServerRequest() {
        SearchBundle sb = getSearchBundle();
        return sb == null ? null : sb.getServerRequest();
    }

    public Request getClientRequest() {
        SearchBundle sb = getSearchBundle();
        return sb == null ? null : sb.getClientRequest();
    }

    @Override
    public String serialize() {
        return super.serialize();    //To change body of overridden methods use File | Settings | File Templates.
    }
}

