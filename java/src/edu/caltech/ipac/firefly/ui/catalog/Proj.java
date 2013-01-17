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
//    private String _fullName;
    private List<Catagory> _catagories= new ArrayList<Catagory>();

    public Proj(String shortName) {
        _shortName= shortName;
//        _fullName= fullName;
    }

    public String getShortProjName() { return _shortName; }
//    public String getFullProjName() { return _fullName; }

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
