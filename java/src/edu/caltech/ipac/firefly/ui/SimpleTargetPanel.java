package edu.caltech.ipac.firefly.ui;

import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.Preferences;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.ReqConst;
import edu.caltech.ipac.firefly.data.form.PositionFieldDef;
import edu.caltech.ipac.firefly.rpc.TargetServices;
import edu.caltech.ipac.firefly.ui.input.AsyncInputFieldGroup;
import edu.caltech.ipac.firefly.ui.input.InputField;
import edu.caltech.ipac.firefly.ui.input.SimpleInputField;
import edu.caltech.ipac.firefly.visualize.ActiveTarget;
import edu.caltech.ipac.firefly.visualize.VisUtil;
import edu.caltech.ipac.targetgui.net.Resolver;
import edu.caltech.ipac.util.ComparisonUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.dd.EnumFieldDef;
import edu.caltech.ipac.util.dd.ValidationException;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.ResolvedWorldPt;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static edu.caltech.ipac.firefly.util.PositionParser.Input.Name;
import static edu.caltech.ipac.firefly.util.PositionParser.Input.Position;

/**
 * Created by IntelliJ IDEA.
 * User: tlau
 * Date: Nov 25, 2010
 * Time: 5:35:55 PM
 * To change this template use File | Settings | File Templates.
 */
public class SimpleTargetPanel extends Composite implements AsyncInputFieldGroup {

    public static final String TARGET_NAME_KEY = "TargetPanel.field.targetName";
    public static final String RESOLVED_BY_KEY = "SimpleTargetPanel.field.resolvedBy";
    public static final String POSITION_KEY = "SimpleTargetPanel.field.position";

    public static final String RESOLVE_BY_PREF = "TargetResolver";
    private static final NumberFormat nf = NumberFormat.getFormat("#.######");

    private enum ResolveAction {None, Resolving, Failed}

    private VerticalPanel mainPanel;
    private PositionFieldDef posFd;
    private InputField posField;
    private InputField resolveByField;
    private boolean ignoreValidation;
    private SimplePanel feedback = new SimplePanel();
    private ResolveTimer resolveTimer = new ResolveTimer();
    private Widget helpWidget;
    private ResolveTask _activeTask;
    private String _lastInput = "";
    private List<Resolver> _supportedResolvers = new ArrayList<Resolver>(6);

    //    public SimpleTargetPanel() { this("NED", "Simbad", "ptf"); }
    public SimpleTargetPanel() {
        this("nedthensimbad", "SimbadthenNED");
    }

    public SimpleTargetPanel(String... resolvers) {
        init(resolvers);
        initWidget(mainPanel);
    }

//====================================================================
//  convenience unvalidated getter/setter
//====================================================================


    public String getTargetName() {
        String v = posFd.getObjectName();
        if (v == null) v = "";
        return v;
    }

    public InputField getInputField() { return posField; }

    public void setTargetName(String name) {
        setTarget(name, null, getResolver());
    }

    public Resolver getResolver() {
        return parseResolver(resolveByField.getValue());
    }

    public Resolver parseResolver(String resolveStr) {
        Resolver retval = Resolver.parse(resolveStr);
        if (!_supportedResolvers.contains(retval)) retval = Resolver.NONE;

        if (!retval.equals(Resolver.NONE)) {
            Preferences.set(RESOLVE_BY_PREF, resolveStr);
            ((EnumFieldDef) resolveByField.getFieldDef()).setDefaultValue(resolveStr);
        }
        return retval;
    }


    public void setResolver(Resolver resolver) {
        if (resolver != null && _supportedResolvers.contains(resolver)) {
            resolveByField.setValue(resolver.getKey());
        } else {
            resolveByField.setValue(_supportedResolvers.get(0).getKey());
        }
        updateFeedback();
    }


    public ResolvedWorldPt getPos() {
        ResolvedWorldPt pos;
        if (posFd.getInputType() == Position) {
            WorldPt fdPos = posFd.getPosition();
            pos = ResolvedWorldPt.makePt(fdPos);
        } else if (ResolverCache.isCached(posFd.getObjectName(), getResolver())) {
            pos = ResolverCache.get(posFd.getObjectName(), getResolver());
        } else {
            pos = null;
        }
        return pos;
    }

    public WorldPt getJ2000Pos() {
        WorldPt pos = getPos();
        WorldPt retval = pos;
        if (pos != null && !pos.getCoordSys().equals(CoordinateSys.EQ_J2000)) {
            retval = VisUtil.convert(pos, CoordinateSys.EQ_J2000);
        }
        return retval;
    }


    public List<Param> getFieldValues() {
        List<Param> list = new ArrayList<Param>(3);
        WorldPt wp = getPos();
        if (wp != null) {
            list.add(new Param(ReqConst.USER_TARGET_WORLD_PT, new WorldPt(wp).toString()));
        }
        if (posFd.getInputType() == Name) {
            list.add(new Param(TARGET_NAME_KEY, posFd.getObjectName()));
        }
        list.add(new Param(RESOLVED_BY_KEY, getResolver().toString()));
        return list;
    }

    public void setFieldValues(List<Param> list) {

        String posName = null;
        WorldPt wp = null;
        Resolver resolver = Resolver.NED;

        for (Param p : list) {
            if (p.getName().equals(ReqConst.USER_TARGET_WORLD_PT)) {
                wp = WorldPt.parse(p.getValue());
            } else if (p.getName().equals(TARGET_NAME_KEY)) {
                posName = p.getValue();
            } else if (p.getName().equals(RESOLVED_BY_KEY)) {
                resolver = Resolver.parse(p.getValue());
            }
        }
        setTarget(posName, wp, resolver);
        updateFeedback();
    }


    public void getFieldValuesAsync(final AsyncCallback<List<Param>> cb) {
        if (isAsyncCallRequired() && isResolveNeeded()) {
            resolveTargetName(true, new AsyncCallback<ResolvedWorldPt>() {
                public void onFailure(Throwable caught) {
                    cb.onFailure(caught);
                }

                public void onSuccess(ResolvedWorldPt wpt) {
                    cb.onSuccess(getFieldValues());
                }
            });
        } else {
            cb.onSuccess(getFieldValues());
        }
    }

    public boolean isAsyncCallRequired() {
        return isResolveNeeded();
    }

    public ActiveTarget.PosEntry getTarget() {
        String tname = getTargetName();
        tname = StringUtils.isEmpty(tname) ? null : tname;
        return new ActiveTarget.PosEntry(getPos(), tname, getResolver(), false);
    }

    public void setTarget(ActiveTarget.PosEntry target) {
        if (target != null) {
            setTarget(target.getName(), target.getPt(), target.getResolver(), target.isComputed());
        }
    }

    private void setTarget(String name, WorldPt wp, Resolver resolver) {
        setTarget(name, wp, resolver, false);
    }

    private void setTarget(String name, WorldPt wp, Resolver resolver, boolean computed) {
        if (wp == null && !StringUtils.isEmpty(name)) {
            posField.setValue(name);
            setTarget(null, false);
            triggerResolverWithWait();
        } else {
            setTarget(ResolvedWorldPt.makePt(wp, name, resolver), computed);
        }
    }

    private void setTarget(ResolvedWorldPt wp, boolean computed) {
        if (wp != null) {
            if (!StringUtils.isEmpty(wp.getObjName())) {
                posField.setValue(wp.getObjName());
            } else {
                posField.setValue(PositionFieldDef.formatPosForTextField(wp));
            }

        }
        Resolver resolver = (wp != null) ? wp.getResolver() : null;
        setResolver(resolver);

        if (wp != null &&
                !computed &&
                resolver != null &&
                resolver != Resolver.NONE &&
                !StringUtils.isEmpty(wp.getObjName())) {
            ResolverCache.put(wp);
        }
        updateActive();
        updateFeedback();
    }


    public void setIgnoreValidation(boolean ignoreValidation) {
        this.ignoreValidation = ignoreValidation;
    }

    public boolean validate() {
        if (ignoreValidation) {
            return true;
        }

        boolean retval = posField.validate() && GwtUtil.validateBlank(posField);

        if (retval) updateActive();
        return retval;
    }


    private void updateActive() {
        String tname = getTargetName();
        tname = StringUtils.isEmpty(tname) ? null : tname;
        ActiveTarget.getInstance().setActive(tname, getPos(), getResolver(), false);
    }


//====================================================================
//
//====================================================================

    public void setBorder(boolean border) {
        if (border)
            mainPanel.setBorderWidth(1);
        else
            mainPanel.setBorderWidth(0);
    }

    private void init(String resolvers[]) {

        helpWidget = getHelp();
        SimpleInputField posWrap = SimpleInputField.createByProp(POSITION_KEY);
        posField = posWrap.getField();
        posFd = (PositionFieldDef) posField.getFieldDef();


        Resolver r;
        for (String rStr : resolvers) {
            r = Resolver.parse(rStr);
            if (r != Resolver.NONE) _supportedResolvers.add(r);
        }
        if (_supportedResolvers.size() == 0) {
            _supportedResolvers.add(Resolver.NED);
            _supportedResolvers.add(Resolver.Simbad);
        }
        resolveByField = createResolverField(_supportedResolvers);
//        resolveByField = FormBuilder.createField(RESOLVED_BY_KEY);

        String resolveByPref = Preferences.get(RESOLVE_BY_PREF);


        if (!StringUtils.isEmpty(resolveByPref)) {
            Resolver def = parseResolver(resolveByPref);
            if (def != Resolver.NONE) setResolver(def);
        }


        addChangeListeners();

        feedback.setSize("400px", "3em");
//        feedback.setWidth("400px");
        GwtUtil.setStyle(resolveByField, "marginTop", "6px ");

        HorizontalPanel top = new HorizontalPanel();
        top.add(posWrap);
        top.add(resolveByField);
        top.setSpacing(1);

        mainPanel = new VerticalPanel();
        mainPanel.add(top);
        mainPanel.add(feedback);
        mainPanel.setCellVerticalAlignment(top, VerticalPanel.ALIGN_MIDDLE);
        mainPanel.setCellVerticalAlignment(feedback, VerticalPanel.ALIGN_TOP);
        updateFeedback();
    }


    private InputField createResolverField(List<Resolver> resolvers) {
        EnumFieldDef fd = new EnumFieldDef(RESOLVED_BY_KEY);
        fd.setShortDesc("Select resolver to retrieve coordinates");
        fd.setPreferWidth(125);
        boolean first = true;
        for (Resolver resolver : resolvers) {
            if (resolver != Resolver.NONE) {
                fd.addItem(resolver.getKey(), resolver.getUserDesc());
                if (first) fd.setDefaultValue(resolver.getKey());
                first = false;
            }
        }
        return FormBuilder.createField(fd);
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
        return new ArrayList<Widget>(Arrays.asList(posField, resolveByField)).iterator();
    }

    public boolean remove(Widget w) {
        throw new UnsupportedOperationException("This operation is not allowed");
    }

//====================================================================
//  private methods
//====================================================================

    private void addChangeListeners() {

        KeyDownHandler posQueryKph = new KeyDownHandler() {
            public void onKeyDown(KeyDownEvent ev) {
                int c = ev.getNativeKeyCode();
                if (c == KeyCodes.KEY_TAB || c == KeyCodes.KEY_ENTER) {
                    updateAsync(true);
                } else {
                    updateAsync(false);
                }
            }
        };

        ValueChangeHandler<String> tgtChangeHan = new ValueChangeHandler<String>() {
            public void onValueChange(ValueChangeEvent<String> ev) {
                updateAsync(true);
            }
        };

        posField.getFocusWidget().addKeyDownHandler(posQueryKph);
        resolveByField.addValueChangeHandler(tgtChangeHan);
//        posField.addValueChangeHandler(tgtChangeHan);

    }


    private void triggerResolverWithWait() {
        if (ResolverCache.isCached(getTargetName(), getResolver())) {
            triggerResolver(false);
        } else {
            resolveTimer.cancel();
            resolveTimer.restart();
        }
    }

    private void triggerResolver(boolean mask) {
        resolveTimer.cancel();
        if (posFd.getInputType() == Name) {
            String name = posFd.getObjectName();

            if (_activeTask == null) {
                resolveTargetName(mask, null);
            } else if (!ComparisonUtil.equals(_activeTask.getName(), name)) {
                cancelResolverTask();
                resolveTargetName(mask, null);
            } else {
                // do nothing correct resolve in progress
            }
        } else {
            cancelResolverTask();
        }
    }


    private boolean isResolveNeeded() {
        String name = getTargetName();
        Resolver resolver = getResolver();

        return (!StringUtils.isEmpty(name) &&
                resolver != Resolver.NONE &&
                !ResolverCache.isCached(name, resolver));
    }

    private void resolveTargetName(boolean mask, final AsyncCallback<ResolvedWorldPt> callback) {
        String name = getTargetName();
        if (!StringUtils.isEmpty(name)) {
            Resolver resolver = getResolver();
            if (isResolveNeeded()) {
                if (ResolverCache.isCached(name, resolver)) {
                    ResolvedWorldPt wp = ResolverCache.get(name, resolver);
                    resolveSuccess(name, wp, callback);
                } else {
                    if (_activeTask != null) _activeTask.cancel();
                    _activeTask = new ResolveTask(name, resolver, callback);
                    _activeTask.setAutoMask(mask);
                    _activeTask.start();
                }
            } else {
                updateFeedback();
            }
        }
    }

    private void resolveSuccess(String name,
                                final ResolvedWorldPt pos,
                                final AsyncCallback<ResolvedWorldPt> callback) {
        if (name.equals(getTargetName())) {
            updateActive();
            updateFeedback();
            if (callback != null) {
                DeferredCommand.addCommand(new Command() {
                    public void execute() {
                        callback.onSuccess(pos);
                    }
                });
            }
        }
    }


    private void cancelResolverTask() {
        if (_activeTask != null) _activeTask.cancel();
    }

    private void updateAsync(final boolean resolveNow) {
        if (_activeTask != null) _activeTask.cancel();
        DeferredCommand.addCommand(new Command() {
            public void execute() {
                updateFeedback();

                if (posFd.getInputType() == Name) {
                    if (resolveNow) {
                        triggerResolver(true);
                    } else {
                        triggerResolverWithWait();
                    }
                }
            }
        });
    }

    private void updateFeedback() {
        updateFeedback(ResolveAction.None);
    }

    private void updateFeedback(ResolveAction resAction) {
        String s = null;
        String val = posField.getValue();
        String wrapBegin = "<div style=\"text-align: center; line-height: 1;\">";
        String wrapEnd = "</div>";
        try {

            if (!StringUtils.isEmpty(val) && posFd.validate(val)) {
                if (posFd.getInputType() == Position) {
                    WorldPt pt = posFd.getPosition();
                    if (pt != null) s = PositionFieldDef.formatPosForHelp(pt);
                } else {
                    String name = posFd.getObjectName();
                    ResolvedWorldPt resolvedPos = ResolverCache.get(name, getResolver());

                    if (resAction == ResolveAction.Failed) {
                        s = bold(name) + ": Could not resolve position using " + getResolver().getUserDesc();
                    } else if (resAction == ResolveAction.Resolving) {
                        s = bold(name) + " <i>resolving now using</i> " + getResolver().getUserDesc();
                    } else if (resolvedPos != null) {
                        s = PositionFieldDef.formatTargetForHelp(resolvedPos);
                    } else {
                        s = "Target Name: " + bold(name);
                    }
                }
            } else {
                s = null;
            }
        } catch (ValidationException e) {
            s = null;
        }

        if (s != null) feedback.setWidget(new HTML(wrapBegin + s + wrapEnd));
        else feedback.setWidget(helpWidget);

    }

    private String bold(String s) {
        return "<b>" + s + "</b>";
    }


    private Widget getHelp() {
        HorizontalPanel hp = new HorizontalPanel();
        GwtUtil.setStyle(hp, "fontSize", "13px");
        VerticalPanel vp = new VerticalPanel();

        hp.setSpacing(5);
//        vp.setSpacing(5);

        hp.add(new HTML("<i>Examples:</i>"));
        hp.add(vp);
        vp.add(new HTML("\'m81\'&nbsp;&nbsp;&nbsp; \'ngc 13\'&nbsp;&nbsp;&nbsp; \'12.34 34.89\'" +
                                "&nbsp;&nbsp;&nbsp; \'46.53, -0.251 gal\'"));
//        vp.add(new HTML(""));
        HTML line2 = new HTML("\'19h17m32s 11d58m02s equ j2000\'&nbsp;&nbsp;&nbsp; \'12.3, 8.5 b1950\'");
        GwtUtil.setStyle(line2, "paddingTop", "4px");
        vp.add(line2);

        return GwtUtil.centerAlign(hp);
    }


    private class ResolveTimer extends Timer {
        @Override
        public void run() {
            triggerResolver(false);
        }

        public void restart() {
            this.schedule(500);
        }
    }

    public class ResolveTask extends ServerTask<ResolvedWorldPt> {

        private final AsyncCallback<ResolvedWorldPt> callback;
        private final String name;
        private final Resolver resolver;

        public ResolveTask(String name, Resolver resolver, AsyncCallback<ResolvedWorldPt> callback) {
            super(mainPanel, "Resolving...", true);
            this.name = name;
            this.resolver = resolver;
            this.callback = callback;

        }

        public String getName() {
            return name;
        }

        public void doTask(AsyncCallback<ResolvedWorldPt> acb) {
            TargetServices.App.getInstance().resolveName(name, resolver, acb);
            updateFeedback(ResolveAction.Resolving);
        }

        public void onSuccess(ResolvedWorldPt wp) {
            if (wp!=null) {
                ResolverCache.put(wp, resolver);
                resolveSuccess(name, wp, callback);
                _activeTask = null;
            }
            else {
                onFailure(new Exception("Resolve Failed"));
            }
        }

        public void onFailure(Throwable caught) {
            String msg = caught.getCause() == null ? caught.getMessage() : caught.getCause().getMessage();
            posField.forceInvalid("Resolve Failed: " + msg);
            if (callback != null) callback.onFailure(caught);
            updateFeedback(ResolveAction.Failed);
            _activeTask = null;
        }

        @Override
        protected void onCancel(boolean byUser) {
            if (byUser) {
                posField.forceInvalid("Resolve Canceled.");
                updateFeedback(ResolveAction.Failed);
                if (callback != null) callback.onFailure(null);
            }
            _activeTask = null;
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