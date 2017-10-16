/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.astro.IpacTableException;
import edu.caltech.ipac.firefly.data.TableServerRequest;

import java.io.File;
import java.io.IOException;

/**
 * Date: 9/13/17
 *
 * @author loi
 * @version $Id: $
 */
interface CanGetDataFile {
    File getDataFile(TableServerRequest request) throws IpacTableException, IOException, DataAccessException;
}
