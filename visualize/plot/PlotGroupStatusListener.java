package edu.caltech.ipac.visualize.plot;

import java.util.EventListener;

/**
 * A listener that is called when a PlotGroup status has a changed
 * @author Trey Roby
 */
public interface PlotGroupStatusListener extends EventListener {

    /**
     * Called when a plot is added to this PlotGroup
     *
     * @param ev the event
     */
    public abstract void plotAdded(PlotGroupStatusEvent ev);

    /**
     * Called when a plot is added to this PlotGroup
     *
     * @param ev the event
     */
    public abstract void plotRemoved(PlotGroupStatusEvent ev);

    /**
     * Called when a plot is added to this PlotGroup
     *
     * @param ev the event
     */
    public abstract void colorBandAdded(PlotGroupStatusEvent ev);

    /**
     * Called when a plot is added to this PlotGroup
     *
     * @param ev the event
     */
    public abstract void colorBandRemoved(PlotGroupStatusEvent ev);

    /**
     * Called when a color band in this PlotGroup's plot is shown
     *
     * @param ev the event
     */
    public abstract void colorBandShowing(PlotGroupStatusEvent ev);

    /**
     * Called when a color band in this PlotGroup's plot is hidden
     *
     * @param ev the event
     */
    public abstract void colorBandHidden(PlotGroupStatusEvent ev);


    /**
     * Called when a plot in this PlotGroup is shown
     *
     * @param ev the event
     */
    public abstract void plotShowing(PlotGroupStatusEvent ev);

    /**
     * Called when a plot in this PlotGroup is hidden
     *
     * @param ev the event
     */
    public abstract void plotHidden(PlotGroupStatusEvent ev);

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
