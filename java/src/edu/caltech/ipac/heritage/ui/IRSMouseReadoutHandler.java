package edu.caltech.ipac.heritage.ui;

import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.util.WebAssert;
import edu.caltech.ipac.firefly.visualize.Readout;
import edu.caltech.ipac.firefly.visualize.ScreenPt;
import edu.caltech.ipac.firefly.visualize.VisUtil;
import edu.caltech.ipac.firefly.visualize.WebDefaultMouseReadoutHandler;
import edu.caltech.ipac.firefly.visualize.WebMouseReadoutHandler;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.heritage.data.entity.IRSInfoData;
import edu.caltech.ipac.heritage.rpc.SearchServices;
import edu.caltech.ipac.heritage.rpc.SearchServicesAsync;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.ImagePt;
import edu.caltech.ipac.visualize.plot.ImageWorkSpacePt;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
/**
 * User: roby
 * Date: Jun 30, 2010
 * Time: 9:42:04 AM
 */


/**
 * @author Trey Roby
 */
public class IRSMouseReadoutHandler implements WebMouseReadoutHandler {

    private static final String UM=  " &#x3bc;m";
    public static final int J2000_HMS= 1;
    public static final int J2000_DEC= 2;
    public static final int IMAGE_PX = 3;
    public static final int GAL      = 4;
    public static final int B1950_HMS= 5;
    //public static final int OFFSET   = 6;
    public static final int WL       = 6;
    public static final int PIXVAL   = 7;
    //public static final int XY       = 8;
    //public static final int PIX_SIZE= 7;

    //private static final String OFFSET_STR= "Offset";
    private static final String WL_STR= "Wave Length";
    //private static final String XY_STR= "XY";
    private static final String PIXVAL_STR= "Pixel Value";
    public static final int ROW_CNT= 8;
    private static final int TITLE_ROW= 0;
    private static final int MOUSE_DELAY_MS= 200;

    private List<DataCache> _savedPts= new ArrayList<DataCache>(10);
    private DataTimer _dataTimer= new DataTimer();
    private static final int MAX_DATA_PTS= 200;
    private static NumberFormat _nf   = NumberFormat.getFormat("#.###");
    private static NumberFormat _wave_nf   = NumberFormat.getFormat("#.##");

//=======================================================================
//-------------- Method from WebMouseReadoutHandler Interface -----------
//=======================================================================

    public int getRows(WebPlot plot) { return ROW_CNT; }

    public int getColumns(WebPlot plot) { return 1; }



    public void computeMouseExitValue(WebPlot plot,
                                      Readout readout,
                                      int row,
                                      int column) {
        readout.setValue(row,column,"", "");
    }

    public void computeMouseValue(WebPlot plot,
                                  Readout readout,
                                  int row,
                                  int column,
                                  ImagePt ipt,
                                  ScreenPt screenPt,
                                  long callID) {
//        readout.setValue(row,column,"row: "+row, "value: "+ row);

        if (row== TITLE_ROW) { /// set all the rows from here
            doReadout(plot,readout,ipt,screenPt);
        }
    }


    public List<Integer> getRowsWithOptions() { return null; }
    public List<String> getRowOptions(int row) { return null; }
    public void setRowOption(int row, String op) {}
    public String getRowOption(int row) { return ""; }

//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    private void doReadout(WebPlot plot,
                            Readout readout,
                            ImagePt ipt,
                            ScreenPt screenPt) {
//        readout.setTitle("test");
        _dataTimer.cancel();
        WebDefaultMouseReadoutHandler.Result r;
        r= WebDefaultMouseReadoutHandler.getReadoutByImagePt(plot,ipt,screenPt,
                                                             WebDefaultMouseReadoutHandler.WhichDir.BOTH,
                                                             WebDefaultMouseReadoutHandler.ReadoutMode.DECIMAL,
                                                             CoordinateSys.PIXEL);
        readout.setValue(IMAGE_PX,0,r._label,r._value);
        r= WebDefaultMouseReadoutHandler.getPixelSize(plot);
        //readout.setValue(PIX_SIZE,0,r._label,r._value);
        ImageWorkSpacePt iwspt = plot.getImageWorkSpaceCoords(ipt);
        DataCache dc= getSavedData(plot,iwspt);
        if (dc!=null) {
            updateIRSData(readout, dc._data);
        }
        else {
            clearIRSDataTmp(readout);
            _dataTimer.setupCall(iwspt, plot,readout);
            _dataTimer.schedule(MOUSE_DELAY_MS);
        }

    }


    private void clearIRSDataTmp(Readout readout) {
        readout.setValue(J2000_HMS,0,CoordinateSys.EQ_J2000.getShortDesc(), " ");
        readout.setValue(J2000_DEC,0,CoordinateSys.EQ_J2000.getShortDesc(), " ");
        readout.setValue(GAL,0,      CoordinateSys.GALACTIC.getShortDesc(), " ");
        readout.setValue(B1950_HMS,0,CoordinateSys.EQ_B1950.getShortDesc(), " ");
        //readout.setValue(OFFSET   ,0,OFFSET_STR, " ");
        readout.setValue(WL,0,       WL_STR, " ");
        // readout.setValue(XY,0,       XY_STR, " ");
        readout.setValue(PIXVAL,0,       PIXVAL_STR, " ");


    }


    private void updateIRSData(Readout readout, IRSInfoData data) {
        WebDefaultMouseReadoutHandler.Result r;
        WorldPt j2wp= new WorldPt(data._ra, data._dec);

        if (data!=null && (!Double.isNaN(data._ra) || !Double.isNaN(data._dec))) {
            r= WebDefaultMouseReadoutHandler.getHmsBoth(j2wp.getLon(),j2wp.getLat(),j2wp.getCoordSys());
            readout.setValue(J2000_HMS,0,r._label,r._value);
            readout.setValue(J2000_DEC,0,j2wp.getCoordSys().getShortDesc(),
                             _nf.format(j2wp.getLon())+", "+ _nf.format(j2wp.getLat()));

            WorldPt gal= VisUtil.convert(j2wp, CoordinateSys.GALACTIC);
            readout.setValue(GAL,0,gal.getCoordSys().getShortDesc(),
                             _nf.format(gal.getLon())+", "+ _nf.format(gal.getLat()));

            WorldPt b19= VisUtil.convert(j2wp, CoordinateSys.EQ_B1950);
            r= WebDefaultMouseReadoutHandler.getHmsBoth(b19.getLon(),b19.getLat(),b19.getCoordSys());
            readout.setValue(B1950_HMS,0,r._label,r._value);

            //readout.setValue(OFFSET,0, OFFSET_STR, data._offset+"");
            readout.setValue(PIXVAL,0,PIXVAL_STR, _nf.format(data._pixelVal));
            if (!Double.isNaN(data._wavelength)) {
                readout.setValue(WL,0,WL_STR, _wave_nf.format(data._wavelength)+UM,true);
            }
            else {
                readout.setValue(WL,0,WL_STR, "",true);
            }
            //readout.setValue(XY,0,XY_STR, data._x+", "+data._y);
        }
        else {
            clearIRSDataTmp(readout);
        }
    }



    private DataCache getSavedData(WebPlot p, ImageWorkSpacePt pt) {
        DataCache retval= null;
        if (pt==null) return retval;
        for(DataCache fc : _savedPts) {
            if (fc.testMatch(p,pt)) {
                retval= fc;
                break;
            }
        }
        return retval;
    }

    private void addData(WebPlot p, ImageWorkSpacePt pt, IRSInfoData data) {
        DataCache min= null;
        if (_savedPts.size() >= MAX_DATA_PTS) {
            for(DataCache dc : _savedPts) {
                if (min==null || min.getTime()>=dc.getTime()) {
                    min= dc;
                }
            }
            WebAssert.tst(min!=null,
                          "Did not find a saved flux to delete");
            _savedPts.remove(min);
        }


        // add new
        DataCache newDC= getSavedData(p,pt);
        if (newDC!=null) {
            _savedPts.remove(newDC);
        }
        newDC= new DataCache(p,data,pt);
        _savedPts.add(newDC);
    }


    private void getIRSData(final WebPlot plot,
                            final Readout readout,
                            final ImageWorkSpacePt pt)  {
        SearchServicesAsync rpc= SearchServices.App.getInstance();
        rpc.getIRSFileInfo(plot.getPlotState(),pt,new AsyncCallback<IRSInfoData>() {
            public void onFailure(Throwable caught) {
                clearIRSDataTmp(readout);
            }
            public void onSuccess(IRSInfoData result) {
                addData(plot,pt,result);
                updateIRSData(readout,result);
            }
        } );
    }




    private class DataTimer extends Timer {
        ImageWorkSpacePt _pt;
        private WebPlot _plot;
        private Readout _readout;

        public void run() { getIRSData(_plot, _readout, _pt); }

        public void setupCall(ImageWorkSpacePt pt,
                              WebPlot plot,
                              Readout readout) {
            _pt= pt;
            _plot= plot;
            _readout= readout;
        }
    }

    private static class DataCache {
        private final WebPlot _plot;
        private final IRSInfoData _data;
        private final ImageWorkSpacePt _pt;
        private final long _date;

        public DataCache(WebPlot plot, IRSInfoData data, ImageWorkSpacePt pt) {
            _plot= plot;
            _pt= pt;
            _data= data;
            _date= new Date().getTime();
        }
        public boolean equals(Object o) {
            boolean retval= false;
            if (o instanceof DataCache) {
                DataCache other= (DataCache)o;
                retval= (_plot==other._plot) && other._pt.equals(_pt);
            }
            return retval;
        }


        public boolean testMatch(WebPlot plot, ImageWorkSpacePt pt) {
            return (_plot==plot) && pt.equals(_pt);
        }

        public IRSInfoData getData() {return _data; }
        public long getTime() { return _date;}
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
