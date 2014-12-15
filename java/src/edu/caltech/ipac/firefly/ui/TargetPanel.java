package edu.caltech.ipac.firefly.ui;


import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.astro.CoordException;
import edu.caltech.ipac.firefly.core.Preferences;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.ReqConst;
import edu.caltech.ipac.firefly.data.form.LatFieldDef;
import edu.caltech.ipac.firefly.data.form.LonFieldDef;
import edu.caltech.ipac.firefly.rpc.TargetServices;
import edu.caltech.ipac.firefly.ui.input.InputField;
import edu.caltech.ipac.firefly.ui.input.InputFieldGroup;
import edu.caltech.ipac.firefly.visualize.ActiveTarget;
import edu.caltech.ipac.firefly.visualize.CoordinateSysListener;
import edu.caltech.ipac.firefly.visualize.VisUtil;
import edu.caltech.ipac.firefly.visualize.conv.CoordUtil;
import edu.caltech.ipac.astro.net.Resolver;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.dd.EnumFieldDef;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.ResolvedWorldPt;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Date: Sep 22, 2008
 *
 * @author loi
 * @version $Id: TargetPanel.java,v 1.31 2011/10/11 15:40:40 xiuqin Exp $
 */
public class TargetPanel extends Composite implements InputFieldGroup, UsesFormHub {

    public static final String TARGET_NAME_KEY = "TargetPanel.field.targetName";
    public static final String RESOLVED_BY_KEY = "TargetPanel.field.resolvedBy";
    public static final String RA_KEY = "TargetPanel.field.ra";
    public static final String DEC_KEY = "TargetPanel.field.dec";
    public static final String COORDSYS_KEY = "TargetPanel.field.coordSys";

    public static final String RESOLVE_BY_PREF = "TargetResolver";

    private ServerTask  nameResolver;
    private String      lastResolved;
    private WorldPt     lastpos; // needed for coordinate conversion; null means invalid position
    private HorizontalPanel mainPanel;
    private InputField targetField;
    private InputField resolveByField;
    private InputField raField;
    private InputField decField;
    private InputField coordSysField;
    private List<InputField> fields;
    private CoordinateSysListener[] coordSysListeners;
    private boolean ignoreValidation;
    private FormHub formHub;

    public TargetPanel() {
        init();
        initWidget(mainPanel);
        fields = Arrays.asList(targetField, resolveByField, raField, decField, coordSysField);

    }

//====================================================================
//  Method form UsesFormHub
//====================================================================

    public void bind(FormHub formHub) {
        this.formHub= formHub;
//        if (formHub!=null) {
//            formHub.getEventManager().addListener(FormHub.FIELD_VALUE_CHANGE, new WebEventListener() {
//                public void eventNotify(WebEvent ev) {
//                    Param param= (Param)ev.getData();
//                    PopupUtil.showInfo(param.getName() + " changed to " + param.getValue());
//                }
//            });
//        }
    }


//====================================================================
//  convenience unvalidated getter/setter
//====================================================================


    public void setTarget(ActiveTarget.PosEntry target) {
        if (target==null) return;
        String tname= StringUtils.isEmpty(target.getName()) ? "" : target.getName();
        setResolver(target.getResolver());

        setPos(target.getPt());
        if (!target.isComputed() &&
            target.getResolver()!=Resolver.NONE &&
            !StringUtils.isEmpty(tname)) {
            setLastResolved(tname,target.getResolver());
        }
        setTargetName(StringUtils.isEmpty(target.getName()) ? "" : target.getName());
    }

    public String getTargetName() {
        String v = targetField.getValue();
        if (v==null) v="";
        return v;
    }

    public void setTargetName(String name) {
        targetField.setValue(name);
    }

    public Resolver getResolver() {
        String resolveStr= resolveByField.getValue();
        Resolver retval;
        if (resolveStr.equalsIgnoreCase("ned")) {
            retval= Resolver.NED;
        } else if (resolveStr.equalsIgnoreCase("simbad")) {
            retval= Resolver.Simbad;
        } else {
            retval= Resolver.NONE;
        }
        if (!retval.equals(Resolver.NONE)) {
            Preferences.set(RESOLVE_BY_PREF, resolveStr);
            ((EnumFieldDef)resolveByField.getFieldDef()).setDefaultValue(resolveStr);
        }
        return retval;
    }


    public void setResolver(Resolver resolver) {
        if (resolver==Resolver.NED) {
            resolveByField.setValue("ned");
        } else if (resolver==Resolver.Simbad) {
            resolveByField.setValue("simbad");
        } else {
            resolveByField.setValue("none");
        }
    }



    public float getRa() {
        if (StringUtils.isEmpty(raField.getValue())) return Float.NaN;
        try {
            return LonFieldDef.getFloat(raField.getValue(), getCoordSys().isEquatorial());
        } catch (Exception e) {
            return Float.NaN;
        }
    }

    /**
     *
     * @param wpt world point
     * @return true if the set was successful, i.e. the coordinates could be converted to a string such as HMS
     */
    public boolean setPos(WorldPt wpt) {
        boolean retval= true;
        if (wpt!=null) {
            try {
                CoordinateSys cs= wpt.getCoordSys();
                raField.setValue(CoordUtil.convertLonToString(wpt.getLon(), cs.isEquatorial()));
                decField.setValue(CoordUtil.convertLatToString(wpt.getLat(), cs.isEquatorial()));
                setCoordSys(cs.toString());
            } catch (CoordException e) {
                retval= false;
            }
            lastpos = wpt;
        }
        else {
            raField.reset();
            decField.reset();
            setCoordSys(CoordinateSys.EQ_J2000.toString());
            lastpos = null;
        }
        return retval;
    }

    public float getDec() {
        if (StringUtils.isEmpty(decField.getValue())) return Float.NaN;
        try {
            return LatFieldDef.getFloat(decField.getValue(), getCoordSys().isEquatorial());
        } catch (Exception e) {
            return Float.NaN;
        }
    }

    public String getLonStr() {  return raField.getValue(); }
    public String getLatStr() { return decField.getValue(); }

    public void setCoordSys(String coordsys) {
        try {
            coordSysField.setValue(coordsys);
            CoordinateSys cs = getCoordinateSys(coordsys);
            notifyCoordSysChanged(cs);
        } catch (CoordException e) {
            e.printStackTrace();
        }
    }

    public CoordinateSys getCoordSys() {
        try {
            return getCoordinateSys(coordSysField.getValue());
        } catch (Exception e) {
            return null;
        }
    }

    public String getCoordSysStr() { return coordSysField.getValue(); }


    public WorldPt getPos() {
        float ra = getRa();
        float dec = getDec();
        CoordinateSys cs = getCoordSys();
        if ( Float.isNaN(ra) || Float.isNaN(dec) || cs == null ) {
            return null;
        } else {
            return new WorldPt(ra, dec, cs);
        }
    }

    public WorldPt getJ2000Pos() {
        WorldPt pos = getPos();
        if (pos != null && !pos.getCoordSys().equals(CoordinateSys.EQ_J2000)) {
            try {
                return  convertToJ2000(getRa(), getDec(), getCoordSys());
            } catch (CoordException e) {
                GWT.log("Fail to convert coordinate", e);
            }
        }
        return pos;
    }

    public void resolvePosition(AsyncCallback<WorldPt> callback) {
        resolveTargetName(getTargetName(), getResolver(), callback);
    }

    public List<Param> getFieldValues() {
        List<Param> list= GwtUtil.getFieldValues(getFields());
        if (getPos() != null) {
            list.add(new Param(ReqConst.USER_TARGET_WORLD_PT,getPos().toString()));
        }
        return list;
    }

    public void setFieldValues(List<Param> list) {
        for(Param p : list) {
            if (p.getName().equals(ReqConst.USER_TARGET_WORLD_PT)) {
                try {
                    WorldPt wp= WorldPt.parse(p.getValue());
                    setPos(wp);
                } catch (IllegalArgumentException e) {/*ignore*/ }
            }
        }
        GwtUtil.setFieldValues(list, fields);
    }

    public void setIgnoreValidation(boolean ignoreValidation) {
        this.ignoreValidation = ignoreValidation;
    }

    public boolean validate() {
        if (ignoreValidation) {
            return true;
        }

        boolean retval = true;
        for(InputField f : getFields()) {
            if ( !f.validate() ) {
                retval = false;
            }
        }
        retval = retval & GwtUtil.validateBlank(raField);
        retval = retval & GwtUtil.validateBlank(decField);

        if (retval) updateActive();
        return retval;
    }

    public List<InputField> getFields() {
        return fields;
    }

    private void updateActive() {
        String tname= getTargetName();
        tname= StringUtils.isEmpty(tname) ? null : tname;
        ActiveTarget.getInstance().setActive(tname,getPos(), getResolver(),false);
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

    private void init() {

        targetField = FormBuilder.createField(TARGET_NAME_KEY);
        resolveByField = FormBuilder.createField(RESOLVED_BY_KEY);
        String resolveByPref = Preferences.get(RESOLVE_BY_PREF);
        if (!StringUtils.isEmpty(resolveByPref)) {
            ((EnumFieldDef)resolveByField.getFieldDef()).setDefaultValue(resolveByPref);
        }
        raField = FormBuilder.createField(RA_KEY);
        decField = FormBuilder.createField(DEC_KEY);
        coordSysField = FormBuilder.createField(COORDSYS_KEY);

        Widget byTarget = FormBuilder.createPanel(125, targetField, resolveByField);
        Widget byRaDec = FormBuilder.createPanel(125, raField, decField, coordSysField);

        coordSysListeners = new CoordinateSysListener[2];
        coordSysListeners[0] = (CoordinateSysListener)raField.getFieldDef();
        coordSysListeners[1] = (CoordinateSysListener)decField.getFieldDef();
        addChangeListeners();

        mainPanel = new HorizontalPanel();
        mainPanel.add(byTarget);
        mainPanel.add(byRaDec);

//        GXTUtil.makeEvenHeight(byTarget, byRaDec);
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
        ArrayList<Widget> fieldWidgets = new ArrayList<Widget>(fields.size());
        for (InputField f : fields) {
            if (f != null) fieldWidgets.add(f);
        }
        return fieldWidgets.iterator();
    }

    public boolean remove(Widget w) {
        throw new UnsupportedOperationException("This operation is not allowed");
    }

//====================================================================
//  private methods
//====================================================================

    private void addChangeListeners() {

        // empty targetField name if user enters raField or decField
        ValueChangeHandler<String> posChangeHan = new ValueChangeHandler<String>(){
            public void onValueChange(ValueChangeEvent<String> ev) {
                lastpos = getPos();
            }
        };
        raField.addValueChangeHandler(posChangeHan);
        decField.addValueChangeHandler(posChangeHan);

        // when user starts entering position clear
        // target name
        KeyPressHandler kph = new KeyPressHandler() {
            public void onKeyPress(KeyPressEvent event) {
                if (getResolver() != Resolver.NONE)
                   targetField.reset();
            }
        };
        raField.getFocusWidget().addKeyPressHandler(kph);
        decField.getFocusWidget().addKeyPressHandler(kph);

        // instant targetField resolution: field change listeners
        ValueChangeHandler<String> tgtChangeHan = new ValueChangeHandler<String>(){
            public void onValueChange(ValueChangeEvent<String> ev) {
                triggerResolveTargetName();
            }
        };
        targetField.addValueChangeHandler(tgtChangeHan);
        resolveByField.addValueChangeHandler(tgtChangeHan);
        /**
        targetField.getFocusWidget().addKeyPressHandler(new KeyPressHandler() {
            public void onKeyPress(KeyPressEvent ev) {
                char keyCode= ev.getCharCode();
                if (keyCode== KeyCodes.KEY_ENTER) {
                    triggerResolveTargetName();
                }
            }
        });
        **/

        // convert coordinates if coord sys has switched
        coordSysField.addValueChangeHandler(new ValueChangeHandler<String>(){
            public void onValueChange(ValueChangeEvent<String> ev) {
                try {
                    String newValue = coordSysField.getValue();
                    if (newValue != null ) {
                        CoordinateSys cs = getCoordinateSys(newValue);
                        notifyCoordSysChanged(cs);
                        if (lastpos != null && !StringUtils.isEmpty(raField.getValue()) && !StringUtils.isEmpty(decField.getValue())) {
                            WorldPt wpt = convert(lastpos.getLon(), lastpos.getLat(), lastpos.getCoordSys(), cs);
                            raField.setValue(CoordUtil.convertLonToString(wpt.getLon(), cs.isEquatorial()));
                            decField.setValue(CoordUtil.convertLatToString(wpt.getLat(), cs.isEquatorial()));
                            lastpos = wpt;
                        } else {
                            lastpos = null;
                        }
                    }
                } catch (CoordException ce) {
                    raField.validate();
                    decField.validate();
                }
            }
        });
    }

    public boolean isResolveNeeded() {
        return isResolveNeeded(getTargetName(), getResolver());
    }

    private void triggerResolveTargetName() {
        String value = targetField.getValue();
        if (value != null) {
            String val = value.trim();
            if (val.length()>0) {
                resolveTargetName(getTargetName(), getResolver(), null);
            }
        }
    }

    private boolean isResolveNeeded(String targetName, Resolver resolvedBy) {
        return (targetName != null && targetName.trim().length()>0 &&
                resolvedBy!=Resolver.NONE &&
                !isLastResolved(targetName,resolvedBy));
    }

    private void resolveTargetName(final String targetName, final Resolver resolvedBy, final AsyncCallback<WorldPt> callback) {
        if ( isResolveNeeded(targetName, resolvedBy) ) {
            ServerTask<ResolvedWorldPt> task = new ServerTask<ResolvedWorldPt>(mainPanel,"Resolving...", true) {
                public void doTask(AsyncCallback<ResolvedWorldPt> acb) {
                    if (this.equals(nameResolver)) {
                        raField.reset();
                        decField.reset();
                        setCoordSys(CoordinateSys.EQ_J2000_STR);
                        TargetServices.App.getInstance().resolveName(targetName, resolvedBy, acb);
                    }
                }

                public void onSuccess(ResolvedWorldPt pos) {
                    if (!StringUtils.isEmpty(targetField)) { // check that target name was not cleared in between
                        if (this.equals(nameResolver)) {
                            try {
                                raField.setValue(CoordUtil.convertLonToString(pos.getLon(), true));
                                decField.setValue(CoordUtil.convertLatToString(pos.getLat(), true));
                                setLastResolved(targetName,resolvedBy);
                                updateActive();
                            } catch (CoordException e) {
                                raField.setValue(String.valueOf(pos.getLon()));
                                decField.setValue(String.valueOf(pos.getLat()));
                            }
                            if (callback != null) {
                                callback.onSuccess(pos);
                            }
                            lastpos = pos;
                        } else {
                            GWT.log("!!!!!!!!!!!!! not NAME_RESOLVER", null);
                        }
                    }
                }

                public void onFailure(Throwable caught) {
                    if (this.equals(nameResolver)) {
                        String msg = caught.getCause() == null ? caught.getMessage() : caught.getCause().getMessage();
                        targetField.forceInvalid("Resolve Failed: "+msg);
                        //PopupUtil.showInfo(targetField, "Resolve Failed", msg);
                    }
                }
            };
            this.nameResolver = task;
            task.start();
        } else {
            if (callback != null) {
                try {
                    callback.onSuccess(convertToJ2000(getRa(), getDec(), getCoordSys()));
                } catch (CoordException e) {
                    callback.onFailure(e);
                }
            }
        }
    }

    private void setLastResolved(String targetName, Resolver resolvedBy) {
        lastResolved = targetName + getRa() + resolvedBy + getDec();
    }

    private boolean isLastResolved(String targetName, Resolver resolvedBy) {
        return lastResolved != null && lastResolved.equals(targetName + getRa() + resolvedBy + getDec());
    }

    private CoordinateSys getCoordinateSys(String coordSys)
        throws CoordException {
        CoordinateSys cs = CoordinateSys.parse(coordSys);
        if (cs == null) {
            throw new CoordException("Unsupported Coordinate System");
        }
        return cs;
    }

    private WorldPt convertToJ2000(float ra, float dec, CoordinateSys coordSys)
            throws CoordException {
        if (Float.isNaN(ra) || Float.isNaN(dec)) {
            return null;
        }
        return convert(ra, dec, coordSys, CoordinateSys.EQ_J2000);
    }

    private WorldPt convert(double ra, double dec, CoordinateSys oldCS, CoordinateSys newCS)
            throws CoordException {
        WorldPt wpIn = new WorldPt(ra, dec, oldCS);
        return VisUtil.convert(wpIn, newCS);
    }

    private void notifyCoordSysChanged(CoordinateSys cs) {
        for (CoordinateSysListener csl : coordSysListeners) {
            csl.onCoordinateSysChange(cs);
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