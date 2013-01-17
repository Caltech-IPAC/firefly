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
