package edu.caltech.ipac.table.io;

import edu.caltech.ipac.table.DataGroup;

import static edu.caltech.ipac.table.TableUtil.getAliasName;
import static edu.caltech.ipac.util.StringUtils.applyIfNotEmpty;

import edu.caltech.ipac.util.StringUtils;
import uk.ac.starlink.table.*;
import uk.ac.starlink.votable.*;
import static edu.caltech.ipac.util.StringUtils.isEmpty;
import edu.caltech.ipac.firefly.server.util.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.net.URL;
import edu.caltech.ipac.table.*;

/**
 * Implementation of a StarTable based on data from DataGroup
 *
 * Created by cwang on 2/26/19.
 */
public class DataGroupStarTable extends RandomStarTable {
    private DataGroup dataGroup;
    private List<String> columns;
    private static Logger.LoggerImpl LOG = Logger.getLogger();

    public DataGroupStarTable(DataGroup dataGroup, List<String> columns) {
        this.dataGroup = dataGroup;
        this.columns = columns;
    }

//====================================================================
// override RandomStarTable
//====================================================================

    public ColumnInfo getColumnInfo(int idx) {
        String cname = columns.get(idx);
        var sampleData = dataGroup.size() > 0 ? dataGroup.getData(cname, 0) : null ;
        return convertToColumnInfo(dataGroup.getDataDefintion(cname), sampleData);
    }

    public Object getCell(long irow, int icol) throws IOException {
        return dataGroup.getData(columns.get(icol), (int) irow);
    }

    public int getColumnCount() {
        return columns.size();
    }

    public long getRowCount() {
        return dataGroup.size();
    }

    public String getName() {
        return dataGroup.getAttribute(TableMeta.NAME);
    }

    @Override
    public List getParameters() {
        List list = super.getParameters();
        String utype = dataGroup.getAttribute(TableMeta.UTYPE);
        if (!StringUtils.isEmpty(utype)) {
            list = list == null ? new ArrayList() : list;
            list.add(new DescribedValue(new DefaultValueInfo(VOStarTable.UTYPE_INFO), utype));
        }
        return list;
    }

//====================================================================
//  internal helper functions
//====================================================================

    private static ColumnInfo convertToColumnInfo(DataType dt, Object data) {
        Class dType = data != null ? data.getClass() : dt.getDataType();

        // name, datatype, <DESCRIPTION>
        String desc = dt.getDesc();
        String cname = getAliasName(dt);
        ColumnInfo col = new ColumnInfo(cname, dType, desc);
        // LINK child
        List<LinkInfo> links = dt.getLinkInfos();
        if (links.size() > 0 && !isEmpty(links.get(0).getHref())) {
               try {
                   URL url = new URL(links.get(0).getHref());
                   addDescribedValue(col, new DefaultValueInfo("reference", URL.class), url);
               } catch (Exception e) {
                   LOG.info("href in " + dt.getKeyName() + " is not a java.net.URL");
               }
        }

        // ID
        applyIfNotEmpty(dt.getID(), v -> addDescribedValue(col, VOStarTable.ID_INFO, v));
        // unit
        applyIfNotEmpty(dt.getUnits(), col::setUnitString);
        // precision
        applyIfNotEmpty(dt.getPrecision(), v -> addDescribedValue(col, VOStarTable.PRECISION_INFO,
                                                                  (v.startsWith("F") ? v.substring(1) : v)));
        // width
        if (dt.getWidth() > 0) {
            addDescribedValue(col, VOStarTable.WIDTH_INFO, dt.getWidth());
        }
        // ref or LINK
        applyIfNotEmpty(dt.getRef(), v -> addDescribedValue(col, VOStarTable.REF_INFO, v));
        // ucd
        applyIfNotEmpty(dt.getUCD(), col::setUCD);
        // utype
        applyIfNotEmpty(dt.getUType(), col::setUtype);

        if (dType == Integer.class) {
            col.setNullable(false);
        }

        // handle array type
        if (!StringUtils.isEmpty(dt.getArraySize())) {
            int[] shape = dt.getShape();
            if (shape.length > 0) {
                if (dt.getDataType() == String.class) {
                    col.setElementSize(shape[0]);
                    col.setShape(Arrays.copyOfRange(shape, 1, shape.length));
                } else {
                    col.setShape(shape);
                }
            }
        }

        return col;
    }

    private static void addDescribedValue(ColumnInfo cInfo, ValueInfo vInfo, Object value) {
        DescribedValue dVal = new DescribedValue(vInfo, value);
        cInfo.setAuxDatum(dVal);
    }
}