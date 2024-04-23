/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.ConfigTest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.network.HttpServiceInput;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.util.FileUtil;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static edu.caltech.ipac.firefly.server.query.DaliUtil.*;
import static edu.caltech.ipac.table.TableUtil.readAnyFormat;
import static org.junit.Assert.*;

public class DaliUtilTest extends ConfigTest {
	static File ufile = null;

	@BeforeClass
	public static void setUp() {
		// needed by test because it's dealing with code running in a server's context, ie  SearchProcessor, RequestOwner, etc.
		setupServerContext(null);

		String input = """
|   ra      |    dec    |   n,obs  |    V     |   Sp, Type   |
|   double  |    double |   int    |   float  |   char       |
  165.466279  -34.704730      5       11.27       K6Ve        
  123.4       5.67            9       8.9         K6Ve-1      
""";
		try {
			ufile = File.createTempFile("test-", ".tbl");
			ufile.deleteOnExit();
		} catch (IOException ignore) {}
		FileUtil.writeStringToFile(ufile, input);
	}

	@Test
	public void basic() throws DataAccessException {
		TableServerRequest req = new TableServerRequest();
		req.setParam(MAXREC, "100");
		req.setParam(REQUEST, "doQuery");
		req.setParam(UPLOAD, ufile.getAbsolutePath());
		HttpServiceInput input = HttpServiceInput.createWithCredential("/test");

		DaliUtil.populateKnownInputs(input, req);

		assertEquals("100", input.getParams().get(MAXREC));
		assertEquals("doQuery", input.getParams().get(REQUEST));
		assertEquals("table1,param:ufile1", input.getParams().get(UPLOAD));
		assertTrue("has upload file", input.getFiles().containsKey("ufile1"));
	}

	@Test
	public void maxrecOverflow() {
		try {
			TableServerRequest req = new TableServerRequest();
			req.setParam(MAXREC, Integer.MAX_VALUE+"");
			HttpServiceInput input = HttpServiceInput.createWithCredential("/test");
			DaliUtil.populateKnownInputs(input, req);
			fail("Expect MAXREC to throw IllegalArgumentException");
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}
	}

	@Test
	public void basicCnames() throws DataAccessException, IOException {
		TableServerRequest req = new TableServerRequest();
		req.setParam(UPLOAD, ufile.getAbsolutePath());
		req.setParam(UPLOAD_COLUMNS, "ra,dec");
		HttpServiceInput input = HttpServiceInput.createWithCredential("/test");

		DaliUtil.populateKnownInputs(input, req);
		DataGroup table = readAnyFormat(input.getFiles().get("ufile1"));
		assertEquals(2, table.getDataDefinitions().length);
		assertEquals(2, table.size());
		assertEquals(165.466279, table.getData("ra", 0));
	}

	@Test
	public void complexCnames() throws DataAccessException, IOException {
		TableServerRequest req = new TableServerRequest();
		req.setParam(UPLOAD, ufile.getAbsolutePath());
		req.setParam(UPLOAD_COLUMNS, "\"n,obs\", \"Sp, Type\"");
		HttpServiceInput input = HttpServiceInput.createWithCredential("/test");

		DaliUtil.populateKnownInputs(input, req);
		DataGroup table = readAnyFormat(input.getFiles().get("ufile1"));
		assertEquals(2, table.getDataDefinitions().length);
		assertEquals(2, table.size());
		assertEquals(5, table.getData("n,obs", 0));
	}

	@Test
	public void uploadUri() throws DataAccessException {
		TableServerRequest req = new TableServerRequest();
		req.setParam(UPLOAD, "VOS://example.authority!tempSpace/foo.vot");	// uppercase VOS to check for case-insensitive matching
		HttpServiceInput input = HttpServiceInput.createWithCredential("/test");

		DaliUtil.populateKnownInputs(input, req);

		assertEquals("table1,VOS://example.authority!tempSpace/foo.vot", input.getParams().get(UPLOAD));
	}

	@Test
	public void nameAs() throws DataAccessException {
		TableServerRequest req = new TableServerRequest();
		req.setParam(UPLOAD, "vos://example.authority!tempSpace/foo.vot");
		HttpServiceInput input = HttpServiceInput.createWithCredential("/test");

		DaliUtil.handleUpload(input, req, "mytbl");

		assertEquals("mytbl,vos://example.authority!tempSpace/foo.vot", input.getParams().get(UPLOAD));
	}

}