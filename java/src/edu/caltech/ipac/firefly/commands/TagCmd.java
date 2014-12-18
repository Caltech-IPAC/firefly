package edu.caltech.ipac.firefly.commands;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.GeneralCommand;
import edu.caltech.ipac.firefly.core.RequestCmd;
import edu.caltech.ipac.firefly.core.RequestHandler;
import edu.caltech.ipac.firefly.core.BaseCallback;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.TagInfo;
import edu.caltech.ipac.firefly.rpc.UserServices;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.visualize.ui.TagsDialog;
import edu.caltech.ipac.util.StringUtils;

/**
 * @author loi
 * @version $Id: TagCmd.java,v 1.16 2011/10/28 19:04:31 loi Exp $
 */
public class TagCmd extends RequestCmd {
    public static final String TAG_NAME = "tagName";
    public static final String COMMAND_NAME = "tag";

    TagResolver _tagResolver; // lasy instantiation - use getTagResolver()

    public TagCmd() {
        super(COMMAND_NAME, "Query Tag");

    }

    public void setTagReslover(TagResolver resolver) {
        _tagResolver = resolver;
    }

    public TagResolver getTagResolver() {
        if (_tagResolver == null) {
            _tagResolver = new DefaultTagResolver();
        }
        return _tagResolver;
    }

    protected void doExecute(Request req, AsyncCallback<String> callback) {

        final String tagName = req.getParam(TAG_NAME);
        if (StringUtils.isEmpty(tagName)) {
        } else {
            getTagResolver().resolve(tagName);
        }

    }

    public static String makeUrlString(String tagName) {
        return GWT.getHostPageBaseURL() + "#" + Request.ID_KEY + "=tag&" + TAG_NAME + "=" + tagName;
    }

    public static class TagItCmd extends GeneralCommand {
        public static final String COMMAND_NAME = "tagIt";

        public TagItCmd() {
            super(COMMAND_NAME);
        }

        protected void doExecute() {

            if (Application.getInstance().hasSearchResult()) {
                PopupUtil.showInputDialog(null, "Enter a description for this tag:", "", new ClickHandler(){
                    public void onClick(ClickEvent event) {
                        doTag(String.valueOf(event.getSource()), new BaseCallback(){
                                    public void doSuccess(Object result) {}});
                    }
                }, null);

            }
            else {
                PopupUtil.showInfo(null, "Query First",
                                   "Do a query and get some results then " +
                        "you can tag them for later use ");

            }
        }

        public static void doTag(String desc, final AsyncCallback callback) {
            String queryString = Application.getInstance().getRequestHandler().getStateInfo(RequestHandler.Context.INCL_SEARCH);
            UserServices.App.getInstance().addTag(queryString, desc,
                    new AsyncCallback<TagInfo>(){
                        public void onFailure(Throwable caught) {
                            PopupUtil.showSevereError(caught);
                            callback.onFailure(caught);
                        }

                        public void onSuccess(TagInfo result) {
                            Request req = new Request(TagCmd.COMMAND_NAME);
                            req.setParam(TAG_NAME, result.getTagName());
                            
                            TagsDialog.showTagInfo(result.getTagName(), result.getDescription(), makeUrlString(result.getTagName()));
                            callback.onSuccess(null);

//                            PopupUtil.showInfo(
//                                    "Tag name: <b>" + result.getTagName() + "</b><br><br>" +
//                                    "URL: " +
//                                    makeUrlString(result.getTagName())
//                                );
                        }
                    });
        }

    }

    public static interface TagResolver {
        public void resolve(String tagname);
    }

    public static class DefaultTagResolver implements TagResolver {

        public DefaultTagResolver() {}

        public void resolve(final String tagName) {
            UserServices.App.getInstance().getTag(tagName,
                    new AsyncCallback<TagInfo>(){
                        public void onFailure(Throwable caught) {
                            PopupUtil.showError("Data Error", "Unkown Tag Name: " + tagName);
                        }

                        public void onSuccess(TagInfo result) {
                            Request req = Request.parse(result.getHistoryToken());
                            Application.getInstance().processRequest(req);
                        }
                    });
        }
    }
}

