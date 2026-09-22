/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.table;

import edu.caltech.ipac.firefly.ConfigTest;
import edu.caltech.ipac.firefly.data.DecimateInfo;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.SortInfo;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.db.DbAdapter;
import edu.caltech.ipac.firefly.server.db.DbMonitor;
import edu.caltech.ipac.firefly.server.db.DuckDbAdapter;
import edu.caltech.ipac.firefly.server.db.DuckDbReadable;
import edu.caltech.ipac.firefly.server.db.EmbeddedDbUtil;
import edu.caltech.ipac.firefly.server.db.HsqlDbAdapter;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.DecimationProcessor;
import edu.caltech.ipac.firefly.server.query.EmbeddedDbProcessor;
import edu.caltech.ipac.firefly.server.query.SearchManager;
import edu.caltech.ipac.firefly.server.query.tables.IpacTableFromSource;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.util.FileLoader;
import edu.caltech.ipac.util.decimate.DecimateKey;
import org.apache.logging.log4j.Level;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static edu.caltech.ipac.firefly.data.TableServerRequest.TBL_FILE_TYPE;
import static edu.caltech.ipac.firefly.server.db.DuckDbAdapter.*;
import static edu.caltech.ipac.firefly.server.util.QueryUtil.SEARCH_REQUEST;
import static edu.caltech.ipac.util.FormatUtil.Format;
import static org.junit.Assert.*;

public class DuckDbAdapterTest extends ConfigTest {

	@Before
	public void setUp() {
		if (false) Logger.setLogLevel(Level.TRACE);			// for debugging.
	}

	@After
	public void tearDown() {
		DbMonitor.cleanup(true, true);
	}

	/**
	 * test results based on the constructed TableServerRequest
	 */
	@Test
	public void testParquetDbAdapter() throws DataAccessException {
		File testFile = FileLoader.resolveFile(DuckDbAdapterTest.class, "/iris.parquet");

		TableServerRequest req = new TableServerRequest(IpacTableFromSource.PROC_ID);
		req.setParam(ServerParams.SOURCE, testFile.getAbsolutePath());
		req.setFilters(List.of("\"sepal.width\" > 3"));
		var dgp = new SearchManager().getDataGroup(req);
		var data = dgp.getData();

		// test total rows, returned rows, and cols
		assertEquals(67, dgp.getRowCount());
		assertEquals(7, data.getDataDefinitions().length);
	}

	/**
	 * test reading a source file directly into a DataGroup, without a dbFile
	 */
	@Test
	public void testReadIntoDataGroup() throws DataAccessException {
		File testFile = FileLoader.resolveFile(DuckDbAdapterTest.class, "/iris.parquet");

		DataGroup info = DuckDbReadable.getInfo(Format.PARQUET, testFile.getAbsolutePath());
		DataGroup table = DuckDbReadable.read(Format.PARQUET, testFile.getAbsolutePath());

		assertEquals("all of the rows are read", info.size(), table.size());
		assertEquals("same columns as getInfo", info.getDataDefinitions().length, table.getDataDefinitions().length);
		assertEquals("no ROW_IDX/ROW_NUM added", 5, table.getDataDefinitions().length);

		// column info from the source is applied to the returned table
		DataType sepalWidth = table.getDataDefintion("sepal.width");
		assertNotNull("sepal.width column exists", sepalWidth);
		assertEquals(Double.class, sepalWidth.getDataType());
		assertEquals(3.5, (Double) table.getData("sepal.width", 0), 0.0001);
		assertEquals("Setosa", table.getData("variety", 0));

		// same table read via a detached adapter
		DataGroup viaAdapter = DuckDbReadable.getDetachedAdapter(Format.PARQUET).read(testFile.getAbsolutePath());
		assertNull("not tied to a dbFile", DuckDbReadable.getDetachedAdapter(Format.PARQUET).getDbFile());
		assertEquals(table.size(), viaAdapter.size());
	}

	/** Ensures direct read and ingest paths produce the same table metadata. */
	@Test
	public void testReadMatchesIngestPath() throws Exception {
		String pq = voTableParquet();

		// -- path A: ingest into a dbFile, then read it back out the way the app does
		DuckDbReadable attached = DuckDbReadable.castInto(Format.PARQUET, new DuckDbAdapter(
				new File(ServerContext.getWorkingDir(), "read-cmp.duckdb")));
		attached.initDbFile();
		attached.ingestDataDirectly(pq, null);
		DataGroup viaIngest = attached.execQuery("select * from DATA", "DATA");
		attached.close(true);

		// -- path B: read straight into a DataGroup
		DataGroup viaRead = DuckDbReadable.read(Format.PARQUET, pq);

		assertEquals("ingest adds ROW_IDX/ROW_NUM; read does not",
				viaIngest.getDataDefinitions().length - 2, viaRead.getDataDefinitions().length);

		for (DataType col : viaRead.getDataDefinitions()) {
			DataType ingested = viaIngest.getDataDefintion(col.getKeyName());
			assertNotNull("column %s is in both".formatted(col.getKeyName()), ingested);
			assertEquals("column info for " + col.getKeyName(), colInfo(ingested), colInfo(col));
		}

		assertEquals("title", viaIngest.getTitle(), viaRead.getTitle());
		assertEquals("keywords", meta(viaIngest.getTableMeta().getKeywords()), meta(viaRead.getTableMeta().getKeywords()));
		assertEquals("attributes", meta(viaIngest.getAttributeList()), meta(viaRead.getAttributeList()));
		assertEquals("params", viaIngest.getParamInfos().size(), viaRead.getParamInfos().size());
		assertEquals("groups", viaIngest.getGroupInfos().size(), viaRead.getGroupInfos().size());
		assertEquals("resources", viaIngest.getResourceInfos().size(), viaRead.getResourceInfos().size());
		// the VOTable declares one of each, so these comparisons are 1 == 1 rather than 0 == 0
		assertEquals("params survive both paths", 1, viaRead.getParamInfos().size());
		assertEquals("groups survive both paths", 1, viaRead.getGroupInfos().size());
		assertEquals("links survive both paths", 1, viaRead.getLinkInfos().size());
		assertEquals("resources survive both paths", 1, viaRead.getResourceInfos().size());

		assertEquals("row count", viaIngest.size(), viaRead.size());
		for (int r = 0; r < viaRead.size(); r++) {
			for (DataType col : viaRead.getDataDefinitions()) {
				assertEquals("%s[%d]".formatted(col.getKeyName(), r),
						String.valueOf(viaIngest.getData(col.getKeyName(), r)),
						String.valueOf(viaRead.getData(col.getKeyName(), r)));
			}
		}
	}

	/** Parquet file with an embedded IVOA VOTable. */
	private static String voTableParquet() {
		return FileLoader.resolveFile(DuckDbAdapterTest.class, "/iris-with-votable.parquet").getAbsolutePath();
	}

	/** Ensures schema-derived array info is preserved when absent from the VOTable. */
	@Test
	public void testGetInfoUsesSourceSchema() throws DataAccessException {
		String pq = voTableParquet();
		DataGroup info = DuckDbReadable.getInfo(Format.PARQUET, pq);

		assertEquals("columns come from the file", 6, info.getDataDefinitions().length);
		assertEquals("row count", 150, info.size());

		DataType flux = info.getDataDefintion("flux");
		assertNotNull("flux column exists", flux);
		assertTrue("flux is an array in the file, though the VOTable does not say so", flux.isArrayType());
		assertEquals("the VOTable's meta is still applied", "Jy", flux.getUnits());

		DataType sepalLength = info.getDataDefintion("sepal.length");
		assertNotNull("sepal.length column exists", sepalLength);
		assertEquals(Double.class, sepalLength.getDataType());
		assertEquals("cm", sepalLength.getUnits());
		assertEquals("phys.size.length", sepalLength.getUCD());
		assertEquals("sepal length", sepalLength.getDesc());

		// and read() sees the same columns it will decode the ResultSet with
		DataGroup table = DuckDbReadable.read(Format.PARQUET, pq);
		assertEquals(info.getDataDefinitions().length, table.getDataDefinitions().length);
		assertTrue("flux stays an array", table.getDataDefintion("flux").isArrayType());
	}

	/** Ensures DD_COLS covers all supported DataType properties. */
	@Test
	public void testDdColsCarriesEveryDataTypeProperty() throws Exception {
		Set<String> notCarried = Set.of(
				"KeyName",      // identifies the column; applying it would rename the column it is applied to
				"DataType",     // derived from TypeDesc, so that an unknown desc keeps the source's class
				"PrefWidth");   // display only, never persisted

		DataType src = new DataType("acol", String.class);
		Map<String, Object> expected = new LinkedHashMap<>();
		for (Method setter : DataType.class.getMethods()) {
			String prop = setter.getName().startsWith("set") ? setter.getName().substring(3) : null;
			if (prop == null || setter.getParameterCount() != 1 || notCarried.contains(prop)) continue;
			Object val = sampleFor(setter.getParameterTypes()[0], prop);
			if (val == null) continue;         // no sample for this kind of property; nothing to assert
			setter.invoke(src, val);
			expected.put(prop, val);
		}
		assertTrue("found DataType properties to check", expected.size() > 15);

		DataType dest = new DataType("acol", String.class);
		EmbeddedDbUtil.applyInfoToDataType(dest, src);

		List<String> missing = new ArrayList<>();
		for (Map.Entry<String, Object> e : expected.entrySet()) {
			Method getter = getterFor(e.getKey());
			assertNotNull("no getter for " + e.getKey(), getter);
			if (!carried(e.getValue(), getter.invoke(dest)))  missing.add(e.getKey());
		}
		assertTrue("not carried by DD_COLS: %s -- add them to DD_COLS, or to notCarried above with a reason"
				.formatted(missing), missing.isEmpty());
	}

	/** LinkInfo and friends have no equals(), so a carried collection is compared by size rather than by value */
	private static boolean carried(Object want, Object got) {
		if (want instanceof Collection<?> c)  return got instanceof Collection<?> g && g.size() == c.size();
		return want.equals(got);
	}

	/** a distinctive, non-default value for a property of the given type, or null if we have no sample for it */
	private static Object sampleFor(Class<?> type, String prop) {
		if (type == String.class)                   return "val_" + prop;
		if (type == int.class)                      return 42;
		if (type == boolean.class)                  return true;
		if (type == DataType.Visibility.class)      return DataType.Visibility.hidden;
		if (type == List.class)                     return List.of(new LinkInfo());
		return null;
	}

	private static Method getterFor(String prop) {
		for (String prefix : new String[]{"get", "is"}) {
			try { return DataType.class.getMethod(prefix + prop); } catch (NoSuchMethodException ignored) {}
		}
		return null;
	}

	/** Ensures CSV columns are correctly typed and values are preserved. */
	@Test
	public void testReadAnyFormatCsv() throws Exception {
		File csv = new File(ServerContext.getWorkingDir(), "readany.csv");
		Files.writeString(csv.toPath(), """
				ra,dec,name,nobs,flag
				5.5,14.02,alpha,3,true
				-3.25,14.05,beta,7,false
				""");
		try {
			DataGroup table = TableUtil.readAnyFormat(csv);

			assertEquals("rows", 2, table.size());
			assertEquals("columns", 5, table.getDataDefinitions().length);
			assertEquals(Double.class, table.getDataDefintion("ra").getDataType());
			assertEquals(String.class, table.getDataDefintion("name").getDataType());
			assertEquals(Boolean.class, table.getDataDefintion("flag").getDataType());
			assertEquals(5.5, (Double) table.getData("ra", 0), 0.0001);
			assertEquals("beta", table.getData("name", 1));
			assertEquals(3L, table.getData("nobs", 0));
		} finally {
			csv.delete();
		}
	}

	/** Ensures CSV comment lines are automatically detected. */
	@Test
	public void testCommentLinesAreDetected() throws Exception {
		File csv = new File(ServerContext.getWorkingDir(), "commented.csv");
		Files.writeString(csv.toPath(), """
				#Table1
				ra,dec,name
				1.5,2.5,alpha
				#a comment in the middle
				3.5,4.5,beta
				""");
		try {
			DataGroup table = DuckDbReadable.read(Format.CSV, csv.getAbsolutePath());

			assertEquals("comment lines are not rows", 2, table.size());
			assertEquals("the header is still the header", 3, table.getDataDefinitions().length);
			assertNotNull("ra column exists", table.getDataDefintion("ra"));
			assertEquals(Double.class, table.getDataDefintion("ra").getDataType());
			assertEquals(3.5, (Double) table.getData("ra", 1), 0.0001);
			assertEquals("beta", table.getData("name", 1));
		} finally {
			csv.delete();
		}
	}

	/** Ensures files under allowed directories are readable. */
	@Test
	public void testReadFromSubdirOfAllowedDir() throws Exception {
		File sub = new File(ServerContext.getWorkingDir(), "sub_a/sub_b");
		sub.mkdirs();
		File csv = new File(sub, "nested.csv");
		Files.writeString(csv.toPath(), "ra,dec\n1.5,2.5\n");
		try {
			DataGroup table = DuckDbReadable.read(Format.CSV, csv.getAbsolutePath());
			assertEquals("a file under an allowed dir is readable", 1, table.size());
			assertEquals(1.5, (Double) table.getData("ra", 0), 0.0001);
		} finally {
			csv.delete();
			sub.delete();
			sub.getParentFile().delete();
		}
	}

	/** Ensures remote sources are rejected. */
	@Test
	public void testRemoteSourceIsRefused() {
		try {
			DuckDbReadable.read(Format.CSV, "https://no-such-host-xyz.example/x.csv");
			fail("a remote source should be refused while external access is off");
		} catch (DataAccessException expected) {
			String msg = String.valueOf(expected.getMessage()) + expected.getCause();
			assertTrue("refused on configuration, not by a failed network call: " + msg,
					msg.toLowerCase().contains("disabled") || msg.toLowerCase().contains("permission"));
		}
	}

	/** Ensures xtype survives the DuckDB ingest/read round-trip. */
	@Test
	public void testXTypeSurvivesTheDdTable() throws Exception {
		DataType ra = new DataType("ra", Double.class);
		ra.setXType("adql:TIMESTAMP");
		ra.setUType("ss:my.utype");
		ra.setUnits("deg");
		DataGroup dg = new DataGroup("xtype test", List.of(ra));
		dg.add(new Object[]{1.5});

		DuckDbAdapter db = new DuckDbAdapter(new File(ServerContext.getWorkingDir(), "xtype.duckdb"));
		db.initDbFile();
		db.ingestData(() -> dg, db.getDataTable());
		DataGroup back = db.execQuery("select * from DATA", "DATA");
		db.close(true);

		DataType col = back.getDataDefintion("ra");
		assertNotNull("ra survived the ingest", col);
		assertEquals("xtype survives the DD table", "adql:TIMESTAMP", col.getXType());
		assertEquals("utype still survives too", "ss:my.utype", col.getUType());
		assertEquals("deg", col.getUnits());
	}

	/** every bit of column info the two paths are expected to agree on */
	private static String colInfo(DataType dt) {
		return "%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%d|%s|%s|%s|%s|%s|%s".formatted(dt.getKeyName(), dt.getDataType(),
				dt.getTypeDesc(), dt.getLabel(), dt.getUnits(), dt.getUCD(), dt.getUType(), dt.getXType(), dt.getArraySize(),
				dt.isArrayType(), dt.getVisibility(), dt.getWidth(), dt.getPrecision(), dt.getID(), dt.getFormat(),
				dt.getNullString(), dt.getDesc(), dt.getLinkInfos().size());
	}

	private static String meta(List<DataGroup.Attribute> attribs) {
		return attribs.stream().map(a -> "%s=%s(kw:%s)".formatted(a.getKey(), a.getValue(), a.isKeyword()))
				.sorted().collect(Collectors.joining(", "));
	}

	/**
	 * test DuckDB decimate_key() function
	 */
	@Test
	public void testDecimateKey() throws Exception {
		// stats from file
		//┌─────────────┬─────────────┬───────────┬───────────┬───────────────┬─────────────────────┬──────────────────────┬─────────┐
		//│ column_name │ column_type │    min    │    max    │ approx_unique │         avg         │         std          │  count  │
		//├─────────────┼─────────────┼───────────┼───────────┼───────────────┼─────────────────────┼──────────────────────┼─────────┤
		//│ ra          │ DOUBLE      │ 149.41147 │ 150.82684 │        122939 │ 150.10669643252479  │ 0.4002817385008964   │ 1005002 │
		//│ dec         │ DOUBLE      │ 1.498815  │ 2.91273   │        269551 │ 2.219145634837543   │ 0.3942764400346131   │ 1005002 │

		double xMin = 149.41147;
		double xMax = 150.82684;
		double yMin = 1.498815;
		double yMax = 2.91273;


	// code from doDecimation; to calculate nXs, nYs, xUnit, yUnit
		int nXs = (int)Math.sqrt(100_000 * 1.0);	// number of cells on the x-axis
		int nYs = (int)Math.sqrt(100_000 / 1.0);  	// number of cells on the x-axis

		double xUnit = (xMax - xMin)/nXs;        // the x size of a cell
		double yUnit = (yMax - yMin)/nYs;        // the y size of a cell

		// case when min and max values are the same
		if (xUnit == 0) xUnit = Math.abs(xMin) > 0 ? Math.abs(xMin) : 1;
		if (yUnit == 0) yUnit = Math.abs(yMin) > 0 ? Math.abs(yMin) : 1;

		// increase cell size a bit to include max values into grid
		xUnit += xUnit/1000.0/nXs;
		yUnit += yUnit/1000.0/nYs;
	//-----------------

		DecimateKey deciKey = new DecimateKey(xMin, yMin, nXs, nYs, xUnit, yUnit);

		File testFile = FileLoader.resolveFile(DuckDbAdapterTest.class, "/table_1mil.csv");

		TableServerRequest req = new TableServerRequest(IpacTableFromSource.PROC_ID);
		req.setParam(ServerParams.SOURCE, testFile.getAbsolutePath());
		req.setInclColumns(String.format("\"ra\", \"dec\", decimate_key(\"ra\", \"dec\", %.15f, %.15f, %d, %d, %.15f, %.15f) as dkey", xMin, yMin, nXs, nYs, xUnit, yUnit));
		var dgp = new SearchManager().getDataGroup(req);
		var dbData = dgp.getData();
		var csvData = DuckDbReadable.read(Format.CSV, testFile.getAbsolutePath());

		for (int i = 0; i < csvData.size(); i++) {
			DataObject row = csvData.get(i);

			double xval = row.getDouble("ra", 0);
			double yval = row.getDouble("dec", 0);
			String dkey = deciKey.getKey(xval, yval);
			assertEquals(String.format("row: %d(%f,%f)-(%f,%f)", i, xval, yval, dbData.getData("ra", i), dbData.getData("dec", i)),
					dkey,
					dbData.getData("dkey", i)
					);
		}
	}

	/**
	 * normal SQL regression test
	 */
	@Test
	public void testNormalQuery() throws DataAccessException {
		File duckFile = FileLoader.resolveFile(DuckDbAdapterTest.class, "/cars.csv");

		TableServerRequest duckReq = new TableServerRequest(IpacTableFromSource.PROC_ID);
		duckReq.setParam(ServerParams.SOURCE, duckFile.getAbsolutePath());
		duckReq.setPageSize(-1);

		TableServerRequest hsqlReq = (TableServerRequest) duckReq.cloneRequest();
		hsqlReq.setMeta(TBL_FILE_TYPE, HsqlDbAdapter.NAME);

		SearchManager sm = new SearchManager();

		DataGroup duckTbl = sm.getDataGroup(duckReq).getData();
		DataGroup hsqlTbl = sm.getDataGroup(hsqlReq).getData();

		fullTableTest(duckTbl, hsqlTbl);

		// apply filter
		duckReq.setInclColumns(Stream.of("model", "hp", "gear").map(s -> "\"" + s + "\"").collect(Collectors.joining(", ")) );
		hsqlReq.setInclColumns(duckReq.getInclColumns());
		duckReq.setFilters(List.of("\"gear\" > 3"));
		hsqlReq.setFilters(duckReq.getFilters());

		duckTbl = sm.getDataGroup(duckReq).getData();
		hsqlTbl = sm.getDataGroup(hsqlReq).getData();

		fullTableTest(duckTbl, hsqlTbl);

		// apply filter and sort
		duckReq.setSortInfo(SortInfo.parse("SortInfo:DESC,\"hp\""));
		hsqlReq.setSortInfo(duckReq.getSortInfo());

		duckTbl = sm.getDataGroup(duckReq).getData();
		hsqlTbl = sm.getDataGroup(hsqlReq).getData();

		fullTableTest(duckTbl, hsqlTbl);
	}

	/**
	 * compairing decimation between HsqlDB and DuckDB
	 */
	@Test
	public void testDecimateQuery() throws DataAccessException {
		File duckFile = FileLoader.resolveFile(DuckDbAdapterTest.class, "/table_1mil.csv");

		TableServerRequest duck = new TableServerRequest(IpacTableFromSource.PROC_ID);
		duck.setParam(ServerParams.SOURCE, duckFile.getAbsolutePath());
		duck.setInclColumns(Stream.of("ra", "dec").map(s -> "\"" + s + "\"").collect(Collectors.joining(", ")) );
		duck.setPageSize(-1);

		TableServerRequest hsql = (TableServerRequest) duck.cloneRequest();
		hsql.setMeta(TBL_FILE_TYPE, HsqlDbAdapter.NAME);

		TableServerRequest duckReq = new TableServerRequest(DecimationProcessor.ID);
		duckReq.setParam(SEARCH_REQUEST, JsonTableUtil.toJsonTableRequest(duck).toJSONString());
		duckReq.setParam(DecimationProcessor.DECIMATE_INFO, new DecimateInfo("ra", "dec").toString());
		duckReq.setPageSize(-1);
		duckReq.setSortInfo(new SortInfo("dkey"));

		TableServerRequest hsqlReq = (TableServerRequest) duckReq.cloneRequest();
		hsqlReq.setParam(SEARCH_REQUEST, JsonTableUtil.toJsonTableRequest(hsql).toJSONString());

		DataGroup duckTbl = new SearchManager().getDataGroup(duckReq).getData();
		DataGroup hsqlTbl = new SearchManager().getDataGroup(hsqlReq).getData();		// failed. TODO

		fullTableTest(duckTbl, hsqlTbl);

		// apply filter and sort to the original table, then check again.
		duck.setSqlFilter("\"dec\" > 2");
		duck.setSortInfo(new SortInfo("icmag"));
		// create same req for HsqlDB version
		hsql = (TableServerRequest) duck.cloneRequest();
		hsql.setMeta(TBL_FILE_TYPE, HsqlDbAdapter.NAME);

		// update deci request
		duckReq.setParam(SEARCH_REQUEST, JsonTableUtil.toJsonTableRequest(duck).toJSONString());
		hsqlReq.setParam(SEARCH_REQUEST, JsonTableUtil.toJsonTableRequest(hsql).toJSONString());

		duckTbl = new SearchManager().getDataGroup(duckReq).getData();
		hsqlTbl = new SearchManager().getDataGroup(hsqlReq).getData();

		// then do the same test
		fullTableTest(duckTbl, hsqlTbl);

	}

	@Test
	public void testCleanup() throws DataAccessException {

		File duckFile = FileLoader.resolveFile(DuckDbAdapterTest.class, "/iris.parquet");

		TableServerRequest treq = new TableServerRequest(IpacTableFromSource.PROC_ID);
		treq.setParam(ServerParams.SOURCE, duckFile.getAbsolutePath());
		treq.setPageSize(1);  // we won't be testing data

		EmbeddedDbProcessor proc = (EmbeddedDbProcessor) SearchManager.getProcessor(IpacTableFromSource.PROC_ID);
		DbAdapter dbAdapter = proc.getDbAdapter(treq);

		assertEquals("DuckDB is used for parquet files", DuckDbReadable.NAME, dbAdapter.getName());

		// load the data into the database; 4 tables will be created.  DATA, DATA_DD, DATA_META, and DATA_AUX
		proc.getData(treq);
		assertEquals("Data table and its associated tables are created", 4, dbAdapter.getTableNames().size());

		// sort by model.  this should create a resultset: a thin (ROW_IDX, ROW_NUM) index table, a view
		// joining it back to DATA, plus that view's own _DD, _META, and _AUX -- 5 new objects total.
		treq.setSortInfo(new SortInfo("sepal.width"));
		proc.getData(treq);
		assertEquals("New set of temp tables created for the request", 9, dbAdapter.getTableNames().size());

		dbAdapter.clearCachedData();
		assertEquals("Temp tables are removed", 4, dbAdapter.getTableNames().size());

		dbAdapter.close(true);
		assertFalse("DuckDb file is deleted", dbAdapter.getDbFile().exists());
	}

	@Test
	public void testLikeSubstitution() {
		// replace uppercase LIKE
		assertEquals("WHERE col ILIKE '%abc%'",
				replaceLike("WHERE col LIKE '%abc%'"));

		// replace lower case like
		assertEquals("WHERE col ILIKE '%xyz%'",
				replaceLike("WHERE col like '%xyz%'"));

		// ignore when inside of single quotes
		assertEquals("WHERE col = 'This is LIKE something'",
				replaceLike("WHERE col = 'This is LIKE something'"));

		// ignore when inside of double quotes
		assertEquals("WHERE \"col like\" = 'Some string LIKE pattern'",
				replaceLike("WHERE \"col like\" = 'Some string LIKE pattern'"));

		// LIKE outside and inside quotes
		assertEquals("WHERE abc ILIKE '%abc%' and col = 'Some string LIKE pattern'",
				replaceLike("WHERE abc like '%abc%' and col = 'Some string LIKE pattern'"));

		// no match; return original
		assertEquals("WHERE col like-one '%xyz%'",
				replaceLike("WHERE col like-one '%xyz%'"));

		// mixing single and double quotes
		assertEquals("'test ILIKE inside\" badly quoted ILIKE ",
				replaceLike("'test like inside\" badly quoted like "));
	}

	@Test
	public void testDuckDbExternalAccessIsRestrictedToAllowedDirs() throws Exception {
		DuckDbAdapter db = new DuckDbAdapter((File) null);

		File allowedCsv = new File(ServerContext.getWorkingDir(), "duckdb-allowed.csv");
		Files.writeString(allowedCsv.toPath(), "id,name\n1,allowed\n");
		assertEquals(1, db.execQuery("select * from read_csv('%s')".formatted(allowedCsv.getAbsolutePath()), null).size());

		File deniedCsv = new File("./duckdb-denied.csv");
		Files.writeString(deniedCsv.toPath(), "id,name\n1,denied\n");
		try {
			db.execQuery("select * from read_csv('%s')".formatted(deniedCsv.getAbsolutePath()), null);
			fail("DuckDB should block reads outside allowed_directories");
		} catch (DataAccessException expected) {
			assertNotNull(expected.getMessage());
		} finally {
			allowedCsv.delete();
			deniedCsv.delete();
		}
	}

//====================================================================
//  PRIVATE section
//====================================================================

	private static void fullTableTest(DataGroup t1, DataGroup t2, String ...checkCols) {
		assertEquals(t1.size(), t2.size());
		assertEquals(t1.getDataDefinitions().length, t2.getDataDefinitions().length);

		DataType[] colsTocheck = checkCols != null ? Arrays.stream(checkCols).map(t1::getDataDefintion).toArray(DataType[]::new) : t1.getDataDefinitions();

		for (int i = 0; i < t1.size(); i++) {
			DataObject t1row = t1.get(i);
			DataObject t2row = t2.get(i);
			for (DataType c : colsTocheck) {
				assertEquals(String.format("Cell (%d,%s)", i, c.getKeyName()), getVal(t1row, c), getVal(t2row, c));
			}
		}
	}

	private static Object getVal(DataObject row, DataType c) {
		return c.isNumeric() ? row.getDouble(c.getKeyName(), 0) : row.getStringData(c.getKeyName());
	}

}