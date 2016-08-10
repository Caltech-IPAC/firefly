/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util;

import edu.caltech.ipac.util.expr.Expression;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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


    public static List<String> getNumericCols(DataType[] dataTypes) {
        List<String> numericCols = new ArrayList<>();
        for (DataType dt : dataTypes) {
            Class type = dt.getDataType();
            if (type.equals(Double.class) ||
                    type.equals(Float.class) ||
                    type.equals(Long.class) ||
                    type.equals(Integer.class)) {
                numericCols.add(dt.getKeyName());
            }
        }
        return numericCols;
    }

    public static class DoubleValueGetter {
        DataType col;

        Expression colExpr = null;
        ArrayList<DataType> colDataTypes = null;

        public DoubleValueGetter(DataType[] dataTypes, String columnNameOrExpr) {
            col = getDataDefinition(dataTypes, columnNameOrExpr);
            if (col == null) {
                // column must be an expression
                colExpr = new Expression(columnNameOrExpr, getNumericCols(dataTypes));
                if (!colExpr.isValid()) {
                    System.out.println("invalid column \""+colExpr.getInput()+"\": "+colExpr.getErrorMessage());
                    colExpr = null;
                } else {
                    Set<String> vars = colExpr.getParsedVariables();
                    colDataTypes = new ArrayList<>(vars.size());
                    DataType dt;
                    for (String var : vars) {
                        dt = getDataDefinition(dataTypes, var);
                        // exclude predefined variables like pi or e
                        if (dt != null) {
                            colDataTypes.add(dt);
                        }
                    }
                }
            }
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
            return (col != null || colExpr != null);
        }

        public boolean isExpression() {
            return colExpr != null;
        }

        public double getValue(DataObject row) {
            double val;
            if (colExpr == null) {
                val = getDouble(row.getDataElement(col));
            } else {
                // col is an expression
                for (DataType dt : colDataTypes) {
                    colExpr.setVariableValue(dt.getKeyName(), getDouble(row.getDataElement(dt)));
                }
                val = colExpr.getValue();
            }
            return val;
        }

        public String getFormattedValue(DataObject row) {
            String val;
            if (colExpr == null) {
                val = row.getFormatedData(col);
            } else {
                val = null;
            }
            return val;
        }
    }
}
