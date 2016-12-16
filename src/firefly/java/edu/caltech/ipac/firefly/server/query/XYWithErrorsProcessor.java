/**
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupWriter;
import edu.caltech.ipac.util.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;


/**
 * @author tatianag
 */
@SearchProcessorImpl(id = "XYWithErrors")
public class XYWithErrorsProcessor extends IpacTablePartProcessor {
    private static final String SEARCH_REQUEST = "searchRequest";
    private static final String X_COL_EXPR = "xColOrExpr";
    private static final String Y_COL_EXPR = "yColOrExpr";
    private static final String XERR_COL_EXPR = "xErrColOrExpr";
    private static final String XERR_LOW_COL_EXPR = "xErrLowColOrExpr";
    private static final String XERR_HIGH_COL_EXPR = "xErrHighColOrExpr";
    private static final String YERR_COL_EXPR = "yErrColOrExpr";
    private static final String YERR_LOW_COL_EXPR = "yErrLowColOrExpr";
    private static final String YERR_HIGH_COL_EXPR = "yErrHighColOrExpr";

    private static final Col NO_COL = new Col();

    @Override
    protected File loadDataFile(TableServerRequest request) throws IOException, DataAccessException {
        String searchRequestJson = request.getParam(SEARCH_REQUEST);
        DataGroup dg = SearchRequestUtils.dataGroupFromSearchRequest(searchRequestJson);
        String xColOrExpr = request.getParam(X_COL_EXPR);
        String yColOrExpr = request.getParam(Y_COL_EXPR);
        String xErrColOrExpr = request.getParam(XERR_COL_EXPR);
        String xErrLowColOrExpr = request.getParam(XERR_LOW_COL_EXPR);
        String xErrHighColOrExpr = request.getParam(XERR_HIGH_COL_EXPR);
        String yErrColOrExpr = request.getParam(YERR_COL_EXPR);
        String yErrLowColOrExpr = request.getParam(YERR_LOW_COL_EXPR);
        String yErrHighColOrExpr = request.getParam(YERR_HIGH_COL_EXPR);

        boolean hasXError = !StringUtils.isEmpty(xErrColOrExpr);
        boolean hasXLowError = !StringUtils.isEmpty(xErrLowColOrExpr);
        boolean hasXHighError = !StringUtils.isEmpty(xErrHighColOrExpr) ;
        boolean hasYError = !StringUtils.isEmpty(yErrColOrExpr);
        boolean hasYLowError = !StringUtils.isEmpty(yErrLowColOrExpr);
        boolean hasYHighError = !StringUtils.isEmpty(yErrHighColOrExpr);
        if ((hasXError || hasXLowError || hasXHighError || hasYError || hasYLowError || hasYHighError) && dg.size()>=QueryUtil.DECI_ENABLE_SIZE) {
            throw new DataAccessException("Errors for more than "+QueryUtil.DECI_ENABLE_SIZE+" are not supported");
        }

        // the output table columns: rowIdx, x, y, [[left, right], [low, high]]
        DataType[] dataTypes = dg.getDataDefinitions();

        // create the array of getters, which know how to get double values
        ArrayList<Col> colsLst = new ArrayList<>();
        colsLst.add(getCol(dataTypes, xColOrExpr,"x", false));
        colsLst.add(getCol(dataTypes, yColOrExpr, "y", false));

        if (hasXError) {
            colsLst.add(getCol(dataTypes, xErrColOrExpr, "xErr", true));
        }
        if (hasXLowError) {
            colsLst.add(getCol(dataTypes, xErrLowColOrExpr, "xErrLow", true));
        }
        if (hasXHighError) {
            colsLst.add(getCol(dataTypes, xErrHighColOrExpr, "xErrHigh", true));
        }
        if (hasYError) {
            colsLst.add(getCol(dataTypes, yErrColOrExpr, "yErr", true));
        }
        if (hasYLowError) {
            colsLst.add(getCol(dataTypes, yErrLowColOrExpr, "yErrLow", true));
        }
        if (hasYHighError) {
            colsLst.add(getCol(dataTypes, yErrHighColOrExpr, "yErrHigh", true));
        }

        Col[] cols = colsLst.toArray(new Col[colsLst.size()]);

        // create the array of output columns
        ArrayList<DataType> columnList = new ArrayList<>();
        columnList.add(new DataType("rowIdx", Integer.class));
        ArrayList<DataGroup.Attribute> colMeta = new ArrayList<>();
        for (Col col : cols) {
            if (col.getter.isExpression()) {
                columnList.add(new DataType(col.colname, col.colname, Double.class, DataType.Importance.HIGH, "", false));
            } else {
                columnList.add(dg.getDataDefintion(col.colname).copyWithNoColumnIdx(columnList.size()));
                colMeta.addAll(IpacTableUtil.getAllColMeta(dg.getAttributes().values(), col.colname));
            }
        }
        DataType columns [] = columnList.toArray(new DataType[columnList.size()]);

        // create the return data group
        DataGroup retval = new DataGroup("XY with Errors", columns);

        DataObject retrow;
        int ncols = cols.length;
        Col col;
        DataType dt;
        double val;
        String formatted;
        DataType dtRowIdx = columns[0];

        for (int rIdx = 0; rIdx < dg.size(); rIdx++) {
            DataObject row = dg.get(rIdx);
            retrow = new DataObject(retval);
            for (int c = 0; c < ncols ; c++) {
                col = cols[c];
                val = col.getter.getValue(row);
                if (Double.isNaN(val) && !col.canBeNaN) {
                    retrow = null;
                    break;
                } else {
                    dt = columns[c+1];
                    formatted = col.getter.getFormattedValue(row);
                    if (formatted == null) {
                        retrow.setDataElement(dt, QueryUtil.convertData(dt.getDataType(), val));
                    } else {
                        retrow.setFormattedData(dt, formatted);
                    }
                }
            }
            if (retrow != null) {
                retrow.setDataElement(dtRowIdx, rIdx);
                retval.add(retrow);
            }
        }

        for (Col c : cols) {
            colMeta.add(new DataGroup.Attribute(c.exprColName, c.colOrExpr));
        }
        retval.setAttributes(colMeta);

        retval.shrinkToFitData();
        File outFile = createFile(request);
        DataGroupWriter.write(outFile, retval);
        return outFile;
    }

    private Col getCol(DataType[] dataTypes, String colOrExpr, String exprColName, boolean canBeNaN) throws DataAccessException {
        Col col = new Col(dataTypes, colOrExpr, exprColName, canBeNaN);
        if (!col.getter.isValid()) {
            throw new DataAccessException("Invalid column or expression: "+colOrExpr);
        }
        return col;
    }


    private static class Col {
        DataObjectUtil.DoubleValueGetter getter;
        String colname;
        String exprColName;
        String colOrExpr;
        boolean canBeNaN;

        Col() {
            this.canBeNaN = true;
        }

        Col(DataType[] dataTypes, String colOrExpr, String exprColName, boolean canBeNaN) {
            this.colOrExpr = colOrExpr;
            this.exprColName = exprColName;
            this.getter = new DataObjectUtil.DoubleValueGetter(dataTypes, colOrExpr);
            if (getter.isExpression()) {
                this.colname = exprColName;
            } else {
                this.colname = colOrExpr;
            }
            this.canBeNaN = canBeNaN;
        }
    }
}

