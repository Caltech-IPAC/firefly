/**
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupWriter;
import edu.caltech.ipac.util.*;
import edu.caltech.ipac.firefly.data.Param;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author tatianag
 */
@SearchProcessorImpl(id = "XYGeneric")
public class XYGenericProcessor extends IpacTablePartProcessor {
    private static final String SEARCH_REQUEST = "searchRequest";
    private static final String ColExpKey = "ColOrExp";

    @Override
    protected File loadDataFile(TableServerRequest request) throws IOException, DataAccessException {
        String searchRequestJson = request.getParam(SEARCH_REQUEST);
        DataGroup dg = SearchRequestUtils.dataGroupFromSearchRequest(searchRequestJson);
        List<Param> allParams = request.getParams();
        ArrayList<XYWithErrorsProcessor.Col> colsLst = new ArrayList<>();
        ArrayList<TextCol> txtcolsLst = new ArrayList<>();

        // the output table columns with the names like x, y, z, r, t, values, labels, etc.
        DataType[] dataTypes = dg.getDataDefinitions();
        List<String> numericCols = DataObjectUtil.getNumericCols(dataTypes);

        for (Param p: allParams) {
            String name = p.getName();
            String colName;
            String val;
            XYWithErrorsProcessor.Col  aCol;
            TextCol tCol;

            if (name.endsWith(ColExpKey)) {
                colName = name.substring(0, name.length() - ColExpKey.length());
                val = p.getValue();

                if (stringContainsItemFrom(val, numericCols)) {
                    aCol = XYWithErrorsProcessor.getCol(dataTypes, val, colName, false);
                    colsLst.add(aCol);
                } else {
                    tCol = getTextCol(dataTypes, val, colName);
                    txtcolsLst.add(tCol);
                }
            }
        }

        XYWithErrorsProcessor.Col[] cols = colsLst.toArray(new XYWithErrorsProcessor.Col[colsLst.size()]);
        TextCol[] textcols = txtcolsLst.toArray(new TextCol[txtcolsLst.size()]);

        // create the array of output columns
        ArrayList<DataGroup.Attribute> colMeta = new ArrayList<>();
        ArrayList<DataType> columnList = new ArrayList<>();

        XYWithErrorsProcessor.createColumnsToList(columnList, dg, cols, colMeta);
        createColumnsOnTextCol(columnList, dg, textcols, colMeta);
        DataType columns [] = columnList.toArray(new DataType[columnList.size()]);

        // create the return data group
        DataGroup  retVal = new DataGroup("XY Generic", columns);
        DataObject retRow;
        String     formatted;
        DataType   dt;
        int        numCols = cols.length;

        for (int rIdx = 0; rIdx < dg.size(); rIdx++) {
            DataObject row = dg.get(rIdx);
            retRow = new DataObject(retVal);

            // render data from numeric columns
            for (int c = 0; c < numCols ; c++) {
                XYWithErrorsProcessor.Col col = cols[c];
                double val;

                val = col.getter.getValue(row);
                if (Double.isNaN(val) && !col.canBeNaN) {
                    retRow = null;
                    break;
                } else {
                    dt = columns[c];
                    formatted = col.getter.getFormattedValue(row);
                    if (formatted == null) {
                        retRow.setDataElement(dt, QueryUtil.convertData(dt.getDataType(), val));
                    } else {
                        retRow.setFormattedData(dt, formatted);
                    }
                }
            }

            // render data from text columns
            if (retRow != null && textcols.length > 0) {
                for (int c = 0; c < textcols.length; c++) {
                    TextCol tcol = textcols[c];
                    dt = columns[c + numCols];

                    formatted = tcol.getFormattedValue(row);
                    if (formatted == null) {
                        retRow.setDataElement(dt, tcol.getValue(row));
                    } else {
                        retRow.setFormattedData(dt, formatted);
                    }
                }
            }

            if (retRow != null) {
                retVal.add(retRow);
            }
        }

        for (XYWithErrorsProcessor.Col c : cols) {
            colMeta.add(new DataGroup.Attribute(c.exprColName, c.colOrExpr));
        }
        for (TextCol c : textcols) {
            colMeta.add(new DataGroup.Attribute(c.colname, c.colOrExpr));
        }
        retVal.setAttributes(colMeta);
        File outFile = createFile(request);
        DataGroupWriter.write(outFile, retVal);
        return outFile;
    }


    private boolean stringContainsItemFrom(String srcStr, List<String> strList) {
        for (String colStr : strList) {
            if (srcStr.contains(colStr)) {
                return true;
            }
        }
        return false;
    }

    private TextCol getTextCol(DataType[] dataTypes, String colOrExpr, String exprColName) throws DataAccessException {
        TextCol tCol = new TextCol(dataTypes, colOrExpr, exprColName);
        if (!tCol.isValid(dataTypes)) {
            throw new DataAccessException("Invalid column or expression: "+colOrExpr);
        }
        return tCol;
    }

    private static class TextCol {
        DataType col;
        String colname;
        String colOrExpr;

        TextCol(DataType[] dataTypes, String colOrExpr, String exprColName) {
            this.colOrExpr = colOrExpr;
            this.colname = exprColName;
            this.col = null;
            for (DataType dt : dataTypes) {
                if (dt.getKeyName().equals(colOrExpr)) {
                    this.col = dt;
                }
            }
        }

        boolean isValid(DataType[] dataTypes) {
            return (col != null);
        }

        String getValue(DataObject row) {
            if (col == null) {
                return "";
            }

            Object val = row.getDataElement(col);
            if (val instanceof String) {
                return (String) val;
            } else {
                try {
                    return String.valueOf(val);
                }
                catch (Exception ex)
                {
                    return "";
                }
            }
        }

        String getFormattedValue(DataObject row){
            return row.getFormatedData(col);
        }
    }

    private static void createColumnsOnTextCol(ArrayList<DataType>columnList, DataGroup dg,
                                                     TextCol[] textCols,
                                                     ArrayList<DataGroup.Attribute> colMeta) {
        DataType dt, dtDef;

        for (TextCol col : textCols) {
            dtDef = dg.getDataDefintion(col.colOrExpr);
            dt = dtDef.copyWithNoColumnIdx(columnList.size());
            dt.setMaxDataWidth(dtDef.getMaxDataWidth());
            dt.setFormatInfo(dtDef.getFormatInfo());
            columnList.add(dt);
            colMeta.addAll(IpacTableUtil.getAllColMeta(dg.getAttributes().values(), col.colOrExpr));
        }
    }

}

