/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize.imagesources;

import java.util.List;

/**
 * @author Trey Roby
 */
public interface ImageMasterDataSourceType {
    List<ImageMasterDataEntry> getImageMasterData();
}
