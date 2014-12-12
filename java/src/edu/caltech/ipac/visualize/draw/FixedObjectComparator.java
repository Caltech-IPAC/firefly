package edu.caltech.ipac.visualize.draw;

import edu.caltech.ipac.gui.table.SortInfo;
import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.util.ComparisonUtil;
import edu.caltech.ipac.util.DataType;

import java.util.Comparator;


class FixedObjectComparator implements Comparator<Integer> {

   private int            _sortCol= FixedObjectGroup.ENABLED_IDX;
   private int            _sortDir= SortInfo.SORTED_ASCENDING;
   private final FixedObjectGroup _group;

   public FixedObjectComparator(FixedObjectGroup group) {
        _group= group;
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
       
   
   public int compare(Integer int1, Integer int2) {
      int retval= 0;
      FixedObject fo1= _group.get( int1 );
      FixedObject fo2= _group.get( int2 );
      switch (_sortCol) {
          case FixedObjectGroup.ENABLED_IDX   :
              retval= ComparisonUtil.doCompare(fo1.isEnabled(),
                                               fo2.isEnabled() );
              break;
          case FixedObjectGroup.HILIGHT_IDX   :
              retval= ComparisonUtil.doCompare(fo1.isHiLighted(),
                                               fo2.isHiLighted() );
              break;
          case FixedObjectGroup.SHOW_NAME_IDX :
              retval= ComparisonUtil.doCompare(fo1.getShowName(),
                                               fo2.getShowName());
              break;
          case FixedObjectGroup.SHAPE_IDX  :
              retval= 0;
              //retval= compareShapes(fo1.getSkyShape(),
              //                      fo2.getSkyShape() );
              break;
          case FixedObjectGroup.TNAME_IDX     :
              retval= fo1.getTargetName().compareTo(
                             fo1.getTargetName() );
              break;
          case FixedObjectGroup.USER_RA_IDX   :
              retval= ComparisonUtil.doCompare(fo1.getPosition().getLon(),
                                               fo2.getPosition().getLon() );
              break;
          case FixedObjectGroup.USER_DEC_IDX  :
              retval= ComparisonUtil.doCompare(fo1.getPosition().getLat(),
                                               fo2.getPosition().getLat() );
              break;
          default:
              Assert.tst(_sortCol >= FixedObjectGroup.BASE_NUM_COLUMNS);
//                     String keyName=
//                        _extraData[_sortCol-
//                           FixedObjectGroup.BASE_NUM_COLUMNS].getKeyName();

              DataType fdt= _group.getExtraDataElement(_sortCol-
                                            FixedObjectGroup.BASE_NUM_COLUMNS);
              Object ed1= fo1.getExtraData(fdt);
              Object ed2= fo2.getExtraData(fdt);
              if (ed1 instanceof Number && ed2 instanceof Number) {
                  retval= ComparisonUtil.doCompare(
                                 (Number)ed1,(Number)ed2);
              }
              else {
                  String s1= (ed1!=null) ? ed1.toString() : null;
                  String s2= (ed2!=null) ? ed2.toString() : null;
                  retval= ComparisonUtil.doCompare(s1,s2);
              }
              break;

      } // end switch
      if (_sortDir==SortInfo.SORTED_DECENDING) retval*= -1;
      return retval; 
   }
 
//   private int doCompare(boolean v1, boolean v2) {
//     int retval= 0;
//     if      (!v1 && v2) retval= -1;
//     else if (v1 && !v2) retval=  1;
//     return retval;
//   }
//
//   private int doCompare(float v1, float v2) {
//     int retval= 0;
//     if      (v1 < v2) retval= -1;
//     else if (v1 > v2) retval=  1;
//     return retval;
//   }
//
//   private int doCompare(double v1, double v2) {
//     int retval= 0;
//     if      (v1 < v2) retval= -1;
//     else if (v1 > v2) retval=  1;
//     return retval;
//   }
//
//   private int doCompare(int v1, int v2) {
//     int retval= 0;
//     if      (v1 < v2) retval= -1;
//     else if (v1 > v2) retval=  1;
//     return retval;
//   }
//
//   private int doCompare(Number n1, Number n2) {
//      int retval= 0;
//      Assert.tst(n1.getClass() == n2.getClass());
//      if      (n1 instanceof Double)
//                retval= doCompare(n1.doubleValue(), n2.doubleValue() );
//      else if (n1 instanceof Float)
//                retval= doCompare(n1.floatValue(),  n2.floatValue() );
//      else if (n1 instanceof Long)
//                retval= doCompare(n1.longValue(),   n2.longValue() );
//      else if (n1 instanceof Integer)
//                retval= doCompare(n1.intValue(),    n2.intValue() );
//      else if (n1 instanceof Byte)
//                retval= doCompare(n1.byteValue(),   n2.byteValue() );
//      else if (n1 instanceof Short)
//                retval= doCompare(n1.shortValue(),  n2.shortValue() );
//      else
//                Assert.stop();
//
//      return retval;
//   }

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
