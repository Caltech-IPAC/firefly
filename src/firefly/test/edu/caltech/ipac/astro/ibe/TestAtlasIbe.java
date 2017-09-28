package edu.caltech.ipac.astro.ibe;

import edu.caltech.ipac.astro.IpacTableException;
import edu.caltech.ipac.astro.IpacTableReader;
import edu.caltech.ipac.astro.ibe.datasource.AtlasIbeDataSource;
import edu.caltech.ipac.firefly.ConfigTest;
import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.IpacTableUtil;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class TestAtlasIbe extends ConfigTest {

    @Test
    public void testIbe2Call() {

        AtlasIbeDataSource atlas = new AtlasIbeDataSource(AtlasIbeDataSource.DS.SEIP);
//        AtlasIbeDataSource atlas = new AtlasIbeDataSource(AtlasIbeDataSource.DS.MSX);
        IBE ibe = new IBE(atlas);
        try {
            String basedir = ".";
            // query the metadata
            File results = new File(basedir, "meta.tbl");
            results.deleteOnExit();
            ibe.getMetaData(results);

            // query via position
            results = new File(basedir, "ibe.tbl");
            results.deleteOnExit();
            IbeQueryParam param = new IbeQueryParam("280,-8", ".45");    // m31
//                atlas.makeQueryParam()
            if (atlas.getDataset().startsWith("spitzer")) {
                param.setWhere("band_name in ( 'IRAC2' ) and file_type='science' and fname like '%.mosaic.fits'"); //specific to SEIP
            } else {
                param.setWhere("band_name in (\'E\')"); //specific to SEIP
            }
            ibe.query(results, param);

            // retrieve all data types with cutout options
//            DataGroup datatmp[] = VoTableUtil.voToDataGroups(results.getAbsolutePath());

//            Assert.assertTrue("no data found", datatmp.length > 0);

//            if(datatmp.length>0) {
            DataGroup data = IpacTableReader.readIpacTable(results, "result.tbl");
            Assert.assertTrue("no data found", data.size() > 0);

            for (int idx = 0; idx < data.size(); idx++) {
                DataObject row = data.get(idx);
                Map<String, String> dinfo = IpacTableUtil.asMap(row);
                IbeDataParam dparam = ibe.getIbeDataSource().makeDataParam(dinfo);
                // dparam.setCutout(true, "10.768479,41.26906", ".1");
                try {
                    File fileTmp = new File(basedir);
                    fileTmp.deleteOnExit();
                    FileInfo fileInfo = ibe.getData(dparam, null, fileTmp, null);
                    new File(fileInfo.getInternalFilename()).deleteOnExit();
                    Assert.assertTrue("file is empty", fileInfo.getSizeInBytes() > 0);
                } catch (IOException ex) {
                    throw new IOException("Line " + idx + ": Unable to retrieve " + dinfo.get("file_type") + " data.", ex);
                }
            }
//            }

        } catch (IOException e) {
            LOG.error("problem ", e.getMessage());
            e.printStackTrace();
        } catch (IpacTableException e) {
            LOG.error("problem ", e.getMessage());
            e.printStackTrace();
        }
    }
}


