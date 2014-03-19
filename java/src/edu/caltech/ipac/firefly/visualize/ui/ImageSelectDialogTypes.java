package edu.caltech.ipac.firefly.visualize.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.VerticalPanel;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.fftools.FFToolEnv;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.MaskPane;
import edu.caltech.ipac.firefly.ui.input.SimpleInputField;
import edu.caltech.ipac.firefly.ui.table.TabPane;
import edu.caltech.ipac.firefly.util.PropConst;
import edu.caltech.ipac.firefly.util.WebAppProperties;
import edu.caltech.ipac.firefly.util.WebAssert;
import edu.caltech.ipac.firefly.util.WebClassProperties;
import edu.caltech.ipac.firefly.util.WebUtil;
import edu.caltech.ipac.firefly.visualize.PlotWidgetOps;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.ZoomType;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.dd.ValidationException;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.util.ArrayList;
import java.util.List;

/**
 * User: roby
 * Date: Jan 26, 2009
 * Time: 3:45:42 PM
 */


/**
 * @author Trey Roby
 */
public class ImageSelectDialogTypes {

    private final ImageSelectPanel imageSelectPanel;
    private final WebClassProperties _prop;
    private final List<PlotTypeUI> _types = new ArrayList<PlotTypeUI>(8);

    ImageSelectDialogTypes(ImageSelectPanel imageSelectPanel, WebClassProperties prop) {
        this.imageSelectPanel = imageSelectPanel;
        _prop = prop;
    }

    List<PlotTypeUI> getPlotTypes() {
        if (_types.size() == 0) {
            _types.add(new IssaType());
            _types.add(new TwoMassType());
            _types.add(new WiseType());
            _types.add(new MsxType());
            _types.add(new DssType());
            _types.add(new SloanDssType());
            _types.add(new FileType());
            _types.add(new URLType());
            _types.add(new BlankType());
//            _types.add(new FileTypeAlreadyOnServer());
        }
        return _types;
    }

    protected void insertZoomType(WebPlotRequest request) {
//        request.setZoomType(ZoomType.SMART);
        request.setZoomType(ZoomType.FULL_SCREEN);
        request.setZoomToWidth(imageSelectPanel.getPlotWidgetWidth());
    }

    //======================================================================
//------------------ ISSA ----------------------------------------------
//======================================================================
    public class IssaType extends PlotTypeUI {
        private final SimpleInputField _issaFields = SimpleInputField.createByProp(_prop.makeBase("issa.types"));

        IssaType() {
            super(true, true, false, false);
        }

        public void addTab(TabPane<Panel> tabs) {
            VerticalPanel vp = new VerticalPanel();
            vp.add(_issaFields);
            vp.setSpacing(10);
            tabs.addTab(vp, _prop.getTitle("issa"));
        }

        public WebPlotRequest createRequest() {
            WebPlotRequest request;
            String band = _issaFields.getValue().substring(5);
            WorldPt pos = imageSelectPanel.getJ2000Pos();
            if (_issaFields.getValue().startsWith("issa-")) {

                request = WebPlotRequest.makeISSARequest(pos, band,
                                                         imageSelectPanel.getStandardPanelDegreeValue());
            } else {
                request = WebPlotRequest.makeIRISRequest(pos, band,
                                                         imageSelectPanel.getStandardPanelDegreeValue());
            }
            insertZoomType(request);
            //todo remove here down
//           request.setRotate(true);
//           request.setRotateNorth(true);
//           request.setRotateNorthType(CoordinateSys.GALACTIC);
//           request.setPostCrop(true);
//           request.setCropPt1(new ImagePt(30,30));
//           request.setCropPt2(new ImagePt(250,250));
            //todo remove here up
            return request;
        }

        public void updateSizeArea() {
            WebAppProperties webProp = Application.getInstance().getProperties();
            double minDeg = webProp.getDoubleProperty(_prop.makeBase("issa.size." + PropConst.MIN), 0);
            double maxDeg = webProp.getDoubleProperty(_prop.makeBase("issa.size." + PropConst.MAX), 0);
            double defDeg = webProp.getDoubleProperty(_prop.makeBase("issa.size." + PropConst.DEFAULT), 0);
            imageSelectPanel.updateSizeIfChange(minDeg, maxDeg, defDeg);
        }

        public String getDesc() {
            return _prop.getTitle("issa");
        }
    }

    //======================================================================
//------------------ 2MASS ---------------------------------------------
//======================================================================
    public class TwoMassType extends PlotTypeUI {
        private final SimpleInputField _2massFields = SimpleInputField.createByProp(_prop.makeBase("2mass.types"));

        TwoMassType() {
            super(true, true, false, false);
        }

        public void addTab(TabPane<Panel> tabs) {
            VerticalPanel vp = new VerticalPanel();
            vp.add(_2massFields);
            vp.setSpacing(10);
            tabs.addTab(vp, _prop.getTitle("2mass"));
        }

        public WebPlotRequest createRequest() {


            WorldPt pos = imageSelectPanel.getJ2000Pos();
            WebPlotRequest req = WebPlotRequest.make2MASSRequest(pos,
                                                                 _2massFields.getValue(),
                                                                 imageSelectPanel.getStandardPanelDegreeValue());
            insertZoomType(req);
            return req;
        }

        public void updateSizeArea() {
            WebAppProperties webProp = Application.getInstance().getProperties();
            double minDeg = webProp.getDoubleProperty(_prop.makeBase("2mass.size." + PropConst.MIN), 0);
            double maxDeg = webProp.getDoubleProperty(_prop.makeBase("2mass.size." + PropConst.MAX), 0);
            double defDeg = webProp.getDoubleProperty(_prop.makeBase("2mass.size." + PropConst.DEFAULT), 0);
            imageSelectPanel.updateSizeIfChange(minDeg, maxDeg, defDeg);
        }

        public String getDesc() {
            return _prop.getTitle("2mass");
        }
    }

    //======================================================================
//------------------ MSX -----------------------------------------------
//======================================================================
    public class MsxType extends PlotTypeUI {
        private final SimpleInputField _msxFields = SimpleInputField.createByProp(_prop.makeBase("msx.types"));

        MsxType() {
            super(true, true, false, false);
        }

        public void addTab(TabPane<Panel> tabs) {
            VerticalPanel vp = new VerticalPanel();
            vp.setSpacing(10);
            vp.add(_msxFields);
            tabs.addTab(vp, _prop.getTitle("msx"));
        }

        public WebPlotRequest createRequest() {
            WorldPt pos = imageSelectPanel.getJ2000Pos();
            WebPlotRequest req = WebPlotRequest.makeMSXRequest(pos, _msxFields.getValue(),
                                                               imageSelectPanel.getStandardPanelDegreeValue());
            insertZoomType(req);
            return req;
        }

        public void updateSizeArea() {
            WebAppProperties webProp = Application.getInstance().getProperties();
            double minDeg = webProp.getDoubleProperty(_prop.makeBase("msx.size." + PropConst.MIN), 0);
            double maxDeg = webProp.getDoubleProperty(_prop.makeBase("msx.size." + PropConst.MAX), 0);
            double defDeg = webProp.getDoubleProperty(_prop.makeBase("msx.size." + PropConst.DEFAULT), 0);
            imageSelectPanel.updateSizeIfChange(minDeg, maxDeg, defDeg);
        }

        public String getDesc() {
            return _prop.getTitle("msx");
        }
    }

//======================================================================
//------------------ DSS ----------------------------------------------
//======================================================================
    public class DssType extends PlotTypeUI {
        private final SimpleInputField _dssFields = SimpleInputField.createByProp(_prop.makeBase("dss.types"));

        DssType() {
            super(true, true, false, false);
        }

        public void addTab(TabPane<Panel> tabs) {
            VerticalPanel vp = new VerticalPanel();
            vp.setSpacing(10);
            vp.add(_dssFields);
            tabs.addTab(vp, _prop.getTitle("dss"));
        }

        public WebPlotRequest createRequest() {
            WorldPt pos = imageSelectPanel.getJ2000Pos();
            WebPlotRequest req = WebPlotRequest.makeDSSRequest(pos, _dssFields.getValue(),
                                                               imageSelectPanel.getStandardPanelDegreeValue());
            insertZoomType(req);
            return req;
        }

        public void updateSizeArea() {
            WebAppProperties webProp = Application.getInstance().getProperties();
            double minDeg = webProp.getDoubleProperty(_prop.makeBase("dss.size." + PropConst.MIN), 0);
            double maxDeg = webProp.getDoubleProperty(_prop.makeBase("dss.size." + PropConst.MAX), 0);
            double defDeg = webProp.getDoubleProperty(_prop.makeBase("dss.size." + PropConst.DEFAULT), 0);
            imageSelectPanel.updateSizeIfChange(minDeg, maxDeg, defDeg);
        }

        public String getDesc() {
            return _prop.getTitle("dss");
        }
    }



//======================================================================
//------------------ SDSS ----------------------------------------------
//======================================================================
    public class SloanDssType extends PlotTypeUI {
        private final SimpleInputField _sdssFields = SimpleInputField.createByProp(_prop.makeBase("sdss.types"));

        SloanDssType() {
            super(true, true, false, false);
        }

        public void addTab(TabPane<Panel> tabs) {
            VerticalPanel vp = new VerticalPanel();
            vp.setSpacing(10);
            vp.add(_sdssFields);
            tabs.addTab(vp, _prop.getTitle("sdss"));
        }

        public WebPlotRequest createRequest() {
            WorldPt pos = imageSelectPanel.getJ2000Pos();
            WebPlotRequest req = WebPlotRequest.makeSloanDSSRequest(pos, _sdssFields.getValue(),
                                                               imageSelectPanel.getStandardPanelDegreeValue());
            insertZoomType(req);
            return req;
        }

        public void updateSizeArea() {
            WebAppProperties webProp = Application.getInstance().getProperties();
            double minDeg = webProp.getDoubleProperty(_prop.makeBase("sdss.size." + PropConst.MIN), 0);
            double maxDeg = webProp.getDoubleProperty(_prop.makeBase("sdss.size." + PropConst.MAX), 0);
            double defDeg = webProp.getDoubleProperty(_prop.makeBase("sdss.size." + PropConst.DEFAULT), 0);
            imageSelectPanel.updateSizeIfChange(minDeg, maxDeg, defDeg);
        }

        public String getDesc() {
            return _prop.getTitle("sdss");
        }
    }



//======================================================================
//------------------ WISE ----------------------------------------------
//======================================================================
    public class WiseType extends PlotTypeUI {
        private final SimpleInputField _wiseTypes = SimpleInputField.createByProp(_prop.makeBase("wise.types"));
        private final SimpleInputField _wiseBands = SimpleInputField.createByProp(_prop.makeBase("wise.bands"));

        WiseType() {
            super(true, true, false, false);
        }

        public void addTab(TabPane<Panel> tabs) {
            HorizontalPanel hp = new HorizontalPanel();
            hp.add(_wiseTypes);
            hp.add(_wiseBands);
            hp.setSpacing(10);
            tabs.addTab(hp, _prop.getTitle("wise"));
            GwtUtil.setStyle(_wiseBands, "paddingLeft", "10px");
        }

        public WebPlotRequest createRequest() {
            WorldPt pos = imageSelectPanel.getJ2000Pos();
            WebPlotRequest req = WebPlotRequest.makeWiseRequest(pos, _wiseTypes.getValue(), _wiseBands.getValue(),
                                                                imageSelectPanel.getStandardPanelDegreeValue());
            insertZoomType(req);
            return req;
        }

        public void updateSizeArea() {
            WebAppProperties webProp = Application.getInstance().getProperties();
            double minDeg = webProp.getDoubleProperty(_prop.makeBase("wise.size." + PropConst.MIN), 0);
            double maxDeg = webProp.getDoubleProperty(_prop.makeBase("wise.size." + PropConst.MAX), 0);
            double defDeg = webProp.getDoubleProperty(_prop.makeBase("wise.size." + PropConst.DEFAULT), 0);
            imageSelectPanel.updateSizeIfChange(minDeg, maxDeg, defDeg);
        }

        public String getDesc() {
            return _prop.getTitle("wise");
        }
    }


//======================================================================
//------------------ Blank Image ---------------------------------------
//======================================================================
    public class BlankType extends PlotTypeUI {
        private final SimpleInputField _size = SimpleInputField.createByProp(_prop.makeBase("blank.size"));

        BlankType() {
            super(true, true, false, false);
        }

        public void addTab(TabPane<Panel> tabs) {
            HorizontalPanel hp = new HorizontalPanel();
            hp.add(_size);
            hp.setSpacing(10);
            tabs.addTab(hp, _prop.getTitle("blank"));
            GwtUtil.setStyle(_size, "paddingLeft", "10px");
        }

        public WebPlotRequest createRequest() {
            WorldPt pos = imageSelectPanel.getJ2000Pos();
            int sizeV= Integer.parseInt(_size.getValue());
            float arcsecPerPix= (imageSelectPanel.getStandardPanelDegreeValue()*3600) / sizeV;
            WebPlotRequest req = WebPlotRequest.makeBlankPlotRequest(pos, arcsecPerPix , sizeV, sizeV);
            insertZoomType(req);
            return req;
        }

        public void updateSizeArea() {
            imageSelectPanel.updateSizeIfChange(.01, 30, .5);
        }

        public String getDesc() {
            return _prop.getTitle("blank");
        }
    }





    //======================================================================
//------------------ Fits Files ----------------------------------------
//======================================================================
    public class FileType extends PlotTypeUI {
        final private FormPanel _form = new FormPanel();
        private String _file = null;
        private FileUpload _upload = new FileUpload();
        private MaskPane _maskPane;
        private PlotWidgetOps _ops= null;
        private String cacheKey= "FitsUpload-"+System.currentTimeMillis();


        FileType() {
            super(false, false, true, false);
        }

        public void addTab(TabPane<Panel> tabs) {

            String url= FFToolEnv.isAPIMode() ?
                                      GWT.getModuleBaseURL() + "servlet/FireFly_FitsUpload?cacheKey="+cacheKey :
                                      GWT.getModuleBaseURL() + "servlet/FireFly_FitsUpload";
            _form.setAction(WebUtil.encodeUrl(url));
            _form.setEncoding(FormPanel.ENCODING_MULTIPART);
            _form.setMethod(FormPanel.METHOD_POST);
            _form.addStyleName("image-select-file-submit");
            VerticalPanel panel = new VerticalPanel();
            panel.setSpacing(10);
            _form.setWidget(panel);
            _upload.setName("uploadFormElement");
//            panel.add(new Hidden("ext", "fits"));
            panel.add(_upload);
            panel.setSpacing(5);

            _form.addSubmitHandler(new FormPanel.SubmitHandler() {
                public void onSubmit(FormPanel.SubmitEvent ev) {
//                    Window.alert("I submitted the file");

                    _maskPane = GwtUtil.mask("Uploading", imageSelectPanel.getMainPanel(), MaskPane.MaskHint.OnDialog);
                }
            });

            _form.addSubmitCompleteHandler(new FormPanel.SubmitCompleteHandler() {
                public void onSubmitComplete(FormPanel.SubmitCompleteEvent ev) {
//                    Window.alert("I got the results");
                    String results = ev.getResults();
                    if (results!=null) {
                        if (results.startsWith("<")) {
                            results = results.substring(results.indexOf('>') + 1);
                            _file = results.substring(0, results.indexOf('<'));
                        } else {
                            _file = results;
                        }
                    }
                    else {
                        _file = cacheKey;
                    }
                    _maskPane.hide();
                    imageSelectPanel.hide();
                    imageSelectPanel.plot(_ops, FileType.this);

                }
            });
            tabs.addTab(_form, _prop.getTitle("file"));
        }

        public WebPlotRequest createRequest() {
            WebAssert.tst(_file != null, "file should never be null at this point");
            WebPlotRequest request = WebPlotRequest.makeFilePlotRequest(_file);
            request.setTitleOptions(WebPlotRequest.TitleOptions.FILE_NAME);
            insertZoomType(request);
            _file = null;
            return request;
        }

        protected boolean validateInput() throws ValidationException {
            if (StringUtils.isEmpty(_upload.getFilename())) {
                throw new ValidationException(_prop.getError("file.noinput"));
            }
            return true;
        }

        public void updateSizeArea() {
        }

        public String getDesc() {
            return _prop.getTitle("file");
        }

        public void submit(PlotWidgetOps ops) {
            _ops= ops;
            _form.submit();
        }
    }

//======================================================================
//------------------ Fits Files On Server ----------------------------------------
//======================================================================

    public class FileTypeAlreadyOnServer extends PlotTypeUI {
        private final SimpleInputField _rFile = SimpleInputField.createByProp(_prop.makeBase("fileOnServer.filenameRed"), new SimpleInputField.Config("100px"));
        private final SimpleInputField _gFile = SimpleInputField.createByProp(_prop.makeBase("fileOnServer.filenameGreen"), new SimpleInputField.Config("100px"));
        private final SimpleInputField _bFile = SimpleInputField.createByProp(_prop.makeBase("fileOnServer.filenameBlue"), new SimpleInputField.Config("100px"));

        FileTypeAlreadyOnServer() {
            super(false, false, false, true);
        }

        public void addTab(TabPane<Panel> tabs) {


            VerticalPanel panel = new VerticalPanel();
            panel.setSpacing(10);
            panel.add(_rFile);
            panel.add(_gFile);
            panel.add(_bFile);
            panel.setSpacing(5);
            tabs.addTab(panel, _prop.getTitle("fileOnServer"));
        }

        public WebPlotRequest createRequest() {
            WebPlotRequest request = WebPlotRequest.makeFilePlotRequest(_rFile.getValue());
            insertZoomType(request);
            return request;
        }

        @Override
        public WebPlotRequest[] createThreeColorRequest() {
            return new WebPlotRequest[]{
                    StringUtils.isEmpty(_rFile.getValue()) ?
                    null : WebPlotRequest.makeFilePlotRequest(_rFile.getValue()),
                    StringUtils.isEmpty(_gFile.getValue()) ?
                    null : WebPlotRequest.makeFilePlotRequest(_gFile.getValue()),
                    StringUtils.isEmpty(_bFile.getValue()) ?
                    null : WebPlotRequest.makeFilePlotRequest(_bFile.getValue()),
            };
        }

        protected boolean validateInput() throws ValidationException {
            if (StringUtils.isEmpty(_rFile.getValue())) {
                throw new ValidationException(_prop.getError("fileOnServer.noinput"));
            }
            return true;
        }

        public void updateSizeArea() {
        }

        public String getDesc() {
            return _prop.getTitle("fileOnServer");
        }

        public int getHeight() {
            return 150;
        }

        public boolean isThreeColor() {
            return !(StringUtils.isEmpty(_gFile.getValue()) && StringUtils.isEmpty(_bFile.getValue()));
        }

    }

//======================================================================
//------------------ URL -----------------------------------------------
//======================================================================
    public class URLType extends PlotTypeUI {
        private final SimpleInputField _urlField = SimpleInputField.createByProp(_prop.makeBase("url.input"));

        URLType() {
            super(false, false, false, false);
        }

        public void addTab(TabPane<Panel> tabs) {
            VerticalPanel vp = new VerticalPanel();
            vp.add(_urlField);
            tabs.addTab(vp, _prop.getTitle("url"));

        }

        public WebPlotRequest createRequest() {
            WebPlotRequest req = WebPlotRequest.makeURLPlotRequest(_urlField.getValue());
            req.setTitleOptions(WebPlotRequest.TitleOptions.FILE_NAME);
            insertZoomType(req);
            return req;
        }

        public void updateSizeArea() {
        }

        public String getDesc() {
            return _prop.getTitle("url");
        }

        protected boolean validateInput() throws ValidationException {
            if (StringUtils.isEmpty(_urlField.getValue())) {
                throw new ValidationException(_prop.getError("url.noinput"));
            }
            return true;
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
