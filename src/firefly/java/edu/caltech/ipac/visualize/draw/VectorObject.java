/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.draw;

import edu.caltech.ipac.firefly.visualize.VisUtil;
import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.ImagePlot;
import edu.caltech.ipac.visualize.plot.ImagePt;
import edu.caltech.ipac.visualize.plot.ImageWorkSpacePt;
import edu.caltech.ipac.visualize.plot.PlotContainer;
import edu.caltech.ipac.visualize.plot.ProjectionException;
import edu.caltech.ipac.visualize.plot.Pt;
import edu.caltech.ipac.visualize.plot.WorldPt;

import javax.swing.*;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class VectorObject implements ShapeObject {

    private FixedObjectGroup _pts;
    private Map<ImagePlot,PlotInfo>  _plotMap    = new HashMap<>(20);
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

    public VectorObject(WorldPt pts[]) {
        this(new LineShape(), pts);
    }

    public LineShape   getLineShape()   { return _line; }
    public StringShape getStringShape() { return _stringShape; }


    public void drawOnPlot(ImagePlot p, Graphics2D g2) {
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
        for(var entry: _plotMap.entrySet()) {
            pInfo= entry.getValue();
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
                    imageOldpts= pInfo.convert(oldpts);
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
//            pInfo._plot.repair(rNew);
        }
    }


    public void addPlotView(PlotContainer container) {
        for(ImagePlot p : container) addPlot(p);
    }

    public WorldPt getWorldPt(int i) {
        Assert.argTst(_pts.isWorldCoordSys(),"This Vector Object does not " +
                      "support World Coordinates, " +
                      "you must constuct the vector with world " +
                      "coordinates to use this method.");
        FixedObject fo= _pts.get(i);
        return fo.getPosition();
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
        if (pt instanceof WorldPt wpt) {
            pt= VisUtil.convert( wpt, _csys);
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
       }
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

    private void addPlot(ImagePlot p) {
        PlotInfo pInfo= new PlotInfo(p);
        pInfo.computeTransform();
        _plotMap.put(p, pInfo);
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
        for(var entry : _plotMap.entrySet()) {
            pInfo= entry.getValue();
            pInfo.updatePt(i,pt);
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
        ImagePlot _plot;
        ImagePt              _lastStrPt= null;
        PlotInfo( ImagePlot p) {
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
