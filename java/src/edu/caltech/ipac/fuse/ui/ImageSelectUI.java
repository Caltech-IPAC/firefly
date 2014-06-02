package edu.caltech.ipac.fuse.ui;
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
import edu.caltech.ipac.firefly.data.dyn.xstream.FormTag;
import edu.caltech.ipac.firefly.fuse.data.config.ImageSetTag;
import edu.caltech.ipac.firefly.fuse.data.config.MissionTag;
import edu.caltech.ipac.firefly.rpc.UserServices;
import edu.caltech.ipac.firefly.ui.Form;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.RadioGroupInputField;
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


    public ImageSelectUI(DataSetInfo dsInfo) {
        this.dsInfo = dsInfo;
    }

    public Widget makeUI() {
        final FlexTable panel = new FlexTable();
        panel.setCellSpacing(5);
        panel.setStyleName("component-background");
        FlexTable.FlexCellFormatter flexCellFormatter = panel.getFlexCellFormatter();
        flexCellFormatter.setVerticalAlignment(0,0, HasVerticalAlignment.ALIGN_TOP);
        flexCellFormatter.setWidth(0,0, "220px");
        flexCellFormatter.setWidth(0,1, "710px");
        //GwtUtil.setStyle(panel, "lineHeight", "100px");



        UserServices.App.getInstance().getMissionConfig(dsInfo.getId().toLowerCase(), new BaseCallback<MissionTag>() {
            @Override
            public void doSuccess(MissionTag result) {
                if (result != null) {

                    List<ImageSetTag> iltag = result.getImagesetList();
                    if (iltag.size() > 0) {
                        panel.clear();

                        EnumFieldDef fd = new EnumFieldDef("imageSets");
                        fd.setOrientation(EnumFieldDef.Orientation.Vertical);
                        final List<EnumFieldDef.Item> items = new ArrayList<EnumFieldDef.Item>(iltag.size());
                        final List<FormTag> fTags = new ArrayList<FormTag>(iltag.size());


                        for (int i = 0; i < iltag.size(); i++) {
                            final ImageSetTag aTag = iltag.get(i);
                            items.add(new EnumFieldDef.Item(aTag.getName(), aTag.getTitle()));
                            fTags.add(iltag.get(i).getForm());
                        }
                        fd.addItems(items);
                        final RadioGroupInputField rgFld = new RadioGroupInputField(fd);
                        GwtUtil.setStyle(rgFld, "padding", "5px");
                        rgFld.addValueChangeHandler(new ValueChangeHandler<String>() {
                            @Override
                            public void onValueChange(ValueChangeEvent<String> event) {
                                String newVal = event.getValue();
                                for (int i = 0; i < items.size(); i++) {
                                    if (items.get(i).getName().equals(newVal)) {
                                        Form form = GwtUtil.createSearchForm(fTags.get(i), null);
                                        form.setStyleName("expand-fully");
                                        panel.setWidget(0, 1, form);
                                        break;
                                    }
                                }
                            }
                        });
                        panel.setWidget(0, 0, rgFld);
                        rgFld.setValue(items.get(0).getName());
                        if (rgFld.isVisible()) {
                            ValueChangeEvent.fire(rgFld, items.get(0).getName());
                        }

                    }
                } else {
                    Label label = new Label("Image View is not ready yet for " + dsInfo.getUserDesc());
                    panel.setWidget(0, 1, label);
                }
                panel.addStyleName("expand-fully");
            }
        });

        ScrollPanel panelHolder = new ScrollPanel(GwtUtil.wrap(panel, 20, 20, 20, 20));
        panelHolder.addStyleName("expand-fully");
        return panelHolder;
    }

    public List<Param> getFieldValues() {
        return Collections.emptyList(); //todo
    }

    public void setFieldValues(List<Param> list) {
        //todo
    }

    public boolean validate() {
        return true; //todo
    }

    public String makeRequestID() {
        return "SomeImageRequestID";
    }

    public Iterator<Widget> iterator() {
        List<Widget> l= Collections.emptyList();
        return l.iterator();
    }

    public void add(Widget w) { throw new UnsupportedOperationException("operation not allowed"); }
    public void clear() { throw new UnsupportedOperationException("operation not allowed"); }
    public boolean remove(Widget w) { throw new UnsupportedOperationException("operation not allowed"); }



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
