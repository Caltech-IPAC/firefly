package edu.caltech.ipac.firefly.ui.catalog;

import edu.caltech.ipac.util.ComparisonUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
/**
 * User: roby
 * Date: Oct 30, 2009
 * Time: 2:47:22 PM
 */


/**
 * @author Trey Roby
*/
public class Proj implements Iterable<Catagory> {
    private String _shortName;
    private List<Catagory> _catagories= new ArrayList<Catagory>();

    public Proj(String shortName) {
        _shortName= shortName;
    }

    public String getShortProjName() { return _shortName; }

    public void addCatagory(Catagory catagory) {
        _catagories.add(catagory);
    }

    public boolean contains(Catagory catagory) { return _catagories.contains(catagory); }
    public int indexOf(Catagory catagory) { return _catagories.indexOf(catagory); }
    public Catagory get(int idx) { return _catagories.get(idx); }
    public Iterator<Catagory> iterator() { return _catagories.iterator(); }
    public int getCatagoryCount() { return _catagories.size(); }

    @Override
//    public String toString() { return _fullName +":" + _shortName; }
    public String toString() { return _shortName; }

    @Override
    public boolean equals(Object other) {
        boolean retval= false;
        if (other==this) {
            retval= true;
        }
        else if (other!=null && other instanceof Proj) {
            Proj p= (Proj)other;
            if (ComparisonUtil.equals(_shortName,p._shortName)) {
                retval= true;
            }
        }
        return retval;
    }

    @Override
    public int hashCode() { return toString().hashCode(); }
}

