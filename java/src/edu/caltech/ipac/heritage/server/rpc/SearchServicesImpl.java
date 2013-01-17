package edu.caltech.ipac.heritage.server.rpc;

import edu.caltech.ipac.firefly.core.RPCException;
import edu.caltech.ipac.firefly.server.rpc.BaseRemoteService;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.visualize.VisContext;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.heritage.data.entity.DataType;
import edu.caltech.ipac.heritage.data.entity.IRSInfoData;
import edu.caltech.ipac.heritage.data.entity.download.HeritageFileRequest;
import edu.caltech.ipac.heritage.rpc.SearchServices;
import edu.caltech.ipac.heritage.server.persistence.UtilDao;
import edu.caltech.ipac.heritage.server.query.SearchManager;
import edu.caltech.ipac.heritage.server.visualize.IRSImageInfo;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.StringKey;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.Header;
import edu.caltech.ipac.visualize.plot.CoveragePolygons;
import edu.caltech.ipac.visualize.plot.ImageWorkSpacePt;

import java.awt.Point;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;


/**
 * @author loi
 * @version $Id: SearchServicesImpl.java,v 1.46 2012/02/23 03:27:44 booth Exp $
 */
public class SearchServicesImpl extends BaseRemoteService implements SearchServices {
    public static final String DEC_COL_NAME = "dec";
    public static final String RA_COL_NAME = "ra";


    public CoveragePolygons getAorCoverage(int requestID) {
        return new SearchManager().getAorCoverage(requestID);
    }

    public Map<String, String> getAbstractInfo(int progId) throws RPCException {
        try {
            return new SearchManager().getAbstractInfo(progId);
        } catch (Throwable e) {
            throw createRPCException(e);
        }
    }

    public List<String> getObservers() throws RPCException {
        try {
            return new SearchManager().getObservers();
        } catch (Throwable e) {
            throw createRPCException(e);
        }
    }

    public List<Integer> getBcdIds(String pbcdFile, List<Integer> fileRowIndices) throws RPCException {
        try {
            return new SearchManager().getBcdIds(pbcdFile, fileRowIndices);
        } catch (Throwable e) {
            throw createRPCException(e);
        }
    }



    public IRSInfoData getIRSFileInfo(PlotState state, ImageWorkSpacePt inIpt) throws RPCException {
        try{
            DataType dataType= getDataType(state);
            String dataid= state.getWebPlotRequest(Band.NO_BAND).getParam(HeritageFileRequest.DATA_ID);
            File filename= VisContext.getOriginalFile(state, Band.NO_BAND);
            StringKey filesKey = new StringKey("IRSLookup_Files_" + dataid);
            IRSImageInfo info;

            Cache cache = CacheManager.getCache(Cache.TYPE_HTTP_SESSION);
            if(cache.isCached(filesKey)){
                info = (IRSImageInfo) cache.get(filesKey);
            } else {

                Fits fits = new Fits(filename);
                BasicHDU[] myHDUs = fits.read();
                Header header = myHDUs[0].getHeader();

                Collection<String> filenames;
                if(dataType.equals(DataType.BCD)){
                    filenames = UtilDao.getBcdWavsamp(Integer.parseInt(dataid));
                } else {
                    filenames = UtilDao.getPbcdWavsamp(Integer.parseInt(dataid));
                }

                info = new IRSImageInfo(header, filenames, filename);

                cache.put(filesKey, info);
            }

            Point p = new Point((int)inIpt.getX(),(int)inIpt.getY());
            IRSInfoData data = info.getInfoData(p);
            //IRSInfoData data = new IRSInfoData();
            //data._wavelength= 466;
            //data._ra= 1.0;
            //data._dec= 2.0;
            Logger.debug("get IRS file info for id: "+dataid+ ", file:" + filename);

            return data;
        } catch (Throwable e) {
            throw createRPCException(e);
        }
    }


    private DataType getDataType(PlotState state) {
        WebPlotRequest request= state.getWebPlotRequest(Band.NO_BAND);
        DataType dataType;
        try {
            dataType= Enum.valueOf(DataType.class,request.getParam(HeritageFileRequest.DATA_TYPE));
        } catch (NullPointerException e) {
            dataType=null;
        }
        if (dataType!=DataType.BCD && dataType!=DataType.PBCD) {
            dataType=null;
        }
        return dataType;
    }

}

/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313)
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
