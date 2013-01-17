package edu.caltech.ipac.firefly.commands;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.VerticalPanel;
import edu.caltech.ipac.firefly.core.RequestCmd;
import edu.caltech.ipac.firefly.core.layout.LayoutManager;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.visualize.graph.XYGraphWidget;


public class XYCmd extends RequestCmd {

    public static final String COMMAND = "XYCmd";
    private VerticalPanel _main;
    private XYGraphWidget _xy = new XYGraphWidget();


    public XYCmd() {
        super(COMMAND);
    }




    public boolean init() {

        _main = new VerticalPanel();
        _main.setSpacing(20);

        _main.add(_xy);
        registerView(LayoutManager.DROPDOWN_REGION, _main);
        return true;
    }

    protected void doExecute(Request req, AsyncCallback<String> callback) {
        _xy.makeNewChart();
        _xy.update();

    }



}