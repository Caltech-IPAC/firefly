/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util;

import edu.caltech.ipac.table.DataObject;
import edu.caltech.ipac.table.DataType;

/**
 * @author tatianag
 */
public class DataObjectUtil {

    /**
    * return Double.NaN if val is null or not a double
    * @param val value
    * @return double
    */
   public static double getDouble(Object val) {
       if (val != null) {
           if (val instanceof Double) {
               return (Double) val;
           } else if (val instanceof Number) {
               return ((Number)val).doubleValue();
           } else {
               try {
                   return Double.parseDouble(String.valueOf(val));
               } catch(NumberFormatException ex) {}
           }
       }
       return Double.NaN;

   }

    public static class DoubleValueGetter {
        DataType col;


        public DoubleValueGetter(DataType[] dataTypes, String columnNameOrExpr) {
            col = getDataDefinition(dataTypes, columnNameOrExpr);
        }

        private static DataType getDataDefinition(DataType[] dataTypes, String key) {
            for (DataType dt : dataTypes) {
                if (dt.getKeyName().equals(key)) {
                    return dt;
                }
            }
            return null;

        }

        public boolean isValid() {
            return (col != null);
        }

        public boolean isExpression() {
            return false;
        }

        public double getValue(DataObject row) {
            return getDouble(row.getDataElement(col));
        }

        public String getFormattedValue(DataObject row) {
            return row.getFixedFormatedData(col);
        }
    }
}
