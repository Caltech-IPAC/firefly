/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.astro;

import edu.caltech.ipac.util.CollectionUtil;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataGroupQuery;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.IpacTableUtil;
import edu.caltech.ipac.util.decimate.DecimateKey;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

/**
 * This class is responsible for two things.
 * One, define the syntax of the statement used for querying
 * a DataGroup.
 * And two, provides methods to read, write, and execute that statement.
 *
 * Select statement: <p>
 * <code>
 *      SELECT [INTO output_file_name] [COL column-argument]
 *      FROM input_file_name [FOR for-condition] [WITH with-condition] [ORDER_BY col-name... ASC|DESC]

 * </code>
 * condition-statement: <p>
 * <code>
 *      (col-name|index) operator value
 *
 * </code>
 *
 *      column-argument: a list of column names or indices separated by comma(','), or 'all' for all columns.
 *      for-condition: a list of condition-statement separated by 'and'.
 *      with-condition: use 'complete_header' to include header info
 *      sort_info:  ORDER_ASC_BY
 *      col-name: the name of the column.
 *      index: index of the column.  index starts from 0.
 *      operator: can be one of '> < = ! >= <= IN LIKE'
 *
 * <p>
 *
 *
 * Condition statement: <p>
 * <column_name or index>
 *
 *Sample select statements:
 * <code>
 *   select col 1,2   from big.tbl  into small.tbl with complete_header
 *   select from big.tbl  col 3,x,y for "exit_status = success and x < 3.3"
 *   select into small.tbl  col all  for  "3 > 4.3 and 3 < 5.6" from big.tbl  with complete_header
 *   select from big.tbl  for "exit_status = success and x < 3.3 order_by x,y desc"
 * </code>
 *
 * this one select column 1 and 2 from big.tble if the reqkeys contains 1,2,3,6, or 9 in the ref_file.tbl file:
 * <code>
 *   select col 1,2   from big.tbl  into small.tbl for $ref_file.tbl:reqkey IN (1,2,3,6,9)
 * </code>
 *
 * this one uses a reserved keyword ROWID to filter out only matching row numbers.
 * it will extract the first 2 rows from big.tbl where the value in column 1 is > 12.1
 * NOTE:  ROWID and column index starts from zero.
 * <code>
 *   select from big.tbl  for "1 > 12.1" and ROWID IN (0,1)
 * </code>
 *
 * @author Loi Ly
 * @version $id:$
 */
public class DataGroupQueryStatement {

    public static final String IGNORE_CHARS = "#";
    private static final String[] keywords = new String[] {"SELECT", "FOR", "FROM", "INTO", "WITH", "COL",
                        "ORDER_BY", "ORDER_BY", "DESC", "ASC"};

    private File _fromFile;
    private File _intoFile;
    private DataGroupQuery _query;

    public DataGroupQueryStatement(File fromFile, File intoFile, DataGroupQuery query) {
        _fromFile = fromFile;
        _intoFile = intoFile;
        _query = query;
    }

    /**
     * executes the  current query on the given file(s).
     * returns the resulting DataGroup.
     */
    public void executeInline() throws IpacTableException, IOException {

        DataGroup.Attribute addedComments = new DataGroup.Attribute("created from statement: " + toString());

        getQuery().doQuery(getFromFile(), getIntoFile(), addedComments);
    }

    /**
     * executes the  current query on the given file(s).
     * returns the resulting DataGroup.
     */
    public void execute(OutputStream outStream) throws IpacTableException, IOException {

        if (outStream == null) {
            if (getIntoFile() != null && getIntoFile().canWrite()) {
                outStream = new BufferedOutputStream(new FileOutputStream(getIntoFile()), IpacTableUtil.FILE_IO_BUFFER_SIZE);
            } else {
                throw new IOException("Unable to write into output file:" + getIntoFile());
            }
        }
        DataGroup.Attribute addedComments = new DataGroup.Attribute("created from statement: " + toString());

        getQuery().doQuery(getFromFile(), outStream, addedComments);
    }


    /**
     * executes the  current query on the given file(s).
     * returns the resulting DataGroup.
     */
    public DataGroup execute() throws IpacTableException, IOException {
        DataGroup source = getFromData();
        DataGroup results = getQuery().doQuery(source);
        results.addAttribute(null, "created from statement: " + toString());
        if (getIntoFile() != null) {
            IpacTableWriter.save(getIntoFile(), results);
        }
        return results;
    }

    public DataGroup getFromData() throws IpacTableException {
        if (getFromFile() == null) return null;

        return IpacTableReader.readIpacTable(getFromFile(), null, "Source Table");
    }

    public DataGroup getIntoData() throws IpacTableException {
        if (getIntoFile() == null) return null;

        return IpacTableReader.readIpacTable(getIntoFile(), "From Select Statement");
    }

    public void validate() throws InvalidStatementException {

        if (_fromFile == null) {
            throw new InvalidStatementException("From file missing from statement");
        }
    }

    /**
     * returns the String representation of this statement.
     */
    public String toString() {

        DataGroupQuery query = getQuery();

        List<String> cols = query.getColumnNames();
        List<CollectionUtil.Filter<DataObject>> filters = query.getDataFilters();
        boolean headers = query.getHeaderFilters().size() == 0;

        String stmt = "select";

        if (getIntoFile() != null) {
            stmt += " into " + getIntoFile().getPath();
        }

        stmt += " col ";
        if (cols.size() == 0) {
            stmt += "all";
        } else {
            for(Iterator<String> itr = cols.iterator(); itr.hasNext(); ) {
                stmt += itr.next();
                if (itr.hasNext()) {
                    stmt += ", ";
                }
            }
        }

        if (getFromFile() != null) {
            stmt += " from " + getFromFile().getPath();
        }

        if ( filters.size() > 0) {
            stmt += " for \"";
            for(Iterator<CollectionUtil.Filter<DataObject>> itr = filters.iterator(); itr.hasNext(); ) {
                stmt += valueOf(itr.next());
                if (itr.hasNext()) {
                    stmt += " and ";
                }
            }
            stmt += "\"";
        }

        if (headers) {
            stmt += " with complete_header";
        }

        if (query.getOrderBy() != null) {
            stmt += " ORDER_" + query.getSortDir().name() + "_BY";
            stmt += " " + query.getOrderBy();
        }

        return stmt;
    }

    /**
     * parses the statement into a DataGroupQueryStatement object.
     * @param statement
     */
    public static DataGroupQueryStatement parseStatement(String statement) throws InvalidStatementException {

        String colStmt = null;
        String fromStmt = null;
        String forStmt = null;
        String intoStmt = null;
        String withStmt = null;
        String orderBy = null;
        DataGroupQuery.SortDir orderDir = DataGroupQuery.SortDir.ASC;

        if ( !statement.trim().toUpperCase().startsWith("SELECT") ) {
            throw new IllegalArgumentException("Statement must starts with the word 'select'");
        }

        StringTokenizer tokens = new StringTokenizer(statement, " ");
        String prevKW = "";
        boolean keywordOnly = false;        // this flag is to process the last keyword.. without parameters.  ie.. ASC/DESC
        while (tokens.hasMoreTokens() || keywordOnly) {

            StringBuffer buffer = new StringBuffer();
            String kw = parseToKeyword(tokens, buffer);

            if (prevKW.equals("FOR")) {
                forStmt = buffer.toString();
            } else if ( prevKW.equals("FROM")) {
                fromStmt = buffer.toString();
            } else if ( prevKW.equals("INTO")) {
                intoStmt = buffer.toString();
            } else if ( prevKW.equals("WITH")) {
                withStmt = buffer.toString();
            } else if ( prevKW.equals("COL")) {
                colStmt = buffer.toString();
            } else if ( prevKW.equals("ORDER_BY")) {
                orderBy = buffer.toString().trim();
            } else if ( prevKW.equals("DESC")) {
                orderDir = DataGroupQuery.SortDir.DESC;
            }
            if (keywordOnly) {
                keywordOnly = false;
            } else {
                prevKW = kw;
                keywordOnly = kw != null && !tokens.hasMoreTokens();
            }
        }

        DataGroupQuery query = new DataGroupQuery();
        // for now, always query with complete headers
        // no headers filter will be added.
        if (colStmt != null && !colStmt.trim().toUpperCase().equals("ALL")) {
            query.addColumns(split(colStmt, ","));
        }

        if (forStmt != null) {
//            query.addDataFilters(parseForStmt(forStmt));
            String[] filters = split(forStmt, " and ");
            for (String s : filters) {
                query.addDataFilters(parseFilter(s));
            }
        }
        File fromFile = fromStmt == null ? null : new File(fromStmt.trim());
        File intoFile = intoStmt == null ? null : new File(intoStmt.trim());
        if(orderBy != null) {
            String[] cols = orderBy.replaceAll("\\s", "").split(",");
            query.setOrderBy(cols);
        }
        if(orderBy != null) {
            query.setSortDir(orderDir);
        }

        DataGroupQueryStatement stmt = new DataGroupQueryStatement( fromFile, intoFile, query );
        stmt.validate();

        return stmt;
    }

    private static String[] split(String s, String pattern) {
        String[] ss = s.split(pattern);
        for (int i = 0; i < ss.length; i++) {
            ss[i] = ss[i].trim();
        }
        return ss;
    }

    private static String parseToKeyword(StringTokenizer tokens, StringBuffer buffer) {

        while (tokens.hasMoreTokens()) {
            String t = tokens.nextToken();

            for (String kw : keywords) {
                if (t.toUpperCase().equals(kw)) {
                    return kw;
                }
            }
            buffer.append(" ").append(t);
        }
        return null;
    }

//=========================================================================
//  getters/setters
//=========================================================================

    public File getFromFile() {
        return _fromFile;
    }

    public File getIntoFile() {
        return _intoFile;
    }

    public DataGroupQuery getQuery() {
        return _query;
    }

    public void setFromFile(File fromFile) {
        _fromFile = fromFile;
    }

    public void setIntoFile(File intoFile) {
        _intoFile = intoFile;
    }

    public void setQuery(DataGroupQuery query) {
        _query = query;
    }

//=========================================================================
//  private/protected methods
//=========================================================================
    public static CollectionUtil.Filter<DataObject> parseFilter(String statement) {
        String[] vals = statement.split("\\s", 3);
        if ( vals.length != 3 ) {
            throw new IllegalArgumentException("Invalid FOR condition.");
        }
        String col = vals[0].replaceAll(IGNORE_CHARS, "");
        DataGroupQuery.OpType optype = DataGroupQuery.getOpType(vals[1]);
        if (vals[0].startsWith(DecimateKey.DECIMATE_KEY+"(")) {
            return new DataGroupQuery.DecimateKeyFilter(vals[0], optype, vals[2]);
        } else if (DataGroupQuery.isNumber(col) ) {
            return new DataGroupQuery.DataFilter(Integer.parseInt(vals[0]), optype, vals[2]);
//        } else if(col.startsWith("$")) {
//            return new DataGroupQuery.ExtFileFilter(col, vals[2]);
        } else {
            return new DataGroupQuery.DataFilter(col, optype, vals[2]);
        }
    }

    public static CollectionUtil.Filter<DataObject>[] parseForStmt(String forStmt) {
        String[] filters = split(forStmt, " and ");
        CollectionUtil.Filter<DataObject>[] retval = (CollectionUtil.Filter<DataObject>[])(new CollectionUtil.Filter[filters.length]);
        for (int i = 0; i < filters.length; i++) {
            retval[i] = parseFilter(filters[i]);
        }
        return retval;
    }

//    public static DataGroupQuery.DataObjectFilter[] parseForStmt(String forStmt) {
//        String s = forStmt.replaceFirst("\\([^()]+\\)", "@");
//        System.out.println(forStmt);
//        System.out.println(s);
//        String v = forStmt.substring(s.indexOf("@")+1, forStmt.length() - s.length() +1);
//        System.out.println(v);
//        return null;
//    }

    public static DataGroupQuery.DataFilterCondition parseCondition(String statement) {
        if (statement.indexOf(" and ") >= 0 && statement.indexOf(" or ") >= 0) {
            throw new IllegalArgumentException(
                    "Ambiguous condition: both 'or' and 'and' in one condition.  Use '()' to define order.");
        }
        if (statement.indexOf("(") >= 0 || statement.indexOf(")") >= 0) {
            throw new IllegalArgumentException(
                    "Invalid open/close parenthesis pair");
        }
        boolean isAnd = statement.indexOf(" and ") >=0;
        String[] filters = isAnd ? statement.split(" and ") :
                            statement.split(" or ");
        DataGroupQuery.DataFilterCondition c = new DataGroupQuery.DataFilterCondition(isAnd ?
                                CollectionUtil.Condition.Operator.and : CollectionUtil.Condition.Operator.or);
        for (String s : filters) {
            c.addFilter(parseExpression(s));
        }
        return c;
    }

    public static CollectionUtil.Filter<DataObject> parseExpression(String statement) {
        String[] vals = statement.split("\\s", 3);
        if ( vals.length != 3 ) {
            throw new IllegalArgumentException("Invalid FOR condition.");
        }
        DataGroupQuery.OpType optype = DataGroupQuery.getOpType(vals[1]);
        if (vals[0].startsWith(DecimateKey.DECIMATE_KEY+"(")) {
            return new DataGroupQuery.DecimateKeyFilter(vals[0], optype, vals[2]);
        }
        if (DataGroupQuery.isNumber(vals[0]) ) {
            return new DataGroupQuery.DataFilter(Integer.parseInt(vals[0]), optype, vals[2]);
        } else {
            return new DataGroupQuery.DataFilter(vals[0], optype, vals[2]);
        }
    }

    public static String valueOf(CollectionUtil.Filter<DataObject> filter) {

        if (filter == null) return "";
        if (filter instanceof DataGroupQuery.DataFilter) {
            DataGroupQuery.DataFilter f = (DataGroupQuery.DataFilter)filter;
            String col = f.getColName() != null ? f.getColName() :
                        String.valueOf(f.getColIdx());
            return String.format("%s %s %s", col, f.getOptype().getOp(), f.getCompareTo());
        } else if (filter instanceof DataGroupQuery.DataFilterCondition) {
            DataGroupQuery.DataFilterCondition c = (DataGroupQuery.DataFilterCondition)filter;
            StringBuffer str = new StringBuffer("(");
            for(Iterator<CollectionUtil.Filter<DataObject>> itr = c.getFilters().iterator(); itr.hasNext(); ) {
                str.append(valueOf(itr.next()));
                if (itr.hasNext()) {
                    str.append( (c.getOperator().equals(CollectionUtil.Condition.Operator.and) ? " and " : " or "));
                }
            }
            str.append(")");
            return str.toString();
        }
        return "";
    }

    public static void main(String[] args) {
        NumberFormat format = NumberFormat.getInstance();

        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long allocatedMemory = runtime.totalMemory();
        System.out.println("memory used/max:" + allocatedMemory/1024 + "/" + maxMemory/1024);

        // testing ROWID nested filtering.
        String sql = "select col 1,2,3,5,8,9,10 from " + args[0] + "  for 1 > 0 into " + args[0] + ".done";
        try {

            long ctime = System.currentTimeMillis();
            DataGroupQueryStatement.parseStatement(sql).execute(new FileOutputStream(new File(args[0] + ".done1")));
            System.out.println("elapsed time:" + (System.currentTimeMillis() - ctime) / 1000 + "s");

            runtime = Runtime.getRuntime();
            maxMemory = runtime.maxMemory();
            allocatedMemory = runtime.totalMemory();
            System.out.println("memory used/max:" + allocatedMemory/1024 + "/" + maxMemory/1024);


            ctime = System.currentTimeMillis();
            DataGroupQueryStatement.parseStatement(sql).executeInline();
            System.out.println("elapsed time:" + (System.currentTimeMillis() - ctime) / 1000 + "s");

            runtime = Runtime.getRuntime();
            maxMemory = runtime.maxMemory();
            allocatedMemory = runtime.totalMemory();
            System.out.println("memory used/max:" + allocatedMemory/1024 + "/" + maxMemory/1024);


            ctime = System.currentTimeMillis();
            DataGroupQueryStatement.parseStatement(sql).execute();
            System.out.println("elapsed time:" + (System.currentTimeMillis() - ctime) / 1000 + "s");

            runtime = Runtime.getRuntime();
            maxMemory = runtime.maxMemory();
            allocatedMemory = runtime.totalMemory();
            System.out.println("memory used/max:" + allocatedMemory/1024 + "/" + maxMemory/1024);

        } catch (IpacTableException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidStatementException e) {
            e.printStackTrace();
        }


        if (true) return;


//        String statement = args == null || args.length == 0 ?
//                "select into /Users/loi/out.tbl col 1, 3, blendid, x, y " +
//                "from /Users/loi/mosaic_detect.tbl " +
//                "for 1 > 5 and y ! 70.3 order_by blendid, x,y desc" :
//                args[0];
//
//        DataGroupQueryStatement queryStatement = null;
//        try {
//            queryStatement = DataGroupQueryStatement.parseStatement(statement);
//        } catch (InvalidStatementException e) {
//            e.printStackTrace();
//        }
//        try {
//            queryStatement.execute();
//        } catch (IpacTableException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        String forStmt = "((a = 7 and b = 4) or (c = 5)) and d = 6";
        parseForStmt(forStmt);

    }

}

