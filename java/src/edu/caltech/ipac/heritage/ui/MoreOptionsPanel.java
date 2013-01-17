package edu.caltech.ipac.heritage.ui;

import com.google.gwt.user.client.ui.*;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.ui.input.CheckBoxGroupInputField;
import edu.caltech.ipac.firefly.ui.input.InputFieldGroup;
import edu.caltech.ipac.heritage.data.entity.DataType;
import edu.caltech.ipac.util.dd.EnumFieldDef;
import edu.caltech.ipac.firefly.ui.Component;
import edu.caltech.ipac.firefly.ui.input.SimpleInputField;
import edu.caltech.ipac.firefly.core.Preferences;
import edu.caltech.ipac.firefly.core.HelpManager;
import edu.caltech.ipac.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * @author tatianag
 * $Id: MoreOptionsPanel.java,v 1.22 2012/09/18 23:08:08 tatianag Exp $
 */
public class MoreOptionsPanel  extends Component implements InputFieldGroup {

    // date fields used for filtering
    public static final String START_DATE_KEY = "MoreOptions.field.startDate";
    public static final String END_DATE_KEY = "MoreOptions.field.endDate";

    public static final String PRODTYPE_KEY = "MoreOptions.field.prodtype";
    private static final String PRODTYPE_L3_KEY = "MoreOptions.field.prodtype.l3";
    private static final String PRODTYPE_L3IRS_KEY = "MoreOptions.field.prodtype.l3irs";
    private static final String PRODTYPE_I_KEY = "MoreOptions.field.prodtype.i";

    public static final String PRODTYPE_PREF = "DisplayProdTypes";
    public static final String AOR = "aor";
    public static final String PBCD = "pbcd";
    public static final String BCD = "bcd";
    public static final String IRS_ENHANCED ="irsenhanced";
    public static final String SUPERMOSAIC ="supermosaic";
    public static final String SOURCE_LIST ="sourcelist";
    public static final String INVENTORY="inventory";

    private SimplePanel mainPanel = new SimplePanel();
    private static Request lastReq;
    private static List lastProdTypes;

    private SimpleInputField prodType;
    private SimpleInputField prodTypeL3;
    private SimpleInputField prodTypeI;

    private boolean withLevel3;
    private boolean withInventory;


    public MoreOptionsPanel(DataType[] extraProdTypes) {
        ArrayList<DataType> prodTypeLst = new ArrayList<DataType>(extraProdTypes.length);
        for (DataType pt : extraProdTypes) { prodTypeLst.add(pt); }
        this.withLevel3 = prodTypeLst.contains(DataType.IRS_ENHANCED) || prodTypeLst.contains(DataType.SM);
        this.withInventory = prodTypeLst.contains(DataType.LEGACY);

        prodType = SimpleInputField.createByProp(PRODTYPE_KEY);
        prodType.getField().addValueChangeHandler(new ValueChangeHandler<String>() {
            public void onValueChange(ValueChangeEvent valueChangeEvent) {
                if (validate()) {
                    Preferences.set(PRODTYPE_PREF, getProdTypeValue());
                    ((EnumFieldDef)prodType.getFieldDef()).setDefaultValue(prodType.getValue());
                }
            }
        });
        prodTypeL3 = SimpleInputField.createByProp(prodTypeLst.contains(DataType.SM) ? PRODTYPE_L3_KEY : PRODTYPE_L3IRS_KEY);
        prodTypeI = SimpleInputField.createByProp(PRODTYPE_I_KEY);
        if (!withLevel3) {
            prodTypeL3.setVisible(false);
        } else {
            prodTypeL3.getField().addValueChangeHandler(new ValueChangeHandler<String>() {
                public void onValueChange(ValueChangeEvent valueChangeEvent) {
                    if (validate()) {
                        Preferences.set(PRODTYPE_PREF, getProdTypeValue());
                        ((EnumFieldDef)prodTypeL3.getFieldDef()).setDefaultValue(prodTypeL3.getValue());
                    }
                }
            });
        }
        if (!withInventory) {
            prodTypeI.setVisible(false);
        } else {
            prodTypeI.getField().addValueChangeHandler(new ValueChangeHandler<String>() {
                public void onValueChange(ValueChangeEvent valueChangeEvent) {
                    if (validate()) {
                        Preferences.set(PRODTYPE_PREF, getProdTypeValue());
                        ((EnumFieldDef)prodTypeI.getFieldDef()).setDefaultValue(prodTypeI.getValue());
                    }
                }
            });

        }

        HorizontalPanel prodTypeLabel = new HorizontalPanel();
        prodTypeLabel.add(new Label("Display search results in tabs for:"));
        Widget hi = HelpManager.makeHelpIcon("prodtype.desc");
        //prodTypeLabel.add(hi);
        prodTypeLabel.setSpacing(5);
        prodTypeLabel.setCellVerticalAlignment(prodTypeLabel, HorizontalPanel.ALIGN_MIDDLE);
        prodTypeLabel.setWidth("600px");

        HorizontalPanel hp = new HorizontalPanel();
        hp.add(prodType);
        hp.add(prodTypeL3);
        hp.add(prodTypeI);
        hp.add(hi);
        hp.setCellVerticalAlignment(hi, HorizontalPanel.ALIGN_MIDDLE);
        hp.setWidth("100%");

        VerticalPanel vp = new VerticalPanel();
        vp.add(prodTypeLabel);
        vp.add(hp);
        mainPanel.add(vp);

       initWidget(mainPanel);

    }

    private void syncProdType(String updatedProdType) {

        // comma separated sting of values, order can change with time
       if (!StringUtils.isEmpty(updatedProdType)) {
           List<String> prodTypes = Arrays.asList(updatedProdType.split(","));
           String parsedProdTypes = "";
           if (prodTypes.contains(AOR)) {
               parsedProdTypes += StringUtils.isEmpty(parsedProdTypes) ? AOR : ","+AOR;
           }
           if (prodTypes.contains(PBCD)) {
               parsedProdTypes += StringUtils.isEmpty(parsedProdTypes) ? PBCD : ","+PBCD;
           }
           if (prodTypes.contains(BCD)) {
               parsedProdTypes += StringUtils.isEmpty(parsedProdTypes) ? BCD : ","+BCD;
           }
           if (StringUtils.isEmpty(parsedProdTypes)) { parsedProdTypes = CheckBoxGroupInputField.NONE; }
           if (!prodType.getValue().equals(parsedProdTypes)) {
               prodType.setValue(parsedProdTypes);
           }

           if (withLevel3) {
               parsedProdTypes = "";
               if (prodTypes.contains(IRS_ENHANCED)) {
                   parsedProdTypes += StringUtils.isEmpty(parsedProdTypes) ? IRS_ENHANCED : ","+IRS_ENHANCED;
               }
               if (prodTypes.contains(SUPERMOSAIC)) {
                   parsedProdTypes += StringUtils.isEmpty(parsedProdTypes) ? SUPERMOSAIC : ","+SUPERMOSAIC;
               }
               if (prodTypes.contains(SOURCE_LIST)) {
                   parsedProdTypes += StringUtils.isEmpty(parsedProdTypes) ? SOURCE_LIST : ","+SOURCE_LIST;
               }


               if (StringUtils.isEmpty(parsedProdTypes)) { parsedProdTypes = CheckBoxGroupInputField.NONE; }
               if (!prodTypeL3.getValue().equals(parsedProdTypes)) {
                   prodTypeL3.setValue(parsedProdTypes);
               }
           }

           if (withInventory) {
               parsedProdTypes = "";
               if (prodTypes.contains(INVENTORY)) {
                   parsedProdTypes += StringUtils.isEmpty(parsedProdTypes) ? INVENTORY : ","+INVENTORY;
               }
               if (StringUtils.isEmpty(parsedProdTypes)) { parsedProdTypes = CheckBoxGroupInputField.NONE; }
               if (!prodTypeI.getValue().equals(parsedProdTypes)) {
                   prodTypeI.setValue(parsedProdTypes);
               }
           }
       }
    }



    public static boolean isAorRequested(Request req) {
       return  isProdtypeRequested(req, AOR);
    }

    public static boolean isBcdRequested(Request req) {
       return  isProdtypeRequested(req, BCD);
    }

    public static boolean isPbcdRequested(Request req) {
       return  isProdtypeRequested(req, PBCD);
    }

    public static boolean isIrsEnhancedRequested(Request req) {
       return  isProdtypeRequested(req, IRS_ENHANCED) && InstrumentPanel.isIrsEnhancedRequested(req);
    }

    public static boolean isOnlyIrsEnhancedRequested(Request req) {
       return  isOnlyProdtypeRequested(req, IRS_ENHANCED);
    }

    public static boolean isSupermosaicRequested(Request req) {
       return  isProdtypeRequested(req, SUPERMOSAIC) && InstrumentPanel.isSupermosaicRequested(req);
    }

    public static boolean isOnlySupermosaicRequested(Request req) {
       return  isOnlyProdtypeRequested(req, SUPERMOSAIC);
    }

    public static boolean isSourceListRequested(Request req) {
       return  isProdtypeRequested(req, SOURCE_LIST);
    }

    public static boolean isInventoryRequested(Request req) {
       return  isProdtypeRequested(req, INVENTORY);
    }


    private static boolean isProdtypeRequested(Request req, String prodtype) {
        // TODO: all or none?
        List prodTypes;
        if (req.equals(lastReq)) {
            prodTypes = lastProdTypes;
        } else {
            String val = req.getParam(PRODTYPE_KEY);
            if (StringUtils.isEmpty(val)) {
                // if prodtype is not defined.. assume all.
                return true;
            }
            prodTypes = Arrays.asList(val.split(","));
            lastProdTypes = prodTypes;
            lastReq = req;
        }
         return prodTypes.contains(prodtype);
    }

    private static boolean isOnlyProdtypeRequested(Request req, String prodType) {
        String val = req.getParam(PRODTYPE_KEY);
        return !StringUtils.isEmpty(val) && val.equals(prodType);
   }


//====================================================================
//  HasWidgets implementation delegate to TabPane
//====================================================================

    public void add(Widget w) {
        mainPanel.add(w);
    }

    public void clear() {
        mainPanel.clear();
    }

    public Iterator<Widget> iterator() {
        return mainPanel.iterator();
    }

    public boolean remove(Widget w) {
        return mainPanel.remove(w);
    }

//====================================================================
//  InputFieldGroup
//====================================================================


    public List<Param> getFieldValues() {
        List<Param> ret = new ArrayList<Param>(1);
        ret.add(new Param(PRODTYPE_KEY, getProdTypeValue()));
        return ret;
    }

    public void setFieldValues(List<Param> list) {
        for (Param p : list) {
            if (p.getName().equals(PRODTYPE_KEY)) {
                syncProdType(p.getValue());
                return;
            }
        }
        String updatedProdType = Preferences.get(PRODTYPE_PREF);
        if (!StringUtils.isEmpty(updatedProdType)) {
            syncProdType(updatedProdType);
        }
    }

    public boolean validate() {
        boolean noValue = prodType.getValue().equals(CheckBoxGroupInputField.NONE);
        if (withLevel3) { noValue = noValue && prodTypeL3.getValue().equals(CheckBoxGroupInputField.NONE); }
        if (withInventory) { noValue = noValue && prodTypeI.getValue().equals(CheckBoxGroupInputField.NONE); }
        if (noValue) {
            mainPanel.addStyleName("firefly-inputfield-error");
        } else {
            mainPanel.removeStyleName("firefly-inputfield-error");
        }

        return !noValue;
    }

    /*
    public void setL3Visible(boolean visible) {
        prodTypeL3.setVisible(visible);
        withLevel3 = visible;
    }
    */

    public void setInventoryVisible(boolean visible) {
        prodTypeI.setVisible(visible);
        withInventory = visible;
    }


    private String getProdTypeValue() {
        String val = "";
        if (!prodType.getValue().equals(CheckBoxGroupInputField.NONE)) {
            val = prodType.getValue();
        }
        if (withLevel3 && !prodTypeL3.getValue().equals(CheckBoxGroupInputField.NONE)) {
            if (!StringUtils.isEmpty(val)) { val += ","; }
            val += prodTypeL3.getValue();
        }
        if (withInventory && !prodTypeI.getValue().equals(CheckBoxGroupInputField.NONE)) {
            if (!StringUtils.isEmpty(val)) { val += ","; }
            val += prodTypeI.getValue();
        }
        return val;
    }

}
