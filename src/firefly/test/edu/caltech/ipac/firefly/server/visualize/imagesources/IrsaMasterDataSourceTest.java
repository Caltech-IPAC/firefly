/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize.imagesources;

import edu.caltech.ipac.firefly.ConfigTest;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.table.DataGroup;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.*;

public class IrsaMasterDataSourceTest extends ConfigTest {

	private static final String MASTER_TABLE = "/edu/caltech/ipac/firefly/resources/irsa-image-master-table.csv";

	@Test
	public void testReadMasterTableFromClasspath() throws Exception {
		DataGroup dg = new IrsaMasterDataSource().getDataFromMasterTable(MASTER_TABLE);

		assertNotNull("the master table is readable", dg);
		assertTrue("it has rows", dg.size() > 0);

		// the columns the image ids are built from must stay text; a numeric guess would change the ids
		for (String cname : List.of("missionId", "surveyKey", "wavebandId", "imageId")) {
			assertNotNull("%s column exists".formatted(cname), dg.getDataDefintion(cname));
			assertEquals("%s is text".formatted(cname), String.class, dg.getDataDefintion(cname).getDataType());
		}
		assertEquals("2MASS", dg.getData("missionId", 0));
		assertEquals("asky", dg.getData("surveyKey", 0));

		File[] spills = ServerContext.getTempWorkDir().listFiles((d, n) -> n.startsWith("master-"));
		assertEquals("the copy does not outlive the read", 0, spills == null ? 0 : spills.length);
	}

	/** the master table is only useful if it still builds the entries the image source list is made of */
	@Test
	public void testCreateDataList() throws Exception {
		List<ImageMasterDataEntry> entries = new IrsaMasterDataSource().createDataList(MASTER_TABLE);

		assertNotNull(entries);
		assertTrue("entries were built", entries.size() > 0);

		ImageMasterDataEntry first = entries.get(0);
		assertEquals("2MASS", first.getParamString(ImageMasterDataEntry.PARAMS.MISSION_ID));
		assertNotNull("every entry gets an imageId", first.getParamString(ImageMasterDataEntry.PARAMS.IMAGE_ID));
		for (ImageMasterDataEntry e : entries) {
			assertNotNull("imageId is never null", e.getParamString(ImageMasterDataEntry.PARAMS.IMAGE_ID));
		}
	}
}
