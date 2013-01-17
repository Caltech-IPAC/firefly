package edu.caltech.ipac.heritage.commands;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.RequestCmd;
import edu.caltech.ipac.firefly.core.layout.LayoutManager;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.Version;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.util.WebUtil;

/**
 * Date: Sep 18, 2008
 *
 * @author loi
 * @version $Id: HeritageHomeCmd.java,v 1.12 2012/08/23 20:30:11 roby Exp $
 */
public class HeritageHomeCmd extends RequestCmd {
    public static final String COMMAND_NAME = "HeritageHome";
    public static final String HOME_URL = WebUtil.encodeUrl(GWT.getModuleBaseURL() + "heritage_home.html");

    private HTML main;
    private Label versionLabel;
    private FlowPanel view;

    public HeritageHomeCmd() {
        super(COMMAND_NAME);

        view = new FlowPanel();
        main = new HTML();
        versionLabel= new Label();

        main.setWidth("800px");
        DOM.setElementAttribute(view.getElement(), "align", "center");
        DOM.setElementAttribute(main.getElement(), "align", "left");
        view.add(main);
        view.add(GwtUtil.getFiller(1, 40));
        view.add(versionLabel);
    }

    @Override
    public boolean init() {

        RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, HOME_URL);
        try {
            builder.sendRequest(null, new RequestCallback(){
                public void onResponseReceived(com.google.gwt.http.client.Request req, Response res) {
                    main.setHTML(res.getText());
                    DOM.setStyleAttribute(versionLabel.getElement(),"paddingTop", "5px");
                    DOM.setStyleAttribute(versionLabel.getElement(),"float", "right");


                    Application.getInstance().findVersion(new AsyncCallback<Version>() {
                        public void onFailure(Throwable caught) { }

                        public void onSuccess(Version v) {
                            versionLabel.setText(v.toString());
                        }
                    });
                }

                public void onError(com.google.gwt.http.client.Request request, Throwable exception) {
                }
            });
        } catch (RequestException e) {
            e.printStackTrace();
        }

        registerView(LayoutManager.CONTENT_REGION, view);
        return true;
    }

    /**
     * Override execute from RequestCmd because execute uses a GWT.runAsync and don't want to force other code to load
     * @param req the Reqeust
     * @param callback the callback
     */
    @Override
    public void execute(Request req, final AsyncCallback<String> callback) {
        if (!isInit()) {
            setInit(init());
        }
        callback.onSuccess("success!");
    }

    protected void doExecute(Request req, AsyncCallback<String> callback) {
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
