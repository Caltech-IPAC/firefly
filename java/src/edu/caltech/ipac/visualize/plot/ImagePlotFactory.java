package edu.caltech.ipac.visualize.plot;

import edu.caltech.ipac.client.ClientLog;
import edu.caltech.ipac.gui.DialogSupport;
import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.visualize.controls.PlotTypeDialog;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.util.ArrayDataInput;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * Date: Nov 16, 2004
 * @author Trey Roby
 */
public class ImagePlotFactory {
    public static final int NEW_FRAME= 0;
    public static final int ADD_TO_FRAME= 1;
    public static final int OVERLAY=  2;
    public static final int NO_INPUT= 3;


//============================================================================
//---------------------- Public Static Factory Methods -----------------------
//============================================================================

    public static ImageAction[] makeImagePlot(final JFrame      f,
                                              final InputStream stream,
                                              final boolean     compressed,
                                              final PlotGroup   plotGroup,
                                              final ThreeColorParams threeColorParams,
                                              final boolean     overlayReproject,
                                              final float       initialZoomLevel)
                                                   throws FitsException,
                                                          GeomException {
        ImageAction retval[]= makeImagePlot(f,
                                            new Fits(stream,compressed),
                                            plotGroup, threeColorParams,
                                            overlayReproject, initialZoomLevel);
        FileUtil.silentClose(stream);
        return retval;
    }


    public static ImageAction[] makeImagePlot(JFrame    f,
                                              String    filename,
                                              PlotGroup plotGroup,
                                              ThreeColorParams threeColorParams,
                                              boolean   overlayReproject,
                                              float     initialZoomLevel)
                                               throws FitsException,
                                                      GeomException {
        ImageAction[] retval = makeImagePlot(f, new Fits(filename), plotGroup,
                                             threeColorParams, overlayReproject,
                                             initialZoomLevel);
        for(ImageAction ia : retval) {
            ia.getPlot().setRefFitsFile(new File(filename));
        }
        return retval;
    }

    public static ImageAction[] makeImagePlot(JFrame    f,
                                              Fits      fits,
                                              PlotGroup plotGroup,
                                              ThreeColorParams threeColorParams,
                                              boolean   overlayReproject,
                                              float     initialZoomLevel)
                                          throws FitsException,
                                                 GeomException {
        FitsException fitsE;
        ImageAction plotAry[];
        ImagePlot basePlot = null;
        if (plotGroup!=null) {
            basePlot = (ImagePlot) plotGroup.getBasePlot();
        }
        try {
            if(basePlot!=null && basePlot.getImageData()!=null) {
                if(!plotGroup.isOverlayEnabled()) {
                    throw new FitsException(
                        "You may not overlay this image " +
                        "over an image that does "        +
                        "not contain projection information");

                }
                plotAry= makePlotAry(f, fits,basePlot,plotGroup,
                                     threeColorParams,
                                     overlayReproject,
                                     initialZoomLevel);
            }
            else {
                plotAry= makePlotAry(f, fits,null,plotGroup,
                                     threeColorParams,
                                     overlayReproject,
                                     initialZoomLevel);
            }

        } catch (OutOfMemoryError e) {
            fitsE = new FitsException("Out Of Memory");
            fitsE.initCause(e);
            throw fitsE;
        } catch (IOException ioe) {
            fitsE = new FitsException("Could not reproject");
            fitsE.initCause(ioe);
            throw fitsE;
        } catch (FitsException fe) {
            throw fe;
        } catch (GeomException geomE) {
            throw geomE;
        } catch (Exception anyE) {
            anyE.printStackTrace();
            fitsE = new FitsException("Could not read file or stream");
            fitsE.initCause(anyE);
            ClientLog.warning("Got an unexpected exception ",
                              anyE.toString());
            fitsE.printStackTrace();
            throw fitsE;
        }
        try {
            ArrayDataInput is= fits.getStream();
            if (is!=null) is.close();
        } catch (IOException ignore) {/*ignore*/ }
        return plotAry;
    }


    public static ImageAction [] makePlotAry(JFrame           f,
                                             Fits             fits,
                                             ImagePlot        baseOverlayPlot,
                                             PlotGroup        plotGroup,
                                             ThreeColorParams threeColorParams,
                                             boolean          overlayReproject,
                                             float            initialZoomLevel)
                   throws FitsException,
                          IOException,
                          GeomException {
        List<ImageAction> plotList= new LinkedList<ImageAction>();
        FitsRead[] fitsReadArray;

        DialogSupport.setWaitCursor(true);

        try {
            long startTime= new Date().getTime();
            fitsReadArray =  FitsRead.createFitsReadArray(fits);
            Runtime rt= Runtime.getRuntime();
            float mem= (rt.totalMemory() - rt.freeMemory()) / 1048576.0F;
            ClientLog.brief( String.format(
                                          "File read: %.2f sec, mem: %.2f Megs",
                                          (new Date().getTime()- startTime)/1000.0, mem ));
        } finally {
            DialogSupport.setWaitCursor(false);
        }



        List<FitsReadParams> paramsList= prepareParamsList(f, fitsReadArray,
                                                           baseOverlayPlot,
                                                           plotGroup,
                                                           threeColorParams,
                                                           overlayReproject);

        FitsReadParams lastParams= null;
        for(FitsReadParams params: paramsList) {
            if (lastParams!=null) {
                params._baseOverlayPlot= lastParams._baseOverlayPlot;
                params._plotGroup= lastParams._plotGroup;
                params._threeColorRefPlot= lastParams._threeColorRefPlot;
            }
            makePlot(params,plotList,initialZoomLevel);
            lastParams= params;
        }
        return plotList.toArray(new ImageAction[plotList.size()]);
    }


    public static List<FitsReadParams>
                       prepareParamsList(JFrame           f,
                                         FitsRead[] fitsReadArray,
                                         ImagePlot        baseOverlayPlot,
                                         PlotGroup        plotGroup,
                                         ThreeColorParams threeColorParams,
                                         boolean          overlayReproject) {

        FitsReadParams params;
        String    actionType[];
        List<FitsReadParams> paramsList=
                        new ArrayList<FitsReadParams>(fitsReadArray.length);

        if (fitsReadArray.length==1) {
            params= new FitsReadParams();
            paramsList.add(params);
            params._fitsRead= fitsReadArray[0];
            params._baseOverlayPlot= baseOverlayPlot;
            params._plotGroup= plotGroup;
            params._overlayReproject= overlayReproject;
            params._returnAction= NO_INPUT;
            if (threeColorParams!=null) {
                params._threeColor= threeColorParams.isThreeColor();
                params._threeColorRefPlot= threeColorParams.getRefPlot();
                params._band= threeColorParams.getColorBand();
            }
        }
        else {
            boolean threeColor= (threeColorParams!=null) ?
                                threeColorParams.isThreeColor() : false;
            actionType= PlotTypeDialog.selectPlotType(f,fitsReadArray,
                                                      overlayReproject ,
                                                      threeColor);
            boolean isCube= false;
            int extCnt=fitsReadArray[fitsReadArray.length-1].getExtensionNumber();
            for(int i=0; i<fitsReadArray.length && !isCube; i++) {
                isCube=fitsReadArray[i].getPlaneNumber()>1;
            }



            for(int i=0; i<fitsReadArray.length; i++) {
                if(!actionType[i].equals(PlotTypeDialog.NOT_USED)) {
                    params= new FitsReadParams();
                    paramsList.add(params);
                    params._fitsRead= fitsReadArray[i];
                    params._baseOverlayPlot= baseOverlayPlot;
                    params._plotGroup= plotGroup;
                    params._overlayReproject= overlayReproject;
                    params._returnAction= determineAction(actionType[i]);
                    params._extIdx= fitsReadArray[i].getExtensionNumber();
                    params._cubeIdx= fitsReadArray[i].getPlaneNumber();
                    params._isCube= isCube;
                    params._totalExt= extCnt;

                    if (threeColorParams!=null) {
                        params._threeColorRefPlot= threeColorParams.getRefPlot();
                    }

                    if(actionType[i].equals(PlotTypeDialog.RED)) {
                        params._band=ImagePlot.RED;
                        params._threeColor=true;
                    }
                    else if(actionType[i].equals(PlotTypeDialog.GREEN)) {
                        params._band=ImagePlot.GREEN;
                        params._threeColor=true;
                    }
                    else if(actionType[i].equals(PlotTypeDialog.BLUE)) {
                        params._band=ImagePlot.BLUE;
                        params._threeColor=true;
                    }
                    else if(actionType[i].equals(PlotTypeDialog.OVERLAY)) {
                        params._threeColor=false;
                        params._overlayReproject= true;
                    }
                    else {
                        params._threeColor=false;
                    }
                }
            }
        }


        if (paramsList.size()>0) {
            params=paramsList.get(0);
            if (params._baseOverlayPlot==null && params._overlayReproject &&
                !params._threeColor) {
                //find a base plot and put it at position 0
                FitsReadParams foundParam= null;
                int            foundIdx= 0;
                for(int i=1; (i<paramsList.size() && foundParam==null); i++) {
                    params= paramsList.get(i);
                    if (!params._overlayReproject) {
                        foundParam= params;
                        foundIdx= i;
                    }
                }
                Assert.tst(foundParam);
                if (foundParam!=null) {
                    paramsList.remove(foundIdx);
                    paramsList.add(0,foundParam);
                }
            }
        }

        return paramsList;
    }

    public static void makePlot(FitsReadParams params,
                                List<ImageAction>  plotList,
                                float          initialZoomLevel)
                                                       throws FitsException,
                                                              IOException,
                                                              GeomException {
        ImagePlot plot= null;

        long startTime= new Date().getTime();
        if (params._threeColor && params._threeColorRefPlot!=null) { // three color additional plots
            params._threeColorRefPlot.addThreeColorBand(params._fitsRead,
                                                        params._band);
        }
        else if (params._baseOverlayPlot!=null &&
                 (params._threeColor || params._overlayReproject)) {
            if (params._threeColor) { // first three color, first plot overlay
                params._fitsRead= FitsRead.createFitsReadWithGeom(
                               params._fitsRead,
                               params._baseOverlayPlot.getFitsRead(), false);
                plot= new ImagePlot(params._plotGroup,
                                    params._fitsRead,
                                    initialZoomLevel, true);
                plotList.add(makeImageAction(plot, OVERLAY, params));
                params._threeColorRefPlot= plot;
            }
            else { // standard overlay plot
                params._fitsRead= FitsRead.createFitsReadWithGeom(
                                         params._fitsRead,
                                         params._baseOverlayPlot.getFitsRead(),
                                         true);
                plot= new ImagePlot(params._plotGroup,
                                    params._fitsRead,
                                    initialZoomLevel);
                plotList.add(makeImageAction(plot, OVERLAY,params));
            }
        }
        else { // normal plot or normal plot that is first three color
            plot= new ImagePlot(null, params._fitsRead,
                                initialZoomLevel, params._threeColor);
            if (params._threeColor) {
                plot.setThreeColorBand(params._fitsRead,  params._band);
                params._threeColorRefPlot= plot;
            }
            plotList.add(makeImageAction(plot, params._returnAction, params));
        }
        if (plot!=null && params._baseOverlayPlot==null) {
            params._baseOverlayPlot= plot;
            params._plotGroup= plot.getPlotGroup();
        }

        Runtime rt= Runtime.getRuntime();
        float mem= (rt.totalMemory() - rt.freeMemory()) / 1048576.0F;
        ClientLog.brief( String.format(
                            "Image creation: %.2f sec, mem: %.2f Megs",
                            (new Date().getTime()- startTime)/1000.0, mem ));
    }

    private static ImageAction makeImageAction(ImagePlot      plot,
                                               int            action,
                                               FitsReadParams params ) {
        return new ImageAction(plot,action, params._isCube, params._cubeIdx,
                               params._extIdx, params._totalExt);
    }


    private static int determineAction(final String inAction) {
        int retval;

        if (inAction==null) {
            retval= NO_INPUT;
        }
        else if (inAction.equals(PlotTypeDialog.SEP_FRAME)) {
            retval= NEW_FRAME;
        }
        else if (inAction.equals(PlotTypeDialog.SAME_FRAME)) {
            retval= ADD_TO_FRAME;
        }
        else if (inAction.equals(PlotTypeDialog.OVERLAY)) {
            retval= OVERLAY;
        }
        else {
            retval= NO_INPUT;
        }
        return retval;
    }


    public static class ThreeColorParams {

        private boolean     _threeColor;
        private ImagePlot   _threeColorRefPlot;
        private int         _band;

        public ThreeColorParams(final boolean   threeColor,
                                ImagePlot threeColorRefPlot,
                                final int       band) {
            _threeColor= threeColor;
            _threeColorRefPlot= threeColorRefPlot;
            _band= band;
        }

        public boolean isThreeColor() { return _threeColor; }
        public ImagePlot getRefPlot() { return _threeColorRefPlot; }
        public int getColorBand() { return _band; }

    }

    public static class ImageAction {
        private ImagePlot _plot;
        private int       _action;
        final boolean     _isCube;
        final int         _cubeIdx;
        final int         _extIdx;
        final int         _totalExt;
        public ImageAction(final ImagePlot plot,
                           final int action,
                           final boolean isCube,
                           final int     cubeIdx,
                           final int     extIdx,
                           final int     totalExt) {
            Assert.argTst(action==NEW_FRAME ||
                          action==ADD_TO_FRAME ||
                          action==OVERLAY ||
                          action==NO_INPUT,
                          "action must be NEW_FRAME, "+
                          "ADD_TO_FRAME or OVERLAY" );
            _plot= plot;
            _action= action;
            _isCube= isCube;
            _cubeIdx= cubeIdx;
            _extIdx= extIdx;
            _totalExt= totalExt;
        }
        public ImagePlot getPlot() { return _plot; }
        public int       getAction() { return _action; }
        public boolean   isCube() { return _isCube; }
        public int       getCubeIdx() { return _cubeIdx; }
        public int       getExtIdx() { return _extIdx; }
        public int       getTotalExt() { return _totalExt; }
    }




    private static class FitsReadParams {
        FitsRead         _fitsRead;
        ImagePlot        _baseOverlayPlot;
        PlotGroup        _plotGroup= null;
        boolean          _overlayReproject;
        boolean          _isCube;
        int              _cubeIdx=-1;
        int              _extIdx=0;
        int              _totalExt=0;
        private boolean     _threeColor= false;
        private ImagePlot   _threeColorRefPlot= null;
        private int         _band= ImagePlot.RED;
        private int         _returnAction=NO_INPUT;
    }
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
