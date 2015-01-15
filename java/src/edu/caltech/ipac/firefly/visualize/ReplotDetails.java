/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize;


/**
 * The event that is passed when a WebPlot status has a changed
 * @author Trey Roby
 */
public class ReplotDetails {


    public enum Reason {ZOOM,ZOOM_COMPLETED, STRETCH, BAND_ADDED, BAND_REMOVED, BAND_SHOWING,
                        BAND_HIDDEN, PLOT_ADDED, PLOT_REMOVED, IMAGE_RELOADED,
                        COLOR_CHANGE, REPARENT, UNKNOWN }

    private final WebPlot _plot;
    private final WebPlotGroup _plotGroup;
    private final Band _colorBand;
    private final Reason _reason;


    /**
     * Create a new ReplotDetails class
     * @param plotGroup source of the event.
     * @param plot the plot added or removed
     * @param reason the Reason enum
     */
    public ReplotDetails(WebPlotGroup plotGroup, WebPlot plot, Reason reason) {
        this(plotGroup, plot, reason, Band.NO_BAND);
    }
    /**
     * Create a new ReplotDetails class
     * @param plotGroup source of the event.
     * @param plot the plot added or removed
     * @param reason the Reason enum
     * @param band which color band
     */
    public ReplotDetails(WebPlotGroup plotGroup,
                         WebPlot plot,
                         Reason reason,
                         Band band) {
        _plotGroup= plotGroup;
        _plot= plot;
        _reason= reason;
        _colorBand= band;
    }

    public WebPlot getPlot() {return _plot;}
    public WebPlotGroup getPlotGroup() {return _plotGroup;}
    public Band getColorBand() {return _colorBand;}
    public Reason getReplotReason()  {return _reason; }

}
