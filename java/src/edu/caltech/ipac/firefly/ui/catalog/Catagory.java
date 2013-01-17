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
