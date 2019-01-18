package edu.caltech.ipac.table.io;

import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.ParamInfo;
import static edu.caltech.ipac.util.StringUtils.applyIfNotEmpty;
import uk.ac.starlink.table.*;
import uk.ac.starlink.votable.*;
import static edu.caltech.ipac.util.StringUtils.isEmpty;
import edu.caltech.ipac.firefly.server.util.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.net.*;
import java.net.URL;
import edu.caltech.ipac.table.*;

/*
 * Created by cwang on 2/26/19.
 */
public class DataGroupStarTable extends RandomStarTable {
    private ColumnInfo[] colInfos;
    private DataGroup dataGroup;
    private List<DataType> columnsDef;
    private long totalRow;
    private int totalColumn;
    private List<DataGroup.Attribute> tblMeta;
    private List<ParamInfo> paramInfos;

    private static Logger.LoggerImpl LOG = Logger.getLogger();

    public DataGroupStarTable(DataGroup dg, ColumnInfo[] colInfoAry, List<DataType> allColumns) {
        super();
        dataGroup = dg;
        colInfos = colInfoAry;
        totalRow = dataGroup.size();
        totalColumn = colInfos.length;
        tblMeta = dataGroup.getTableMeta().getKeywords();
        paramInfos = dataGroup.getParamInfos();
        columnsDef = allColumns;
    }

    public DataGroup getDataGroup() {
        return dataGroup;
    }

    public List<DataType> getColumns() { return columnsDef; }

    public long getRowCount() {
        return totalRow;
    }

    public int getColumnCount() {
        return totalColumn;
    }

    public ColumnInfo getColumnInfo(int icol) {
        return colInfos[icol];
    }

    public String getName() {
       return getTableMetaValue(TableMeta.NAME, tblMeta);
    }

    public URL getURL() {
        String urlStr = getTableMetaValue(TableMeta.REF, tblMeta);

        try {
            return urlStr == null ? null : new URL(urlStr);
        } catch (MalformedURLException e) {
            return null;
        }
    }


    public DescribedValue getParameterByName(String name) {
        List<ParamInfo> params = paramInfos.stream()
                .filter(pInfo -> (pInfo.getKeyName() != null)&&(pInfo.getKeyName().equals(name)))
                .collect(Collectors.toList());

        // check paramInfo first
        if (params.size() != 0) {
            return convertToDescribedValue(params.get(0));
        } else {
            return null;
        }
    }

    public List<DescribedValue> getParameters() {
        // both paramInfo and info are combined

        List<DescribedValue> pInfoSet = paramInfos.stream()
                .map(pInfo -> convertToDescribedValue(pInfo))
                .collect(Collectors.toList());


        return pInfoSet;
    }

    public Object[] getRow(long irow) {
        List<Object> objs = new ArrayList<>();

        for (int i = 0; i < totalColumn; i++) {
            objs.add(getCell(irow, i));
        }
        return objs.toArray();
    }

    public Object getCell(long lrow, int icol) {
        return dataGroup.getData(colInfos[icol].getName(), (int) lrow);
    }

    // the following supports the above StarTable interfaces

    public static DescribedValue convertToDescribedValue(ParamInfo p) {
        if (p == null) return null;

        ValueInfo cInfo = convertToValueInfo(p);

        return new DescribedValue(cInfo, p.getValue());
    }


    public static DescribedValue convertToDescribedValue(DataGroup.Attribute att) {
        if (att == null) return null;

        ColumnInfo col = new ColumnInfo(att.getKey(), String.class, null);

        return new DescribedValue(col, att.getValue());
    }

    public static ValueInfo convertToValueInfo(DataType dt) {
        return convertToColumnInfo(dt, null);
    }

    public static ColumnInfo convertToColumnInfo(DataType dt, String outputFormat) {
        Class dType = dt.getDataType();

        // name, datatype, <DESCRIPTION>
        String desc = dt.getDesc();
        ColumnInfo col = new ColumnInfo(dt.getKeyName(), dType, desc);
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

        return col;
    }

    private static void addDescribedValue(ColumnInfo cInfo, ValueInfo vInfo, Object value) {
        DescribedValue dVal = new DescribedValue(vInfo, value);

        cInfo.setAuxDatum(dVal);
    }


    public static String getTableMetaValue(String keyName, List<DataGroup.Attribute> meta) {
        List<DataGroup.Attribute> atts = meta.stream()
                .filter(oneAtt -> (!oneAtt.isComment())&&(oneAtt.getKey().equals(keyName)))
                .collect(Collectors.toList());

        return atts.size() == 0 ? null : atts.get(0).getValue();
    }

    public static List<DataGroup.Attribute> getInfosFromMeta(List<DataGroup.Attribute> meta) {
        String[] tableMetaNotInfo = { TableMeta.ID, TableMeta.REF,
                                      TableMeta.UCD, TableMeta.UTYPE,
                                      TableMeta.NAME, TableMeta.DESC};

        List<String> attList = Arrays.asList(tableMetaNotInfo);
        List<DataGroup.Attribute> infoList = meta.stream()
                .filter(oneAtt -> (!oneAtt.isComment()) && !attList.contains(oneAtt.getKey()))
                .collect(Collectors.toList());
        return infoList;
    }
}