package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.client.net.FailedRequestException;
import edu.caltech.ipac.client.net.URLDownload;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupReader;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupWriter;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;


@SearchProcessorImpl(id = "IpacTableFromSource")
public class IpacTableFromSource extends IpacTablePartProcessor {

    protected File loadDataFile(TableServerRequest request) throws IOException, DataAccessException {

        String source = request.getParam("source");
        if (StringUtils.isEmpty(source)) {
            throw new DataAccessException("Required parameter 'source' is not given.");
        }
        File outf;
        URL url = makeUrl(source);
        if (url != null) {
            outf = createFile(request, ".ul");
            try {
                URLDownload.getDataToFile(new URL(source), outf);
                DataGroupReader.Format format = DataGroupReader.guessFormat(outf);
                if (format != DataGroupReader.Format.IPACTABLE) {
                    if ( format == DataGroupReader.Format.UNKNOWN) {
                        throw new DataAccessException("Unsupported file format, url:" + url);
                    } else {
                        DataGroup dg = DataGroupReader.readAnyFormat(outf);
                        DataGroupWriter.write(outf, dg, 0);
                    }
                };
            } catch (FailedRequestException e) {
                e.printStackTrace();
                throw new DataAccessException("Unable to retrieve data from URL:" + source);
            }
        } else {
            outf = new File(source);
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

