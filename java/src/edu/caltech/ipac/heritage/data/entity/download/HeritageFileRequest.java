package edu.caltech.ipac.heritage.data.entity.download;

import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.heritage.data.entity.DataType;

import java.io.Serializable;

/**
 * @author tatianag
 *         $Id: HeritageFileRequest.java,v 1.3 2011/03/25 22:41:49 tatianag Exp $
 */
public class HeritageFileRequest extends ServerRequest implements Serializable {
    public static final String DATA_TYPE = "datatype";
    public static final String DATA_ID = "dataid";
    public static final String IRS_IMAGE = "IrsImage";

    public HeritageFileRequest() {}

    public HeritageFileRequest(DataType dataType, String dataId, boolean irsImage) {
        super("heritageFileRequest");
        setParam(DATA_TYPE, dataType.name());
        setParam(DATA_ID, dataId);
        setParam(IRS_IMAGE, irsImage+"");
    }

    public boolean hasDataType(DataType dataType) {
        return dataType.name().equals(getParam(DATA_TYPE));
    }

    public long getDataId() {
        String dataIdStr = getParam(DATA_ID);
        return Long.parseLong(dataIdStr);
    }

    public String getDataIdAsStr() {
        return getParam(DATA_ID);
    }


    public boolean isIrsImage() {
        return getBooleanParam(IRS_IMAGE);
    }
}


