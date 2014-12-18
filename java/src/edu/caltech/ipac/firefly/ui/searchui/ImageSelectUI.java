package edu.caltech.ipac.firefly.ui.searchui;
/**
 * User: roby
 * Date: 2/14/14
 * Time: 11:35 AM
 */


import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.*;
import edu.caltech.ipac.firefly.core.BaseCallback;
import edu.caltech.ipac.firefly.data.DataSetInfo;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.dyn.xstream.FormTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.ParamTag;
import edu.caltech.ipac.firefly.fuse.data.config.DataSourceTag;
import edu.caltech.ipac.firefly.fuse.data.config.ImageSetTag;
import edu.caltech.ipac.firefly.fuse.data.config.MissionTag;
import edu.caltech.ipac.firefly.rpc.UserServices;
import edu.caltech.ipac.firefly.ui.Form;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.RadioGroupInputField;
import edu.caltech.ipac.firefly.ui.input.InputField;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.dd.EnumFieldDef;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * @author Trey Roby
 */
public class ImageSelectUI implements DataTypeSelectUI {

    private DataSetInfo dsInfo;
    private Form form = null;
    private ImageSetTag currentImageSet;


    public ImageSelectUI(DataSetInfo dsInfo) {
        this.dsInfo = dsInfo;
    }

    public String getDataDesc() {
        return currentImageSet==null ? "Image Data" : currentImageSet.getName();
    }

    public Widget makeUI() {
        final FlowPanel panel = new FlowPanel();

        UserServices.App.getInstance().getMissionConfig(dsInfo.getId().toLowerCase(), new BaseCallback<MissionTag>() {
            @Override
            public void doSuccess(MissionTag result) {
                if (result != null) {

                    final List<ImageSetTag> iltag = result.getImagesetList();
                    if (iltag.size() > 0) {
                        panel.clear();


                        EnumFieldDef fd = new EnumFieldDef("imageSets");
                        fd.setOrientation(EnumFieldDef.Orientation.Vertical);
                        final List<EnumFieldDef.Item> items = new ArrayList<EnumFieldDef.Item>(iltag.size());

                        for (final ImageSetTag aTag : iltag) {
                            items.add(new EnumFieldDef.Item(aTag.getName(), aTag.getTitle()));
                        }
                        fd.addItems(items);
                        final RadioGroupInputField rgFld = new RadioGroupInputField(fd);
                        GwtUtil.setStyles(rgFld,
                                "padding", "5px",
                                "display", "inline-block",
                                "verticalAlign", "top",
                                "width", "25%",
                                "height", "200px",
                                "overflow", "auto"
                        );
                        rgFld.addValueChangeHandler(new ValueChangeHandler<String>() {
                            public void onValueChange(ValueChangeEvent<String> event) {
                                String newVal = event.getValue();
                                for (int i = 0; i < items.size(); i++) {
                                    if (items.get(i).getName().equals(newVal)) {
                                        currentImageSet = iltag.get(i);
                                        FormTag ftag = currentImageSet.getForm();
                                        if (form != null) panel.remove(1);
                                        form = GwtUtil.createSearchForm(ftag, null);
                                        GwtUtil.setStyles(form,
                                                "backgroundColor", "white",
                                                "border", "1px solid black",
                                                "display", "inline-block",
                                                "verticalAlign", "top",
                                                "width", "70%",
                                                "height", "200px",
                                                "overflow", "auto"
                                        );
                                        panel.add(form);
                                        break;
                                    }
                                }
                            }
                        });

                        panel.add(rgFld);

                        rgFld.setValue(items.get(0).getName());
                        if (rgFld.isVisible()) {
                            ValueChangeEvent.fire(rgFld, items.get(0).getName());
                        }

                    }
                } else {
                    Label label = new Label("Image View is not ready yet for " + dsInfo.getUserDesc());
                    panel.add(label);
                    form = null;
                    currentImageSet = null;
                }
            }
        });

        panel.addStyleName("expand-fully");
        return GwtUtil.wrap(panel, 20, 20, 20, 20);
    }

    public List<Param> getFieldValues() {
        Request paramHolder = new Request();

        // todo: handling async fields?
        form.populateRequest(paramHolder);
        ArrayList<Param> params = new ArrayList<Param>();
        for (Param p : paramHolder.getParams()) {
            if (form.containsField(p.getName()) && !StringUtils.isEmpty(p.getValue())) {
                params.add(p);
            }
        }
        DataSourceTag dataSource = currentImageSet.getDataSource();
        for (ParamTag p : dataSource.getParam()) {
            params.add( new Param(p.getKey(), p.getValue()));
        }
        return params;
    }

    public void setFieldValues(List<Param> list) {

        if (form == null) return;

        form.reset();
        for (Param p : list) {
            InputField f = form.getField(p.getName());
            if (f != null) {
                f.setValue(p.getValue());
            }
        }
    }

    public boolean validate() {
        return form.validate();
    }

    public String makeRequestID() {
        if (currentImageSet != null) {
            return currentImageSet.getDataSource().getSearchProcId();
        } else {
            return "UnknownImageRequestID";
        }
    }

    public Iterator<Widget> iterator() {
        List<Widget> l= Collections.emptyList();
        return l.iterator();
    }

    public void add(Widget w) { throw new UnsupportedOperationException("operation not allowed"); }
    public void clear() { throw new UnsupportedOperationException("operation not allowed"); }
    public boolean remove(Widget w) { throw new UnsupportedOperationException("operation not allowed"); }



}

