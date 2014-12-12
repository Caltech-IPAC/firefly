package edu.caltech.ipac.visualize.draw;


import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.util.TableConnectionList;
import edu.caltech.ipac.visualize.plot.PlotView;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Maintain a list of AORs for the program.  This is the central data 
 * structure.  Most utilities will be build around this class.
 *
 * @author Trey Roby
 * @version $Id: PlotBoundaryList.java,v 1.2 2007/05/01 23:08:11 booth Exp $
 *
 */
public class PlotBoundaryList implements TableConnectionList,
                                 Iterable<PlotBoundaryElement> {


//======================================================================
//----------------------- private / protected variables ----------------
//======================================================================
    private   PropertyChangeSupport _propChange= new PropertyChangeSupport(this);
    protected List<PlotBoundaryElement>  _list = newList();
//    protected boolean             _doingBulkUpdates= false;
    private PlotBoundaryElement _current= null;

//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public PlotBoundaryList() { }


//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    /**
     * Begin a bulk update.  A bulk update is when many updates are
     * done without any of the listeners being called.  This allows
     * for much more efficient processing when making large changes.
     */
    //public void beginBulkUpdate() { _doingBulkUpdates = true; }

    /**
     * End a bulk update.  A bulk update is when many updates are
     * done without any of the listeners being called.  This allows
     * for much more efficent processing when making large changes.
     * When this method is called it fires a PropertyChange event.
     * Listeners who watch other update property change event should 
     * also watch the BULK_UPDATE event.
     */
//    public void endBulkUpdate()   {
//        if (_doingBulkUpdates) {
//            _propChange.firePropertyChange ( BULK_UPDATE, null, this);
//        }
//        _doingBulkUpdates= false;
//        updateAllSavedStatus();
//    }

    /**
     */
    public void add(PlotBoundaryElement element)    {
        if (element != null) {
            _list.add(element);
            //if (!_doingBulkUpdates) {
            firePropertyChange ( ADD, null, element);
            //}
        }
    }


    public void setPlotView(PlotView pv) {
        for(PlotBoundaryElement e : _list) e.setPlotView(pv);
    }

    /**
     */
    public void remove(PlotBoundaryElement element) throws PropertyVetoException {
        Assert.tst(element, "ProcessingFlow must not be null");
        PlotBoundaryElement newElement= null;
        int line= _list.indexOf(element);
        if ( (line+1) < _list.size()) newElement= _list.get(line+1);
        if (line > -1) {
             boolean noCurrent= (element == _current);
             _list.remove(element);
             //if (!_doingBulkUpdates) {
             firePropertyChange ( REMOVE, element, newElement);
             //}
             if (noCurrent) setCurrent(null);
        }
    }

    /**
     * Delete all the AOR in the AORList.  This method will do a bulk update.
     * Therefore the only property change that will occur is BULK_UPDATE
     */
    public void deleteAll() {
       //beginBulkUpdate();
       _list.clear();
       _current= null;
       //endBulkUpdate();
    }


    public void setCurrent(PlotBoundaryElement element) {
        PlotBoundaryElement old= _current;
        if (old != element) {
            _current= element;
            firePropertyChange ( CURRENT, old, _current);
        }
    }

    /**
     * Set an AOR index in the list to be current.  The method will fire the
     * CURRENT property change event.  If the AOR index is not in the list
     * then the will be no current AOR.
     * @param idx the index of the aor to become current.
     */
    public void setCurrent(int idx) {
          setCurrent(_list.get(idx));
    }

    /**
     * Return the current AOR.
     */
    public PlotBoundaryElement  getCurrent() { return _current; }


    public int indexOf(PlotBoundaryElement element) { return indexOf(element); }


    /**
     */
    public PlotBoundaryElement get(int idx)     { return _list.get(idx); }


    /**
     */
    public Iterator<PlotBoundaryElement> iterator() { return _list.iterator(); }


//=======================================================================
//-------------- Methods from TableConnectionList Interface -------------
//--------------  note - addPropertyChangeListener is also in this 
//---------------       interfacce put define with the add/remove listeners
//---------------       listeners section
//=======================================================================

    public int size()            { return _list.size(); }
    public int indexOf(Object o) { return _list.indexOf((PlotBoundaryElement)o); }

//=====================================================================
//----------- add / remove property Change listener methods -----------
//=====================================================================

    /**
     * Add a property changed listener.
     * @param p  the PropertyChangeListener
     */
    public void addPropertyChangeListener (PropertyChangeListener p) {
       _propChange.addPropertyChangeListener (p);
    }

    /**
     * Remove a property changed listener.
     * @param p  the PropertyChangeListener
     */
    public void removePropertyChangeListener (PropertyChangeListener p) {
       _propChange.removePropertyChangeListener (p);
    }

    protected void firePropertyChange(String reason,
                                      Object oldV,
                                      Object newV) {
      //computeCurrentSaveFile();
      _propChange.firePropertyChange( reason, oldV, newV);
    }

    
  // ================================================================
  // -------------------- Factory Methods ---------------------------
  // ================================================================

    protected List<PlotBoundaryElement> newList() {
        return new ArrayList<PlotBoundaryElement>(10);
    }

    protected Set  newSet()  { return new HashSet(10); }



}
/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED ?AS-IS? TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
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
