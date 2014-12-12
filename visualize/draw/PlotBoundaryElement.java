package edu.caltech.ipac.visualize.draw;

import edu.caltech.ipac.visualize.plot.WorldPt;
import edu.caltech.ipac.visualize.plot.ImagePt;
import edu.caltech.ipac.visualize.plot.ImageWorkSpacePt;
import edu.caltech.ipac.visualize.plot.Plot;
import edu.caltech.ipac.visualize.plot.PlotView;
import edu.caltech.ipac.visualize.plot.PlotViewStatusListener;
import edu.caltech.ipac.visualize.plot.PlotViewStatusEvent;
import edu.caltech.ipac.visualize.plot.Corners;
import edu.caltech.ipac.visualize.plot.ProjectionException;
import nom.tam.fits.FitsException;
import edu.caltech.ipac.visualize.net.AnyFitsParams;
import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.client.ClientLog;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

/**
 * Date: May 31, 2006
 *
 * @author Trey Roby
 * @version $id:$
 */
public class PlotBoundaryElement implements PlotViewStatusListener {

    private final File _fitsFile;
    private final AnyFitsParams _afp;
    private final VectorObject _vector;
    private final WorldPt _corners[];
    private final Map<Plot,ImagePt[]> _transforms= new HashMap<Plot,ImagePt[]>(3);
    private PlotView _pv= null;
    private final boolean _isFile;

//============================================================================
//---------------------------- Constructors ----------------------------------
//============================================================================
    public PlotBoundaryElement(WorldPt corners[], AnyFitsParams afp) {
        Assert.argTst(corners.length==4, "Your WorldPt array must have 4 elements");
        _corners = new WorldPt[5];
        for (int i = 0; i < corners.length; i++) {
            _corners[i] = corners[i];
        }
        _corners[4] = corners[0];
        _vector = new VectorObject(_corners);
        _fitsFile = null;
        _afp = afp;
        _isFile = false;
    }

    public PlotBoundaryElement(WorldPt corners[], File fitsFile) {
        Assert.argTst(corners.length==4,
                      "Your WorldPt array must have 4 elements");
        _corners= corners;
        _vector= new VectorObject(corners);
        _fitsFile= fitsFile;
        _afp = null;
        _isFile = true;
    }


    public PlotBoundaryElement(File fitsFile) {

        VectorObject vo= null;
        _corners= new WorldPt[5];
//        _corners= new WorldPt[4];
        _fitsFile= fitsFile;
        try {
            WorldPt plotCorners[]= Corners.findCorners(fitsFile);
            //TODO: populate four corners
            for(int i= 0; (i<4); i++) _corners[i]= plotCorners[i];
            _corners[4]= _corners[0];
            vo= new VectorObject(_corners);
        } catch (FitsException e) {
        } catch (ProjectionException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        _vector= vo;
        _afp = null;
        _isFile = true;
    }


//============================================================================
//---------------------------- Public Methods --------------------------------
//============================================================================

    public boolean isInside(WorldPt pt, Plot p) {
        boolean retval= false;
        try {
            ImageWorkSpacePt imPt = p.getImageCoords(pt);
            retval= isInside(imPt,p);
        } catch (ProjectionException e) {
            retval= false;
        }
        return retval;
    }

    public boolean isInside(ImageWorkSpacePt imPt, Plot p) {
        boolean retval= false;
        if (_vector!=null) {
            if (_transforms.containsKey(p)) {
                ImagePt imCorners[]= _transforms.get(p);
                double c1X= imCorners[0].getX();  // corner 1 X
                double c1Y= imCorners[0].getY();  // corner 1 Y
                double c3X= imCorners[2].getX();  // corner 3 X
                double c3Y= imCorners[2].getY();  // corner 3 Y
                double x= imPt.getX();
                double y= imPt.getY();
                retval= ((x < c1X && x > c3X) || (x > c1X && x < c3X)) &&
                        ((y < c1Y && y > c3Y) || (y > c1Y && y < c3Y));
            }
        }
        return retval;
    }

    public VectorObject getVectorObject() { return _vector;}

    public boolean isFile() {return _isFile; }

    public File getFitsFile() { return _fitsFile; }
        /*if (_fitsFile != null) {
            return _fitsFile;
        } else if (_afp != null) {
            try {
                return (File) _afp.getURL().getContent();
            } catch (IOException e) {
                ClientLog.warning(e.toString());
            }
        } else {
            return null;
        }
        return null;
    }*/

    public String getFileName() {
        if (_fitsFile != null) {
            return _fitsFile.toString();
        } else if (_afp != null) {
            return _afp.getURL().toString();
        } else {
            return null;
        }
    }

    public File downloadFits () {
        try {
            if (_afp != null) {
                return (File) _afp.getURL().getContent();
            }
        } catch (IOException e) {
            ClientLog.warning(e.toString());
        }
        return null;
    }
    //public WorldPt[] getCorners() { return _corners;}

    public void setPlotView(PlotView pv) {
        if (_pv!=null) {
            _pv.removePlotViewStatusListener(this);
            for(Plot p : pv) removePlot(p);
        }
        _pv= pv;
        if (_pv!=null) {
            _pv.addPlotViewStatusListener(this);
            for(Plot p : pv) addPlot(p);
        }
    }

    private void addPlot(Plot p) {
        ImagePt imCorners[]= new ImagePt[4];
        try {
            for(int i=0; (i<4); i++) {
		ImageWorkSpacePt ip = p.getImageCoords(_corners[i]);
                imCorners[i]= new ImagePt(ip.getX(), ip.getY());
            }
            _transforms.put(p,imCorners);
        } catch (ProjectionException e) {
            ClientLog.warning("Could not transform corners for plot: ",
                              p.getPlotDesc());
        }
    }

    private  void removePlot(Plot p) {
        _transforms.remove(p);
    }



//============================================================================
//--------- Methods from PlotViewStatusListener Interface --------------------
//============================================================================

    public void plotAdded(PlotViewStatusEvent ev) {
        addPlot(ev.getPlot());
    }

    public void plotRemoved(PlotViewStatusEvent ev) {
        removePlot(ev.getPlot());
    }

//============================================================================
//---------------------------- Private / Protected Methods -------------------
//============================================================================

//============================================================================
//---------------------------- Factory Methods -------------------------------
//============================================================================

//============================================================================
//---------------------------- Inner Classes ---------------------------------
//============================================================================

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
