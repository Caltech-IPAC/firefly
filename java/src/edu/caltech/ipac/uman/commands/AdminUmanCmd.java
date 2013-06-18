package edu.caltech.ipac.uman.commands;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.CommonRequestCmd;
import edu.caltech.ipac.firefly.core.GeneralCommand;
import edu.caltech.ipac.firefly.core.RPCException;
import edu.caltech.ipac.firefly.core.layout.LayoutManager;
import edu.caltech.ipac.firefly.core.layout.Region;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.firefly.data.table.RawDataSet;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.userdata.UserInfo;
import edu.caltech.ipac.firefly.rpc.SearchServices;
import edu.caltech.ipac.firefly.rpc.UserServices;
import edu.caltech.ipac.firefly.ui.Form;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.ServerTask;
import edu.caltech.ipac.firefly.ui.creator.PrimaryTableUI;
import edu.caltech.ipac.firefly.ui.creator.WidgetFactory;
import edu.caltech.ipac.firefly.ui.panels.SearchPanel;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.ui.table.TablePreviewEventHub;
import edu.caltech.ipac.firefly.ui.table.builder.PrimaryTableUILoader;
import edu.caltech.ipac.firefly.util.DataSetParser;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static edu.caltech.ipac.uman.data.UmanConst.ADMIN_ROLE;
import static edu.caltech.ipac.uman.data.UmanConst.BACK_TO_URL;
import static edu.caltech.ipac.uman.data.UmanConst.TITLE_AREA;

/**
 * @author loi
 * $Id: UmanCmd.java,v 1.13 2012/11/19 22:05:43 loi Exp $
 */
abstract public class AdminUmanCmd extends UmanCmd {

    public AdminUmanCmd(String command) {
        super(command);
    }

    @Override
    protected void checkAccess(Request req, AsyncCallback<String> callback) {
        doCheckAccess(ADMIN_ROLE, req, callback);
    }

    @Override
    protected Form createForm() {
        return null;
    }

    @Override
    protected void doExecute(Request req, AsyncCallback<String> callback) {

        final String backTo = req.getParam(BACK_TO_URL);
        if (!StringUtils.isEmpty(backTo)) {
            Application.getInstance().setAppData(BACK_TO_URL, backTo);
            // workaround for bad url parsing
            req.setDoSearch(false);
        }

        setStatus("", false);
        setResults(getForm());

        req.setDoSearch(getAutoSubmit());
        checkAccess(req, callback);
    }

    protected boolean getAutoSubmit() {
        return true;
    }

    protected int getInt(TableData.Row row, String cname ) {
        try {
            return Integer.parseInt((String) row.getValue(cname));
        } catch (Exception e) {
            return -1;
        }
    }

    protected String getString(TableData.Row row, String cname ) {
        Object v = row.getValue(cname);
        return v == null ? "" : v.toString().trim();
    }

}
