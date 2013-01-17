package edu.caltech.ipac.heritage.data.entity.download;

import edu.caltech.ipac.firefly.data.DownloadRequest;
import edu.caltech.ipac.heritage.data.entity.DataType;
import edu.caltech.ipac.heritage.data.entity.WaveLength;

import java.util.Collection;
import java.io.Serializable;

/**
 * @author tatianag
 *         $Id: HeritageDownloadRequest.java,v 1.3 2010/11/29 22:14:28 roby Exp $
 */
public class HeritageDownloadRequest extends DownloadRequest implements Serializable {

    private Collection<DataType> _dataTypes;
    private Collection<WaveLength> _wavelenghts;

    public HeritageDownloadRequest() {}

    public HeritageDownloadRequest(DownloadRequest req, Collection<DataType> dataTypes, Collection<WaveLength> wavelengths) {
        _dataTypes= dataTypes;
        _wavelenghts= wavelengths;
        copyFrom(req);
        setRequestId("heritageDownload");
        setDataSource("Spitzer");
    }

    public Collection<DataType> getDataTypes() { return _dataTypes; }
    public Collection<WaveLength> getWaveLengths() { return _wavelenghts; }

}
