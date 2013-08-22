package edu.caltech.ipac.heritage.ui;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.data.SpecificPoints;
import edu.caltech.ipac.firefly.data.table.BaseTableData;
import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.table.AbstractTablePreview;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.ui.table.TablePreviewEventHub;
import edu.caltech.ipac.firefly.util.MinMax;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.visualize.graph.SpectrumMetaSource;
import edu.caltech.ipac.firefly.visualize.graph.XYPlotBasicWidget;
import edu.caltech.ipac.firefly.visualize.graph.XYPlotMeta;
import edu.caltech.ipac.heritage.data.entity.DataType;
import edu.caltech.ipac.heritage.searches.HeritageSearch;
import edu.caltech.ipac.util.StringUtils;

import java.util.Map;


/**
 * @author tatianag
 *         $Id: $
 */
public class SourceListSEDPreview extends AbstractTablePreview {

    public static SLBand[] SL_BANDS = {
        new SLBand("I1", "Spitzer/IRAC 3.6", 3.6, 3.179, 3.955, "i1_fluxtype", "i1_f_ap1", "i1_df_ap1"),
        new SLBand("I2", "Spitzer/IRAC 4.5", 4.5, 3.955, 5.015, "i2_fluxtype", "i2_f_ap1", "i2_df_ap1"),
        new SLBand("I3", "Spitzer/IRAC", 5.8, 5.015, 6.442, "i3_fluxtype", "i3_f_ap1", "i3_df_ap1"),
        new SLBand("I4", "Spitzer/IRAC", 8.0, 6.442, 9.343, "i4_fluxtype", "i4_f_ap1", "i4_df_ap1"),
        new SLBand("M1", "Spitzer/MIPS 24", 23.675, 20.335, 29.462, "m1_fluxtype", "m1_f_psf", "m1_df_psf"),
        new SLBand("J", "2MASS/J 1.25", 1.235, 1.105, 1.349, null, "j", "dj"),
        new SLBand("H", "2MASS/H 1.65", 1.662, 1.504, 1.709, null, "h", "dh"),
        new SLBand("K", "2MASS/J 2.17", 2.159, 1.989, 2.316, null, "k", "dk"),
        new SLBand("W1", "WISE 3.3", 3.35, 3.3394, 3.3658, null, "wise1", "dwise1"),
        new SLBand("W2", "WISE 4.7", 4.60, 4.586, 4.6196, null, "wise2", "dwise2"),
        new SLBand("W3", "WISE 12", 11.56, 11.5162, 11.6054, null, "wise3", "dwise3"),
        new SLBand("W4", "WISE 23", 22.08, 21.9699, 22.2067, null, "wise4", "dwise4")
    };

    private static String [] cols = {"wavelength", "flux_density"};
    private SimplePanel display;

    private XYPlotBasicWidget xyPlotWidget;

    public SourceListSEDPreview(String name) {
        super(name, "Display spectral energy distribution plot");

        XYPlotMeta meta = new XYPlotMeta("Source SED Preview", 300, 300, SpectrumMetaSource.getInstance());
        meta.setPlotDataPoints(XYPlotMeta.PlotStyle.POINTS);
        xyPlotWidget = new XYPlotBasicWidget(meta);
        display = new SimplePanel();
        display.setWidget(xyPlotWidget);
        display.setSize("100%", "100%");
    }


    @Override
    protected void updateDisplay(TablePanel table) {
        if (table == null || table.getTable() == null || !GwtUtil.isOnDisplay(getDisplay())) {
            return;
        }

        TableData.Row selRow = table.getTable().getHighlightedRow();
        SpecificPoints specificPoints = new SpecificPoints();
        specificPoints.setDescription("SED points with annotations");
        BaseTableData model = new BaseTableData(cols);

        double flux, error;
        Object fluxVal, errorVal;
        int id = 0;
        for (SLBand band : SL_BANDS) {
            int fluxType = 1;
            if (!StringUtils.isEmpty(band.fluxTypeCol)) {
                fluxType = Integer.parseInt(selRow.getValue(band.fluxTypeCol).toString());
            }
            // Use flux densities detected with SNR>=3 (*FluxType=1)
            if (fluxType == 1) {
                fluxVal = selRow.getValue(band.fluxCol);
                if (!StringUtils.isEmpty(fluxVal)) {
                    flux = Double.parseDouble(fluxVal.toString());
                    errorVal = selRow.getValue(band.errorCol);
                    if (!StringUtils.isEmpty(errorVal)) {
                        error = Double.parseDouble(errorVal.toString());
                    } else {
                        error = 0;
                    }
                    specificPoints.addPoint(id++, band.name,
                            band.desc+": "+fluxVal.toString()+" uJy<br>Uncertainty: "+(error==0 ? "unknown":errorVal.toString()+" uJy"),
                            new MinMax(band.wavelengthMin,band.wavelengthMax, band.wavelength),
                            new MinMax(flux-error, flux+error, flux));
                    model.addRow(new String[]{band.wavelength+"",fluxVal.toString()});
                }
            }
        }
        DataSet dataSet  = new DataSet(model);
        dataSet.getColumn(0).setUnits("microns");
        dataSet.getColumn(0).setType("double");
        dataSet.getColumn(1).setUnits("uJy");
        dataSet.getColumn(1).setType("double");

        if (specificPoints.getNumPoints()>0) {
            TableMeta meta = new TableMeta();
            meta.setAttribute(SpecificPoints.SERIALIZATION_KEY, specificPoints.toString());
            dataSet.setMeta(meta);
        }

        xyPlotWidget.makeNewChart(dataSet, selRow.getValue("objid").toString());
    }


    @Override
    public void bind(TablePreviewEventHub hub) {
        super.bind(hub);
        WebEventListener wel =  new WebEventListener(){
            public void eventNotify(WebEvent ev) {
                final TablePanel table = (TablePanel) ev.getSource();
                if (table != null ) {
                    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                        public void execute() {
                            if (GwtUtil.isVisible(table.getElement())) {
                                if (updateTabVisible(table)) {
                                    updateDisplay(table);
                                }
                            }
                        }
                    });
                }
            }
        };
        hub.getEventManager().addListener(TablePreviewEventHub.ON_ROWHIGHLIGHT_CHANGE, wel);
        hub.getEventManager().addListener(TablePreviewEventHub.ON_TABLE_SHOW, wel);

    }

    @Override
    public Widget getDisplay() {
        return display;
    }

    @Override
    public boolean isInitiallyVisible() { return false; }

    private boolean updateTabVisible(TablePanel table) {

        boolean show= false;
        if (table!=null && table.getDataModel().getTotalRows() > 0) {
            Map<String,String> meta = table.getDataset().getMeta().getAttributes();
            DataType dType= DataType.parse(meta.get(HeritageSearch.DATA_TYPE));
            if (dType==DataType.SOURCE_LIST) {
                show = true;
            }
        }
        getEventHub().setPreviewEnabled(this,show);
        return show;
    }

    private static class SLBand {
        final String name;
        final String desc;
        final double wavelength;
        final double wavelengthMin;
        final double wavelengthMax;
        final String fluxTypeCol;
        final String fluxCol;
        final String errorCol;

        public SLBand(String name, String desc, double wavelength, double wavelengthMin, double wavelengthMax, String fluxTypeCol, String fluxCol, String errorCol) {
            this.name = name;
            this.desc = desc;
            this.wavelength = wavelength;
            this.wavelengthMin = wavelengthMin;
            this.wavelengthMax = wavelengthMax;
            this.fluxTypeCol = fluxTypeCol;
            this.fluxCol = fluxCol;
            this.errorCol = errorCol;
        }
    }
}
