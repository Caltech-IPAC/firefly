/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.net;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.util.download.CacheHelper;
import edu.caltech.ipac.util.download.DownloadListener;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.ResponseMessage;

import java.io.File;
import java.io.IOException;

/*
 * This is the new class that all the visualization network request 
 * go through.  It is a static class that uses 
 * client.net.NetCache to determine if it
 * can get a request from the cache or it has to go to the network.
 * There is one public method here for each network request the 
 * the visualization package does.  When we add a new type of 
 * request we will add new methods here.
 */
public class VisNetwork {

//    private static File getTraditionIrsaImage(IrsaImageParams params) throws FailedRequestException {
//        File f= CacheHelper.getFile(params);
//        if (f == null)  {          // if not in cache
//            f= CacheHelper.makeFitsFile(params);
//            try {
//                IrsaImageGetter.lowlevelGetIrsaImage(params, f);
//            } catch (IOException e) {
//                throw ResponseMessage.simplifyNetworkCallException(e);
//            }
//            CacheHelper.getFileCache().put(params,f);
//        }
//        return f;
//    }
//
//
//    private static File getIbeImage(BaseIrsaParams params) throws FailedRequestException {
//        File f= CacheHelper.getFile(params);
//        if (f == null)  {          // if not in cache
//            try {
//                f= IbeImageGetter.lowlevelGetIbeImage(params);
//            } catch (IOException e) {
//                throw ResponseMessage.simplifyNetworkCallException(e);
//            }
//            CacheHelper.getFileCache().put(params,f);
//        }
//        return f;
//    }
//
//    private static File getSloanDssImage(SloanDssImageParams params) throws FailedRequestException {
//        File f= CacheHelper.getFile(params);
//        if (f == null)  {          // if not in cache
//            f= CacheHelper.makeFitsFile(params);
//            try {
//                SloanDssImageGetter.lowlevelGetSloanDssImage(params, f);
//            } catch (IOException e) {
//                throw ResponseMessage.simplifyNetworkCallException(e);
//            }
//            CacheHelper.putFile(params,f);
//        }
//        return f;
//    }
//
//    private static File getDssImage(DssImageParams params) throws FailedRequestException {
//        File f= CacheHelper.getFile(params);
//        if (f == null)  {          // if not in cache
//            f= CacheHelper.makeFitsFile(params);
//            try {
//                DssImageGetter.lowlevelGetDssImage(params, f);
//            } catch (IOException e) {
//                throw ResponseMessage.simplifyNetworkCallException(e);
//            }
//            CacheHelper.putFile(params,f);
//        }
//        return f;
//    }
//
//
    private static FileInfo getImage(ImageServiceParams params, DownloadListener dl) throws FailedRequestException {
        File f= CacheHelper.getFile(params);
        if (f == null)  {          // if not in cache
            f= CacheHelper.makeFitsFile(params);
            try {
                if (params instanceof IrsaImageParams) {
                    if (params.getType()== ImageServiceParams.ImageSourceTypes.TWOMASS) {
                        f= IbeImageGetter.lowlevelGetIbeImage((IrsaImageParams) params);
                    }
                    else {
                        IrsaImageGetter.lowlevelGetIrsaImage((IrsaImageParams) params, f);
                    }
                }
                else if (params instanceof DssImageParams) {
                    DssImageGetter.lowlevelGetDssImage((DssImageParams)params, f);
                }
                else if (params instanceof SloanDssImageParams) {
                    SloanDssImageGetter.lowlevelGetSloanDssImage((SloanDssImageParams)params, f);
                }
                else if (params instanceof WiseImageParams) {
                    f= IbeImageGetter.lowlevelGetIbeImage((ImageServiceParams)params);
                }
            } catch (IOException e) {
                throw ResponseMessage.simplifyNetworkCallException(e);
            }
            CacheHelper.putFile(params,f);
        }
        return new FileInfo(f);
    }




//    /**
//     * Retrieve an image, this call should only be used in server mode
//     * @param params net parameters
//     * @param dl download lister to monitor progress and cancel
//     * @return one more more files, almost always this will be one file
//     * @throws FailedRequestException when anything fails
//     */
//    public static FileInfo getImage(NetParams params, DownloadListener dl) throws FailedRequestException {
//       FileInfo retval= null;
//      if (params instanceof IrsaImageParams) {
//          if (((IrsaImageParams)params).getType()== IrsaImageParams.IrsaTypes.TWOMASS) {
//              f= IbeImageGetter.lowlevelGetIbeImage((BaseIrsaParams) params) :
//          }
//          else {
//              IrsaImageGetter.lowlevelGetIrsaImage((BaseIrsaParams)params, f);
//          }
//          retval= new FileInfo(f);
//      }
//      else if (params instanceof DssImageParams) {
//          File f=  getDssImage( (DssImageParams)params);
//          retval= new FileInfo(f);
//      }
//      else if (params instanceof SloanDssImageParams) {
//          File f=  getSloanDssImage( (SloanDssImageParams)params);
//          retval= new FileInfo(f);
//      }
//      else if (params instanceof WiseImageParams) {
//          File f=  getIbeImage((BaseIrsaParams) params);
//          retval= new FileInfo(f);
//      }
//      else if (params instanceof AnyUrlParams) {
//          Logger.error("using AnyUrlParams is with VisNetwork is deprecated, use LockingVisNetwork instead");
//      }
//      else {
//          Assert.tst(false, "Should never be here");
//      }
//
//      return retval;
//   }

}
