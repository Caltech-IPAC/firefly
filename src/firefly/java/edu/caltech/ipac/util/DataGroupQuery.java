/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util;

import edu.caltech.ipac.astro.IpacTableException;
import edu.caltech.ipac.astro.IpacTableReader;
import edu.caltech.ipac.astro.IpacTableWriter;
import edu.caltech.ipac.util.decimate.DecimateKey;
import edu.caltech.ipac.util.expr.Expression;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

/**
 * Use this class as if it's a query builder.  Add filters for data and/or for headers as needed.  Set a list of columns
 * to select from.  If one is not given, all columns will be returned. Once all of the fields are set, invoke {@link
 * #doQuery(DataGroup src) doQuery} to execute the query on the given DataGroup.  A new DataGroup will be created and
 * returned as the result of the query.
 *
 * @author loi
 * @version $id:$
 */
public class DataGroupQuery {

    public static enum SortDir {ASC, DESC, NONE}

    public static enum OpType {
        GREATER_THAN(">"), LESS_THAN("<"), EQUALS("="), NOT_EQUALS("!"),
        GREATER_THAN_EQUALS(">="), LESS_THAN_EQUALS("<="), IN("IN"), LIKE("LIKE");

        String _op;

        OpType(String op) {
            _op = op;
        }

        public String getOp() {
            return _op;
        }

        ;

        @Override
        public String toString() {
            return getOp();
        }

        ;

        public boolean equals(OpType op) {
            if (op.getOp().equals(OpType.IN.getOp())) {
                return _op.equalsIgnoreCase(OpType.IN.getOp());
            } else if (op.getOp().equals(OpType.LIKE.getOp())) {
                return _op.equalsIgnoreCase(OpType.LIKE.getOp());
            } else {
                return super.equals(op);
            }
        }

        ;
    }

    private List<CollectionUtil.Filter<DataObject>> _filters;
    private List<String> _columns;
    private List<CollectionUtil.Filter<DataGroup.Attribute>> _headerFilters;
    private String[] orderBy;
    private SortDir sortDir;

    /**
     * Execute in-line query on a given file.
     *
     * @param src             source file
     * @param dest            output file to write the results to
     * @param addedAttributes additional attributes to include
     * @throws IOException
     * @throws IpacTableException
     */
    public void doQuery(File src, File dest, DataGroup.Attribute... addedAttributes) throws IOException, IpacTableException {
        if (dest == null || !dest.canWrite()) {
            throw new IOException("Unable to write into output file:" + dest);
        }
        doQuery(src, new FileOutputStream(dest), addedAttributes);
        if (orderBy != null && orderBy.length > 0 && !(sortDir.equals(SortDir.NONE))) {
            DataGroup newDG = IpacTableReader.readIpacTable(dest, "doQuery");
            sort(newDG, sortDir, true, orderBy);
            IpacTableWriter.save(dest, newDG);
        }
    }

    /**
     * Execute in-line query on a given file.  The output will be written directly into the output stream. This process
     * will ignore the SORT request of the query.
     *
     * @param src
     */
    public void doQuery(File src, OutputStream dest, DataGroup.Attribute... addedAttributes) throws IOException, IpacTableException {

        if (src == null || !src.canWrite()) {
            throw new IOException("Unable to read input file:" + src);
        }

        BufferedReader reader = new BufferedReader(new FileReader(src), IpacTableUtil.FILE_IO_BUFFER_SIZE);
        PrintWriter writer = new PrintWriter(new BufferedOutputStream(dest, IpacTableUtil.FILE_IO_BUFFER_SIZE));
        int lineNum = 0;
        try {
            String line = reader.readLine();
            lineNum++;
            List<DataType> cols = null;
            boolean hasType = false, hasUnit = false, hasNullStr = false;
            DataGroup dg = null;
            List<DataType> selectedCols = null;

            boolean needToWriteHeader = true;
            while (line != null) {
                if (line.startsWith("\\")) {
                    DataGroup.Attribute attrib = IpacTableUtil.parseAttribute(line);
                    if (CollectionUtil.matches(lineNum, attrib, getHeaderFilters())) {
                        writer.println(line);
                    }
                } else if (line.startsWith("|")) {
                    if (cols == null) {
                        cols = IpacTableUtil.createColumnDefs(line);
                    } else if (!hasType) {
                        IpacTableUtil.setDataType(cols, line);
                        hasType = true;
                    } else if (!hasUnit) {
                        IpacTableUtil.setDataUnit(cols, line);
                        hasUnit = true;
                    } else if (!hasNullStr) {
                        IpacTableUtil.setDataNullStr(cols, line);
                        hasNullStr = true;
                    }
                } else if (line.trim().length() == 0) {
                    // skip
                } else {
                    if (dg == null) {
                        if (cols == null) {
                            throw new IpacTableException("Invalid IPAC table.  No column headers.");
                        }
                        dg = new DataGroup("doQuery", cols);

                        if (getColumnNames().size() > 0) {
                            selectedCols = new ArrayList<DataType>();
                            for (String cname : getColumnNames()) {
                                DataType col = dg.getDataDefintion(cname);
                                if (col == null) {
                                    try {
                                        int idx = Integer.parseInt(cname);
                                        col = dg.getDataDefinitions()[idx];
                                    } catch (Exception e) { //ignore }
                                    }
                                }
                                if (col != null) {
                                    selectedCols.add(col);
                                }
                            }
                        } else {
                            selectedCols = cols;
                        }
                    }

                    DataObject row = IpacTableUtil.parseRow(dg, line, true, true);
                    if (needToWriteHeader) {
                        needToWriteHeader = false;
                        if (addedAttributes != null) {
                            IpacTableUtil.writeAttributes(writer, Arrays.asList(addedAttributes));
                        }
                        IpacTableUtil.writeHeader(writer, selectedCols);
                    }

                    if (CollectionUtil.matches(lineNum, row, getDataFilters())) {
                        if (selectedCols != cols) {
                            IpacTableUtil.writeRow(writer, selectedCols, row);
                        } else {
                            writer.println(line);
                        }
                    }
                }
                line = reader.readLine();
            }
        } finally {
            FileUtil.silentClose(reader);
            FileUtil.silentClose(writer);
        }
    }

    /**
     * Execute this query on the given DataGroup.
     *
     * @param src
     */
    public DataGroup doQuery(DataGroup src) {

        DataType[] types = makeDataType(src);

        DataGroup newDG = new DataGroup(src.getTitle(), types);

        // querying for headers
        ArrayList<DataGroup.Attribute> headerResults = new ArrayList<DataGroup.Attribute>();
        CollectionUtil.filter(src.getAttributes().values(), headerResults, getHeaderFilters());

        for (Iterator itr = headerResults.iterator(); itr.hasNext(); ) {
            DataGroup.Attribute attrib = (DataGroup.Attribute) itr.next();
            newDG.addAttribute(attrib.getKey(), attrib.getValue());
        }

        // querying for data
        ArrayList<DataObject> dataResults = new ArrayList<DataObject>();
        CollectionUtil.filter(src.values(), dataResults, getDataFilters());
        for (DataObject srcObj : dataResults) {
            DataObject newData = new DataObject(newDG);
            for (DataType type : types) {
                newData.setDataElement(type, srcObj.getDataElement(type.getKeyName()));
            }
            newDG.add(newData);
        }

        if (orderBy != null && orderBy.length > 0 && !(sortDir.equals(SortDir.NONE))) {
            sort(newDG, sortDir, true, orderBy);
        }

        return newDG;
    }

    /**
     * sort the given data group according to the given parameters. if doInline is false, a new sorted DataGroup object
     * will be returned.  Otherwise, it will sort the given DataGroup directly.
     *
     * @param src
     * @param colNames
     * @param sortDir
     * @param doInline
     * @return
     */
    public static DataGroup sort(DataGroup src, final SortDir sortDir, boolean doInline, String... colNames) {

        if (colNames == null || colNames.length == 0 || sortDir.equals(SortDir.NONE)) {
            return src;
        }

        final DataType[] dtypes = new DataType[colNames.length];
        for (int i = 0; i < colNames.length; i++) {
            dtypes[i] = src.getDataDefintion(colNames[i]);
// if column does not exists, ignore it.
//            if (dtypes[i] == null) {
//                throw new IllegalArgumentException("The given column name does not exists:" + colNames[i]);
//            }
        }

        DataGroup sortedDG = null;
        if (doInline) {
            sortedDG = src;
        } else {
            try {
                sortedDG = (DataGroup) src.clone();
            } catch (CloneNotSupportedException e) {
                e.printStackTrace(); // should not happen.
                throw new RuntimeException("Unexpected exception:  unable to clone DataGroup");

            }
        }
        Comparator<DataObject> comp = new Comparator<DataObject>() {

            private int doCompare(Object v1, Object v2) {
                int dir = sortDir.equals(SortDir.ASC) ? 1 : sortDir.equals(SortDir.DESC) ? -1 : 0;
                if (v1 == null || v2 == null) {
                    return (v1 == v2 ? 0 : v1 == null ? -1 : 1) * dir;
                }
                if (v1 instanceof Comparable &&
                        v2 instanceof Comparable) {
                    return ((Comparable) v1).compareTo(v2) * dir;
                } else {
                    return v1.toString().compareTo(v2.toString()) * dir;
                }
            }

            public int compare(DataObject row1, DataObject row2) {
                for (DataType dt : dtypes) {
                    if (dt != null) {
                        Object v1 = row1.getDataElement(dt);
                        Object v2 = row2.getDataElement(dt);
                        int cmp = doCompare(v1, v2);
                        if (cmp != 0) {
                            return cmp;
                        }
                    }
                }
                return 0;
            }

        };

        List<DataObject> data = sortedDG.getValues();
        Collections.sort(data, comp);
        return sortedDG;
    }


    CollectionUtil.Filter[] convertFilters(List<DataFilter> filters) {
        List<CollectionUtil.Filter> rval = new ArrayList<CollectionUtil.Filter>();
        if (filters != null && filters.size() > 0) {
            for (ListIterator<DataFilter> itr = filters.listIterator(); itr.hasNext(); ) {
                DataFilter tf = itr.next();
                rval.add(tf);
            }
        }
        return rval.toArray(new CollectionUtil.Filter[rval.size()]);
    }


//=========================================================================
//  setters
//=========================================================================

    public void addDataFilter(String colName, OpType optype, String compareTo) {
        addDataFilters(new DataFilter(colName, optype, compareTo));
    }

    public void addDataFilters(CollectionUtil.Filter<DataObject>... filters) {
        getDataFilters().addAll(Arrays.asList(filters));
    }

    public void addColumns(String... names) {
        getColumnNames().addAll(Arrays.asList(names));
    }

    public void addHeaderFilters(CollectionUtil.Filter<DataGroup.Attribute>... filters) {
        getHeaderFilters().addAll(Arrays.asList(filters));
    }

    public void setOrderBy(String... orderBy) {
        this.orderBy = orderBy;
    }

    public void setSortDir(SortDir sortDir) {
        this.sortDir = sortDir;
    }
//=========================================================================
//  getters
//=========================================================================

    public List<CollectionUtil.Filter<DataObject>> getDataFilters() {
        if (_filters == null) {
            _filters = new ArrayList<CollectionUtil.Filter<DataObject>>();
        }
        return _filters;
    }

    public List<String> getColumnNames() {
        if (_columns == null) {
            _columns = new ArrayList<String>();
        }
        return _columns;
    }

    public List<CollectionUtil.Filter<DataGroup.Attribute>> getHeaderFilters() {
        if (_headerFilters == null) {
            _headerFilters = new ArrayList<CollectionUtil.Filter<DataGroup.Attribute>>();
        }
        return _headerFilters;
    }

    public String[] getOrderBy() {
        return orderBy;
    }

    public SortDir getSortDir() {
        return sortDir;
    }

    //=========================================================================
//  static util methods.
//=========================================================================

    /**
     * Returns an inner-join DataGroup with attributes.  The resulting DataGroup will contain the columns from dgOneCols
     * appended by dgTwoCols.
     *
     * @param dgOne      DataGroup one.
     * @param dgOneCols  Columns to select from one.  null to select all.
     * @param dgTwo      DataGroup two
     * @param dgTwoCols  Columns to select from two.  null to select all.
     * @param comparator comparator used to find matching DataObject
     * @return see description
     */
    public static DataGroup join(DataGroup dgOne, DataType[] dgOneCols,
                                 DataGroup dgTwo, DataType[] dgTwoCols,
                                 Comparator<DataObject> comparator) {
        return join(dgOne, dgOneCols, dgTwo, dgTwoCols, comparator, true, true);

    }

    /**
     * Returns the combine data of the 2 DataGroup based on the given parameters The columns from dgTwo are appended to
     * that of dgOne The join key is not required to be unique
     *
     * @param dgOne             DataGroup one.
     * @param dgOneCols         Columns to select from one.  null to select all.
     * @param dgTwo             DataGroup two
     * @param dgTwoCols         Columns to select from two.  null to select all.
     * @param comparator        comparator used to find matching DataObject
     * @param isInnerJoin       true to perform an inner join, else perform a full outer join
     * @param includeAttributes include both DataGroup's attributes
     * @return Returns the combine data of the 2 DataGroup based on the given parameters
     */
    public static DataGroup join(DataGroup dgOne, DataType[] dgOneCols,
                                 DataGroup dgTwo, DataType[] dgTwoCols,
                                 Comparator<DataObject> comparator,
                                 boolean isInnerJoin, boolean includeAttributes) {

        ArrayList<DataObject> list1 = new ArrayList<DataObject>(dgOne.values());
        Collections.sort(list1, comparator);
        ArrayList<DataObject> list2 = new ArrayList<DataObject>(dgTwo.values());
        Collections.sort(list2, comparator);

        DataType[] dgcols1 = dgOneCols == null ? dgOne.getDataDefinitions() : dgOneCols;
        DataType[] dgcols2 = dgTwoCols == null ? dgTwo.getDataDefinitions() : dgTwoCols;

        // setting up the resulting columns
        List<DataType> def1 = new ArrayList<DataType>(dgcols1.length);
        List<DataType> def2 = new ArrayList<DataType>(dgcols2.length);
        try {
            for (DataType dt : dgcols1) def1.add((DataType) dt.clone());
            for (DataType dt : dgcols2) def2.add((DataType) dt.clone());
        } catch (CloneNotSupportedException e) {
            System.out.println(e.getMessage());
        }
        List<DataType> defs = new ArrayList<DataType>(def1.size() + def2.size());
        defs.addAll(def1);
        defs.addAll(def2);

        DataGroup results = new DataGroup(dgOne.getTitle(), defs);

        // merging keywords
        if (includeAttributes) {
            dgOne.mergeAttributes(dgTwo.getKeywords());
        }

        // start joining
        int currIdx1 = 0, currIdx2 = 0;
        int firstIdx1 = -1, firstIdx2 = -1; // start index of the data objects with the same join key for first and second list

        boolean advanceList1 = true;
        boolean advanceList2 = true;
        DataObject doFirst1 = null;
        DataObject doFirst2 = null;
        while (currIdx1 < list1.size() || currIdx2 < list2.size()) {

            if (advanceList1) {
                if (currIdx1 < list1.size()) {
                    doFirst1 = list1.get(currIdx1);
                    firstIdx1 = currIdx1;
                    currIdx1++;
                    while (currIdx1 < list1.size()) {
                        if (comparator.compare(doFirst1, list1.get(currIdx1)) == 0) {
                            currIdx1++;
                        } else {
                            break;
                        }
                    }
                    // data objects from list1 with the same key have index firstIdx1<=idx<currIdx1 now
                } else {
                    doFirst1 = null;
                }
            }

            if (advanceList2) {
                if (currIdx2 < list2.size()) {
                    doFirst2 = list2.get(currIdx2);
                    firstIdx2 = currIdx2;
                    currIdx2++;
                    while (currIdx2 < list2.size()) {
                        if (comparator.compare(doFirst2, list2.get(currIdx2)) == 0) {
                            currIdx2++;
                        } else {
                            break;
                        }
                    }
                    // data objects from list2 with the same key have index firstIdx2<=idx<currIdx2 now
                } else {
                    doFirst2 = null;
                }
            }

            advanceList1 = false;
            advanceList2 = false;

            int cmpResults;
            if (doFirst1 != null && doFirst2 != null) {
                cmpResults = comparator.compare(doFirst1, doFirst2);
            } else if (doFirst1 != null) {
                cmpResults = -1;
            } else if (doFirst2 != null) {
                cmpResults = 1;
            } else {
                break;
            }

            DataObject data1;
            DataObject data2;
            DataObject data;
            if (cmpResults == 0) {
                for (int idx1 = firstIdx1; idx1 < currIdx1; idx1++) {
                    data1 = list1.get(idx1);
                    for (int idx2 = firstIdx2; idx2 < currIdx2; idx2++) {
                        data2 = list2.get(idx2);
                        data = new DataObject(results);

                        for (DataType dt : def1) {
                            data.setDataElement(dt, data1.getDataElement(dt.getKeyName()));
                        }
                        for (DataType dt : def2) {
                            data.setDataElement(dt, data2.getDataElement(dt.getKeyName()));
                        }
                        results.add(data);
                    }
                }
                advanceList1 = true;
                advanceList2 = true;
            } else if (cmpResults < 0) {
                if (!isInnerJoin) {
                    for (int idx1 = firstIdx1; idx1 < currIdx1; idx1++) {
                        data1 = list1.get(idx1);
                        data = new DataObject(results);
                        for (DataType dt : def1) {
                            data.setDataElement(dt, data1.getDataElement(dt.getKeyName()));
                        }
                        results.add(data);
                    }
                }
                advanceList1 = true;
            } else {
                if (!isInnerJoin) {
                    for (int idx2 = firstIdx2; idx2 < currIdx2; idx2++) {
                        data2 = list2.get(idx2);
                        data = new DataObject(results);
                        for (DataType dt : def2) {
                            data.setDataElement(dt, data2.getDataElement(dt.getKeyName()));
                        }
                        results.add(data);
                    }
                }
                advanceList2 = true;
            }
        }

        return results;
    }

    //    private static DataGroup doSelectedCols(DataGroup dg, DataType[] cols) {
//        if (cols != null) {
//            DataGroupQuery dgq = new DataGroupQuery();
//            for(DataType dt : cols) {
//                dgq.addColumns(dt.getKeyName());
//            }
//            return dgq.doQuery(dg);
//        }
//        return dg;
//    }
//
//
    public static boolean isTrue(Object val, OpType optype, double compareTo) {

        try {
            double dval = StringUtils.isEmpty(val) ? 0 : Double.parseDouble(val.toString());
            switch (optype) {
                case GREATER_THAN:
                    return dval > compareTo;
                case LESS_THAN:
                    return dval < compareTo;
                case EQUALS:
                    return dval == compareTo;
                case NOT_EQUALS:
                    return dval != compareTo;
                case GREATER_THAN_EQUALS:
                    return dval >= compareTo;
                case LESS_THAN_EQUALS:
                    return dval <= compareTo;
                default:
                    return false;
            }
        } catch (NumberFormatException nfx) {
            return false;
        }
    }

    public static boolean isTrue(Object val, OpType optype, String compareTo) {
        String s = val == null ? "" : val.toString();
        int ans = s.toLowerCase().compareTo(compareTo);
        switch (optype) {
            case GREATER_THAN:
                return ans > 0;
            case LESS_THAN:
                return ans < 0;
            case EQUALS:
                return ans == 0;
            case NOT_EQUALS:
                return ans != 0;
            case GREATER_THAN_EQUALS:
                return ans >= 0;
            case LESS_THAN_EQUALS:
                return ans <= 0;
            default:
                return false;
        }
    }

    public static OpType getOpType(String optype) {

        if (optype == null) return null;

        for (OpType t : OpType.values()) {
            if (t.getOp().equalsIgnoreCase(optype)) {
                return t;
            }
        }
        throw new IllegalArgumentException("unrecognized operation type:" + optype);
    }

    public static boolean isNumber(String value) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException nfx) {
            return false;
        }
    }

//=========================================================================
//  private methods
//=========================================================================

    private DataType[] makeDataType(DataGroup dg) {
        ArrayList<DataType> l = new ArrayList<DataType>();
        try {
            if (getColumnNames().size() == 0) {
                for (DataType dt : dg.getDataDefinitions()) {
                    l.add((DataType) dt.clone());
                }
            } else {
                for (String col : getColumnNames()) {
                    DataType dt = dg.getDataDefintion(col);
                    if (dt == null) {
                        try {
                            int idx = Integer.parseInt(col);
                            dt = dg.getDataDefinitions()[idx];
                        } catch (NumberFormatException x) {
                            dt = null;  //colName is neither an existing column or an index.
                        }
                    }
                    if (dt != null) {
                        DataType cdt = (DataType) dt.clone();
                        cdt.setColumnIdx(l.size());
                        l.add(cdt);
                    }
                }
            }
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();  // this should never happen
        }
        return l.toArray(new DataType[l.size()]);
    }

    //=========================================================================
//  inner classes
//=========================================================================
    public interface DataObjectFilter extends CollectionUtil.Filter<DataObject> {
    }

    public static class DataFilterCondition extends CollectionUtil.Condition<DataObject>
            implements DataObjectFilter {
        public DataFilterCondition(Operator operator, DataFilter... filters) {
            super(operator, filters);
        }
    }

// LLY:  removing this feature for now... since it requires IpacTableParser to be in common.
//    /**
//     * This filter uses an external file as its data source.  The syntax is $fileName:columnName IN (idx_array)
//     * where idx_array is a list of row indices to use as the compare to data values.
//     *
//     */
//    public static class ExtFileFilter extends DataFilter {
//        private boolean isSetup = false;
//
//        public ExtFileFilter(String fileColName, String compareTo) {
//            super(fileColName, OpType.IN, compareTo);
//        }
//
//        @Override
//        protected void initFilter() {
//            if (!isSetup) {
//                setup();
//            }
//        }
//
//        protected void setup() {
//            init(convertColumnName(getColName()), OpType.IN, convertCompareTo(getColName(), getCompareTo()), false);
//            isSetup = true;
//        }
//
//        private static String convertCompareTo(String colName, String compareTo) {
//            if (colName == null || !colName.startsWith("$")) {
//                throw new IllegalArgumentException("External File referenced filter must start with '$'fileName:columnName");
//            }
//            String[] ary = colName.split(":", 2);
//            File infile = new File(ary[0].substring(1));
//            String colname = ary[1];
//
//            try {
//                List<Integer> indices = StringUtils.convertToListInteger(compareTo, ",");
//                IpacTableParser.MappedData values = IpacTableParser.getData(infile, indices, colname);
//                return StringUtils.toString(values.values(), ",");
//            } catch (IOException e) {
//                throw new IllegalArgumentException("Unable to gather data from external file:" + colName);
//            }
//        }
//
//        private static String convertColumnName(String colName) {
//            if (colName == null || colName.indexOf(":") < 0) {
//                throw new IllegalArgumentException("External File referenced filter must be in this format $fileName:columnName");
//            }
//            String[] ary = colName.split(":", 2);
//            return ary[1];
//        }
//    }

    public static class DataFilter extends CollectionUtil.FilterImpl<DataObject> {
        private String _colName;
        private OpType _optype;
        private String _compareTo;
        private boolean _isNumber;
        private int _colIdx; // use if _colName is not given
        private transient DataType _dataType; // calculated value;
        private transient boolean _colNameIsExpression; // _colName represents an expression, where variables are column names
        private transient Expression _expression;  // expression stored in _colName
        private transient DataType[] _colDataTypes; // data types of the columns that are variables in the expression
        private transient List<String> _inList;

        public DataFilter(int colIdx, OpType optype, String compareTo) {
            this(null, optype, compareTo, isNumber(compareTo));
            _colIdx = colIdx;
        }

        public DataFilter(String colName, OpType optype, String compareTo) {
            this(colName, optype, compareTo, isNumber(compareTo));
        }

        DataFilter(String colName, OpType optype, String compareTo, boolean isNumber) {
            init(colName, optype, compareTo, isNumber);
        }

        protected void init(String colName, OpType optype, String compareTo, boolean isNumber) {
            _colName = colName;
            _colNameIsExpression = false;
            _optype = optype;
            _compareTo = compareTo == null || compareTo.toLowerCase().equals("null") ? "" : compareTo.toLowerCase();
            _isNumber = _colName != null && _colName.equals(DataGroup.ROWID_NAME) ? true : isNumber;
            if (_optype.equals(OpType.IN)) {
                String v = _compareTo.replaceAll("[(|)|\"|']", "");
                String[] vals = v.split(",");
                _inList = new ArrayList<String>();
                for (String s : vals) {
                    _inList.add(s.trim());
                }
                Collections.sort(_inList);
            }
        }

        @Override
        public boolean accept(int rowId) {
            if (isRowIndexBased()) {
                return Collections.binarySearch(_inList, String.valueOf(rowId)) >= 0;
            } else {
                return false;
            }
        }

        @Override
        public boolean isRowIndexBased() {
            return _colName != null && _colName.equals(DataGroup.ROWID_NAME);
        }

        public boolean accept(DataObject dataObject) {
            initFilter();
            ensureType(dataObject);
            if (_colNameIsExpression) {
                for (DataType dt : _colDataTypes) {
                    try {
                        _expression.setVariableValue(dt.getKeyName(), ((Number) dataObject.getDataElement(dt)).doubleValue());
                    } catch (Exception e) {
                        return false;
                    }
                }
                return isTrue(_expression.getValue(), _optype, Double.parseDouble(_compareTo));
            }
            Object val = _colName.equals(DataGroup.ROWID_NAME) ? dataObject.getRowIdx() : dataObject.getDataElement(_dataType);
            val = val == null || val.toString().toLowerCase().equals("null") ? "" : val;
            if (_optype.equals(OpType.LIKE)) {
                return String.valueOf(val).toLowerCase().indexOf(_compareTo) >= 0;
            } else if (_optype.equals(OpType.IN)) {
                return _inList.contains(String.valueOf(val).toLowerCase());
            } else {
                if (_isNumber && isNumberType(_dataType)) {
                    double compareTo = Double.parseDouble(_compareTo);
                    return isTrue(val, _optype, compareTo);
                } else {
                    return isTrue(val, _optype, _compareTo);
                }
            }
        }

        private boolean isNumberType(DataType dt) {
            if (dt == null) return false;
            Class type = dt.getDataType();
            return type.equals(Double.class) ||
                    type.equals(Float.class) ||
                    type.equals(Long.class) ||
                    type.equals(Integer.class);
        }

        /**
         * call before every filter..
         */
        protected void initFilter() {
        }

        @Override
        public String toString() {
            return (_colName == null ? String.valueOf(_colIdx) : _colName) + _optype + _compareTo;
        }

        //=========================================================================
//   getters
//=========================================================================
        public String getColName() {
            return _colName;
        }

        public OpType getOptype() {
            return _optype;
        }

        public String getCompareTo() {
            return _compareTo;
        }

        public int getColIdx() {
            return _colIdx;
        }

//=========================================================================
//   private methods
//=========================================================================

        private void ensureType(DataObject data) {

            if (_colName != null && _colName.equals(DataGroup.ROWID_NAME)) {
                return;
            }

            if (_dataType == null && !_colNameIsExpression) {

                if (_colName == null) {
                    _dataType = data.getDataDefinitions()[_colIdx];
                    _colName = _dataType.getKeyName();
                } else {
                    _dataType = data.getDataType(_colName);
                    if (_dataType == null) {
                        try {
                            int idx = Integer.parseInt(_colName);
                            _dataType = data.getDataDefinitions()[idx];
                        } catch (NumberFormatException x) {
                            _dataType = null;  //colName is neither an existing column or an index.
                        }
                        if (_dataType == null) {
                            // try to parse expression
                            DataType[] colDefs = data.getDataDefinitions();
                            ArrayList<String> allowedVars = new ArrayList(colDefs.length);
                            for (DataType dt : colDefs) {
                                allowedVars.add(dt.getKeyName());
                            }

                            _expression = new Expression(_colName, allowedVars);
                            if (_expression.isValid()) {
                                Set<String> vars = _expression.getParsedVariables();
                                _colDataTypes = new DataType[vars.size()];
                                DataType varDataType;
                                int varIdx = 0;
                                for (String var : vars) {
                                    varDataType = data.getDataType(var);
                                    if (varDataType == null) {
                                        throw new IllegalArgumentException(var +
                                                " is not defined in this DataGroup");
                                    } else if (!isNumberType(varDataType)) {
                                        // make sure all variables are column names and these columns are numeric
                                        throw new IllegalArgumentException(var + " in expression " + _colName + " is not a numeric column");
                                    }
                                    _colDataTypes[varIdx] = varDataType;
                                    varIdx++;
                                }
                                _colNameIsExpression = true;

                            } else {
                                throw new IllegalArgumentException(_colName + ": " + _expression.getErrorMessage());
                            }
                        }
                    }
                }
            }
        }
    }

    public static class DecimateKeyFilter extends CollectionUtil.FilterImpl<DataObject> {

        String decimateKeyStr;
        DecimateKey decimateKey;
        ArrayList<String> inList;

        DataObjectUtil.DoubleValueGetter xValGetter;
        DataObjectUtil.DoubleValueGetter yValGetter;

        boolean isValid;


        public DecimateKeyFilter(String decimateKeyStr, OpType optype, String compareTo) {
            this.decimateKeyStr = decimateKeyStr;
            if (optype.equals(OpType.IN)) {
                String v = compareTo.replaceAll("[(|)|\"|']", "");
                String[] vals = v.split(",");
                inList = new ArrayList<String>();
                for (String s : vals) {
                    inList.add(s.trim());
                }
                Collections.sort(inList);
            }
            decimateKey = null;
            isValid = true;
        }

        private void initFilter(DataObject obj) {
            if (decimateKey == null && isValid) {
                try {
                    decimateKey = DecimateKey.parse(decimateKeyStr);
                    if (decimateKey == null) {
                        isValid = false;
                    }
                    xValGetter = new DataObjectUtil.DoubleValueGetter(obj.getDataDefinitions(), decimateKey.getXCol());
                    yValGetter = new DataObjectUtil.DoubleValueGetter(obj.getDataDefinitions(), decimateKey.getYCol());
                    isValid = xValGetter.isValid() && yValGetter.isValid();
                    if (!isValid) throw new IllegalArgumentException("Invalid parameter in DecimateKeyFilter");
                } catch (Exception e) {
                    System.out.println("ERROR: can not initialize DecimateKeyFilter ");
                }
            }
        }

        public boolean accept(DataObject obj) {
            initFilter(obj);
            double xVal = xValGetter.getValue(obj);
            double yVal = yValGetter.getValue(obj);
            String key = decimateKey.getKey(xVal, yVal);
            return Collections.binarySearch(inList, key) >= 0;
        }

        @Override
        public String toString() {
            return decimateKeyStr + OpType.IN + "(" + CollectionUtil.toString(inList, ",") + ")";
        }
    }

}

