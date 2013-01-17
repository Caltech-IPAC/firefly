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

