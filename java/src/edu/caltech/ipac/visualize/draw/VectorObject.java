package edu.caltech.ipac.visualize.draw;

import edu.caltech.ipac.util.ClientLog;
import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.ImagePt;
import edu.caltech.ipac.visualize.plot.ImageWorkSpacePt;
import edu.caltech.ipac.visualize.plot.Plot;
import edu.caltech.ipac.visualize.plot.PlotContainer;
import edu.caltech.ipac.visualize.plot.PlotPaintEvent;
import edu.caltech.ipac.visualize.plot.PlotViewStatusEvent;
import edu.caltech.ipac.visualize.plot.Pt;
import edu.caltech.ipac.visualize.plot.WorldPt;
import edu.caltech.ipac.visualize.plot.ProjectionException;

import javax.swing.SwingUtilities;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;


public class VectorObject implements ShapeObject {

    private FixedObjectGroup _pts;
    private Map<Plot,PlotInfo>  _plotMap    = new HashMap<Plot,PlotInfo>(20);
    private List<VectorDataListener> _dataListeners=
        new ArrayList<VectorDataListener>(2);
    private LineShape        _line;
    private StringShape      _stringShape= new StringShape(2,StringShape.SE);
    private boolean          _show   = true;
    private String           _labelStrings[];
    private CoordinateSys    _csys= CoordinateSys.EQ_J2000;


    public VectorObject(LineShape line, WorldPt pts[]) {
        _line= line;
        _pts  = new FixedObjectGroup();
        for(int i= 0; (i < pts.length); i++ ) {
            _pts.add( _pts.makeFixedObject(pts[i]) );
        }
    }

    public VectorObject(LineShape line, ImagePt pts[]) {
        _line= line;
        _pts  = new FixedObjectGroup(false);
        for(int i= 0; (i < pts.length); i++ ) {
            _pts.add( _pts.makeFixedObject(pts[i]) );
        }
    }

    public VectorObject(LineShape line, FixedObjectGroup foGroup) {
        _line= line;
        _pts = foGroup;
    }

    public VectorObject(WorldPt pts[]) {
        this(new LineShape(), pts);
    }

    public VectorObject(ImagePt pts[]) {
        this(new LineShape(), pts);
    }

    public VectorObject(FixedObjectGroup foGroup) {
        this(new LineShape(), foGroup);
    }

    public LineShape   getLineShape()   { return _line; }
    public StringShape getStringShape() { return _stringShape; }


    public void setLabelStrings(String strs[]) {
        _labelStrings= new String[strs.length];
        for (int i=0; (i<_labelStrings.length); i++)
            _labelStrings[i]= strs[i];
        doRepair();
    }

    public void setEnabled(boolean show){
        _show= show;
        doRepair(0,_pts.size()-1, null);
    }

    public boolean  isEnabled()             { return _show;}

    public void setLineType(LineShape line) { _line= line; }

    public void drawOnPlot(Plot p, Graphics2D g2) {
        if (_show) {
            PlotInfo pInfo= _plotMap.get(p);
            Assert.tst(pInfo);            
            g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON  ); //TLau 04/23/08
            _line.draw(g2, pInfo._entry);

            int foundIdx= computeCurrentLabelIdx(pInfo);

            double theta = 0.0F;
            if (pInfo._entry.length == 2) {
                ImagePt[] pt = {pInfo._entry[0].getPt(), pInfo._entry[1].getPt()};

                if (pt[0]!=null && pt[1]!=null) //TLau 04/12/10, make sure pt0 and pt1 are not null.
                    theta = Math.PI - Math.atan2( (pt[1].getY()-pt[0].getY()),
                            (pt[1].getX()-pt[0].getX()) );
            }

            if (foundIdx > -1) {
                pInfo._lastStrPt= pInfo._entry[foundIdx].getPt();
                determineLabelDirection( pInfo, foundIdx);
		        ImageWorkSpacePt _lastStrPt = new ImageWorkSpacePt(
		            pInfo._lastStrPt.getX(), pInfo._lastStrPt.getY());
                //_stringShape.draw(g2, _lastStrPt, 0.0F, _labelStrings);
                _stringShape.draw(g2, _lastStrPt, (float)theta, _labelStrings);
            }
            else {
                pInfo._lastStrPt= null;
            }
        }
    }


    private void doRepair(int beginIdx, int endIdx, Pt oldpts[]) {
        int      j;
        PlotInfo pInfo;
        Rectangle rTmp, rNew;
        AffineTransform trans;
        LineShape.Entry  imageOldpts[]= null;
        int repairLength= endIdx-beginIdx+1;
        LineShape.Entry ptsAry[]= new LineShape.Entry[repairLength];
        int scale;
        int labelIdx;
        for(Map.Entry<Plot,PlotInfo> entry: _plotMap.entrySet()) {
            pInfo= (PlotInfo)entry.getValue();
            labelIdx= computeCurrentLabelIdx(pInfo);
            trans= pInfo._plot.getTransform();
            for(j=0; (j<repairLength); j++) {
                ptsAry[j]= pInfo._entry[beginIdx+j];
            }
            rNew= _line.computeRepair(trans, ptsAry );
            if (oldpts != null) {
                if (oldpts instanceof WorldPt[]) {
                    imageOldpts= pInfo.convert(oldpts);
                }
                else {
                    imageOldpts= pInfo.convert((ImagePt[])oldpts);
                }
                rTmp= _line.computeRepair(trans, imageOldpts);
                rNew= SwingUtilities.computeUnion( (int)rTmp.getX(),
                                                   (int)rTmp.getY(),
                                                   (int)rTmp.getWidth(),
                                                   (int)rTmp.getHeight(),
                                                   rNew);
            }
            /*
             *  repair labels
             */
            if (_labelStrings != null && labelIdx > -1) {
                if (pInfo._entry.length > 1) {
                    determineLabelDirection( pInfo,  labelIdx);
                }
		ImageWorkSpacePt iwspt = new ImageWorkSpacePt(
		    pInfo._entry[labelIdx].getPt().getX(), 
		    pInfo._entry[labelIdx].getPt().getY());
                rTmp= _stringShape.computeRepair(trans,
						 iwspt,
                                                 0.0F, _labelStrings);
                rNew= SwingUtilities.computeUnion( (int)rTmp.getX(),
                                                   (int)rTmp.getY(),
                                                   (int)rTmp.getWidth(),
                                                   (int)rTmp.getHeight(),
                                                   rNew);
            }
            if (_labelStrings != null      &&
                pInfo._lastStrPt != null   &&
                labelIdx > -1              &&
                !pInfo._lastStrPt.equals(pInfo._entry[labelIdx].getPt()) ) {
		ImageWorkSpacePt iwspt = new ImageWorkSpacePt(
		    pInfo._lastStrPt.getX(), pInfo._lastStrPt.getY());
                rTmp= _stringShape.computeRepair(trans, iwspt,
                                                 0.0F, _labelStrings);
                rNew= SwingUtilities.computeUnion( (int)rTmp.getX(),
                                                   (int)rTmp.getY(),
                                                   (int)rTmp.getWidth(),
                                                   (int)rTmp.getHeight(),
                                                   rNew);
            }
            if (pInfo._lastStrPt != null) {
                pInfo._lastStrPt= null;
            }

            /*
             * adjust for scale
             */
            scale= (int)pInfo._plot.getScale() + 1;
            rNew.x= rNew.x - scale;
            rNew.y= rNew.y - scale;
            rNew.width = rNew.width + (2*scale);
            rNew.height= rNew.height+ (2*scale);
            pInfo._plot.repair(rNew);
        }
    }

    private void doRepair() {
        for(Plot p: _plotMap.keySet()) {
            p.repair();
        }
    }

    public void addPlotView(PlotContainer container) {
        for(Plot p : container) addPlot(p);
        container.addPlotViewStatusListener( this);
        container.addPlotPaintListener(this);
    }

    public void removePlotView(PlotContainer container) {
        for(Plot p : container) removePlot(p);
        container.removePlotViewStatusListener( this);
        container.removePlotPaintListener(this);
    }

    public void removeAllPlots() {
        Iterator j= _plotMap.entrySet().iterator();
        PlotInfo pInfo;
        Map.Entry entry;
        while( j.hasNext() ) {
            entry= (Map.Entry)j.next();
            pInfo= (PlotInfo)entry.getValue();
            j.remove();
            if (pInfo._plot.getPlotView()!=null) {
                pInfo._plot.getPlotView().removePlotPaintListener(this);
            }
            pInfo._plot.repair();
        }

    }

    public WorldPt getWorldPt(int i) {
        Assert.argTst(_pts.isWorldCoordSys(),"This Vector Object does not " +
                      "support World Coordinates, " +
                      "you must constuct the vector with world " +
                      "coordinates to use this method.");
        FixedObject fo= _pts.get(i);
        return fo.getPosition();
    }


    public ImagePt getImagePt(int i) {
        Assert.argTst(!_pts.isWorldCoordSys(),"This Vector Object does not " +
                               "support Image coordinates, " +
                               "you must constuct the vector with image " +
                               "coordinates to use this method.");
        FixedObject fo= _pts.get(i);
        return fo.getImagePt();
    }

    public void set(int idx, ImagePt pt) {
        setPoint(idx,pt);
    }

    public void set(int idx, WorldPt pt) {
        setPoint(idx,pt);
    }

    private void setPoint(int idx, Pt pt) {
        int length= _pts.size();
        boolean useWorld= false;
        if (pt instanceof WorldPt) {
            pt= Plot.convert( (WorldPt)pt, _csys);
            useWorld= true;
        }
        FixedObject fo;
        if (idx >= 0 && idx < length) {
            int beginIdx= (idx==0) ? 0 : idx-1;
            int endIdx= (idx==length-1) ? length-1 : idx+1;
            int repairLength= endIdx-beginIdx+1;
            Pt oldpts[] = useWorld ? new WorldPt[repairLength] : new ImagePt[repairLength];
            for(int j=0; (j<repairLength); j++) {
                fo= _pts.get(beginIdx+j);
                oldpts[j]= fo.getPoint();
            }
            updatePoints(idx,pt);
            doRepair(beginIdx, endIdx, oldpts);
            fireVectorChanged();
        }
    }

    public void setCoordinateSys(CoordinateSys csys) {
        if (_pts.isWorldCoordSys()) {
            int len= _pts.size();
            _csys= csys;
            FixedObject fo;
            WorldPt pt;
            for(int i= 0; (i<len); i++) {
                fo= _pts.get(i);
                pt= Plot.convert( fo.getPosition(), csys);
                fo.setPosition(pt);
            }
        }
        else {
            ClientLog.warning(true, "This Vector Object only " +
                               "supports Image coordinates, " +
                               "you must constuct the vector with WorldPt " +
                               "coordinates to use this method.");
        }
    }
    // ===================================================================
    // ------------------  Methods  from PlotViewStatusListener -----------
    // ===================================================================
    public void plotAdded(PlotViewStatusEvent ev) {
        addPlot(ev.getPlot());
    }
    public void plotRemoved(PlotViewStatusEvent ev) {
        removePlot(ev.getPlot());
    }

    // ===================================================================
    // ------------------  Methods  from PlotPaintListener ---------------
    // ===================================================================

    public void paint(PlotPaintEvent ev) {
        drawOnPlot( ev.getPlot(), ev.getGraphics() );
    }
    //===================================================================
    //----------------------- Add / Remove Listener Methods -------------
    //===================================================================

    /**
     * Add a VectorDataListener.
     * @param l the listener
     */
    public void addVectorDataListener(VectorDataListener l) {
        _dataListeners.add(l);
    }
    /**
     * Remove a PlotViewStatusListener.
     * @param l the listener
     */
    public void removeVectorDataListener(VectorDataListener l) {
        _dataListeners.remove(l);
    }

    /**
     * Compute distance in degrees between end points of this vector object
     */
    public double computeDistance() {
        final double    DtoR      = Math.PI/180.0;
        final double    RtoD      = 180.0/Math.PI;

        WorldPt p1 = getWorldPt(0);
        WorldPt p2 = getWorldPt(1);
        double lon1Radius  = p1.getLon() * DtoR;
        double lon2Radius  = p2.getLon() * DtoR;
        double lat1Radius  = p1.getLat() * DtoR;
        double lat2Radius  = p2.getLat() * DtoR;
        double cosine =
            Math.cos(lat1Radius)*Math.cos(lat2Radius)*
            Math.cos(lon1Radius-lon2Radius)
            + Math.sin(lat1Radius)*Math.sin(lat2Radius);

        if (Math.abs(cosine) > 1.0)
            cosine = cosine/Math.abs(cosine);
        double distance = RtoD*Math.acos(cosine);
        return distance;
    }


    //===================================================================
    //------------------------- Private / Protected Methods -------------
    //===================================================================
    private int computeCurrentLabelIdx(PlotInfo pInfo) {
        int retval=-1;
        boolean found= false;
        for (int i=0; (i<pInfo._entry.length && !found); i++) {
            if ( pInfo._entry[i].getPt() != null &&
                 pInfo._plot.pointInPlot(
		     new ImageWorkSpacePt(pInfo._entry[i].getPt().getX(),
		     pInfo._entry[i].getPt().getY()))) {
                found= true;
                retval=i;
            }
        }
        return retval;
    }

    private void addPlot(Plot p) {
        PlotInfo pInfo= new PlotInfo(p);
        pInfo.computeTransform();
        _plotMap.put(p, pInfo);
    }

    private void removePlot(Plot p) {
        PlotInfo pInfo= _plotMap.get(p);
        if (pInfo != null) {
            _plotMap.remove(p);
        }
    }

    private void determineLabelDirection(PlotInfo pInfo, int labelIdx) {

        Assert.tst(pInfo._entry[labelIdx].getPt() != null && labelIdx > -1);

        ImagePt p1= null, p2= null;
        if (labelIdx+1 < pInfo._entry.length) {
            p1= pInfo._entry[labelIdx].getPt();
            if (pInfo._entry[labelIdx+1].getPt() != null) { //TLau 04/22/08
                p2= pInfo._entry[labelIdx+1].getPt();
            }
            else {
                p2= pInfo._entry[labelIdx-1].getPt();
            }
        }
        else {
            p1= pInfo._entry[labelIdx].getPt();
            p2= pInfo._entry[labelIdx].getPt();
        }
        Assert.tst(p1 != null && p2 != null);

        double x1, x2, y1, y2;
        x1= p1.getX();
        y1= p1.getY();
        x2= p2.getX();
        y2= p2.getY();

        if (x1 > x2) {
            if (y1 > y2) {
                _stringShape.setOffsetDirection( StringShape.NW);
            }
            else {
                _stringShape.setOffsetDirection( StringShape.SW);
            }
        }
        else {
            if (y1 > y2) {
                _stringShape.setOffsetDirection( StringShape.NE);
            }
            else {
                _stringShape.setOffsetDirection( StringShape.SE);
            }
        }

    }


    private void updatePoints(int i, Pt pt) {
        _pts.get(i).setPoint(pt);
        PlotInfo pInfo;
        for(Map.Entry<Plot,PlotInfo> entry : _plotMap.entrySet()) {
            pInfo= entry.getValue();
            pInfo.updatePt(i,pt);
        }
    }


    /**
     * fire the <code>VectorDataListener</code>s.
     */
    protected void fireVectorChanged() {
        List<VectorDataListener> newlist;
        VectorDataEvent ev= new VectorDataEvent(this);
        synchronized (this) {
            newlist = new Vector<VectorDataListener>(_dataListeners);
        }

        for(VectorDataListener listener: newlist) {
            listener.dataChanged(ev);
        }
    }

    //===================================================================
    //------------------------- Factory methods -------------------------
    //===================================================================



    //===================================================================
    //------------------------- Private Inner classes -------------------
    //===================================================================

    private class PlotInfo {
        LineShape.Entry           _entry[];
        Plot                      _plot;
        ImagePt              _lastStrPt= null;
        PlotInfo( Plot p) {
            _plot= p;
        }

        public void computeTransform() {
            FixedObject  fo;
            ImagePt pt;
            _entry= new LineShape.Entry[_pts.size()];
            Pt current, last= null;
            Iterator<FixedObject> j= _pts.iterator();
            boolean coordsWrap;
            for(int i= 0; (i<_entry.length); i++) {
                fo= j.next();
                current= null;
                try {
                    if (fo.isWorldCoordSys()) {
                        current= fo.getPosition();
                        ImageWorkSpacePt ipt= _plot.getImageCoords((WorldPt)current);
			pt = new ImagePt(ipt.getX(), ipt.getY());
                        coordsWrap=
                           (last!=null && _plot.coordsWrap((WorldPt) last, (WorldPt) current));
                    }
                    else {
                        current= pt= fo.getImagePt();
                        coordsWrap= false;
                    }
                    if (last!= null) {
                        if (coordsWrap) {
                            _entry[i]= new LineShape.Entry(pt,false);
                            last= null;
                        }
                        else {
                            _entry[i]= new LineShape.Entry(pt,true);
                            last= current;
                        }
                    }
                    else {
                        _entry[i]= new LineShape.Entry(pt,true);
                    }
                } catch (ProjectionException e) {
                    _entry[i]= new LineShape.Entry(null,false);
                    last= null;
                }
                last= current;
            }
        }

        public void updatePt(int idx, Pt pt) {
            if (pt instanceof WorldPt) {
                updatePt(idx,(WorldPt)pt);
            }
            else if (pt instanceof ImagePt) {
                updatePt(idx,(ImagePt)pt);
            }
            else {
                Assert.argTst(true, "Only ImagePt or WorldPt is allowed.");
            }
        }

        public void updatePt(int idx, WorldPt wpt) {
            try {
                ImageWorkSpacePt ip = _plot.getImageCoords(wpt);
                ImagePt pt= new ImagePt(ip.getX(), ip.getY());
                _entry[idx]= new LineShape.Entry(pt, true);
            } catch (ProjectionException e) {
                _entry[idx]= new LineShape.Entry(null, true);
            }
        }

        public void updatePt(int idx, ImagePt pt) {
            _entry[idx]= new LineShape.Entry(pt, true);
        }

        public LineShape.Entry[] convert(Pt pt[]) {
            ImagePt    ipt;
            LineShape.Entry retAry[]= new LineShape.Entry[pt.length];
            Pt    current, last= null;
            boolean coordsWrap;
            for(int i= 0; (i<retAry.length); i++) {
                current= pt[i];
                try {
                    if (current instanceof WorldPt) {
                        ImageWorkSpacePt ip= _plot.getImageCoords((WorldPt)current);
                        ipt= new ImagePt(ip.getX(), ip.getY());
                        coordsWrap=
                           (last!=null && _plot.coordsWrap((WorldPt) last, (WorldPt) current));
                    }
                    else {
                        ipt= (ImagePt)current;
                        coordsWrap= false;
                    }
                    if (last!= null) {
                        if (coordsWrap) {
                            retAry[i]= new LineShape.Entry(ipt,false);
                            last= null;
                        }
                        else {
                            retAry[i]= new LineShape.Entry(ipt,true);
                            last= current;
                        }
                    }
                    else {
                        retAry[i]= new LineShape.Entry(ipt,true);
                    }
                } catch (ProjectionException e) {
                    retAry[i]= new LineShape.Entry(null,false);
                    last= null;
                }
                last= current;
                // System.out.println("convert: retAry["+i+"]= " + retAry[i]);
            } // end loop
            return retAry;
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
