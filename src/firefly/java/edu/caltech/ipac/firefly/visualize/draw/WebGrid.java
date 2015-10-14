/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize.draw;

import com.google.gwt.i18n.client.NumberFormat;
import edu.caltech.ipac.astro.CoordException;
import edu.caltech.ipac.firefly.util.PropertyChangeListener;
import edu.caltech.ipac.firefly.util.PropertyChangeSupport;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotView;
import edu.caltech.ipac.firefly.visualize.conv.CoordUtil;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.ImagePt;
import edu.caltech.ipac.visualize.plot.ImageWorkSpacePt;
import edu.caltech.ipac.visualize.plot.WorldPt;
import java.util.ArrayList;
import java.util.List;




/**
 * Draws a coordinate system grid
 *
 *
**/
public class WebGrid
{
    /**
     * Threshold for convergence of values of coordinate ranges.
     */
    private static final double RANGE_THRESHOLD = 1.02;

     /**
      * Font used to draw the grid coordinates.
     **/
     public static final String	GRID_FONT = "SansSerif";

    /**
     * bound properties
     */
    public  static final String USER_DEFINED_DISTANCE= "UserDefinedDistance";
    public  static final String MIN_USER_DISTANCE    = "MinUserDistance";
    public  static final String MAX_USER_DISTANCE    = "MaxUserDistance";
    public  static final String COORD_SYSTEM         = "CoordSystem";
    

     /**
      * The precomputed lines to be drawn
     **/
     private double[][]            _xLines;
     private double[][]            _yLines;
     private int                   _dWidth; 
     private int                   _dHeight;  
     private int _screenWidth;
     private int _screenHeight;
     private double                _factor = 1.0;

    /**
     * Color used to draw the grid
     **/
    public static final String DEF_GRID_COLOR = "green";
    /**
     * The precomputed label strings to be drawn
    **/
     private String[]              _labels;

	/** 
	 * If true, displays positions as formatted string, 
	 * otherwise displays raw decimal value. 
	**/
     private boolean               _sexigesimal = false;
     private WebPlot                  _plot;
     private PropertyChangeSupport _propChange= new PropertyChangeSupport(this);

     /** the coordinate system that user wants the Grid to be drawn 
        It has to be set to the constants defined in Plot
     */
     private CoordinateSys          _csys;
     private boolean                _paramChanged;
     private boolean		    _aitoff;

     private boolean _userDefinedDistance = false;
     private double  _minUserDistance= 0.25;   // user defined max dist. (deg)
     private double  _maxUserDistance= 3.00;   // user defined min dist. (deg)
	

     private String   _gridColor= DEF_GRID_COLOR;

     private final  Drawer _drawer;
     private boolean useLabels= true;

    //location of the labels
    ImageWorkSpacePt[] _points;
    int _nLivel0;  //line count in levels[0] direction
    int _nLivel1; //line count in levels[1] direction


    /**
      * Creates a new Grid.
      * @param csys the default coordinate system
     **/
   public WebGrid(WebPlotView pv, CoordinateSys csys)
   {
       _csys = csys;
       _paramChanged = true;
       _drawer= new Drawer(pv);
       _drawer.setDataTypeHint(Drawer.DataType.VERY_LARGE);
       _drawer.setHandleImageChanges(false);
       _drawer.setDefaultColor(_gridColor);

       //_drawerTxt=_drawer;
       //_drawerTxt.setDefaultColor(V_LABEL_COLOR);
   }

    public Drawer getDrawer() {
        return _drawer;
    }

    /**
	 * Draws the Grid within the current plot
	 *
	**/
    public boolean  paint() {

        WebPlot plot= _drawer.getPlotView().getPrimaryPlot();
        if (plot==null) return false;
        int dwidth = plot.getImageWidth();
        int dheight = plot.getImageHeight();
        int screenWidth = plot.getScreenWidth();
        int screenHeight = plot.getScreenHeight();
        if (dwidth > 0 && dheight >0) {
            if (_plot != plot || _paramChanged || screenWidth != _screenWidth || screenHeight != _screenHeight) {
                _dWidth = dwidth;
                _dHeight = dheight;
                _screenWidth = screenWidth;
                _screenHeight = screenHeight;

                _factor = _screenWidth /_dWidth;
                if (_factor < 1.0 ) _factor = 1.0;
                _plot = plot;


                WorldPt c = _plot.getWorldCoords(new ImageWorkSpacePt(1,1), _csys);
                _aitoff= (c==null);

                _paramChanged = false;
                computeLines();
            }


            List<DrawObj> drawData= new ArrayList<DrawObj>(300);

             drawLines(drawData);
             _drawer.setData(drawData);
            //_drawerTxt.setData(drawDataVLabel);
        }
        return true;
    }

    public void clear() {
        _drawer.clear();
    }
//=====================================================================
//----------- Public Bound Properties methods -------------------------
//=====================================================================


    public boolean isUseLabels() {
        return useLabels;
    }

    public void setUseLabels(boolean useLabels) {
        this.useLabels = useLabels;
    }

    public void setUserDefinedDistance(boolean userDefined) {
       Boolean oldValue= _userDefinedDistance;
       _userDefinedDistance= userDefined;
       _propChange.firePropertyChange ( USER_DEFINED_DISTANCE, oldValue, 
                                             _userDefinedDistance);
       _paramChanged = true;
   }

   public boolean getUserDefinedDistance() { return _userDefinedDistance; }


   public void setMinUserDistance(double minDist) {
       Double oldValue= _minUserDistance;
       _minUserDistance= minDist;
       _propChange.firePropertyChange ( MIN_USER_DISTANCE, oldValue, 
                                             _minUserDistance);
       _paramChanged = true;
   }

   public double getMinUserDistance() { return _minUserDistance; }


   public void setMaxUserDistance(double maxDist) {
       Double oldValue= _maxUserDistance;
       _maxUserDistance= maxDist;
       _propChange.firePropertyChange ( MAX_USER_DISTANCE, oldValue, 
                                             _maxUserDistance);
       _paramChanged = true;
   }

   public double getMaxUserDistance() { return _maxUserDistance; }


   public void setCoordSystem(CoordinateSys csys) {
      CoordinateSys oldValue= _csys;
      _csys = csys;
      _paramChanged = true;
      _propChange.firePropertyChange ( COORD_SYSTEM, oldValue, _csys);
   }

   public CoordinateSys getCoordSystem() { return _csys; }


   public void setGridColor(String c) {
       _gridColor= c;
   }

   public String getGridColor() {
       return _gridColor;
   }

   public void setSexigesimalLabel(boolean isSexigesmal) {
      _sexigesimal = isSexigesmal;
   }

//=====================================================================
//----------- add / remove property Change listener methods -----------
//=====================================================================

    /**
     * Add a property changed listener.
     * @param p  the listener
     */
    public void addPropertyChangeListener (PropertyChangeListener p) {
       _propChange.addPropertyChangeListener (p);
    }

    /**
     * Remove a property changed listener.
     * @param p  the listener
     */
    public void removePropertyChangeListener (PropertyChangeListener p) {
       _propChange.removePropertyChangeListener (p);
    }


//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================


   protected void drawLines(List<DrawObj>  drawData) {
   //    protected void drawLines(List<DrawObj>  drawData) {
	  /* Draw the lines previously computed. */


       Rectangle bounds = new Rectangle(0,0,
               _plot.getImageWidth(),
               _plot.getImageHeight());

//       if (bounds == null) System.out.println("BOUNDS NULL ");

       //-----
       //-----
       //System.out.println("bounds (X, Y) =  ("+bounds.x+", "+bounds.y +")");
       //System.out.println("width, height =  ("+bounds.width+", "+bounds.height +")");


       //get the locations where to put the labels
         _points =getLabelPoints(bounds);


       int lineCount = _xLines.length;

        for (int i=0; i<lineCount; i++) {

           drawLabeledPolyLine(drawData, bounds, _labels[i],
              _xLines[i], _yLines[i], i);


       }


   }

    /**
     * Calculate where to start writing the label information
     * @param bounds
     * @return
     */
    ImageWorkSpacePt[] getLabelPoints(Rectangle bounds) {


        int lineCount = _xLines.length;
        ImageWorkSpacePt[] points = new ImageWorkSpacePt[lineCount];
        if (_csys.toString().startsWith("EQ")) {


            for (int i = 0; i < lineCount; i++) {

                if (i< _nLivel0){
                    points[i] = new ImageWorkSpacePt(_xLines[i][0], bounds.y);

                }
                else {
                    points[i] = new ImageWorkSpacePt(bounds.x, _yLines[i][0]);
                }

            }

        } else {

            //levels[0] direction labels
             /*
              The lines in this direction have the same number of points _xLines, _yLines.
              Find the middle point which is the half of the length of the _xLines (or _yLines) in this direction.
              Put the label in the middle of this direction.
             */
            int hM = _xLines[0].length/2; //in the middle of the line
            for (int i=0; i< _nLivel0; i++){
             points[i] = new ImageWorkSpacePt(_xLines[i][hM],_yLines[i][hM]);

            }


            //levels[1] direction labels
            /*
              The lines in this direction have the same number of points _xLines, _yLines.
              Find the middle point which is the half of the length of the _xLines (or _yLines) in this direction.
              Put the label in the middle of this direction.
             */
            int vM=_xLines[_nLivel0].length/2;
            for (int i=0; i< _nLivel1; i++){
                 points[i+ _nLivel0] = new ImageWorkSpacePt(_xLines[_nLivel0 +i][vM], _yLines[_nLivel0 +i][vM]);

            }



        }

        return  points;
    }


    protected void drawLabeledPolyLine(List<DrawObj>  drawData, Rectangle bounds,
                                                String label,
                                                double[] x, double[] y, int count){


        //TODO
        //1. check the x and y array to see if there are gaps between adjacent points, if so, add point
        //2. draw the lines


        int lineCount=0;
        //add the  draw line data to the drawData
        ImageWorkSpacePt ipt0, ipt1;
       for (int i=0; i<x.length-1; i++) {

           //check the x[i] and y[i] are inside the image screen
            if (x[i] > -1000 && x[i+1] > -1000 &&
                   ((x[i] >= bounds.x) &&
                   ((x[i] - bounds.x) < bounds.width) &&
                   (y[i] >= bounds.y) &&
                    ((y[i]-bounds.y) < bounds.height) ||
                      // bounds check on x[i+1], y[i+1]
                     (x[i+1] >= bounds.x) &&
                     ((x[i+1] - bounds.x) < bounds.width) &&
                      (y[i+1] >= bounds.y) &&
                       ((y[i+1]-bounds.y) < bounds.height))) {

                ipt0= new ImageWorkSpacePt(x[i],y[i]);
                ipt1= new ImageWorkSpacePt(x[i+1], y[i+1]);

                if (!_aitoff ||  ((Math.abs(ipt1.getX()-ipt0.getX()) < _screenWidth /8) &&   (_aitoff))) {
                     lineCount++;
                     drawData.add(ShapeDataObj.makeLine(ipt0,ipt1));
                  }
            }  // if
        } // for

        // draw the label.

       if (useLabels){
          if (count< _nLivel0) { //vertical line labels

               drawData.add(ShapeDataObj.makeText(_points[count], label));
           }
           else { //Horizontal line labels
               drawData.add(ShapeDataObj.makeText(_points[count], label));
           }
       }




    }




     protected void computeLines() {
	     /* This is where we do all the work. */
	     /* range and levels have a first dimension indicating x or y
	      * and a second dimension for the different values (2 for range)
	      * and a possibly variable number for levels.
	      */
	 double[][] range = getRange();
	 double[][] levels = getLevels(range);
	     


        _nLivel0 =levels[0].length;
        _nLivel1 = levels[1].length;
	    _labels = getLabels(levels);
        int size = _nLivel0 + _nLivel1;

	  _xLines = new double[size][];
	  _yLines = new double[size][];
	  int offset = 0;
	  double[][] points;
	  for (int i=0; i<2; i++) {
	     for (int j=0; j<levels[i].length; j++) {
	        points = findLine(i, levels[i][j], range);
	        _xLines[offset] = points[0];
	        _yLines[offset] = points[1];
	        offset += 1;
	        }
	     }
	}

     protected String[] getLabels(double[][] levels) {
       String labels[] = new String[levels[0].length + levels[1].length];

       int digits;
       double delta;
       double value;

       int offset = 0;
       for (int i=0; i < 2; i += 1)
       {
	 if (levels[i].length < 2)
	 {
		 digits = 1;
	 }
	 else
	 {
		 delta = levels[i][1]-levels[i][0];
		 if (delta < 0) delta += 360;

/*				if (i == 0)
		 {
			 delta /= 15;
		 }*/

		 if (delta > 1)
		 {
			 digits=1;
		 }
		 else if (delta > .2)
		 {
			 digits=2;
		 }
		 else if (delta > .02)
		 {
			 digits=3;
		 }
		 else if (delta > .002)
		 {
			 digits=4;
		 }
		 else if (delta > .0002)
		 {
			 digits=5;
		 }
		 else
		 {
			 digits=6;
		 }
	 }

     NumberFormat nf= NumberFormat.getFormat("#.###");

	 for (int j=0; j < levels[i].length; j += 1)
	 {
	      value = levels[i][j];

	      if (_sexigesimal)
	      {
		   if (i == 0)  // ra label
		   {
			//sSharedCoords.setRa(value);
			//labels[offset] = sSharedCoords.raToString();
			//labels[offset] = String.valueOf(value);
			try {
			   labels[offset] = CoordUtil.dd2sex(value, false,
			                                     true, 3);
			}catch (CoordException cx) {
			   labels[offset] = nf.format(value);
			}
		   }
		   else
		   {
			//sSharedCoords.setDec(value);
			//labels[offset] = sSharedCoords.decToString();
			//labels[offset] = String.valueOf(value);
			try {
			   labels[offset] = CoordUtil.dd2sex(value, true, 
			                                     true, 3);
			}catch (CoordException cx) {
			   labels[offset] = nf.format(value);
			}
		   }
	      }
	      else
	      {
		   //labels[offset] = String.valueOf(value);
		   labels[offset] = nf.format(value);
	   }
	   offset += 1;
	 }
       }
       
       return labels;
       }



     protected double[][] getLevels(double[][] range) 
     {
	  /* Get the levels at which to compute the grid given
	  * the full range of the image.
	  */

	  double[][] levels = new double[range.length][];
	  double min, max;
	  double delta, qdelta=0;

	  for (int i = 0; i < range.length; i += 1)
	  {
	       /* Expect max and min for each dimension */
	       if (range[i].length != 2)
	       {
		       levels[i] = new double[0];
	       }
	       else
	       {
		    min = range[i][0];
		    max = range[i][1];

		    /* Handle special cases. */
		    if (min == max)
		    {
			    levels[i] = new double[0];
		    }
		    else if (Math.abs(min - -90.) < .1 &&
				     Math.abs(max -  90.) < .1)
		    {
			    levels[i]= new double[] {-75.,-60., -45. -30., -15., 0.,
			                             15., 30.,  45., 60.,  75.};

			    continue;
		    }
		    else
		    {
			    delta = max-min;
			    if (delta < 0)
			    {
				    delta += 360;
			    }

/*					if (_sexigesimal && i == 0)
			    {
				    delta /= 15;
			    }*/
			    if (delta > 1)  // more than 1 degree
			    {
				    qdelta = lookup(delta);
			    }
			    else if (60*delta > 1) // more than one arc minute
			    {
				    qdelta = lookup(60*delta)/60;
			    }
			    else if (3600*delta > 1) // more than one arc second
			    {
				    qdelta = lookup(3600*delta)/3600;
			    }
			    else
			    {
				 qdelta = Math.log(3600*delta)/Math.log(10);
				 qdelta = Math.pow(10.,Math.floor(qdelta));
			    }

                if (_userDefinedDistance &&
                        !(_minUserDistance < qdelta &&
                                qdelta < _maxUserDistance)){

                    //(_minUserDistance < qdelta ||
                    // _maxUserDistance > qdelta)) {
                    double minTry=
                            Math.abs(_minUserDistance-qdelta);
                    double maxTry=
                            Math.abs(_maxUserDistance-qdelta);
                    qdelta= (minTry<maxTry) ? _minUserDistance :
                            _maxUserDistance;
                }
            }
		    /*
		    if (_sexigesimal && i == 0)
		    {
			    qdelta *= 15;
		    }*/

		    /* We've now got the increment between levels.
		     * Now find all the levels themselves.
		     */

		    if (max < min)
		    {
			    min -= 360;
		    }

		    double val;
		    if (min < 0)
		    {
			    val = min - min%qdelta;
		    }
		    else
		    {
			    val = min + qdelta - min%qdelta ;
		    }


            int count=0;
		    while (val + count*qdelta < max)
		    {
			    count += 1;
		    }

            if (count<=2){
                  qdelta=qdelta/2.0;
                  count=2*count;
            }

            levels[i] = new double[count];
		    for (int j=0; j<count; j += 1)
		    {
			    levels[i][j] = j*qdelta + val;

			    if (i == 0  && levels[i][j] > 360)
			    {
				    levels[i][j] -= 360;
			    }
			    else if (i == 0 && levels[i][j] < 0)
			    {
				    levels[i][j] += 360;
			    }

		    }
	       }
	  }
	  return levels;
     }

   /** Get a reasonably delta value for the change in
    *  angle, the step size between grid.  We assume val is in degrees.
    *  We also take into account that zooming up makes the
    *  grid too sparse, so the zoom factor is taken when we decide the 
    *  step size between grid.
    *  after zooming, the step size is the original size devided by zoom
    *  factor.
    * @param val the value to lookup
    * @return the lookup value
    *
    */
     public double lookup(double val) 
     {
          double retval;


	  if (val < 1)
	  {
		  retval = val;
	  }
	  else if (val >= 270)
	  {
		  retval = 30;
	  }
	  else if (val > 180)
	  {
		  retval = 30;
	  }
	  else if (val > 90)
	  {
		  retval = 30;
	  }
	  else if (val > 60)
	  {
		  retval = 20;
	  }
	  else if (val > 30)
	  {
		  retval = 10;
	  }
	  else if (val > 23)
	  {	// This case handle full range of RA.
		  retval = 6;
	  }
	  else if (val > 18)
	  {
		  retval = 5;
	  }
	  else if (val > 6)
	  {
		  retval = 2;
	  }
	  else if (val > 3)
	  {
		  retval = 1;
	  }
	  else
	  {
		  retval = 0.5;
	  }

         //original - return retval/_factor;
         if (_factor >=4.0) retval = retval/2.0;
         return retval;

     }

     protected double[][] getRange()
     {
	  double[][] range={{0.,0.},{0.,0.}};
	  int poles=0;  // 0 = no poles, 1=north pole, 2=south pole, 3=both

	  /* Get the range of values for the grid. */

	  /* Check for the poles.  We allow the poles to
	  * be a pixel outside of the image and still consider
	  * them to be included.
	  */

	  boolean wrap = false;	/* Does the image wrap from 360-0. */

	  //System.out.println("coordSys: " + _sharedWp.getCoordSys());
      double sharedLon= 0.0;
      double sharedLat= 90.0;
	  if (_plot.pointInPlot(new WorldPt(sharedLon, sharedLat, _csys)))
	  {
		  range[0][0] = -179.999;
		  range[0][1] =  179.999;
		  range[1][1] = 90;
		  poles += 1;
		  wrap = true;
	  }

      sharedLon= 0.0;
      sharedLat= -90.0;
	  if (_plot.pointInPlot(new WorldPt(sharedLon, sharedLat, _csys)))
	  {
		  range[0][0] = -179.999;
		  range[0][1] =  179.999;
		  range[1][0] = -90;
		  poles += 2;
		  wrap = true;
	  }

	  /* If both poles are included we can just return */
	  if (poles == 3)
	  {
		  return range;
	  }

	  /* Now we have to go around the edges to find the remaining
	   * minima and maxima.
	   */

	  double[][] trange = edgeVals(1, wrap);
	  if (!wrap)
	  {

	       /* If we don't have a pole inside the map, check
		* to see if the image wraps around.  We do this
		* by checking to see if the point at [0, averageDec]
		* is in the map.
		*/

               sharedLon= 0.0;
               sharedLat= (trange[1][0] + trange[1][1])/2;
	       //_sharedWp.setValue(0.0, (trange[1][0] + trange[1][1])/2);
	       //wp = new WorldPt(0.0, (trange[1][0] + trange[1][1])/2);
	       //Pt xy = _plot.getImageCoords(wp);
	       //if (xy != null)
	       if (_plot.pointInPlot(new WorldPt(sharedLon, sharedLat, _csys)))
	       {
		       wrap = true;
			       
		       // Redo min/max
		       trange = edgeVals(1, wrap);
	       }
           else if (_csys.equals(CoordinateSys.GALACTIC)){
               trange=edgeVals(1);
               sharedLon = 0.0;
               sharedLat = (trange[1][0] + trange[1][1]) / 2;
               if (_plot.pointInPlot(new WorldPt(sharedLon, sharedLat, _csys))) {
                   wrap=true;
                   // Redo min/max
                   trange = edgeVals(1, wrap);
               }
           }



     }
	  
	  double[][] xrange = trange;
      int xmin= _plot.getPlotGroup().getGroupImageXMin();
      int adder=2;
	  for (int intervals = xmin+adder; 
                                  intervals < _dWidth; intervals+= adder)
	  {
		  xrange = edgeVals(intervals, wrap);
		  if (testEdge(xrange, trange))
		  {
			  break;
		  }
		  trange = xrange;
          adder*= 2;
	  }

	  if (poles == 0 && wrap)
	  {
		  xrange[0][0] += 360;
	  }
	  else if (poles == 1)
	  {
		  range[1][0] = xrange[1][0];
		  return range;
	  }
	  else if (poles == 2)
	  {
		  range[1][1] = xrange[1][1];
		  return range;
	  }

	  return xrange;
     }

    private double[][] edgeVals(int intervals) {

        double[][] range = {{1.e20, -1.e20},{1.e20, -1.e20}};


        int xmin= _plot.getPlotGroup().getGroupImageXMin();
        int ymin= _plot.getPlotGroup().getGroupImageYMin();
        int xmax= _plot.getPlotGroup().getGroupImageXMax();
        int ymax= _plot.getPlotGroup().getGroupImageYMax();
        double xdelta, ydelta, x, y;


        // four corners.
        // point a[xmin, ymax], the top left point, from here toward to right top point b[xmax, ymax], ie the line
        //   a-b
        y = ymax;
        x = xmin;
        xdelta = (_dWidth / intervals) - 1; //define an interval of the point in line a-b
        ydelta = 0; //no change in the y direction, so ydelta is 0, thus the points should be alone line a-b
        edgeRun(intervals, x, y, xdelta, ydelta, range);

        // Bottom: right to left
         y = ymin;
        x = xmax;
        xdelta = -xdelta;
        edgeRun(intervals, x, y, xdelta, ydelta, range);

        // Left.  Bottom to top.
        xdelta = 0;
        ydelta = (_dHeight / intervals) - 1;
        y = ymin;
        x = xmin;
        edgeRun(intervals, x, y, xdelta, ydelta, range);

        // Right. Top to bottom.
        ydelta = -ydelta;
        y = ymax;
        x = xmax;
        edgeRun(intervals, x, y, xdelta, ydelta, range);

        // grid in the middle
        xdelta = (_dWidth / intervals) - 1;
        ydelta = (_dHeight / intervals) - 1;
        x = xmin;
        y = ymin;
        edgeRun(intervals, x, y, xdelta, ydelta, range);


        return range;
    }

    private void edgeRun (int intervals, double x0, double y0,
                            double dx, double dy, double[][] range
    ) {

        double x = x0;
        double y = y0;

        int i = 0;
        double[] vals = new double[2];
        while (i <= intervals) {


            WorldPt c = _plot.getWorldCoords(new ImageWorkSpacePt(x,y), _csys);
            //LZ TODO debug info the if below
            //look for lower and upper longitude and latitude
            if (c != null) {
                vals[0] = c.getLon();
                vals[1] = c.getLat();

                if (_plot.pointInPlot(new WorldPt (vals[0], vals[1],  _csys))
                            || _csys.equals(CoordinateSys.EQ_B2000)
                            || _csys.equals(CoordinateSys.EQ_J2000) ) {
                    //assign the new lower and upper longitude if found
                    if (vals[0] < range[0][0]) range[0][0] = vals[0];
                    if (vals[0] > range[0][1]) range[0][1] = vals[0];

                    //assign the new lower and upper latitude if found
                    if (vals[1] < range[1][0]) range[1][0] = vals[1];
                    if (vals[1] > range[1][1]) range[1][1] = vals[1];
                }
                //TODO check if all the four range values are found, if not, redo the calculation again until all four are found
            }

            x += dx;
            y += dy;

            ++i;
        }

    }

    protected static boolean testEdge(double[][] xrange, double[][] trange)
     {
	  /* This routine checks if the experimental minima and maxima
	  * are significantly changed from the old test minima and
	  * maxima.  xrange and trange are assumed to be multidimensional
	  * extrema of the form double[ndim][2] with the minimum
	  * in the first element and the maximum in the second.
	  *
	  * Note that xrange is modified to have the most extreme
	  * value of the test or old set of data.
	  */

	  double[] delta = new double[trange.length];

	  /* Find the differences between the old data */
	  for (int i=0; i<trange.length; i+=1)
	  {
		  delta[i] = Math.abs(trange[i][1]-trange[i][0]);
	  }

	  for (int i=0; i<trange.length; i += 1)
	  {

	     double ndelta = Math.abs(xrange[i][1]-xrange[i][0]);

	     /* If both sets have nil ranges ignore this dimension */
	     if (ndelta <= 0. && delta[i] <= 0.)
	     {
	         continue;
	     }

	     /* If the old set was more extreme, use that value. */
	     if (xrange[i][0] > trange[i][0])
	     {
	       xrange[i][0] = trange[i][0];
	     }

	     if (xrange[i][1] < trange[i][1])
	     {
	       xrange[i][1] = trange[i][1];
	     }

	      /* If the previous range was 0 then we've got a
	* substantial change [but see above if both have nil range]
	*/
	     if (delta[i] == 0)
	     {
	         return false;
	     }

	     /* If the range has increased by more than 2% than */
	     if ( Math.abs(xrange[i][1]-xrange[i][0])/delta[i] > 
	                                                    RANGE_THRESHOLD)
	     {
	         return false;
	     }
	  }

	  return true;
     }


     protected double[][] edgeVals(int intervals, boolean wrap) 
     {
	  double[][] range = {{1.e20, -1.e20},{1.e20, -1.e20}};

          int xmin= _plot.getPlotGroup().getGroupImageXMin();
          int ymin= _plot.getPlotGroup().getGroupImageYMin();
          int xmax= _plot.getPlotGroup().getGroupImageXMax();
          int ymax= _plot.getPlotGroup().getGroupImageYMax();
	  double xdelta, ydelta, x, y;

	  // We change directions on this sides to ensure we do all
	  // four corners.

	  // Top: left to right
	  //System.out.println("edgeVals: intervals = " + intervals);
	  xdelta = (_dWidth / intervals) - 1;
	  ydelta = 0;
	  //y = _dHeight-0.5;   
	  //x = 0.5;
	  // make sure that the corner is right 
	  y = ymax;
	  x = xmin;
	  edgeRun(intervals, x, y, xdelta, ydelta, range, wrap);

	  // Bottom: right to left
	  //y = 0.5;
	  //x = _dWidth-0.5;
	  y = ymin;
	  x = xmax;
	  xdelta = -xdelta;
	  edgeRun(intervals, x, y, xdelta, ydelta, range, wrap);

	  // Left.  Bottom to top.
	  xdelta = 0;
	  ydelta = (_dHeight / intervals) - 1;
	  y = ymin;
	  x = xmin;
	  edgeRun(intervals, x, y, xdelta, ydelta, range, wrap);

	  // Right. Top to bottom.
	  ydelta = -ydelta;
	  y = ymax;
	  x = xmax;
	  edgeRun(intervals, x, y, xdelta, ydelta, range, wrap);

          // grid in the middle
	  xdelta = (_dWidth / intervals) - 1;
	  ydelta = (_dHeight / intervals) - 1;
	  x = xmin;
	  y = ymin;
	  edgeRun(intervals, x, y, xdelta, ydelta, range, wrap);


	  return range;
     }

     /**
     x0, y0,dx, dy all are the data values of the image, in image coordinate
     system. <br>
     range shoulde be in astro coordinate system. <br>
     */
     protected void edgeRun (int intervals, double x0, double y0,
			     double dx, double dy, double[][] range,
						     boolean wrap) 
     {
	  double x = x0;
	  double y = y0;
	  
	  int i = 0;
	  double[] vals = new double[2];
	  while (i <= intervals)
	  {

          WorldPt c = _plot.getWorldCoords(new ImageWorkSpacePt(x,y), _csys);
          if (c != null) {
              vals[0] = c.getLon();
              vals[1] = c.getLat();

              if (wrap && vals[0] > 180) vals[0] = vals[0]-360;

              if (vals[0] < range[0][0]) range[0][0] = vals[0];
              if (vals[0] > range[0][1]) range[0][1] = vals[0];
              if (vals[1] < range[1][0]) range[1][0] = vals[1];
              if (vals[1] > range[1][1]) range[1][1] = vals[1];
          }

          x += dx;
          y += dy;

          ++i;
	  }
     }

     /*
     x, y here are the values of lon, lat in degree
     */
     protected double[][] findLine(int coord, double value, double[][] range)
     {  
	  int intervals;
	  double x, dx, y, dy;
	  double[][] opoints, npoints;
	  boolean straight, nstraight;

	  if (coord == 0) // X
	  {
		  x  = value;
		  dx = 0;
		  y  = range[1][0];
		  dy = (range[1][1]-range[1][0])/4;
	  }
	  else // Y
	  {
		  y = value;
		  dy = 0;
		  x = range[0][0];
		  dx = (range[0][1]-range[0][0]);
		  if (dx < 0)	dx = dx+360;
		  dx /= 4;
	  }

	  opoints = findPoints(4, x, y, dx, dy, null);
	  straight = isStraight(opoints);

	  npoints = opoints;

	  intervals = 8;
	  //while (intervals < 2*_dWidth)
	  while (intervals < _screenWidth)
	  {
	       dx /= 2;
	       dy /= 2;
	       npoints = findPoints(intervals, x, y, dx, dy, opoints);
	       nstraight = isStraight(npoints);

	       if (straight && nstraight)
	       {
		       return fixPoints(npoints);
		       //return npoints;
	       }
	       straight = nstraight;
	       opoints = npoints;
	       intervals *= 2;
	  }

	  return fixPoints(npoints);
	  //return npoints;
     }

     protected static boolean isStraight(double[][] points) 
     {
	  /* This function returns a boolean value depending
	   * upon whether the points do not bend too rapidly.
   */

	  int len = points[0].length;
	  if (len < 3) return true;

	  double dx0, dx1, dy0, dy1, len0, len1;
	  double crossp;

	  dx1 = points[0][1]-points[0][0];
	  dy1 = points[1][1]-points[1][0];
	  len1 = (dx1*dx1) + (dy1*dy1);

	  for (int i=1; i < len-1; i += 1)
	  {

	       dx0 = dx1;
	       dy0 = dy1;
	       len0 = len1;

	       dx1 = points[0][i+1]-points[0][i];
	       dy1 = points[1][i+1]-points[1][i];
	       len1 = (dx1*dx1) + (dy1*dy1);
	       if (len0 == 0 || len1 == 0)
	       {
		       continue;
	       }

	       crossp = (dx0*dx1 + dy0*dy1);

	       double cos_sq = (crossp*crossp)/(len0*len1);
	       if (cos_sq == 0) return false;
	       if (cos_sq >= 1) continue;


	       double tan_sq = (1-cos_sq)/cos_sq;
	       if (tan_sq*(len0+len1) > 1)
	       {
		       return false;
	       }
	  }

	  return true;
     }

     protected double[][] findPoints(int intervals, double x0, double y0, 
				  double dx, double dy, double[][] opoints) 
     {
	  // NOTE: there are intervals separate intervals so there are
	  //                 intervals+1 separate points,
	  // Hence the <= in the for loops below.

	  ImagePt xy;
	  double[][] xpoints = new double[2][intervals+1];
	  int i, i0, di;

	  if (opoints != null)
	  {
	       i0 = 1;
	       di = 2;
	       for (i=0; i <= intervals; i += 2)
	       {
		    xpoints[0][i] = opoints[0][i/2];
		    xpoints[1][i] = opoints[1][i/2];
	       }
	  }
	  else
	  {
	       i0 = 0;
	       di = 1;
	  }

         double sharedLon;
         double sharedLat;
	  for (i=i0; i <= intervals; i += di)
	  {

		  double tx= x0+i*dx;
		  if (tx > 360)
		  {
			  tx -= 360;
		  }
		  if (tx < 0)
		  {
			  tx += 360;
		  }

                  sharedLon= tx;
                  sharedLat= y0+i*dy;
		  //_sharedWp.setValue(tx, y0+i*dy);
		  //wp = new WorldPt(tx, y0+i*dy);
		  //xy = _plot.getImageCoords(wp);
		  //if (xy == null)
          xy= null;
          //09/23/15 ?? don't understand this part, if the point is not in the plot,why bother to find it?
          if (!_plot.pointInPlot(new WorldPt(sharedLon, sharedLat, _csys))) {
              WorldPt wpt= new WorldPt(sharedLon, sharedLat, _csys);
              ImageWorkSpacePt ip = _plot.getImageWorkSpaceCoords(wpt);
              if (ip!=null) xy = new ImagePt(ip.getX(), ip.getY());
          }
          else {
              WorldPt wpt= new WorldPt(sharedLon, sharedLat, _csys);
              ImageWorkSpacePt ip = _plot.getImageWorkSpaceCoords(wpt);
              if (ip!=null) xy = new ImagePt(ip.getX(), ip.getY());
          }
          //comment by LZ 9/23/15
          //this line is wrong, if xy==null, xpoints and ypoints can not get value because it is null???
          if (xy==null)new ImagePt(1.e20,1.e20);// ?????? XW
		  xpoints[0][i] = xy.getX();
		  xpoints[1][i] = xy.getY();
	  }

	  return xpoints;
     }

     protected double[][] fixPoints(double[][] points) 
     {
	  // Convert points to fixed values.
	  
	  int len = points[0].length;
	  //int[][] ipt = new int[2][len];


       for (int i=0; i < len; i += 1)
       {
	 if (points[0][i] < 1.e10)
	 {

	      /*
	      ipt[0][i] = (int) Math.round(points[0][i] * _factor);
	      ipt[1][i] = (int) Math.round(
		 ((double)_dHeight - points[1][i]) * _factor);
	      //ipt[0][i] = (int) Math.round(points[0][i]);
	      //ipt[1][i] = (int) Math.round(points[1][i]);
	      */
	 }
	    else
	    {
		 points[0][i] = -10000;
		 points[1][i] = -10000;
	    }
       }

	  return points;
     }


    public static class Rectangle {
        public final int x;
        public final int y;
        public final int width;
        public final int height;
        public Rectangle(int x, int y, int width, int height) {
            this.x= x;
            this.y= y;
            this.width= width;
            this.height= height;
        }
    }
}

