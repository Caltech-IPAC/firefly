package edu.caltech.ipac.astro.target;

import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.util.TableConnectionList;

import java.awt.datatransfer.Transferable;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


/**
 * Maintain a list of Targets for the program.  This is one of the central data
 * structure.  Many utilities will be build around this class.
 *
 * @author Trey Roby, Xiuqin Xu
 * @version $Id: TargetList.java,v 1.7 2006/07/08 00:18:03 tatianag Exp $
 */
public class TargetList implements Serializable,
                                   TableConnectionList,
                                   Iterable<Target> {


    private List<Target>          _list            = makeList();
    private Target                _current         = null;
    private Transferable          _transferable    = null;
    private boolean               _doingBulkUpdates= false;
    private PropertyChangeSupport _propChange=new PropertyChangeSupport(this);
    private   int                 _selectedIndexes[]= null;


    public TargetList() { }

    public void beginBulkUpdate() { _doingBulkUpdates=true; }

    public void endBulkUpdate() {
        if(_doingBulkUpdates) {
            _propChange.firePropertyChange(BULK_UPDATE, null, this);
            int idx=_list.size()-1;
            setCurrent((idx>-1) ? (Target) _list.get(idx) : null);
        }
        _doingBulkUpdates=false;
    }

    public int size() { return _list.size(); }


    public Target get(int index) {
        return (Target) _list.get(index);
    }





    public void setCurrent(int idx) {
        if (idx>-1 && idx<_list.size()) {
            Target t=((Target) _list.get(idx));
            if(t!=null) setCurrent(t);
        }
    }

    public void setCurrent(Target t) {
        Target oldCurrent=_current;
        _current=t;
        if(t!=null && !_list.contains(t)) {
            _list.add(t);
        }
        _propChange.firePropertyChange(CURRENT, oldCurrent, _current);
    }

    public Target getCurrent() {  return _current; }

    public Target getCurrentTarget() {
        return getCurrent();
    }

    public boolean contains(Target t) { return _list.contains(t); }

    public boolean containsName(String targetName) {
        boolean retval=false;
        for(Target t: _list) {
            if(targetName.equals(t.getName())) {
                retval=true;
                break;
            }
        }
        return retval;

    }

    public Iterator<Target> iterator() { return _list.iterator(); }

    public void deleteCurrent() {
        if(_current!=null) {
            int newIdx;
            int idx=_list.indexOf(_current);
            newIdx=(idx==0) ? 0 : idx-1;
            remove(_current);
            if(_list.size()>0)
                setCurrent(newIdx);
            else
                setCurrent(null);
        }
    }

    public void deleteAll() {
        beginBulkUpdate();
        _list.clear();
        _current = null;
        _selectedIndexes = null;
        endBulkUpdate();
    }


    public void remove(Target t) {
        Assert.tst(t, "TargetList.remove: Target must not be null");
        int line=_list.indexOf(t);
        if (line<0) return;
        Target newT=null;
        if((line+1)<_list.size()) newT=(Target) _list.get(line+1);
        if(line>-1) {
            _list.remove(t);
            if(!_doingBulkUpdates) {
                _propChange.firePropertyChange(REMOVE, t, newT);
            }
        }
    }

    public int indexOf(Object t) { return _list.indexOf((Target)t); }

    public int indexOf(Target t) { return indexOf((Object) t); }

    public int getCurrentIdx() {
        return ((_current==null) ? -1 : _list.indexOf(_current));
    }

    public void addTarget(Target target) {
        addTarget(target, true);
    }

    public void addTarget(Target t, boolean makeCurrent) {
        if(t!=null) {
            //cleanoutFlux(target);
            if(!contains(t)) {
                _list.add(t);
                if(!_doingBulkUpdates) {
                    //fireTableDataChanged();
                    _propChange.firePropertyChange(ADD, null, this);
                    if(makeCurrent) setCurrent(t);
                }
            }
            else {
                if(!_doingBulkUpdates && makeCurrent) {
                    if(makeCurrent) setCurrent(t);
                }
            }
        } // end if (target != null)
    }

    public void entryUpdated(Target t) {
        Assert.tst(t, "TargetList.entryUpdated: Target must not be null");
        int line=_list.indexOf(t);
        if(line>-1) {
            //fireTableRowsUpdated( line, line);
        }
        else {
            addTarget(t);
        }
        _propChange.firePropertyChange(ENTRY_UPDATED, null, _current);
    }

    /**
     * replace the orignal target with a new one
     * set current target to be the new target
     */
    public void replaceTarget(Target original, Target target) {
        replaceTarget(original, target, true);
    }

    /**
     * replace the orignal target with a new one
     * set current target to be the new target if makeCurrent is true
     */
    public void replaceTarget(Target original,
                              Target t,
                              boolean makeCurrent) {
        int index=_list.indexOf(original);
        Assert.tst(index>=0, "the orignal target does not exist!");
        _list.set(index, t);
        if(makeCurrent) setCurrent(t);
        _propChange.firePropertyChange(ALL_ENTRIES_UPDATED, null, t);
    }

    public List getTargetList() {
        return Collections.unmodifiableList(_list);
    }

    /**
     * Add a property changed listener.
     *
     * @param p the listener
     */
    public void addPropertyChangeListener(PropertyChangeListener p) {
        _propChange.addPropertyChangeListener(p);
    }

    /**
     * Remove a property changed listener.
     *
     * @param p the listener
     */
    public void removePropertyChangeListener(PropertyChangeListener p) {
        _propChange.removePropertyChangeListener(p);
    }

    protected List<Target> makeList() { return new LinkedList<Target>(); }

    public void setTransferable(Transferable t) { _transferable=t; }

    public Transferable getTransferable() { return _transferable; }


//   public static void cleanoutFlux(Target target) {
//       Iterator i= target.locationIterator();
//       Location l;
//       while (i.hasNext()) {
//          l= (Location)i.next();
//          SpitzerTargetUtil.setFluxEst(target,l,null);
//          SpitzerTargetUtil.setMaxFluxEst(target,l,null);
//       }
//   }

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
