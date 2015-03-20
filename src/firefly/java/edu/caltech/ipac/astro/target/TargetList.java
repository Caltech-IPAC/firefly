/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.astro.target;

import edu.caltech.ipac.util.Assert;

import java.io.Serializable;
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
public class TargetList implements Serializable, Iterable<Target> {


    private List<Target>          _list            = new LinkedList<Target>();
    private Target                _current         = null;
    private boolean               _doingBulkUpdates= false;


    public TargetList() { }

    public void beginBulkUpdate() { _doingBulkUpdates=true; }

    public void endBulkUpdate() {
        if(_doingBulkUpdates) {
            int idx=_list.size()-1;
            setCurrent((idx>-1) ?  _list.get(idx) : null);
        }
        _doingBulkUpdates=false;
    }

    public int size() { return _list.size(); }


    public Target get(int index) {
        return  _list.get(index);
    }

    public void setCurrent(int idx) {
        if (idx>-1 && idx<_list.size()) {
            Target t=( _list.get(idx));
            if(t!=null) setCurrent(t);
        }
    }

    public void setCurrent(Target t) {
        _current=t;
        if(t!=null && !_list.contains(t)) {
            _list.add(t);
        }
    }

    public Target getCurrent() {  return _current; }

    public Target getCurrentTarget() {
        return getCurrent();
    }

    public boolean contains(Target t) { return _list.contains(t); }

    public Iterator<Target> iterator() { return _list.iterator(); }



    public void remove(Target t) {
        Assert.tst(t, "TargetList.remove: Target must not be null");
        int line=_list.indexOf(t);
        if (line<0) return;
        Target newT=null;
        if((line+1)<_list.size()) newT=(Target) _list.get(line+1);
        if(line>-1) {
            _list.remove(t);
        }
    }

    public int indexOf(Object t) { return _list.indexOf((Target)t); }

    public int indexOf(Target t) { return indexOf((Object) t); }


    public void addTarget(Target target) {
        addTarget(target, true);
    }

    public void addTarget(Target t, boolean makeCurrent) {
        if(t!=null) {
            if(!contains(t)) {
                _list.add(t);
                if(!_doingBulkUpdates) {
                    //fireTableDataChanged();
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


}
