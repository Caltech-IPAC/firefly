/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.server.visualize.imagesources;


import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.util.ImageSetConverter;
import edu.caltech.ipac.util.download.FailedRequestException;

import java.io.IOException;
import java.util.List;

/**
 * @author Trey Roby
 */
public class IrsaMasterDataSource implements ImageMasterDataSourceType {

    @Override
    public List<ImageMasterDataEntry> getImageMasterData() {
        try {

            ImageSetConverter ic = new ImageSetConverter(ServerParams.IRSA);
            return ic.getDataList();
        } catch (FailedRequestException  | IOException e) {
            e.printStackTrace();
            return null;
        }

    }
}
