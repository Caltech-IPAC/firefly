/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.astro.ibe;

import java.io.File;
import java.net.URL;
import java.util.Map;

/**
 * Date: 6/2/14
 *
 * @author loi
 * @version $Id: $
 */
public interface IbeFileUploader {

    public int post(File results, String uploadFileParam, File toUpload, URL url, Map<String, String> addtlParams);
}
