package edu.caltech.ipac.firefly.visualize.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.TextResource;
import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.ui.BaseDialog;
import edu.caltech.ipac.firefly.ui.ButtonType;
import edu.caltech.ipac.firefly.util.PropFile;
import edu.caltech.ipac.firefly.util.WebClassProperties;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.PlotWidgetFactory;
import edu.caltech.ipac.firefly.visualize.PlotWidgetOps;
import edu.caltech.ipac.firefly.visualize.Vis;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.util.dd.ValidationException;


/**
 * User: roby
 * Date: Jan 26, 2009
 * Time: 3:45:42 PM
 */


/**
 * @author Trey Roby
 */
public class ImageSelectDialog extends BaseDialog {

    interface PFile extends PropFile { @Source("ImageSelectDialog.prop") TextResource get(); }


    private static final WebClassProperties _prop= new WebClassProperties(ImageSelectDialog.class, (PFile)GWT.create(PFile.class));
    private final ImageSelectPanel imSelPanel;

//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================


    public ImageSelectDialog(PlotWidgetOps ops,
                             String title,
                             boolean addToHistory,
                             AsyncCallback<WebPlot> plotCallback,
                             PlotWidgetFactory plotFactory) {
        super(null, ButtonType.OK_CANCEL_HELP,
              title == null ? _prop.getTitle() : title, "visualization.fitsViewer");
        imSelPanel= new ImageSelectPanel(ops,addToHistory,plotCallback,new DialogComplete(),plotFactory);
        setWidget(imSelPanel.getMainPanel());
    }

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    @Override
    public void setVisible(boolean v) {
        super.setVisible(v);
        imSelPanel.showPanel();
    }







    @Override
    protected void inputComplete() {
        imSelPanel.inputComplete();
    }


    @Override
    protected void inputCanceled() {
        imSelPanel.inputCanceled();
    }

    @Override
    protected boolean validateInput() throws ValidationException {
        return imSelPanel.validateInput();
    }






// =====================================================================
// -------------------- Inner Classes ----------------------------------
// =====================================================================


    public class DialogComplete implements ImageSelectPanel.PanelComplete {
        public void performInputComplete() {
            ImageSelectDialog.this.performInputComplete(null);
        }

        public HideType getHideAlgorythm() {
            return ImageSelectDialog.this.getHideAlgorythm();
        }

        public void setHideAlgorythm(HideType hideType) {
            ImageSelectDialog.this.setHideAlgorythm(hideType);
        }

        public void hide() {
            ImageSelectDialog.this.setVisible(false);
        }
    }


    public static class AsyncCreator {
        private ImageSelectDialog _dialog= null;
        private final MiniPlotWidget _mpw;
        private final String _title;
        private final boolean _addToHistory;
        private final AsyncCallback<WebPlot> _plotCallback;
        private final PlotWidgetFactory _widgetFactory;

        public AsyncCreator(MiniPlotWidget mpw,
                            String title,
                            boolean addToHistory,
                            AsyncCallback<WebPlot> plotCallback,
                            PlotWidgetFactory widgetFactory) {
            _mpw= mpw;
            _title= title;
            _addToHistory= addToHistory;
            _plotCallback= plotCallback;
            _widgetFactory= widgetFactory;
        }


        public void show() {
            Vis.init(new Vis.InitComplete() {
                public void done() {
                    PlotWidgetOps ops= (_mpw!=null) ? _mpw.getOps() : null;
                    if (_dialog==null) {
                        _dialog= new ImageSelectDialog(ops,_title, _addToHistory,
                                                       _plotCallback,_widgetFactory);
                    }
                    _dialog.setVisible(true);
                }
            });
        }

    }

}

