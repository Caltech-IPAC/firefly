package edu.caltech.ipac.firefly.visualize.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.*;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.GeneralCommand;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.ui.BaseDialog;
import edu.caltech.ipac.firefly.ui.ButtonType;
import edu.caltech.ipac.firefly.ui.PopupPane;
import edu.caltech.ipac.firefly.ui.PopupType;
import edu.caltech.ipac.util.dd.ValidationException;
import edu.caltech.ipac.firefly.util.WebUtil;


/**
 * Created by IntelliJ IDEA.
 * User: balandra
 * Date: Jan 14, 2010
 * Time: 2:38:21 PM
 * To change this template use File | Settings | File Templates.
 */
public class TagsDialog extends BaseDialog {

    private static final String INFO_MSG_STYLE= "info-msg";
    public static final String TAG_NAME = "tagName";
    public static final String TAG_DESC = "tagDesc";
    public static final String TAG_URL = "tagUrl";
    public static final String tagToFile = GWT.getModuleBaseURL() + "servlet/TagToFile";
    GeneralCommand cmd;

//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public TagsDialog (String tagName,
                       String tagDesc,
                       String url) {
        super(null, ButtonType.OK_CANCEL, PopupType.STANDARD, tagName, false, false, null);
        createSaveCmd(tagName, tagDesc, url);
        Button b = this.getButton(BaseDialog.ButtonID.CANCEL);        
        b.setText("Save to File");
        b.addClickHandler(new ClickHandler(){
            public void onClick(ClickEvent clickEvent) {
                cmd.execute();
            }
        });
        createContents(tagName,url);

    }

//======================================================================
//----------------------- Static Methods -------------------------------
//======================================================================

    public static void showTagInfo(String tagName,
                                   String tagDesc,
                                   String url) {

        new TagsDialog(tagName, tagDesc, url).setVisible(true);
    }
//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    @Override
    public void setVisible(boolean v) {
        if (v) {

        }
        super.setVisible(v, PopupPane.Align.CENTER);
    }

//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    private void createContents(String tagName, String url) {
       String msg = "Tag name: <b>" + tagName + "</b><br><br>" +
                    "URL: " + url;
       this.setWidget(makeMsg(msg, INFO_MSG_STYLE));
    }

    private void createSaveCmd(final String tagName,final String tagDesc,final String tagUrl){
        cmd = new GeneralCommand("saveTag"){
            protected void doExecute() {
                Frame f = Application.getInstance().getNullFrame();
                f.setUrl(createURL(tagName, tagDesc, tagUrl));
            }
        };
    }

    private String createURL(String tagName, String tagDesc, String tagUrl){
        Param tn = new Param( TAG_NAME, tagName);
        Param td = new Param( TAG_DESC, tagDesc);
        Param tu = new Param( TAG_URL, tagUrl);
        return WebUtil.encodeUrl(tagToFile, tn, td, tu);

    }

    private static Widget makeMsg(String s, String msgStyle) {
        String hStr= "<span style=\"font-size: 120%;\">";
        if(s.length() > 400){
          s = "<div style='width: 350px; height: 250px; overflow: auto;'>" + s + "</div>";
        }
        HTML message = new HTML(hStr+ s + "</span>");
        message.setStyleName(msgStyle);
        SimplePanel panel = new SimplePanel();
        panel.add(message);
        return panel;//new ScrollPanel(message);
    }

    protected void inputComplete() {
    }


    protected void inputCanceled() {
    }

    protected boolean validateInput() throws ValidationException {
        return true;
    }
}
