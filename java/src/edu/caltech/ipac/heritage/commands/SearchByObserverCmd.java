package edu.caltech.ipac.heritage.commands;


import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.ui.FieldDefCreator;
import edu.caltech.ipac.firefly.ui.Form;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.ServerTask;
import edu.caltech.ipac.firefly.ui.input.SimpleInputField;
import edu.caltech.ipac.firefly.ui.input.SuggestBoxInputField;
import edu.caltech.ipac.firefly.ui.panels.CollapsiblePanel;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.heritage.data.entity.DataType;
import edu.caltech.ipac.heritage.rpc.SearchServices;
import edu.caltech.ipac.heritage.searches.SearchByObserver;
import edu.caltech.ipac.heritage.ui.InstrumentPanel;
import edu.caltech.ipac.heritage.ui.MoreOptionsPanel;
import edu.caltech.ipac.util.dd.FieldDef;

import java.util.List;

/**
 * @author tatianag
 * $Id: SearchByObserverCmd.java,v 1.48 2012/09/11 19:10:13 tatianag Exp $
 */
public class SearchByObserverCmd extends HeritageRequestCmd {
    public static final String COMMAND_NAME = "SearchByObserver";
    public static final String OBSERVER_KEY = "SearchByObserver.field.observer";


    private List<String> observers;
    MultiWordSuggestOracle oracle;

    public SearchByObserverCmd() {
        super(COMMAND_NAME);
        oracle = new MultiWordSuggestOracle();
    }

    protected Form createForm() {

        ServerTask<List<String>> task = new ServerTask<List<String>>() {

            public void onSuccess(List<String> result) {
                observers = result;
                if (observers != null)
                for (String o : observers) {
                    if (o != null) {
                        oracle.add(o);
                    }
                }
            }
            public void doTask(AsyncCallback<List<String>> passAlong) {
                SearchServices.App.getInstance().getObservers(passAlong);
            }
        };
        task.start();

        FieldDef fd = FieldDefCreator.makeFieldDef(OBSERVER_KEY);
        SimpleInputField observerField = new SimpleInputField(new SuggestBoxInputField(fd, oracle), new SimpleInputField.Config("125px"), true);

        SuppressReturnHandler srh = new SuppressReturnHandler();
        observerField.getField().addValueChangeHandler(srh);
        final TextBox tb = (TextBox) observerField.getField().getFocusWidget();
        tb.addKeyPressHandler(srh);

        HTML desc = GwtUtil.makeFaddedHelp("Start typing; choose the observer name from available suggestions.");

        VerticalPanel vp = new VerticalPanel();
        vp.add(GwtUtil.getFiller(0, 5));
        vp.add(observerField);
        vp.add(GwtUtil.getFiller(0, 5));
        vp.add(desc);
        vp.add(GwtUtil.getFiller(0, 10));
        MoreOptionsPanel options = new MoreOptionsPanel(new DataType[]{DataType.IRS_ENHANCED});
        vp.add(options);
        //vp.add(GwtUtil.getFiller(0, 5));

        InstrumentPanel instPanel = new InstrumentPanel();
        final CollapsiblePanel moreOptions = new CollapsiblePanel("More options", instPanel, false);

        instPanel.getEventManager().addListener(InstrumentPanel.ON_HIDE, new WebEventListener(){
                public void eventNotify(WebEvent ev) {
                    moreOptions.collapse();
                }
            });
        vp.add(moreOptions);

        Form form = new Form();
        form.setHelpId("searching.byObsName");
        form.add(vp);
        return form;
    }

    protected void processRequest(final Request req, final AsyncCallback<String> callback) {

        createTablePreviewDisplay();

        if (MoreOptionsPanel.isAorRequested(req)) addTable(new SearchByObserver(SearchByObserver.Type.AOR, req));
        if (MoreOptionsPanel.isPbcdRequested(req)) addTable(new SearchByObserver(SearchByObserver.Type.PBCD, req));
        if (MoreOptionsPanel.isBcdRequested(req)) addTable(new SearchByObserver(SearchByObserver.Type.BCD, req));
        if (MoreOptionsPanel.isIrsEnhancedRequested(req))  {
            addTable(new SearchByObserver(SearchByObserver.Type.IRS_ENHANCED, req));
        }

        loadAll();

        this.setResults(getResultsPanel());
    }


    class SuppressReturnHandler implements ValueChangeHandler<String>, KeyPressHandler {
        private boolean hasChanged = false;

        public void onValueChange(ValueChangeEvent<String> stringValueChangeEvent) {
            hasChanged = true;
        }

        public void onKeyPress(KeyPressEvent event) {
            if (event.getCharCode() == KeyCodes.KEY_ENTER) {
                if (hasChanged) {
                    hasChanged = false;
                } else {
                    event.setRelativeElement(null);

                }
            } else {
                hasChanged = false;
            }
        }
    }

}
