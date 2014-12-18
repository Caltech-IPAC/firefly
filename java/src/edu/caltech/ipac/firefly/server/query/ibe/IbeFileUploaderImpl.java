package edu.caltech.ipac.firefly.server.query.ibe;

import edu.caltech.ipac.astro.IpacTableException;
import edu.caltech.ipac.astro.IpacTableReader;
import edu.caltech.ipac.astro.ibe.IBE;
import edu.caltech.ipac.astro.ibe.IbeDataParam;
import edu.caltech.ipac.astro.ibe.IbeFileUploader;
import edu.caltech.ipac.astro.ibe.IbeQueryParam;
import edu.caltech.ipac.astro.ibe.datasource.WiseIbeDataSource;
import edu.caltech.ipac.firefly.server.util.multipart.MultiPartPostBuilder;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.IpacTableUtil;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Map;

/**
 * Date: 6/2/14
 * Implementing IbeFileUploader using HttpClient via MultiPartPostBuilder
 * @author loi
 * @version $Id: $
 */
public class IbeFileUploaderImpl implements IbeFileUploader {

    public int post(File results, String uploadFileParam, File toUpload, URL url, Map<String, String> addtlParams) {
        MultiPartPostBuilder builder = new MultiPartPostBuilder(url.toString());
        builder.addFile(uploadFileParam, toUpload);
        if (addtlParams != null) {
            for (String key : addtlParams.keySet()) {
                builder.addParam(key, addtlParams.get(key));
            }
        }
        try {
            MultiPartPostBuilder.MultiPartRespnse resp = builder.post(new BufferedOutputStream(new FileOutputStream(results)));
            return resp.getStatusCode();
        } catch (IOException e) {
            return 500;
        }
    }

    public static void main(String[] args) {
        // test WISE
        if (true) {
            IBE ibe = new IBE(new WiseIbeDataSource(WiseIbeDataSource.DataProduct.ALLSKY_4BAND_1B));
            try {
                String basedir = "/hydra/ibetest/wise";
                // query the metadata
                File results = new File(basedir, "meta.tbl");
                ibe.getMetaData(results);

                // query via position
                results = new File(basedir, "ibe.tbl");
                IbeQueryParam param = new IbeQueryParam("10.768479,41.26906", ".5");    // m31
//                ibe.query(results, param);
                File infile = new File(basedir, "in.tbl");
                ibe.multipleQueries(results, infile, param);

                // retrieve all data types with cutout options
                DataGroup data = IpacTableReader.readIpacTable(results, "results");
                for (WiseIbeDataSource.DATA_TYPE dtype : WiseIbeDataSource.DATA_TYPE.values()) {
                    for (int idx = 0; idx < data.size(); idx++) {
                        DataObject row = data.get(idx);
                        Map<String, String> dinfo = IpacTableUtil.asMap(row);
                        dinfo.put(WiseIbeDataSource.FTYPE, dtype.name());
                        IbeDataParam dparam = ibe.getIbeDataSource().makeDataParam(dinfo);
                        dparam.setCutout(true, row.getDataElement("in_ra") + "," + row.getDataElement("in_dec"), ".1");
                        try {
                            ibe.getData(new File(basedir), dparam);
                        } catch (IOException ex) {
                            System.out.println("Line " + idx + ": Unable to retrieve " + dtype + " data.");
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (IpacTableException e) {
                e.printStackTrace();
            }
        }
    }
}
