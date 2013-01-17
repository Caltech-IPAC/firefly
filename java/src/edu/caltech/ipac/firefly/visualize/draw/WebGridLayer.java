package edu.caltech.ipac.firefly.visualize.draw;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.Preferences;
import edu.caltech.ipac.firefly.resbundle.images.VisIconCreator;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.MaskPane;
import edu.caltech.ipac.firefly.ui.input.SimpleInputField;
import edu.caltech.ipac.firefly.util.PropertyChangeListener;
import edu.caltech.ipac.firefly.util.PropertyChangeSupport;
import edu.caltech.ipac.firefly.util.WebAssert;
import edu.caltech.ipac.firefly.util.WebClassProperties;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.visualize.PrintableOverlay;
import edu.caltech.ipac.firefly.visualize.ReplotDetails;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotView;
import edu.caltech.ipac.firefly.visualize.ui.GridOptionsPopup;
import edu.caltech.ipac.visualize.plot.CoordinateSys;

import java.util.HashMap;
import java.util.Map;

/**
 * using the Grid class to draw grid on the Plot
 *
 * @version $Id: WebGridLayer.java,v 1.14 2012/11/21 21:12:43 roby Exp $
 * @author		Xiuqin Wu
**/
public class WebGridLayer implements WebEventListener{


    public static final String GRID_EQ_J2000 = "equJ2000";
    public static final String GRID_EQ_J2000D= "equJ2000D";
    public static final String GRID_EQ_B1950 = "equB1950";
    public static final String GRID_EQ_B1950D= "equB1950D";
    public static final String GRID_ECL_J2000= "eclJ2000";
    public static final String GRID_ECL_B1950= "eclB1950";
    public static final String GRID_GAL      = "gal";
    public static final String GRID_SUPERG   = "superG";
    public static final String GRID_NONE     = "none";




    public static final String DRAWER_ID="GridID";
   public static final String COORD_SYSTEM         = "CoordSystem";
    public static final String COORD_PREF= "WebGridLayer.coordSys";
    private static WebClassProperties _prop= new WebClassProperties(GridOptionsPopup.class);
    private static final ShowOnce _showOnce= new ShowOnce();

     /*  the coordinate system that user wants the Grid to be drawn 
	 It has to be set to the constants defined in Plot
      */
     private CoordinateSys         _csys;
    private String         _csysStr;
    private WebGrid _grid;
    private WebPlotView _pv;
    private Map<WebPlot,PlotInfo>    _plotMap   = new HashMap<WebPlot,PlotInfo>(20);
    private boolean               _showing   = false;
    private PropertyChangeSupport _propChange= new PropertyChangeSupport(this);
    private final WebLayerItem _layer;

	/**
	 * Creates a new GridLayer.
     * @param pv the WebPlotView this WebGridLayer is on
	**/
   public WebGridLayer(WebPlotView pv, PrintableOverlay printableOverlay) {
        super();
        _grid = new WebGrid(pv, CoordinateSys.EQ_J2000);
        setCoordSystem(Preferences.get(COORD_PREF,"equJ2000"));
        _pv= pv;
        _grid.setSexigesimalLabel(
                (_csys.equals(CoordinateSys.EQ_J2000) || _csys.equals(CoordinateSys.EQ_B1950)));
        pv.getEventManager().addListener(Name.REPLOT, this);
        pv.getEventManager().addListener(Name.PRIMARY_PLOT_CHANGE, this);

       if (!WebLayerItem.hasUICreator(DRAWER_ID)) {
           WebLayerItem.addUICreator(DRAWER_ID,new GridLayerUIMaker());
       }
       _layer= new WebLayerItem(DRAWER_ID, "Grid", null, _grid.getDrawer(),null,null,printableOverlay);
       _layer.setWorkerObj(this);
   }



    public void setCoordSystem(String coordSys) {
      boolean sexigesimalLabel = false;

      if (coordSys.equals(GRID_EQ_J2000)) {
         _csys = CoordinateSys.EQ_J2000;
         sexigesimalLabel = true;
         }
      else if (coordSys.equals(GRID_EQ_J2000D))
         _csys = CoordinateSys.EQ_J2000;
      else if (coordSys.equals(GRID_ECL_B1950)) {
         _csys = CoordinateSys.EQ_B1950;
         sexigesimalLabel = true;
         }
      else if (coordSys.equals(GRID_EQ_B1950))
         _csys = CoordinateSys.EQ_B1950;
      else if (coordSys.equals(GRID_ECL_J2000))
         _csys = CoordinateSys.ECL_J2000;
      else if (coordSys.equals(GRID_ECL_B1950))
         _csys = CoordinateSys.ECL_B1950;
      else if (coordSys.equals(GRID_GAL))
         _csys = CoordinateSys.GALACTIC;
      else if (coordSys.equals(GRID_SUPERG))
         _csys = CoordinateSys.SUPERGALACTIC;
      else
         WebAssert.tst(false, coordSys);

      _csysStr= coordSys;
      _grid.setCoordSystem(_csys);
      _grid.setSexigesimalLabel(sexigesimalLabel);
      _propChange.firePropertyChange ( COORD_SYSTEM, null, coordSys);

      Preferences.set(COORD_PREF,coordSys);
      if (_showing) _grid.paint();
   }

    public String getCoordSystem() {
        return _csysStr;
    }

   public WebGrid getGrid() {
      return _grid;
   }

   public void setShowing(boolean show) {
       setShowing(show,true);
   }

   public void setShowing(boolean show, boolean showHelp) {
       if (_showing!=show) {
           _showing = show;
           if (show) {
               _pv.addWebLayerItem(_layer);
               _showing= _grid.paint();

               if (showHelp) {
                   VisIconCreator ic= VisIconCreator.Creator.getInstance();
                   Image im= new Image(ic.getLayerBright());
                   HorizontalPanel hp= new HorizontalPanel();
                   hp.add(im);
                   hp.add(new HTML("To change coordinate system or color click on the layer button"));
                   _showOnce.message(_pv, hp);
               }
           }
           else {
               _pv.removeWebLayerItem(_layer);
               _grid.clear();
           }
           _pv.getEventManager().fireEvent(new WebEvent(this,Name.GRID_ANNOTATION));

       }

   }

    public void setGridColor(String c) {
        _grid.setGridColor(c);
        if (_showing) _grid.paint();
    }

    public String getGridColor() {
        return _grid.getGridColor();
    }

    public boolean isShowing() { return _showing; }

    public void drawOnPlot(WebPlot p){
    }


    public boolean isOnAnyPlot() {
        return (_plotMap.size()>0);
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


   // ===================================================================
   // ---------  Methods from WebEventListener Interface---------------
   // ===================================================================

   public void eventNotify(WebEvent ev) {
       if (ev.getName().equals(Name.REPLOT)) {
           ReplotDetails details= (ReplotDetails)ev.getData();
           ReplotDetails.Reason reason= details.getReplotReason();
           if (reason==ReplotDetails.Reason.PLOT_ADDED ||
                   reason==ReplotDetails.Reason.ZOOM) {
               if (_showing) paint();
           }
       }
       else if (ev.getName().equals(Name.PRIMARY_PLOT_CHANGE)) {
           if (_showing && _pv.getPrimaryPlot()!=null) paint();
       }
   }


    private void paint() {
        Runnable painter= new Runnable() {
            public void run() {
                _grid.paint();
            }
        };
        GwtUtil.maskAndExecute("Grid...", _pv.getMaskWidget(),
                     MaskPane.MaskHint.OnComponent, painter);

    }


//===================================================================
//------------------------- Private Inner classes -------------------
//===================================================================

    private static class PlotInfo {
       public int  _csys_int;
       PlotInfo( ) {
        }

    }


    private class GridLayerUIMaker implements WebLayerItem.UICreator {

        private final SimpleInputField _coordSys= SimpleInputField.createByProp(_prop.makeBase("coordSystem"));

        public Widget makeExtraUI(WebLayerItem item) {
            _coordSys.getField().addValueChangeHandler(new ValueChangeHandler<String>() {
                public void onValueChange(ValueChangeEvent ev) {
                    changeAllGridCoords();
                }
            });

            String csysStr= getCoordSystem();
            _coordSys.setValue(csysStr);
            return _coordSys;
        }

        private void changeAllGridCoords() {
            String value= _coordSys.getValue();
            for(WebLayerItem wl : WebLayerItem.getAllWithMatchingID(DRAWER_ID)) {
                ((WebGridLayer)wl.getWorkerObj()).setCoordSystem(value);
            }
        }

    }



    public static class ShowOnce {
        private boolean _shown= false;
        private WebPlotView _pv;
        private Widget _msg;

        public void message(WebPlotView pv, Widget msg) {
            _pv= pv;
            _msg= msg;
            if (!_shown) {
                DeferredCommand.addCommand(new Command() {
                    public void execute() {
                        _pv.showMouseHelp(_msg);
                        _shown= false;
                    }
                });
            }
            _shown= true;
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
