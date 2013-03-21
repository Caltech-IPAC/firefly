package edu.caltech.ipac.firefly.ui.background;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.TextResource;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.background.BackgroundReport;
import edu.caltech.ipac.firefly.rpc.SearchServices;
import edu.caltech.ipac.firefly.ui.BaseDialog;
import edu.caltech.ipac.firefly.ui.ButtonType;
import edu.caltech.ipac.firefly.ui.Form;
import edu.caltech.ipac.firefly.ui.FormBuilder;
import edu.caltech.ipac.firefly.ui.FormHub;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.ServerTask;
import edu.caltech.ipac.firefly.util.PropFile;
import edu.caltech.ipac.firefly.util.WebClassProperties;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
/**
 * User: roby
 * Date: Sep 10, 2010
 * Time: 11:45:11 AM
 */


/**
 * @author Trey Roby
 */
public class DownloadScriptDialog extends BaseDialog {

    interface PFile extends PropFile { @ClientBundle.Source("DownloadScriptDialog.prop") TextResource get(); }
    private static final WebClassProperties _prop= new WebClassProperties(DownloadScriptDialog.class, (PFile)GWT.create(PFile.class));
    private static final String DOWN_ID= _prop.makeBase("downloader");
    private static final String UNCOMP_ID= _prop.makeBase("uncompressor");
    private static final String WGET = "wget";
    private static final String CURL = "curl";
    private static final String UNZIP = "unzip";
    private static final String DITTO = "ditto";
    private static final String DONT = "dont";
    private static int _cnt= 1;


    private final VerticalPanel _main= new VerticalPanel();
//    private final FireflyCss _ffCss=CssData.Creator.getInstance().getFireflyCss();

    private final CheckBox _urlOnly= GwtUtil.makeCheckBox(_prop.makeBase("urlOnly"));
    private final CheckBox _removeZip= GwtUtil.makeCheckBox(_prop.makeBase("removeZip"));
    private final Form _form = new Form();
    private final String _id;
    private final String _dataSource;

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    public DownloadScriptDialog(Widget p, String id, String dataSource) {
        super(p, ButtonType.OK_CANCEL,_prop.getTitle(), "downloads.DownloadScript");
        _id= id;
        _dataSource= dataSource;
        this.setWidget(_main);
        createContents();
    }

    public static void show(Widget p, String id, String dataSource) {
        DownloadScriptDialog d= new DownloadScriptDialog(p,id,dataSource);
        d.setVisible(true);
    }


//=======================================================================
//-------------- Methods from BaseDialog ----------------------
//=======================================================================

    private void createContents() {
        VerticalPanel vp= new VerticalPanel();
        vp.add(FormBuilder.createPanel(150 , DOWN_ID, UNCOMP_ID));
        _form.add(vp);

        _main.add(_form);


        VerticalPanel toggles= new VerticalPanel();
        toggles.setSpacing(5);
        toggles.add(_urlOnly);
        toggles.add(_removeZip);

        _main.add(GwtUtil.centerAlign(toggles));

        updateRemoveZip();

        _urlOnly.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                FormHub hub= _form.getHub();
                boolean h= _urlOnly.getValue();
                hub.setHidden(DOWN_ID, h);
                hub.setHidden(UNCOMP_ID, h);
                updateRemoveZip();
            }
        });


        _form.getHub().getEventManager().addListener(FormHub.FIELD_VALUE_CHANGE, new WebEventListener() {
            public void eventNotify(WebEvent ev) {
                updateRemoveZip();
            }
        });

    }

    private void updateRemoveZip() {
        String uValue= _form.getValue(UNCOMP_ID);
        GwtUtil.setHidden(_removeZip, uValue.equals(DONT) || _urlOnly.getValue());
    }


    @Override
    protected void inputComplete() {
        List<BackgroundReport.ScriptAttributes> list= new ArrayList<BackgroundReport.ScriptAttributes>(4);
        String fname;
        if (_urlOnly.getValue()) {
            list.add(BackgroundReport.ScriptAttributes.URLsOnly);
            fname= "urlList" + _cnt;
        }
        else {
            String dValue= _form.getValue(DOWN_ID);

            fname= "downloader" + _cnt;
            if (!StringUtils.isEmpty(dValue)) {
                if (dValue.equals(WGET)) {
                    list.add(BackgroundReport.ScriptAttributes.Wget);
                }
                else if (dValue.equals(CURL)) {
                    list.add(BackgroundReport.ScriptAttributes.Curl);
                }


            }

            String uValue= _form.getValue(UNCOMP_ID);
            if (!StringUtils.isEmpty(dValue)) {
                if (uValue.equals(UNZIP)) {
                    list.add(BackgroundReport.ScriptAttributes.Unzip);
                }
                else if (uValue.equals(DITTO)) {
                    list.add(BackgroundReport.ScriptAttributes.Ditto);
                }

                if (!uValue.equals(DONT) && _removeZip.getValue()) {
                    list.add(BackgroundReport.ScriptAttributes.RemoveZip);
                }
            }
        }
        if (list.size()>0) {
            DownloadScriptTask task= new DownloadScriptTask(this.getDialogWidget(),_id,_dataSource,fname, list);
            task.start();
        }
        _cnt++;
    }



    private class DownloadScriptTask extends ServerTask<String> {

        private final String _id;
        private final String _fname;
        private final String _dataSource;
        private final List<BackgroundReport.ScriptAttributes> _aList;

        DownloadScriptTask(Widget w,
                           String id,
                           String dataSource,
                           String fname,
                           List<BackgroundReport.ScriptAttributes> aList) {
            super(w,"Building Script", true);
            _id= id;
            _fname= fname;
            _aList= aList;
            _dataSource= dataSource;
        }


        public void onSuccess(String url) {
            if (!StringUtils.isEmpty(url)) {
                url= GWT.getModuleBaseURL()+url;
                Frame f= Application.getInstance().getNullFrame();
                f.setUrl(url);
            }
        }

        public void doTask(AsyncCallback<String> passAlong) {
            SearchServices.App.getInstance().createDownloadScript(_id, _fname , _dataSource, _aList, passAlong);
        }
    }


}

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313) 
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
