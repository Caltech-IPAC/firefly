package edu.caltech.ipac.firefly.server.util;

import edu.caltech.ipac.astro.DataGroupQueryStatement;
import edu.caltech.ipac.astro.IpacTableWriter;
import edu.caltech.ipac.firefly.core.EndUserException;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.SortInfo;
import edu.caltech.ipac.firefly.data.table.BaseTableColumn;
import edu.caltech.ipac.firefly.data.table.BaseTableData;
import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.firefly.data.table.RawDataSet;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupPart;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupReader;
import edu.caltech.ipac.planner.io.IpacTableTargetsParser;
import edu.caltech.ipac.target.Target;
import edu.caltech.ipac.targetgui.TargetList;
import edu.caltech.ipac.util.CollectionUtil;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataGroupQuery;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * Date: Jul 14, 2008
 *
 * @author loi
 * @version $Id: QueryUtil.java,v 1.32 2012/11/03 02:20:23 tlau Exp $
 */
public class QueryUtil {

    public static String makeKey(Object... ids) {
        StringBuffer sb = new StringBuffer();
        for(int i=0; i < ids.length; i++) {
            if(i!=0) {
                sb.append("|");
            }
            sb.append(ids[i]);
        }
        return sb.toString();
    }

    public static String makeUrlBase(String url) {

        if (StringUtils.isEmpty(url)) return "";

        if (url.toLowerCase().startsWith("http"))
            return url;
        else {
            return "http://" + url.trim();
        }
    }

    public static String encodeUrl(String s) {
        StringBuffer sb = new StringBuffer();
        if (s == null) return "";
        if (s.startsWith("?")) {
            sb.append("?");
            s = s.substring(1);
        }
        String[] params = s.split("&");
        for (int i = 0; i < params.length; i++) {
            String[] kw = params[i].split(ServerRequest.KW_VAL_SEP, 2);
            if (kw != null && kw.length > 0) {
                if (!StringUtils.isEmpty(kw[0])) {
                    sb.append(kw[0]);
                    if (kw.length > 1 && !StringUtils.isEmpty(kw[1])) {
                        sb.append(ServerRequest.KW_VAL_SEP).append(encode(kw[1]));
                    }
                    if (i < params.length) {
                        sb.append(ServerRequest.PARAM_SEP);
                    }
                }
            }
        }
        return sb.toString();
    }

    private static String encode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return e.getMessage();
        }
    }

    public static RawDataSet getRawDataSet(DataGroup dg, int startIndex, int pageSize) {
        return convertToRawDataset(dg, startIndex, pageSize);
    }

    public static RawDataSet getRawDataSet(DataGroup dg) {
        return convertToRawDataset(dg,0,20000);
    }


    public static DataSet getDataSet(DataGroup dg, int startIndex, int pageSize) {
        return convertToDataset(dg, startIndex, pageSize);
    }

    public static void doSort(DataGroup dg, SortInfo sortInfo) {
        if (sortInfo != null) {
            String infoStr = dg.getAttribute(SortInfo.SORT_INFO_TAG) == null ? "" : dg.getAttribute(SortInfo.SORT_INFO_TAG).getValue().toString();
            if (!infoStr.equals(sortInfo.toString())) {
                DataGroupQuery.SortDir sortDir = DataGroupQuery.SortDir.valueOf(sortInfo.getDirection().name());
                DataGroupQuery.sort(dg, sortDir, true, sortInfo.getSortColumnAry());
                dg.addAttributes(new DataGroup.Attribute(SortInfo.SORT_INFO_TAG, sortInfo.toString()));
            }
        }
    }

    public static DataGroup doFilter(DataGroup dg, DataGroupQuery.DataFilter... filters) {
        DataGroupQuery query = new DataGroupQuery();
        for(DataGroupQuery.DataFilter f : filters) {
            query.addDataFilters(f);
        }
        dg = query.doQuery(dg);
        dg.addAttributes(new DataGroup.Attribute("filterBy", CollectionUtil.toString(Arrays.asList(filters))));
        return dg;
    }

    /**
     * Converts a list of string into a list of DataGroupQuery.DataFilter using its parseFilter() method.
     * In the process, '!=' is replaced with '!' becuase internally, DataGroupQuery is using '!' to represent NOT.
     * @param filters
     * @return
     */
    public static DataGroupQuery.DataFilter[] convertToDataFilter(List<String> filters) {
        if (filters == null) return null;
        
        List<DataGroupQuery.DataFilter> filterList = new ArrayList<DataGroupQuery.DataFilter>();
        for(String cond : filters) {
            DataGroupQuery.DataFilter filter = DataGroupQueryStatement.parseFilter(cond.replaceAll("!=", "!"));
            filterList.add(filter);
        }
        return filterList.toArray(new DataGroupQuery.DataFilter[filterList.size()]);
    }

    public static DataSet convertToDataset(DataGroup dg, int startIdx, int pageSize) {
        DataType[] types = dg.getDataDefinitions();
        String[] headers = new String[types.length];
        BaseTableColumn[] columns = new BaseTableColumn[types.length];
        for (int i=0; i < types.length; i++) {
            headers[i] = types[i].getKeyName();
            columns[i] = new BaseTableColumn(types[i].getKeyName());

            DataGroup.Attribute label = dg.getAttribute("col." + types[i].getKeyName() + ".Label");
            if (label != null && !StringUtils.isEmpty(label.getValue())) {
                columns[i].setTitle(types[i].getDefaultTitle());
            }
            columns[i].setWidth(types[i].getFormatInfo().getWidth());
            DataType.FormatInfo.Align align = types[i].getFormatInfo().getDataAlign();
            DataSet.Align calign = align == DataType.FormatInfo.Align.RIGHT ? DataSet.Align.RIGHT : DataSet.Align.LEFT;
            columns[i].setAlign(calign);
            columns[i].setWidth(types[i].getFormatInfo().getWidth());
            columns[i].setUnits(types[i].getDataUnit());

            DataGroup.Attribute vis = dg.getAttribute("col." + types[i].getKeyName() + ".Visibility");
            if (vis == null || vis.formatValue().equals("show")) {
                columns[i].setHidden(false);
                columns[i].setVisible(true);
            } else if (vis.formatValue().equals("hide")) {
                columns[i].setHidden(false);
                columns[i].setVisible(false);
            } else {
                columns[i].setHidden(true);
                columns[i].setVisible(false);
            }
        }

        int endIdx = Math.min(dg.size(), startIdx + pageSize);
        if (startIdx >= endIdx) {
            startIdx = (endIdx - pageSize > 0) ? endIdx - pageSize : 0;
        }
        List<DataObject> values = dg.values().subList(startIdx, endIdx);

        // a simple column_name to display_name mappings
        for(BaseTableColumn c : columns) {
            c.setTitle(ColumnMapper.getTitle(c.getName()));
        }

        DataSet dataset = new DataSet(columns);
        dataset.setStartingIdx(startIdx);
        dataset.setTotalRows(dg.size());

        BaseTableData model = new BaseTableData(headers);
        for (DataObject row : values) {
            String[] sdata = new String[row.getData().length];
            String[] data = row.getFormatedData();
            System.arraycopy(data, 0, sdata, 0, data.length);
            model.addRow(sdata);
        }

        // setting attributes into the dataset
        for (String key : dg.getAttributeKeys()) {
            model.setAttribute(key, String.valueOf(dg.getAttribute(key).getValue()));
        }

        dataset.setModel(model);
        return dataset;
    }

    public static RawDataSet convertToRawDataset(DataGroup dg, int startIdx, int pageSize) {

        RawDataSet dataset = new RawDataSet();
        dataset.setStartingIndex(startIdx);

        if (dg != null) {
            int endIdx = Math.min(dg.size(), startIdx + pageSize);
            if (startIdx >= endIdx) {
                startIdx = (endIdx - pageSize > 0) ? endIdx - pageSize : 0;
            }
            ByteArrayOutputStream vals = new ByteArrayOutputStream();

            DataGroup subset = dg.subset(startIdx, endIdx);

            // move attributes into TableMeta.. and clear the ipac table's attributes
            for(Map.Entry<String, DataGroup.Attribute> entry : dg.getAttributes().entrySet()) {
                dataset.getMeta().setAttribute(entry.getKey(), String.valueOf(entry.getValue().getValue()));
            }
            subset.getAttributes().clear();

            try {
                IpacTableWriter.save(vals, subset);
            } catch (IOException e) {
                e.printStackTrace();
            }

            dataset.setTotalRows(dg.size());
            dataset.setDataSetString(vals.toString());
        }
        return dataset;
    }


    /**
     * return Float.NaN if val is null or not a float
     * @param val
     * @return
     */
    public static float getFloat(Object val) {
        if (val != null) {
            if (val instanceof Float) {
                return (Float)val;
            } else {
                try {
                    return Float.parseFloat(String.valueOf(val));
                } catch(NumberFormatException ex) {}
            }
        }
        return Float.NaN;

    }

    /**
     * return Integer.MIN_VALUE if val is null or not an integer
     * @param val
     * @return
     */
    public static int getInt(Object val) {
        if (val != null) {
            if (val instanceof Integer) {
                return (Integer)val;
            } else {
                try {
                    return Integer.parseInt(String.valueOf(val));
                } catch(NumberFormatException ex) {
                    ex.printStackTrace();
                }
            }
        }
        return Integer.MIN_VALUE;
    }

    public static String test(Double d) {
        return unchecked(d.toString());
    }

    @SuppressWarnings("unchecked") // at your own risk
    public static <T> T unchecked(Object o) {

            return (T) o;

    }

    public static Date convertDate(java.util.Date date) {
        return date == null ? null : new Date(date.getTime());
    }

    public static RawDataSet getRawDataSet(DataGroupPart page) {
        RawDataSet rds = convertToRawDataset(page.getData(), 0, Integer.MAX_VALUE);
        rds.setTotalRows(page.getRowCount());
        rds.setStartingIndex(page.getStartRow());
        rds.getMeta().setSource(page.getTableDef().getSource());
        return rds;
    }


    public static <T extends ServerRequest> T assureType(Class<T> s, ServerRequest req) {
        if(s.isAssignableFrom(req.getClass())) {
            return (T)req;
        }
        try {
            T sreq = s.newInstance();
            sreq.copyFrom(req);
            sreq.setRequestId(req.getRequestId());
            return sreq;
        } catch (InstantiationException e) {
            Logger.error(e);
        } catch (IllegalAccessException e) {
            Logger.error(e);
        }
        return null;
    }

    /**
     * Get target list from uploaded file.  It supports tbl, csv, tsv, Spot target list.
     * @param ufile File to parse for targets
     * @return List<Target>
     * @throws DataAccessException
     * @throws IOException
     */
    public static List<Target> getTargetList(File ufile)
            throws DataAccessException, IOException {
        TargetList targetList = new TargetList();
        String parsingErrors = "";
        ArrayList<Target> targets = new ArrayList<Target>();
        final String OBJ_NAME = "objname";
        final String RA = "ra";
        final String DEC= "dec";
        String ra = null, dec = null, name = null;
        try {
            DataGroup dg= DataGroupReader.readAnyFormat(ufile);
            if (dg != null) {
                for (DataType dt: dg.getDataDefinitions()) {
                    if (dt.getKeyName().toLowerCase().equals("object") ||
                            dt.getKeyName().toLowerCase().equals("target")) {
                        dt.setKeyName(OBJ_NAME);
                        break;
                    }
                }
                // Find RA and Dec keys
                for (String key: dg.getKeySet()) {
                    if (key.trim().toLowerCase().equals(RA)) {
                        ra=key;
                    } else if (key.trim().toLowerCase().equals(DEC)) {
                        dec=key;
                    } else if (key.trim().toLowerCase().equals(OBJ_NAME)) {
                        name=key;
                    }
                }

                if (name==null) {
                    if (ra==null || dec==null)
                        throw createEndUserException("IPAC Table file must contain RA and Dec columns.");
                }
                IpacTableTargetsParser.parseTargets(dg, targetList, true);
            }
            if (targetList.size()==0) throw createEndUserException("Unable to uploaded file:" + ufile.getName());

            for (Target t: targetList) {
                //check for invalid targets
                if(t.getCoords() == null || t.getCoords().length() < 1){
                    parsingErrors = parsingErrors + "Invalid Target: " + t.getName() + "<br>";
                } else {
                    targets.add(t);
                }
            }

        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg==null) msg=e.getCause().getMessage();
            if (msg==null) msg="";
            throw new DataAccessException(
                    new EndUserException("Exception while parsing the uploaded file: <br><i>" + msg + "</i>" ,
                               e.getMessage(), e) );
        }
        return targets;
    }

    public static DataAccessException createEndUserException(String msg) {
        return new DataAccessException(new EndUserException(msg, msg) );
    }

    public static DataAccessException createEndUserException(String msg, String details) {
        return new DataAccessException(new EndUserException(msg, details) );
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
