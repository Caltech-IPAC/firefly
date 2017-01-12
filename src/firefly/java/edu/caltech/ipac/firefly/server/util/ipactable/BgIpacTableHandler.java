package edu.caltech.ipac.firefly.server.util.ipactable;

import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.events.FluxAction;
import edu.caltech.ipac.firefly.server.events.ServerEventManager;
import edu.caltech.ipac.util.*;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;

/**
 * Date: 11/21/16
 *
 * @author loi
 * @version $Id: $
 */
public class BgIpacTableHandler extends DataGroupWriter.IpacTableHandler {
    private final CountDownLatch waitOn = new CountDownLatch(1);
    private TableServerRequest request;
    private int maxInteractiveRows;
    private Timer timer;

    public BgIpacTableHandler(File ofile, DataGroup source, TableServerRequest request) {
        this(ofile, Arrays.asList(source.getDataDefinitions()), source.getKeywords(), source.iterator(), request);
    }

    public BgIpacTableHandler(File ofile,  List<DataType> headers, List<DataGroup.Attribute> attributes, Iterator<DataObject> itr, TableServerRequest request) {
        super(ofile, headers, attributes, itr);
        this.request = request;
        maxInteractiveRows = request.getPageSize() < 0 ? Integer.MAX_VALUE : request.getPageSize();
    }

    public void onStart() throws InterruptedException {
        waitOn.await();
    }

    public void onComplete() {
        waitOn.countDown();
        if (timer != null) timer.cancel();
        sendLoadStatusEvents(request.getMeta(), getOutFile(), getRowCount(), DataGroupPart.State.COMPLETED);
    }

    @Override
    public DataObject next() throws IOException {
        DataObject row = getNextRow();
        if (getRowCount() >= maxInteractiveRows && timer == null) {
            // max interactive has reached... stop waiting, and start sending status via server events.
            waitOn.countDown();
            timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                public void run() {
                    sendLoadStatusEvents(request.getMeta(), getOutFile(), getRowCount(), DataGroupPart.State.INPROGRESS);
                }
            }, 2000, 5000);
        }
        return row;
    }

    protected DataObject getNextRow() throws IOException {
        return super.next();
    }


    /**
     * Send an action event message to the client updating the status of a table read/write.
     */
    protected void sendLoadStatusEvents(Map<String,String> meta, File outf, int crows, DataGroupPart.State state) {
        if (meta == null || StringUtils.isEmpty(meta.get("tbl_id"))) return;

        String tblId = String.valueOf( meta.get("tbl_id") );
        FluxAction action = new FluxAction("table.update");
        action.setValue(tblId, "tbl_id");
        action.setValue(crows, "totalRows");
        action.setValue(state.name(), "tableMeta", DataGroupPart.LOADING_STATUS);
        ServerEventManager.fireAction(action);
    }
}





/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
* OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS,
* HOWEVER USED.
*
* IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE
* FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL
* OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO
* PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE
* ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
*
* RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE
* AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR
* ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE
* OF THE SOFTWARE.
*/
