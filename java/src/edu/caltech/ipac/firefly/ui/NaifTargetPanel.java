/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui;

import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.data.EphPair;
import edu.caltech.ipac.firefly.data.MOSRequest;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.rpc.TargetServices;
import edu.caltech.ipac.firefly.ui.input.InputFieldGroup;
import edu.caltech.ipac.firefly.ui.input.SimpleInputField;
import edu.caltech.ipac.firefly.ui.input.SuggestBoxInputField;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.dd.FieldDef;
import edu.caltech.ipac.util.dd.FieldDefSource;
import edu.caltech.ipac.util.dd.StringFieldDef;
import edu.caltech.ipac.util.dd.ValidationException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;


/**
 */
public class NaifTargetPanel extends Composite implements InputFieldGroup {

    public static final String NAIF_NAME_KEY = "NaifTargetPanel.field.naifName";
    public static final String NAIF_ID_KEY   = "NaifTargetPanel.field.naifID";


    private VerticalPanel mainPanel;
    private EphPair      current;
    private SuggestBoxInputField naifField;
    private SimplePanel feedback= new SimplePanel();
    private ResolveTimer resolveTimer= new ResolveTimer();
    private Widget helpWidget;
    private ResolveTask _activeTask;
    private boolean ignoreValidation;

    private SimpleInputField posWrap;
    private List<EphPair> activeList= null;


    public NaifTargetPanel() {
        init();
        initWidget(mainPanel);
    }



    public String getNaifName() {
        String v = naifField.getValue();
        if (v==null) v="";
        return v;
    }

    public void setNaifName(String name) {
        naifField.setValue(name);
    }



    public String getID() {
        return current!=null ? current.getNaifID() : null;
    }

    public String getPrimDes() {
        return current!=null ? current.getPrimaryDes() : null;
    }

    public List<Param> getFieldValues() {
        List<Param> list= new ArrayList<Param>(3);
        if (!StringUtils.isEmpty(getNaifName())) list.add(new Param(MOSRequest.OBJ_NAME,getNaifName()));
        if (!StringUtils.isEmpty(getID())) list.add(new Param(MOSRequest.OBJ_NAIF_ID,getID()));
        if (!StringUtils.isEmpty((getPrimDes()))) list.add(new Param(MOSRequest.OBJ_PRIM_DES,getID()));

        return list;
    }

    public void setFieldValues(List<Param> list) {

        String id= null;
        String name= null;
        String prim="";

        for(Param p : list) {
            if (p.getName().equals(MOSRequest.OBJ_NAIF_ID) ) {
                id= p.getValue();
            }
            else if (p.getName().equals(MOSRequest.OBJ_NAME)) {
                name= p.getValue();
            }
            else if (p.getName().equals(MOSRequest.OBJ_PRIM_DES)) {
                prim= p.getValue();
            }
        }
        setMovingTarget(name,id,prim);
        updateFeedback();
    }

    private void setMovingTarget(String name, String id, String primDes) {
        setNaifName(name);
        if (!StringUtils.isEmpty(name) &&
            !StringUtils.isEmpty(id)  ) {
            current= new EphPair(name,id,primDes);
        }
        updateFeedback();
    }



    public boolean validate() {
        return current!=null || (StringUtils.isEmpty(getNaifName()) && posWrap.validate());
    }



//====================================================================
//
//====================================================================

    public void setBorder(boolean border) {
        if (border) mainPanel.setBorderWidth(1);
        else        mainPanel.setBorderWidth(0);
    }



    private void init() {

        helpWidget= getHelp();

//        FieldDef fd= FieldDefCreator.makeFieldDef(NAIF_NAME_KEY);
        StringFieldDef fd= new NaifFieldDef();
        FieldDefSource fds= new WebPropFieldDefSource(NAIF_NAME_KEY);
        FieldDefCreator.setStringFieldAttributes(fd,fds);
//        fd.setErrMsg("You must choose a name and ID pair");
//        fd.setMaxWidth(0);
//        fd.setPreferWidth(0);
//        fd.setName("NaifTargetPanel.field.naifName");
//        fd.setLabel("Object Name or ID");
//        fd.setShortDesc("Enter the NAIF name or ID to look up");


        naifField= new NaifInputField(fd,new NaifOracle());



        posWrap= new SimpleInputField(naifField,true);





        addChangeListeners();

        feedback.setSize("400px","3em");
        HorizontalPanel top= new HorizontalPanel();
        top.add(posWrap);
        top.setSpacing(1);

        mainPanel = new VerticalPanel();
        mainPanel.add(top);
        mainPanel.add(feedback);
        mainPanel.setCellVerticalAlignment(top, VerticalPanel.ALIGN_MIDDLE);
        mainPanel.setCellVerticalAlignment(feedback, VerticalPanel.ALIGN_TOP);
        updateFeedback();
    }

//====================================================================
//  implementing HasWidget
//====================================================================

    public void add(Widget w) {
        throw new UnsupportedOperationException("This operation is not allowed");
    }

    public void clear() {
        throw new UnsupportedOperationException("This operation is not allowed");
    }

    public Iterator<Widget> iterator() {
        return new ArrayList<Widget>(Arrays.asList(posWrap)).iterator();
    }

    public boolean remove(Widget w) {
        throw new UnsupportedOperationException("This operation is not allowed");
    }

//====================================================================
//  private methods
//====================================================================

    private void addChangeListeners() {

        KeyDownHandler posQueryKph = new KeyDownHandler(){
            public void onKeyDown(KeyDownEvent ev) {
                int c= ev.getNativeKeyCode();
                DeferredCommand.addCommand(new Command() {
                    public void execute() { checkMatch(); }
                });

                if (c==KeyCodes.KEY_TAB || c==KeyCodes.KEY_ENTER) {
                    if (!validate()) {
                        if (current==null && activeList!=null && activeList.size()>0) {
                            setNaifName(activeList.get(0).getName());
                            current= activeList.get(0);
                        }
                    }
                }
                else {
                    //updateAsync(false);
                }
            }
        };

        naifField.addValueChangeHandler(new ValueChangeHandler<String>() {
            public void onValueChange(ValueChangeEvent<String> stringValueChangeEvent) {
                checkMatch();
            }
        });

        naifField.getSuggestBox().addSelectionHandler(new SelectionHandler<SuggestOracle.Suggestion>() {
            public void onSelection(SelectionEvent<SuggestOracle.Suggestion> ev) {
                checkMatch();
            }
        });


        naifField.getFocusWidget().addKeyDownHandler(posQueryKph);

        naifField.getFocusWidget().addBlurHandler(new BlurHandler() {
            public void onBlur(BlurEvent event) {
                if (!validate()) {
                    if (current==null && activeList!=null && activeList.size()>0) {
                        setNaifName(activeList.get(0).getName());
                        current= activeList.get(0);
                        updateFeedback();
                        posWrap.validate();
                    }
                }
            }
        });
    }

    private void checkMatch() {
        String name= getNaifName();
        current= null;
        if (!StringUtils.isEmpty(name) && activeList!=null) {
            for(EphPair pair : activeList) {
                if (name.equals(pair.getName())) {
                    current= pair;
                }
            }
        }

        updateFeedback();
    }


    private void resolveNaifName(String name, AsyncCallback<List<EphPair>> cb) {
        if (!StringUtils.isEmpty(name)) {
            if (ResolverCache.isMovingCached(name)) {
                List<EphPair> idList= ResolverCache.getMoving(name);
                cb.onSuccess(idList);
            }
            else {
                if (_activeTask!=null) _activeTask.cancel();
                _activeTask = new ResolveTask(name,cb);
                _activeTask.setAutoMask(false);
                _activeTask.start();
            }
        }
    }

    private void resolveNaifNameWait(String name, AsyncCallback<List<EphPair>> cb) {
        if (!StringUtils.isEmpty(name)) {
            if (ResolverCache.isMovingCached(name)) {
                List<EphPair> idList= ResolverCache.getMoving(name);
                cb.onSuccess(idList);
            }
            else {
                resolveTimer.startNewCall(name,cb);
            }
        }
    }




    private void updateFeedback() {
        String s;
        String name= getNaifName();
        String wrapBegin= "<div style=\"text-align: center; line-height: 1;\">";
        String wrapEnd= "</div>";

        if (!StringUtils.isEmpty(name)) {
            if (current!=null) {
                s= "Object Name: " + bold(name)+ ",&nbsp;&nbsp; NAIF ID: " + bold(current.getNaifID());
            }
            else {
//                s= "Target Name: " + bold(name);
                s= "";
            }
        }
        else {
            s= null;
        }
        if (s!=null)feedback.setWidget(new HTML(wrapBegin+s+wrapEnd));
        else feedback.setWidget(helpWidget);

    }

    private String bold(String s) { return "<b>"+ s +"</b>"; }


    private Widget getHelp() {
        HorizontalPanel hp= new HorizontalPanel();
        VerticalPanel vp= new VerticalPanel();
        hp.setSpacing(5);
//        vp.setSpacing(5);

        hp.add(new HTML("<i>Examples:</i>"));
        hp.add(vp);
        vp.add(new HTML("\'pallas\'&nbsp;&nbsp;&nbsp; \'2008 yn2\'&nbsp;&nbsp;&nbsp; \'2024703\'" ));
//        vp.add(new HTML(""));

        return GwtUtil.centerAlign(hp);
    }


    private class ResolveTimer extends Timer {
        private String name= null;
        private AsyncCallback<List<EphPair>> cb= null;


        @Override
        public void run() {
            resolveNaifName(name,cb);
        }

        public void restart() { this.schedule(500); }

        public void startNewCall(String name, AsyncCallback<List<EphPair>> cb) {
            if (this.cb!=null) this.cb.onFailure(null);
            cancel();
            this.name= name;
            this.cb= cb;
            restart();

        }
    }

    public class ResolveTask extends ServerTask<ArrayList<EphPair>> {

        private final String name;
        private final AsyncCallback<List<EphPair>> listCallback;

        public ResolveTask(String name,
                           AsyncCallback<List<EphPair>> listCallback) {
            super(mainPanel,"Resolving...", true);
            this.name= name;
            this.listCallback = listCallback;
        }

        public String getName() { return name;}

        public void doTask(AsyncCallback<ArrayList<EphPair>> acb) {
            TargetServices.App.getInstance().resolveNameToNaifIDs(name,acb);
        }

        public void onSuccess(ArrayList<EphPair> idList) {
            ResolverCache.putMoving(name, idList);
            if (listCallback!=null) listCallback.onSuccess(idList);
            _activeTask= null;
        }

        public void onFailure(Throwable caught) {
            _activeTask= null;
        }

        @Override
        protected void onCancel(boolean byUser) {
            _activeTask= null;
        }
    }


    public class NaifOracle extends SuggestOracle {
        @Override
        public void requestSuggestions(Request request, Callback callback) {
            String text= request.getQuery();
            resolveNaifNameWait(text, new NaifOracleCallback(request, callback));
        }

        @Override
        public boolean isDisplayStringHTML() {
            return true;
        }
    }

    public class NaifOracleCallback implements AsyncCallback<List<EphPair>> {
        private SuggestOracle.Callback cb;
        private SuggestOracle.Request request;
        private boolean completed= false;

        NaifOracleCallback(SuggestOracle.Request request, SuggestOracle.Callback cb) {
            this.request= request;
            this.cb= cb;
        }


        public void onFailure(Throwable caught) {
            if (!completed) {
                List<NaifSuggestion> sugList= new ArrayList<NaifSuggestion>(0);
                SuggestOracle.Response response= new SuggestOracle.Response(sugList);
                cb.onSuggestionsReady(request,response);
                activeList= null;
            }
            completed= true;
        }

        public void onSuccess(List<EphPair> result) {
            if (!completed) {
                List<NaifSuggestion> sugList= new ArrayList<NaifSuggestion>(result.size());
                for(EphPair ep : result)  sugList.add(new NaifSuggestion(ep));
                SuggestOracle.Response response= new SuggestOracle.Response(sugList);
                cb.onSuggestionsReady(request,response);
                activeList= result;
            }
            completed= true;
        }
    }

    public class NaifSuggestion implements SuggestOracle.Suggestion {
        private EphPair pair;

        NaifSuggestion(EphPair pair) { this.pair= pair;  }

        public String getDisplayString() {
            return "Name: " + bold(pair.getName()) + ",&nbsp;&nbsp; NAIF ID: "+bold(pair.getNaifID());
        }

        public String getReplacementString() {
            return pair.getName();
        }

    }

    public class NaifFieldDef extends StringFieldDef {
        @Override
        public boolean validateSoft(Object aValue) throws ValidationException {
            return true;
        }

        @Override
        public boolean validate(Object aValue) throws ValidationException {
            boolean v= super.validate(aValue);
            if (v) {
                v= NaifTargetPanel.this.validate();
            }
            return v;
        }
    }

    public class NaifInputField extends SuggestBoxInputField {

        public NaifInputField (FieldDef fd, SuggestOracle suggest) {
            super(fd,suggest);
        }

        @Override
        public boolean validateSoft() {
            try {
                return getFieldDef().validateSoft(getValue());
            } catch (ValidationException e) {
                return false;
            }
        }
    }
}
