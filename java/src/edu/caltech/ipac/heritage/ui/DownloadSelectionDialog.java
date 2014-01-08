package edu.caltech.ipac.heritage.ui;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.LoginManager;
import edu.caltech.ipac.firefly.core.Preferences;
import edu.caltech.ipac.firefly.core.layout.LayoutManager;
import edu.caltech.ipac.firefly.data.DownloadRequest;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.data.userdata.UserInfo;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.PackageTask;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.ui.background.BackgroundManager;
import edu.caltech.ipac.firefly.ui.input.SimpleInputField;
import edu.caltech.ipac.firefly.ui.table.BaseDownloadDialog;
import edu.caltech.ipac.firefly.util.WebAssert;
import edu.caltech.ipac.firefly.util.WebClassProperties;
import edu.caltech.ipac.heritage.data.entity.DataType;
import edu.caltech.ipac.heritage.data.entity.WaveLength;
import edu.caltech.ipac.heritage.data.entity.download.HeritageDownloadRequest;
import edu.caltech.ipac.heritage.data.entity.download.InventoryDownloadRequest;
import edu.caltech.ipac.heritage.searches.HeritageSearch;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.dd.ValidationException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * @author Trey Roby
 * $Id: DownloadSelectionDialog.java,v 1.66 2013/01/09 23:02:56 tatianag Exp $
 */
public class DownloadSelectionDialog extends BaseDownloadDialog {

//    public static final boolean NETWORK= true;

//    private boolean isDialogBuilt;


    public enum DialogType {BCD, POSTBCD, AOR, IRS_ENHANCED, SM, LEGACY, MOS }
//    enum DataType {BCD, POSTBCD, CALIBRATION, RAW, ANCILARY}


    private static WebClassProperties _prop= new WebClassProperties(DownloadSelectionDialog.class);

    private final static String DOWNLOAD_TXT= _prop.getName("download");




//  private DialogBox _dialog= new DialogBox();
    private DockPanel _layout= new DockPanel();
    private VerticalPanel _contents= new VerticalPanel();
    private HorizontalPanel _buttons= new HorizontalPanel();
    private DialogType _type;
    private String _desc;
//  private CheckBox _dataTypes[];
    private Map<DataType,CheckBox> _dtSelection= new LinkedHashMap<DataType,CheckBox>();
//    private Map<WaveLength,CheckBox> _wlSelection= new TreeMap<WaveLength,CheckBox>();
//  private final TextBox _email= new TextBox();
    private SimpleInputField _emailField;
    private CheckBox _useEmail;
    private int _centerX;
    private int _centerY;

//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public DownloadSelectionDialog(DialogType type,
                                   DownloadRequest dataRequest, TableDataView dataset ) {
        super(_prop.getTitle(), "basics.download");
        _type= type;
        setDataView(dataset);
        setDownloadRequest(dataRequest);

    }
//======================================================================
//----------------------- Public Static Methods ------------------------
//======================================================================


//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    @Override
    protected void deferredBuild() {
        buildDialog();
    }

    @Override
    protected void onVisible() {
        super.setAutoLocate(false);
        setDefaultEmail();
    }

//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    private void buildDialog() {
//        isDialogBuilt = false;
//        createButtons();
        createContents();
        _layout.add(_buttons, DockPanel.SOUTH);
        _layout.add(_contents, DockPanel.CENTER);
        setWidget(_layout);
//        _dialog.setWidget(_layout);
//        _dialog.setText(_prop.getTitle());
//        _dialog.addStyleName("download-sel-dialog");
//        isDialogBuilt = true;
    }

    private void createContents()  {
        switch (_type) {

            case BCD :     createBCDContents(); break;
            case POSTBCD : createPostBCDContents(); break;
            case AOR :     createAORContents(); break;
            case LEGACY : createLegacyContents(); break;
            case IRS_ENHANCED : createIrsEnhancedContents(); break;
            case SM : createSupermosaicsContents(); break;
            case MOS: createBCDContents(); break;
            default :
                WebAssert.tst(false, "only know about BCD, POSTBCD, AOR, or LEGACY"); break;
        }


        VerticalPanel dtHolder= new VerticalPanel();
        dtHolder.addStyleName("download-sel-dialog-dataops");
//        if (_type== ButtonType.AOR) {
        dtHolder.addStyleName("highlight-dataops");
//        }
        for(CheckBox cb : _dtSelection.values()) {
            dtHolder.add(cb);
        }

        HTML descWidget= new HTML(_desc);
        descWidget.addStyleName("download-sel-dialog-desc");
        _contents.add(descWidget);
        _contents.add(dtHolder);

        //if (_type== DialogType.AOR) {
            //createAORWaveLengths();
        //}


        _useEmail= GwtUtil.makeCheckBox(_prop.makeBase("useEmail"));
        _useEmail.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) { showEmail(_useEmail.getValue()); }
        });

//        _emailField= GwtUtil.makeTextInput(_prop.makeBase("email"),_email);
        _emailField= SimpleInputField.createByProp(_prop.makeBase("email"));
        setDefaultEmail();

//        VerticalPanel emailPanel= new VerticalPanel();
        _contents.add(_useEmail);
        _contents.add(_emailField);
//        emailPanel.setSpacing(30);
        _emailField.setVisible(_useEmail.getValue());
        _contents.add(GwtUtil.centerAlign(_useEmail));
        _contents.add(GwtUtil.centerAlign(_emailField));
        _contents.setSpacing(5);
        Label holder= new Label();
        holder.setWidth("450px");
        _contents.add(holder);

        Button okb= this.getButton(ButtonID.OK);
        okb.setText(DOWNLOAD_TXT);

    }

    private void showEmail(boolean show) {
        if (show) {
            setDefaultEmail();
         }
        _emailField.setVisible(show);
    }

    private void setDefaultEmail() {
        String prefEmailStr = Preferences.get(BackgroundManager.EMAIL_PREF);
        if (!StringUtils.isEmpty(prefEmailStr)) {
            _emailField.setValue(prefEmailStr);
        } else {
            LoginManager loginManager = Application.getInstance().getLoginManager();
            if (loginManager.isLoggedIn()) {
                UserInfo userInfo = loginManager.getLoginInfo();
                String emailStr = userInfo.getEmail();
                if (!StringUtils.isEmpty(emailStr))  _emailField.setValue(emailStr);
            } else {
                _emailField.reset();
            }
        }
    }


    @Override
    protected boolean validateInput() throws ValidationException {

        Widget w= getDialogWidget();
        _centerX= w.getAbsoluteLeft()+ w.getOffsetWidth()/2;
        _centerY= w.getAbsoluteTop()+ w.getOffsetHeight()/2;
        return validInput();
    }

    protected void inputComplete() {
        startPackaging();
    }

    private boolean validInput() {

        boolean retval= true;
        boolean anyDateTypeChecked= false;
        for(Map.Entry<DataType,CheckBox> entry : _dtSelection.entrySet()) {
            if (entry.getValue().getValue()) {
                anyDateTypeChecked= true;
                break;
            }
        }
//        boolean anyWLChecked= true;
//        if (_type== ButtonType.AOR) {
//            anyWLChecked= false;
//            for(Map.Entry<WaveLength,CheckBox> entry : _wlSelection.entrySet()) {
//                if (entry.getValue().isChecked()) {
//                    anyWLChecked= true;
//                    break;
//                }
//            }
//        }
//        else {
//            anyWLChecked= true;
//        }
//       if (!anyWLChecked || !anyDateTypeChecked) {

        if (!anyDateTypeChecked) {
            retval= false;
            PopupUtil.showError(_prop.getTitle("nonSelected"),
                            _prop.getError("nonSelected"));
//            MessageBox.alert(_prop.getTitle("nonSelected"),
//                             _prop.getError("nonSelected"), null);
        }
        return retval;

    }

    private void createAORContents() {
        _desc= _prop.getName("AORDesc");

        addDTCheckbox(DataType.PBCD, true, true);
        addDTCheckbox(DataType.PBCD_ANCIL).setStyleName("download-sel-dialog-dataops-sub");
        addDTCheckbox(DataType.BCD, true, true);
        addDTCheckbox(DataType.BCD_ANCIL).setStyleName("download-sel-dialog-dataops-sub");
        addDTCheckbox(DataType.CAL);
        addDTCheckbox(DataType.RAW);

    }

    private void createLegacyContents() {
        _desc= _prop.getName("EnhancedDesc");
        addDTCheckbox(DataType.LEGACY, true, true);
        addDTCheckbox(DataType.LEGACY_ANCIL).setStyleName("download-sel-dialog-dataops-sub");
    }

    private void createIrsEnhancedContents() {
        _desc= _prop.getName("IrsEnhancedDesc");
        addDTCheckbox(DataType.IRS_ENHANCED, true, false);
        addDTCheckbox(DataType.PBCD);
        addDTCheckbox(DataType.PBCD_ANCIL).setStyleName("download-sel-dialog-dataops-sub");
        addDTCheckbox(DataType.BCD);
        addDTCheckbox(DataType.BCD_ANCIL).setStyleName("download-sel-dialog-dataops-sub");
        addDTCheckbox(DataType.CAL);
        addDTCheckbox(DataType.RAW);

    }

    private void createSupermosaicsContents() {
        _desc= _prop.getName("SupermosaicDesc");
        addDTCheckbox(DataType.SM, true, false);
        addDTCheckbox(DataType.SM_ANCIL).setStyleName("download-sel-dialog-dataops-sub");
        //addDTCheckbox(DataType.BCD);
        //addDTCheckbox(DataType.BCD_ANCIL).setStyleName("download-sel-dialog-dataops-sub");
    }

    private void createPostBCDContents() {
        _desc= _prop.getName("PostBCDDesc");

        addDTCheckbox(DataType.PBCD, true, true);
        addDTCheckbox(DataType.PBCD_ANCIL).setStyleName("download-sel-dialog-dataops-sub");
        addDTCheckbox(DataType.BCD);
        addDTCheckbox(DataType.BCD_ANCIL).setStyleName("download-sel-dialog-dataops-sub");
        addDTCheckbox(DataType.CAL);
        addDTCheckbox(DataType.RAW);

    }

    private void createBCDContents() {
        _desc= _prop.getName("BCDDesc");

        addDTCheckbox(DataType.BCD, true, true);
        addDTCheckbox(DataType.BCD_ANCIL).setStyleName("download-sel-dialog-dataops-sub");
        addDTCheckbox(DataType.CAL);
        addDTCheckbox(DataType.RAW);

    }

    /*
    private void createAORWaveLengths() {
        HorizontalPanel wlPanel= new HorizontalPanel();
        wlPanel.addStyleName("download-sel-dialog-wl");
        _contents.add(wlPanel);
        HTML title;

        if (includesIRAC()) {

            VerticalPanel ip= new VerticalPanel();
            ip.addStyleName("download-sel-IracWL");

            addWLCheckbox(WaveLength.IRAC_36);
            addWLCheckbox(WaveLength.IRAC_45);
            addWLCheckbox(WaveLength.IRAC_58);
            addWLCheckbox(WaveLength.IRAC_80);

            title= new HTML(_prop.getTitle("irac"));
            title.addStyleName("download-sel-wl-title");
            ip.add(title);


            ip.add(_wlSelection.get(WaveLength.IRAC_36));
            ip.add(_wlSelection.get(WaveLength.IRAC_45));
            ip.add(_wlSelection.get(WaveLength.IRAC_58));
            ip.add(_wlSelection.get(WaveLength.IRAC_80));
            wlPanel.add(ip);
        }

        if (includesMips()) {
            VerticalPanel mp= new VerticalPanel();
            mp.addStyleName("download-sel-MipsWL");
            addWLCheckbox(WaveLength.MIPS_24);
            addWLCheckbox(WaveLength.MIPS_70);

            title= new HTML(_prop.getTitle("mips"));
            title.addStyleName("download-sel-wl-title");
            mp.add(title);

            addWLCheckbox(WaveLength.MIPS_160);
            mp.add(_wlSelection.get(WaveLength.MIPS_24));
            mp.add(_wlSelection.get(WaveLength.MIPS_70));
            mp.add(_wlSelection.get(WaveLength.MIPS_160));
            wlPanel.add(mp);
        }

        if (includesIRS()) {
            HTML irs= new HTML(_prop.getName("IRSwlDesc"));
            irs.addStyleName("download-sel-IrsWL");
            wlPanel.add(irs);
        }
    }

    private boolean includesIRS() {
        return true;
    }

    private boolean includesIRAC() {
        return true;
    }

    private boolean includesMips() {
        return true;
    }

    private void addWLCheckbox(WaveLength wl) {
        addWLCheckbox(wl,true, true);
    }

    private void addWLCheckbox(WaveLength wl,
                               boolean set,
                               boolean modifiable) {
        CheckBox cb= new CheckBox();
        cb.setValue(set);
        cb.setEnabled(modifiable);
        _wlSelection.put(wl, cb);
        switch (wl) {
            case MIPS_24 :
                cb.setHTML(_prop.getName("mips24"));
                break;
            case MIPS_70 :
                cb.setHTML(_prop.getName("mips70"));
                break;
            case MIPS_160:
                cb.setHTML(_prop.getName("mips160"));
                break;
            case IRAC_36 :
                cb.setHTML(_prop.getName("irac36"));
                break;
            case IRAC_45 :
                cb.setHTML(_prop.getName("irac45"));
                break;
            case IRAC_58 :
                cb.setHTML(_prop.getName("irac58"));
                break;
            case IRAC_80 :
                cb.setHTML(_prop.getName("irac80"));
                break;
            default :
                WebAssert.tst(false, "don't know that wavelength");
                break;
        }
    }
    */


    private CheckBox addDTCheckbox(DataType dt) {
        return addDTCheckbox(dt,false, true);
    }


    private CheckBox addDTCheckbox(DataType dt,
                                   boolean set,
                                   boolean modifiable) {
        CheckBox cb= new CheckBox();
        cb.setValue(set);
        cb.setEnabled(modifiable);
        switch (dt) {
            case BCD :
                cb.setHTML( _prop.getName("bcd"));
                break;
            case PBCD :
                cb.setHTML( _prop.getName("postbcd"));
                break;
            case BCD_ANCIL:
                cb.setHTML( _prop.getName("bcd.ancilary"));
                break;
            case PBCD_ANCIL:
                cb.setHTML( _prop.getName("pbcd.ancilary"));
                break;
            case CAL:
                cb.setHTML( _prop.getName("calibration"));
                break;
            case RAW :
                cb.setHTML( _prop.getName("raw"));
                break;
            case LEGACY :
                cb.setHTML( _prop.getName("enhanced"));
                break;
            case LEGACY_ANCIL :
                cb.setHTML( _prop.getName("enhanced.ancilary"));
                break;
            case IRS_ENHANCED :
                cb.setHTML( _prop.getName("irsenhanced"));
                break;
            case SM :
                cb.setHTML( _prop.getName("supermosaic"));
                break;
            case SM_ANCIL :
                cb.setHTML( _prop.getName("supermosaic.ancilary"));
                break;

            default :
                WebAssert.tst(false, "only know about BCD, POSTBCD, CALIBRATION, RAW, ANCILARY, and LEGACY");
                break;
        }
        _dtSelection.put(dt, cb);
        return cb;
    }

    private void startPackaging() {

        TitleRet title= buildTitle();

        switch (_type) {

            case BCD :     break;
            case POSTBCD : break;
            case AOR :     break;
            case LEGACY : break;
            case IRS_ENHANCED : break;
            case SM : break;
            default :
                WebAssert.tst(false, "only know about BCD, POSTBCD, AOR, IRS_ENHANCED, SM, or LEGACY");
                break;
        }


        List<DataType> dtList= new ArrayList<DataType>();
        for(Map.Entry<DataType,CheckBox> entry : _dtSelection.entrySet()) {
            if (entry.getValue().getValue()) {
                dtList.add(entry.getKey());
            }

        }

        String emailStr= null;
        if (_useEmail.getValue() && !StringUtils.isEmpty(_emailField.getValue()))  {
            emailStr= _emailField.getValue();
            Preferences.set(BackgroundManager.EMAIL_PREF, emailStr);
        }

        List<WaveLength> _waveLengthList=Arrays.asList(WaveLength.ALL);

        DownloadRequest dataRequest = getDownloadRequest();

        LayoutManager lman= Application.getInstance().getLayoutManager();
        if (getDataView() != null) {
            dataRequest.setSelectionInfo(getDataView().getSelectionInfo());
        }
        dataRequest.setBaseFileName(title.getFileName());
        dataRequest.setTitle(title.getTitle());
        dataRequest.setEmail(emailStr);
//        _dataRequest.getSearchRequest().setFilters(_dataRequest.getSearchRequest().getFilters());
//        _dataRequest.getSearchRequest().setSortInfo(_dataRequest.getSearchRequest().getSortInfo());
        DownloadRequest downloadRequest;
        if (_type.equals(DialogType.LEGACY)) {
            boolean includeRelated = dtList.contains(DataType.LEGACY_ANCIL);
            downloadRequest = new InventoryDownloadRequest(dataRequest, includeRelated);
        } else {
            if (_type.equals(DialogType.IRS_ENHANCED)) {
                downloadRequest = dataRequest;
                downloadRequest.setRequestId("irsEnhancedDownload");
                downloadRequest.setDataSource("Spitzer");
                downloadRequest.setParam("datatypes", StringUtils.toString(dtList,","));
            } else {
                downloadRequest = new HeritageDownloadRequest(dataRequest, dtList,_waveLengthList);
            }
        }

        Widget maskW=  lman.getRegion(LayoutManager.RESULT_REGION).getDisplay();
        PackageTask.preparePackage(maskW,_centerX,_centerY,downloadRequest);
    }

    private TitleRet buildTitle() {
        String title;
        String baseFileName;
        DownloadRequest dataRequest = getDownloadRequest();
        int cnt= (getDataView() == null) ? dataRequest.getSelectedRows().size() :
                getDataView().getSelectionInfo().getSelectedCount();
        String preTitle= cnt + " ";
        String preFile= cnt + "-";
        String howMuch= cnt>1 ? ".plural" : ".singular";

        preTitle = dataRequest.getTitlePrefix() +preTitle;
        preFile  = dataRequest.getFilePrefix() +preFile;


        String type = dataRequest.getSearchRequest().getRequestId();
        if (type.startsWith("bcd")) {
            title=  preTitle +  _prop.getTitle("bcd"+howMuch);
            baseFileName=  preFile +_prop.getName("bcd.file");
        } else if (type.startsWith("pbcd")){
            title= preTitle + _prop.getTitle("postbcd"+howMuch);
            baseFileName=  preFile +_prop.getName("postbcd.file");
        } else if (type.startsWith("aor")) {
            title= preTitle + _prop.getTitle("aor"+howMuch);
            baseFileName=  preFile +_prop.getName("aor.file");
        } else if (type.startsWith("heritageInventory")) {
            title= preTitle + _prop.getTitle("enhanced"+howMuch);
            baseFileName=  preFile +_prop.getName("enhanced.file");
        } else if (type.equals(HeritageSearch.IRS_ENHANCED_SEARCH_ID)) {
            title= preTitle + _prop.getTitle("irsenhanced"+howMuch);
            baseFileName=  preFile +_prop.getName("irsenhanced.file");
        } else if (type.startsWith("sm")) {
            title= preTitle + _prop.getTitle("supermosaic"+howMuch);
            baseFileName=  preFile +_prop.getName("supermosaic.file");
        } else if (type.startsWith("MOS")) {
            title= preTitle + _prop.getTitle("bcd"+howMuch);
            baseFileName=  preFile +_prop.getName("bcd.file");
        } else {
            WebAssert.tst(false, "only know about BCD, POSTBCD, AOR, IRS_ENHANCED, SM, or LEGACY");
            title= "no title";
            baseFileName= "nofilename";
        }
        return new TitleRet(title,baseFileName);

    }


    private static class TitleRet {
        private final String _title;
        private final String _baseFileName;
        public TitleRet(String title, String baseFileName)  {
            _title= title;
            _baseFileName= baseFileName;
        }
        public String getTitle() { return _title; }
        public String getFileName() { return _baseFileName; }
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