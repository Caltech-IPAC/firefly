/**
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.data.SortInfo;
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
    private static final String COLOR_COL_EXPR = "colorColOrExpr";
    private static final String SIZE_COL_EXPR = "sizeColOrExpr";
    private static final String SORT_COL_OR_EXPR = "sortColOrExpr";


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
        String colorColOrExpr = request.getParam(COLOR_COL_EXPR);
        String sizeColOrExpr = request.getParam(SIZE_COL_EXPR);
        String sortColOrExpr = request.getParam(SORT_COL_OR_EXPR);

        boolean hasXError = !StringUtils.isEmpty(xErrColOrExpr);
        boolean hasXLowError = !StringUtils.isEmpty(xErrLowColOrExpr);
        boolean hasXHighError = !StringUtils.isEmpty(xErrHighColOrExpr) ;
        boolean hasYError = !StringUtils.isEmpty(yErrColOrExpr);
        boolean hasYLowError = !StringUtils.isEmpty(yErrLowColOrExpr);
        boolean hasYHighError = !StringUtils.isEmpty(yErrHighColOrExpr);
        boolean hasSortCol = !StringUtils.isEmpty(sortColOrExpr);
        if ((hasXError || hasXLowError || hasXHighError || hasYError || hasYLowError || hasYHighError) && dg.size()>=QueryUtil.DECI_ENABLE_SIZE) {
            throw new DataAccessException("Errors for more than "+QueryUtil.DECI_ENABLE_SIZE+" are not supported");
        }
        if (hasSortCol && dg.size()>=QueryUtil.DECI_ENABLE_SIZE) {
            throw new DataAccessException("Connected points for more than "+QueryUtil.DECI_ENABLE_SIZE+" are not supported");
        }

        // the output table columns: rowIdx, x, y, [[left, right], [low, high]]
        DataType[] dataTypes = dg.getDataDefinitions();

        // create the array of getters, which know how to get double values
        ArrayList<XYGenericProcessor.Col> colsLst = new ArrayList<>();
        XYGenericProcessor.Col xCol = XYGenericProcessor.getCol(dataTypes, xColOrExpr, "x", false);
        colsLst.add(xCol);
        colsLst.add(XYGenericProcessor.getCol(dataTypes, yColOrExpr, "y", false));

        // columns for color and size maps
        if (!StringUtils.isEmpty(colorColOrExpr)) {
            colsLst.add(XYGenericProcessor.getCol(dataTypes, colorColOrExpr, "color", true));
        }
        if (!StringUtils.isEmpty(sizeColOrExpr)) {
            colsLst.add(XYGenericProcessor.getCol(dataTypes, sizeColOrExpr, "size", true));
        }


        XYGenericProcessor.Col sortCol = null;
        if (hasSortCol) {
            if (sortColOrExpr.equals(xColOrExpr)) {
                sortCol = xCol;
            } else {
                sortCol = XYGenericProcessor.getCol(dataTypes, sortColOrExpr, "sortBy", true);
                colsLst.add(sortCol);
            }
        }

        if (hasXError) {
            colsLst.add(XYGenericProcessor.getCol(dataTypes, xErrColOrExpr, "xErr", true));
        }
        if (hasXLowError) {
            colsLst.add(XYGenericProcessor.getCol(dataTypes, xErrLowColOrExpr, "xErrLow", true));
        }
        if (hasXHighError) {
            colsLst.add(XYGenericProcessor.getCol(dataTypes, xErrHighColOrExpr, "xErrHigh", true));
        }
        if (hasYError) {
            colsLst.add(XYGenericProcessor.getCol(dataTypes, yErrColOrExpr, "yErr", true));
        }
        if (hasYLowError) {
            colsLst.add(XYGenericProcessor.getCol(dataTypes, yErrLowColOrExpr, "yErrLow", true));
        }
        if (hasYHighError) {
            colsLst.add(XYGenericProcessor.getCol(dataTypes, yErrHighColOrExpr, "yErrHigh", true));
        }

        XYGenericProcessor.Col[] cols = colsLst.toArray(new XYGenericProcessor.Col[colsLst.size()]);

        // create the array of output columns
        DataType dt;
        ArrayList<DataType> columnList = new ArrayList<>();
        dt = new DataType("rowIdx", Integer.class);
        dt.setFormatInfo(new DataType.FormatInfo(11)); // max num digits in integer, long - 20
        columnList.add(dt);
        ArrayList<DataGroup.Attribute> colMeta = new ArrayList<>();

        XYGenericProcessor.createColumnsToList(columnList, dg, cols, colMeta);
        DataType columns [] = columnList.toArray(new DataType[columnList.size()]);

        // create the return data group
        DataGroup retval = new DataGroup("XY with Errors", columns);

        DataObject retrow;
        int ncols = cols.length;
        XYGenericProcessor.Col col;
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
                        // we need to have data in the sort column
                        if (col == sortCol) {
                            retrow.setDataElement(dt, QueryUtil.convertData(dt.getDataType(), val));
                        }
                    }
                }
            }
            if (retrow != null) {
                retrow.setDataElement(dtRowIdx, rIdx);
                retval.add(retrow);
            }
        }


        for (XYGenericProcessor.Col c : cols) {
            colMeta.add(new DataGroup.Attribute(c.exprColName, c.colOrExpr));
        }
        if (xCol == sortCol) {
            colMeta.add(new DataGroup.Attribute("sortBy", xCol.colOrExpr));
        }
        retval.setAttributes(colMeta);

        // if sorting is requested - sort
        if (sortCol != null) {
            QueryUtil.doSort(retval, new SortInfo(sortCol.colname));
        }

        // should not shrink, otherwise the formatted data will be lost
        // retval.shrinkToFitData();
        File outFile = createFile(request);
        DataGroupWriter.write(outFile, retval);
        return outFile;
    }

}

