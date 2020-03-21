/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.dpanalyze;

import edu.caltech.ipac.firefly.core.FileAnalysisReport;
import nom.tam.fits.Header;

import java.io.File;
import java.util.Map;

/**
 * @author Trey Roby
 */
public interface DataProductAnalyzer {
    default FileAnalysisReport analyze(FileAnalysisReport inputReport,
                                       File inFile,
                                       String analyzerId,
                                       Map<String,String> params) { return inputReport;}
    default FileAnalysisReport analyzeFits(FileAnalysisReport inputReport,
                                           File inFile,
                                           String analyzerId,
                                           Map<String,String> params,
                                           Header headerAry[]) {return inputReport;}
}
