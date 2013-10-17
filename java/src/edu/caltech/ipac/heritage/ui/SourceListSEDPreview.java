package edu.caltech.ipac.heritage.ui;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.ui.ProvidesResize;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.data.SpecificPoints;
import edu.caltech.ipac.firefly.data.table.BaseTableData;
import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.table.AbstractTablePreview;
import edu.caltech.ipac.firefly.ui.table.EventHub;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
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
public class SourceListSEDPreview extends AbstractTablePreview implements ProvidesResize, RequiresResize {


    // Filter bandpasses:
    // 2MASS: http://www.ipac.caltech.edu/2mass/releases/allsky/doc/sec6_4a.html
    // IRAC: http://irsa.ipac.caltech.edu/data/SPITZER/docs/irac/iracinstrumenthandbook/18/#_Toc296497389
    // WISE: http://wise2.ipac.caltech.edu/docs/release/allsky/expsup/sec1_1.html
    // MIPS: http://irsa.ipac.caltech.edu/data/SPITZER/docs/mips/mipsinstrumenthandbook/6/#_Toc288032270
    public static SLBand[] SL_BANDS = {
        new SLBand("J", "2MASS/J 1.25", 1.235, 1.235-0.162/2, 1.235+0.162/2, null, "j", "dj"),
        new SLBand("H", "2MASS/H 1.65", 1.662, 1.662-0.251/2, 1.662+0.251/2, null, "h", "dh"),
        new SLBand("K", "2MASS/K 2.17", 2.159, 2.159-0.262/2, 2.159+0.262/2, null, "k", "dk"),
        new SLBand("I1", "Spitzer/IRAC 3.6", 3.550, 3.18, 3.92, "i1_fluxtype", "i1_f_ap1", "i1_df_ap1"),
        new SLBand("I2", "Spitzer/IRAC 4.5", 4.493, 4.00, 5.02, "i2_fluxtype", "i2_f_ap1", "i2_df_ap1"),
        new SLBand("I3", "Spitzer/IRAC 5.8", 5.731, 5.02, 6.43, "i3_fluxtype", "i3_f_ap1", "i3_df_ap1"),
        new SLBand("I4", "Spitzer/IRAC 8.0", 7.872, 6.45, 9.33, "i4_fluxtype", "i4_f_ap1", "i4_df_ap1"),
        new SLBand("W1", "WISE 3.3", 3.35, 3.35-0.66/2, 3.35+0.66/2, null, "wise1", "dwise1"),
        new SLBand("W2", "WISE 4.7", 4.60, 4.60-1.04/2, 4.60+1.04/2, null, "wise2", "dwise2"),
        new SLBand("W3", "WISE 12", 11.56, 11.56-5.51/2, 11.56+5.51/2, null, "wise3", "dwise3"),
        new SLBand("W4", "WISE 23", 22.09, 22.09-4.10/2, 22.09+4.10/2, null, "wise4", "dwise4"),
        new SLBand("M1", "Spitzer/MIPS 24", 23.68, 20.8, 26.1, "m1_fluxtype", "m1_f_psf", "m1_df_psf")
    };

    private static String [] cols = {"wavelength", "flux_density"};

    private XYPlotBasicWidget xyPlotWidget;

    public SourceListSEDPreview(String name) {
        super(name, "Display spectral energy distribution plot");

        //XYPlotMeta meta = new XYPlotMeta("Flux Density Distribution", 300, 300, SpectrumMetaSource.getInstance());
        XYPlotMeta meta = new XYPlotMeta("none", 300, 300, SpectrumMetaSource.getInstance());
        meta.setXScale(XYPlotMeta.LOG_SCALE);
        meta.setYScale(XYPlotMeta.LOG_SCALE);
        meta.setPlotStyle(XYPlotMeta.PlotStyle.POINTS);
        XYPlotMeta.UserMeta userMeta = new XYPlotMeta.UserMeta();
        userMeta.setYName("flux density");
        meta.setUserMeta(userMeta);
        xyPlotWidget = new XYPlotBasicWidget(meta);
        xyPlotWidget.setSize("100%", "100%");
        setDisplay(xyPlotWidget);
    }


    @Override
    protected void updateDisplay(TablePanel table) {
        if (table == null || table.getTable() == null || !GwtUtil.isOnDisplay(getDisplay())) {
            return;
        }

        TableData.Row selRow = table.getTable().getHighlightedRow();
        SpecificPoints specificPoints = new SpecificPoints();
        specificPoints.setDescription("SED points with labels");
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
                            band.desc+": "+fluxVal.toString()+" &mu;Jy<br>Uncertainty: "+(error==0 ? "unknown":errorVal.toString()+" &mu;Jy"),
                            new MinMax(band.wavelengthMin,band.wavelengthMax, band.wavelength),
                            new MinMax(flux-error, flux+error, flux));
                    model.addRow(new String[]{band.wavelength+"",fluxVal.toString()});
                }
            }
        }
        DataSet dataSet  = new DataSet(model);
        dataSet.getColumn(0).setUnits("&mu;m");
        dataSet.getColumn(0).setType("double");
        dataSet.getColumn(1).setUnits("&mu;Jy");
        dataSet.getColumn(1).setType("double");

        if (specificPoints.getNumPoints()>0) {
            TableMeta meta = new TableMeta();
            meta.setAttribute(SpecificPoints.SERIALIZATION_KEY, specificPoints.toString());
            dataSet.setMeta(meta);
        }

        xyPlotWidget.makeNewChart(dataSet, selRow.getValue("objid").toString());
    }


    @Override
    public void bind(EventHub hub) {
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
        hub.getEventManager().addListener(EventHub.ON_ROWHIGHLIGHT_CHANGE, wel);
        hub.getEventManager().addListener(EventHub.ON_TABLE_SHOW, wel);

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
