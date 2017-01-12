/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.net;

import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.cache.CacheKey;
import edu.caltech.ipac.util.download.CacheHelper;
import edu.caltech.ipac.util.download.DownloadListener;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.FileData;
import edu.caltech.ipac.util.download.NetParams;

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

    private static final int SEC_IN_DAY= 86400; //60*60*24

    private static File getIrsaImage(IrsaImageParams params) throws FailedRequestException {

        File f;
        if (params.getType()== IrsaImageParams.IrsaTypes.TWOMASS) {
            f= getIbeImage(params);
        }
        else {
            f= getTraditionIrsaImage(params);
        }

        return f;
    }



    private static File getTraditionIrsaImage(IrsaImageParams params)
                                  throws FailedRequestException {
        File f= CacheHelper.getFile(params);

        if (f == null)  {          // if not in cache
            f= CacheHelper.makeFitsFile(params);
            try {
                IrsaImageGetter.lowlevelGetIrsaImage(params, f);
            } catch (IOException e) {
                throw new FailedRequestException("IrsaImageGetter Call failed with IOException",
                                                 "no more detail",e);
            }
            CacheHelper.getFileCache().put(params,f);
        }

        return f;
    }


    private static File getIbeImage(BaseIrsaParams params) throws FailedRequestException {
        File f= CacheHelper.getFile(params);

        if (f == null)  {          // if not in cache
            f= CacheHelper.makeFitsFile(params);
            try {
                IbeImageGetter.lowlevelGetIbeImage(params, f);
            } catch (IOException e) {
                throw new FailedRequestException("IbeImageGetter Call failed with IOException",
                                                 "no more detail",e);
            }
            CacheHelper.getFileCache().put(params,f);
        }
        return f;
    }




    public static File getSloanDssImage(SloanDssImageParams params)
            throws FailedRequestException {
        File f= CacheHelper.getFile(params);
        if (f == null)  {          // if not in cache
            f= CacheHelper.makeFitsFile(params);
            try {
                SloanDssImageGetter.lowlevelGetSloanDssImage(params, f);
            } catch (IOException e) {
                throw new FailedRequestException("SloanDSSImageGetter Call failed with IOException",
                                                 "no more detail",e);
            }
            CacheHelper.putFile(params,f);
        }
        return f;
    }

    public static File getDssImage(DssImageParams params)
                                  throws FailedRequestException {
        File f= CacheHelper.getFile(params);
        if (f == null)  {          // if not in cache
            f= CacheHelper.makeFitsFile(params);
            try {
                DssImageGetter.lowlevelGetDssImage(params, f);
            } catch (IOException e) {
                throw new FailedRequestException("DSSImageGetter Call failed with IOException",
                                                 "no more detail",e);
            }
            CacheHelper.putFile(params,f);
        }
        return f;
    }



    public static FileData getAnyFits(AnyFitsParams params, DownloadListener dl)
                                  throws FailedRequestException {
        File f;

        FileData fData= CacheHelper.getFileData(params);
        if (fData == null)  {          // if not in cache
            String newfile= params.getUniqueString();
            f= CacheHelper.makeFile(newfile);
            try {
                fData= AnyFitsGetter.lowlevelGetFits(params, f,dl);
            } catch (IOException e) {
                throw new FailedRequestException("AnyFitsGetter failed with IOException",
                                                 "no more detail",e);
            }
            CacheHelper.putFile(params,fData);
        }
        return fData;
    }





    /**
     * Retrieve a file from URL and cache it.  If the URL is a gz file then uncompress it and return the uncompress version.
     * @param params the configuration about the retrieve request
     * @param dl a Download listener, only used in server mode
     * @return a FileData of file returned from this URL.
     * @throws FailedRequestException
     */
    public static FileData getAnyUrlImage(AnyUrlParams params, DownloadListener dl)
                                                     throws FailedRequestException {
        FileData retval=CacheHelper.getFileData(params);
        File fileName= (retval==null) ? CacheHelper.makeFile(params.getUniqueString()) : retval.getFile();

        if (retval==null && params.isCompressedFileName()) {  // if we are requesting a gz file then check to see if we cached the unzipped version
            retval=CacheHelper.getFileData(params.getUncompressedKey());
            if (retval==null && fileName.canWrite()) fileName.delete(); // this file should not be in the cache in the this case
        }

        if (retval == null || params.getCheckForNewer())  {          // if not in cache or is in cache & we want to see if there is a newer version
            FileData fd= AnyUrlGetter.lowlevelGetUrlToFile(params,fileName,false,dl);

            CacheKey saveKey= params;
            // if is ends with GZ and it is not compressed then rename the file without the GZ
            if (fd.isDownloaded() &&
                FileUtil.isGZExtension(fd.getFile()) &&  !FileUtil.isGZipFile(fd.getFile())) {

                File uncompFileName= CacheHelper.makeFile(params.getUncompressedKey().getUniqueString());
                boolean success= fd.getFile().renameTo(uncompFileName);
                if (success) {
                    FileUtil.writeStringToFile(fd.getFile(),"placeholder for uncompress file: " +
                                                            uncompFileName.getPath());
                    saveKey= params.getUncompressedKey();
                    fd= new FileData(uncompFileName,fd.getSuggestedExternalName()); // modify the results with the uncompressed file
                }
            }
            if (fd.isDownloaded() || retval==null) {
                retval= fd;
                CacheHelper.putFile(saveKey,fd);
            }
        }
        return retval;
    }


    /**
     * Retrieve an image, this call should only be used in server mode
     * @param params net parameters
     * @param dl download lister to monitor progress and cancel
     * @return one more more files, almost always this will be one file
     * @throws FailedRequestException when anything fails
     */
    public static FileData getImage(NetParams params, DownloadListener dl) throws FailedRequestException {
       FileData retval= null;
       File f;
      if (params instanceof IrsaImageParams) {
          f=  getIrsaImage( (IrsaImageParams)params);
          retval= new FileData(f,null);
      }
      else if (params instanceof DssImageParams) {
          f=  getDssImage( (DssImageParams)params);
          retval= new FileData(f,null);
      }
      else if (params instanceof SloanDssImageParams) {
          f=  getSloanDssImage( (SloanDssImageParams)params);
          retval= new FileData(f,null);
      }
      else if (params instanceof WiseImageParams) {
          f=  getIbeImage((BaseIrsaParams) params);
          retval= new FileData(f,null);
      }
      else if (params instanceof AnyFitsParams) {
          retval=  getAnyFits( (AnyFitsParams)params,dl);
      }
      else if (params instanceof AnyUrlParams) {
          retval=  getAnyUrlImage( (AnyUrlParams)params,dl);
      }
      else {
          Assert.tst(false, "Should never be here");
      }

      return retval;
   }






}
