/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.data.table;

import edu.caltech.ipac.util.HandSerialize;
import edu.caltech.ipac.util.StringUtils;

import java.io.Serializable;

/**
 * Date: Dec 18, 2008
 *
 * @author loi
 * @version $Id: RawDataSet.java,v 1.4 2012/03/12 18:04:40 roby Exp $
 */
public class RawDataSet implements Serializable, HandSerialize {
    private final static String SPLIT_TOKEN= "--RawDataSet--";
    private final static String NL_TOKEN=  "---nl---";

    private int startingIndex;
    private int totalRows;
    private TableMeta meta;
    private String dataSetString;

    public RawDataSet() { }

    public RawDataSet(TableMeta meta, int startingIndex, int totalRows, String dataSetString) {
        this.meta = meta;
        this.startingIndex = startingIndex;
        this.totalRows = totalRows;
        this.dataSetString = dataSetString;
    }

    public TableMeta getMeta() {
        if (meta == null) {
            meta = new TableMeta();
        }
        return meta;
    }

    public void setMeta(TableMeta meta) {
        this.meta = meta;
    }

    public int getStartingIndex() {
        return startingIndex;
    }

    public void setStartingIndex(int startingIndex) {
        this.startingIndex = startingIndex;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public String getDataSetString() {
        return dataSetString;
    }

    public void setDataSetString(String dataSetString) {
        this.dataSetString = dataSetString;
    }

    public String serialize() {
        String dsTmp= dataSetString.replace("\n", NL_TOKEN);
        return StringUtils.combine(SPLIT_TOKEN,
                                   startingIndex+"",
                                   totalRows+"",
                                   meta!=null ? meta.serialize() : null,
                                   dsTmp);
    }

    public static RawDataSet parse(String s) {
        try {
            String sAry[]= StringUtils.parseHelper(s,4,SPLIT_TOKEN);
            int i= 0;
            int startingIndex= Integer.parseInt(sAry[i++]);
            int totalRows=     Integer.parseInt(sAry[i++]);
            TableMeta meta= TableMeta.parse(sAry[i++]);
            String dsTmp= StringUtils.checkNull(sAry[i++]);
            String dataSetString= dsTmp.replace(NL_TOKEN,"\n");
            return new RawDataSet(meta,startingIndex,totalRows,dataSetString);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
