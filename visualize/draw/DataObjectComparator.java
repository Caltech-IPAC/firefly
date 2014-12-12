package edu.caltech.ipac.visualize.draw;

import edu.caltech.ipac.gui.table.SortInfo;
import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.util.ComparisonUtil;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.DataObject;

import java.util.List;
import java.util.Comparator;


class DataObjectComparator implements Comparator {

   private int            _sortCol= FixedObjectGroup.ENABLED_IDX;
   private int            _sortDir= SortInfo.SORTED_ASCENDING;
   private List           _objects;
   private DataType  _extraData[];

   public DataObjectComparator(List objects, DataType extraData[]) {
        _objects= objects;
        _extraData= extraData;
   }
   
   public void setSortColumn(int c) { _sortCol= c;}
   public int  getSortColumn()      { return _sortCol;}
   public void setSortDir(int d)     { 
         Assert.tst( _sortDir == SortInfo.SORTED_ASCENDING ||
                     _sortDir == SortInfo.SORTED_DECENDING );
         _sortDir= d;
   }
   public int  getSortDir()      { return _sortDir;}
   public void reverseSortDir() {
         _sortDir=  (_sortDir == SortInfo.SORTED_ASCENDING) ?
                             SortInfo.SORTED_DECENDING :
                             SortInfo.SORTED_ASCENDING;
   }


    public int compare(Object o1, Object o2) {
        int retval= 0;
        Integer int1= (Integer)o1;
        Integer int2= (Integer)o2;
        DataObject fo1= (DataObject)_objects.get( int1.intValue() );
        DataObject fo2= (DataObject)_objects.get( int2.intValue() );
        DataType fdt= _extraData[ FixedObjectGroup.BASE_NUM_COLUMNS];
        Object ed1= fo1.getDataElement(fdt);
        Object ed2= fo2.getDataElement(fdt);
        if (ed1 instanceof Number && ed2 instanceof Number) {
            retval= ComparisonUtil.doCompare(
                           (Number)ed1,(Number)ed2);
        }
        else {
            String s1= (ed1!=null) ? ed1.toString() : null;
            String s2= (ed2!=null) ? ed2.toString() : null;
            retval= ComparisonUtil.doCompare(s1,s2);
        }

        if (_sortDir==SortInfo.SORTED_DECENDING) retval*= -1;
        return retval;
    }

   public boolean equals(Object obj) { return false; }
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
