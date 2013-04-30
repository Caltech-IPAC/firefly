package edu.caltech.ipac.firefly.server.query;

import com.google.gwt.thirdparty.guava.common.io.Files;
import edu.caltech.ipac.client.net.FailedRequestException;
import edu.caltech.ipac.client.net.URLDownload;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.packagedata.FileInfo;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupReader;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupWriter;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;


@SearchProcessorImpl(id = "IpacTableFromSource")
public class IpacTableFromSource extends IpacTablePartProcessor {

    protected File loadDataFile(TableServerRequest request) throws IOException, DataAccessException {

        String source = request.getParam("source");
        if (StringUtils.isEmpty(source)) {
            String processor = request.getParam("processor");
            if (StringUtils.isEmpty(processor)) {
                throw new DataAccessException("Required parameter 'source' is not given.");
            }
            TableServerRequest sReq = new TableServerRequest(processor, request);
            FileInfo fi = new SearchManager().getFileInfo(sReq);
            if (fi == null) {
                throw new DataAccessException("Unable to get file location info");
            }
            if (fi.getInternalFilename()== null) {
                throw new DataAccessException("File not available");
            }
            if (!fi.hasAccess()) {
                throw new SecurityException("Access is not permitted.");
            }
            source = fi.getInternalFilename();
        }

        URL url = makeUrl(source);
        File outf = null;
        File inf;

        if (url == null) {
            inf = new File(source);
        } else {
            URLConnection conn = URLDownload.makeConnection(url);
            String sfname = URLDownload.getSugestedFileName(conn);
            if (sfname == null) {
                sfname = url.getPath();
            }
            String ext = sfname == null ? null : FileUtil.getExtension(sfname);
            ext = StringUtils.isEmpty(ext) ? ".ul" : "." + ext;
            inf = createFile(request, ext);
            try {
                URLDownload.getDataFromOpenURLToFile(conn, inf, null, false, true, false, Long.MAX_VALUE);
            } catch (FailedRequestException e) {
                inf = null;
            }
        }

        if (inf == null || !inf.canRead()) {
            throw new DataAccessException("Unable to read the source file:" + source);
        }

        DataGroupReader.Format format = DataGroupReader.guessFormat(inf);
        if (format == DataGroupReader.Format.IPACTABLE) {
            if (url == null) {      // file is on filesystem
                outf = createFile(request, ".tbl");
                Files.copy(inf, outf);
            } else {
                outf = inf;
            }
        } else {
            if ( format != DataGroupReader.Format.UNKNOWN) {
                outf = createFile(request, ".tbl");
                DataGroup dg = DataGroupReader.readAnyFormat(inf);
                DataGroupWriter.write(outf, dg, 0);
            } else {
                throw new DataAccessException("Source file has an unknown format:" + source);
            }
        }

        return outf;
    }

    private URL makeUrl(String source) {
        try {
            return new URL(source);
        } catch (MalformedURLException e) {
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

