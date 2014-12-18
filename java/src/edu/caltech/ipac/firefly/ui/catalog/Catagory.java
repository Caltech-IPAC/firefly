package edu.caltech.ipac.firefly.ui.catalog;

import edu.caltech.ipac.util.ComparisonUtil;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
/**
 * User: roby
 * Date: Oct 30, 2009
 * Time: 2:47:54 PM
 */


/**
 * @author Trey Roby
*/
public class Catagory implements Iterable<Catalog> {
    private String _name;
    private List<Catalog> _catalogs= new ArrayList<Catalog>(20);


    public Catagory(String name) { _name= name; }

    public String getCatagoryName() { return _name; }

    public void addCatalog(Catalog catalog) {
        _catalogs.add(catalog);
    }

    public boolean contains(Catalog catalog) { return _catalogs.contains(catalog); }
    public int indexOf(Catalog catalog) { return _catalogs.indexOf(catalog); }
    public Catalog get(int idx) { return _catalogs.get(idx); }
    public List<Catalog> getCatalogList() { return _catalogs; }

    public Iterator<Catalog> iterator() { return _catalogs.iterator(); }

    @Override
    public String toString() { return _name; };

    @Override
    public int hashCode() { return toString().hashCode(); }

    @Override
    public boolean equals(Object other) {
        boolean retval= false;
        if (other==this) {
            retval= true;
        }
        else if (other!=null && other instanceof Catagory) {
            Catagory p= (Catagory)other;
            if (ComparisonUtil.equals(_name,p._name)) {
                retval= true;
            }
        }
        return retval;
    }

}

