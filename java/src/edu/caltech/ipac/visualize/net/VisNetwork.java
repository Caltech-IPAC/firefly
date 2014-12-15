package edu.caltech.ipac.visualize.net;

import edu.caltech.ipac.client.net.CacheHelper;
import edu.caltech.ipac.client.net.DownloadListener;
import edu.caltech.ipac.client.net.FailedRequestException;
import edu.caltech.ipac.client.net.FileData;
import edu.caltech.ipac.client.net.HostPort;
import edu.caltech.ipac.client.net.NetParams;
import edu.caltech.ipac.client.net.NetworkManager;
import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.ParseException;
import edu.caltech.ipac.util.action.ClassProperties;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheKey;
import edu.caltech.ipac.visualize.draw.FixedObjectGroup;

import java.awt.Component;
import java.awt.Window;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

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

   private final static ClassProperties _prop= new ClassProperties(
                                                             VisNetwork.class);
    private static final int SEC_IN_DAY= 86400; //60*60*24




    private static File getIrsaImage(IrsaImageParams params,
                                     Window w,
                                     boolean moreCallsComming) throws FailedRequestException {

        File f;
        if (params.getType()== IrsaImageParams.IrsaTypes.TWOMASS) {
            f= getIbeImage(params, w, moreCallsComming);
        }
        else {
            f= getTraditionIrsaImage(params,w,moreCallsComming);
        }

        return f;
    }



    private static File getTraditionIrsaImage(IrsaImageParams params,
                                              Window w,
                                              boolean moreCallsComming)
                                  throws FailedRequestException {
        File f= CacheHelper.getFile(params);

        if (f == null)  {          // if not in cache
            f= CacheHelper.makeFitsFile(params);
            if (CacheHelper.isServer()) {
                try {
                    IrsaImageGetter.lowlevelGetIrsaImage(params, f);
                } catch (IOException e) {
                    throw new FailedRequestException("IrsaImageGetter Call failed with IOException",
                                                     "no more detail",e);
                }
            }
            else {
                IrsaImageGetter.getIrsaImage(params, f, w, moreCallsComming);
            }

            CacheHelper.getFileCache().put(params,f);
        }

        return f;
    }


    private static File getIbeImage(BaseIrsaParams params,
                                    Window w,
                                    boolean moreCallsComing) throws FailedRequestException {
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




   // added by Xiuqin Wu for Irsa catalog search
   public static File getIrsaCatalog(IrsaCatalogParams params, Window w)
                                             throws FailedRequestException {

       File f= CacheHelper.getFile(params);
       if (f == null)  {          // if not in cache
           f= CacheHelper.makeTblFile(params);
           IrsaCatalogGetter.getCatalog(params, f, w );
           CacheHelper.putFile(params,f);
       }
       return f;
   }

   public static File getSkyViewCatalog(SkyViewCatalogParams params, Window w) 
                                             throws FailedRequestException {
       File f= CacheHelper.getFile(params);
       if (f == null)  {          // if not in cache
           f= CacheHelper.makeTblFile(params);
           SkyViewCatalogGetter.getCatalog(params, f, w );
           CacheHelper.putFile(params,f);
       }
       return f;
   }

    public static File getSloanDssImage(SloanDssImageParams params,  Window w)
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

    public static File getDssImage(DssImageParams params,  Window w)
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


    public static File getSkyViewImage(SkyViewImageParams params,
                                       Window             w)
                                  throws FailedRequestException {

        File f= CacheHelper.getFile(params);
        if (f == null)  {          // if not in cache

            String newfile= params.getUniqueString();
            String ext= FileUtil.getExtension(newfile);
            if (!ext.equalsIgnoreCase(FileUtil.FIT) &&
                !ext.equalsIgnoreCase(FileUtil.FITS) &&
                !ext.equalsIgnoreCase(FileUtil.GZ)) {
                newfile= newfile + ".fits";
            }
            f= CacheHelper.makeFile(newfile);
            try {
                SkyViewImageGetter.lowlevelGetImage(params, f,null);
            } catch (IOException e) {
                throw new FailedRequestException("SkyViewImageGetter failed with IOException",
                                                 "no more detail",e);
            }
            CacheHelper.putFile(params,f);
        }
        return f;
    }

    public static FileData getAnyFits(AnyFitsParams params,  Window w, DownloadListener dl)
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



//    public static FileData[] getAnyUrl(URL url, boolean useSuggestedFilename, Component c)
//                   throws FailedRequestException {
//        Assert.tst(!CacheHelper.isServer());
//        AnyUrlParams params= new AnyUrlParams(url);
//        FileData retval[]= CacheHelper.getFileDataAry(params);
//        if (retval == null)  {          // if not in cache
//            String newfile= params.getUniqueString();
//            File templeteFile= CacheHelper.makeFile(newfile);
//            retval= AnyUrlGetter.getUrl(params, templeteFile,
//                                          useSuggestedFilename,
//                                          c);
//            CacheHelper.putFile(params,retval);
//        }
//        return retval;
//    }


    /**
     * Retrieve a file from URL and cache it.  If the URL is a gz file then uncompress it and return the uncompress version.
     * @param params the configuration about the retrieve request
     * @param c any component, only used in client mode
     * @param dl a Download listener, only used in server mode
     * @return a array of FileData of file returned from this URL.  This is usually length of 1.
     * @throws FailedRequestException
     */
    public static FileData[] getAnyUrlImage(AnyUrlParams params, Component c, DownloadListener dl)
                                                     throws FailedRequestException {
        FileData retval[]=CacheHelper.getFileDataAry(params);
        File fileName= (retval==null) ? CacheHelper.makeFile(params.getUniqueString()) : retval[0].getFile();

        if (retval==null && params.isCompressedFileName()) {  // if we are requesting a gz file then check to see if we cached the unzipped version
            retval=CacheHelper.getFileDataAry(params.getUncompressedKey());
            if (retval==null && fileName.canWrite()) fileName.delete(); // this file should not be in the cache in the this case
        }

        if (retval == null || params.getCheckForNewer())  {          // if not in cache or is in cache & we want to see if there is a newer version
            FileData[] results= CacheHelper.isServer() ? AnyUrlGetter.lowlevelGetUrlToFile(params,fileName,false,dl) :
                                                         AnyUrlGetter.getUrl(params, fileName, false, c);

            CacheKey saveKey= params;
            FileData fd= results[0];
            // if is ends with GZ and it is not compressed then rename the file without the GZ
            if (results.length==1 && fd.isDownloaded() &&
                FileUtil.isGZExtension(fd.getFile()) &&  !FileUtil.isGZipFile(fd.getFile())) {

                File uncompFileName= CacheHelper.makeFile(params.getUncompressedKey().getUniqueString());
                boolean success= fd.getFile().renameTo(uncompFileName);
                if (success) {
                    FileUtil.writeStringToFile(fd.getFile(),"placeholder for uncompress file: " +
                                                            uncompFileName.getPath());
                    saveKey= params.getUncompressedKey();
                    results[0]= new FileData(uncompFileName,fd.getSugestedExternalName()); // modify the results with the uncompressed file
                }
            }
            if (fd.isDownloaded() || retval==null) {
                retval= results;
                CacheHelper.putFile(saveKey,results);
            }
        }
        return retval;
    }

    public static File getAnyUrl(URL url, Component  c)
                   throws FailedRequestException {
        File f;

        AnyUrlParams params= new AnyUrlParams(url);
        f= CacheHelper.getFile(params);

        if (f == null)  {          // if not in cache
            f= CacheHelper.makeFile(params.getUniqueString());

            if (CacheHelper.isServer()) {
                AnyUrlGetter.lowlevelGetUrlToFile(params,f,false,null);
            }
            else {
                AnyUrlGetter.getUrl(params, f, c);
            }
            CacheHelper.putFile(params,f);
        }
        return f;
    }


   public static File getNedImage(NedImageParams params,
                                  Window         w)
                                 throws FailedRequestException {
       File f= CacheHelper.getFile(params);
       if (f == null)  {          // if not in cache
           String newfile= params.getUniqueString();
           f= CacheHelper.makeFile(newfile);
           if (CacheHelper.isServer()) {
               try {
                   HostPort nedServer=
                     NetworkManager.getInstance().getServer(NetworkManager.NED_SERVER);
                   NedImageGetter.lowlevelGetNedImage(nedServer.getHost(), params, f);
               } catch (IOException e) {
                   throw new FailedRequestException("NedImageGetter Call failed with IOException",
                                                    "no more detail",e);
               }
           }
           else {
               NedImageGetter.getNedImage(params, f, w);
           }
           CacheHelper.putFile(params,f);
       }

       return f;
   }

   public static File getNedPreviewGif(NedImageParams params) 
                                            throws IOException {

       File f= CacheHelper.getFile(params);
       if (f == null)  {          // if not in cache
           f= CacheHelper.makeFile(params.getUniqueString());
           NetworkManager manager= NetworkManager.getInstance();
           HostPort server= manager.getServer(NetworkManager.NED_SERVER);
           NedImageGetter.lowlevelGetPreviewGif(server.getHost(), params, f);
           CacheHelper.putFile(params,f);
       }
       return f;
   }

   public static File getNedCatalog(NedCatalogParams params, Window w) 
                                             throws FailedRequestException {

       File f= CacheHelper.getFile(params);
       if (f == null)  {          // if not in cache
           String newfile= params.getUniqueString() + ".tbl";
           f= CacheHelper.makeFile(newfile);
           NedCatalogGetter.getCatalog(params, f, w);
           CacheHelper.putFile(params, f);
       }
       return f;
   }

    /**
     * Retrieve an image, this call should only be used in server mode
     * @param params net parameters
     * @param dl download lister to monitor progress and cancel
     * @return one more more files, almost always this will be one file
     * @throws FailedRequestException when anything fails
     */
    public static FileData[] getImageSrv(NetParams params, DownloadListener dl) throws FailedRequestException {
        Assert.argTst(CacheHelper.isServer(), "Should only be used in server mode");
        return getImage(params,null,dl,false);
    }

    public static FileData[] getImage(NetParams params, Window w)
                                  throws FailedRequestException {
        Assert.argTst(!CacheHelper.isServer(), "Should only be used in client mode");
       return getImage(params,w,false);
    }

    public static FileData[] getImage(NetParams params, Window w, boolean moreCallsComing)
            throws FailedRequestException {
        Assert.argTst(!CacheHelper.isServer(), "Should only be used in client mode");
        return getImage(params,w,null,moreCallsComing);

    }

    private static FileData[] getImage(NetParams params,
                                       Window w,
                                       DownloadListener dl,
                                       boolean moreCallsComing) throws FailedRequestException {
       FileData retval[]= null;
       File f;
      if (params instanceof IrsaImageParams) {
          f=  getIrsaImage( (IrsaImageParams)params, w, moreCallsComing);
          retval= new FileData[] {new FileData(f,null)};
      }
      else if (params instanceof DssImageParams) {
          f=  getDssImage( (DssImageParams)params, w);
          retval= new FileData[] {new FileData(f,null)};
      }
      else if (params instanceof SloanDssImageParams) {
          f=  getSloanDssImage( (SloanDssImageParams)params, w);
          retval= new FileData[] {new FileData(f,null)};
      }
      else if (params instanceof NedImageParams) {
          f=  getNedImage( (NedImageParams)params, w);
          retval= new FileData[] {new FileData(f,null)};
      }
      else if (params instanceof WiseImageParams) {
          f=  getIbeImage((BaseIrsaParams) params, w, moreCallsComing);
          retval= new FileData[] {new FileData(f,null)};
      }
      else if (params instanceof SkyViewImageParams) {
          f=  getSkyViewImage( (SkyViewImageParams)params, w);
          retval= new FileData[] {new FileData(f,null)};
      }
      else if (params instanceof AnyFitsParams) {
          retval=  new FileData[] {getAnyFits( (AnyFitsParams)params, w,dl)};
      }
      else if (params instanceof AnyUrlParams) {
          retval=  getAnyUrlImage( (AnyUrlParams)params, w,dl);
      }
      else {
          Assert.tst(false, "Should never be here");
      }
      if (retval!=null && retval.length==0) throw new FailedRequestException("fd.length==0");

      return retval;
   }






   public static AstroImageInformation [] queryNedImages(
                                                 NedImQueryParams params,
                                                 Window           w) 
                                               throws FailedRequestException {
      AstroImageInformation  info[];
      Cache cache= CacheHelper.getObjectCache();
      info= (AstroImageInformation [])cache.get(params);
       if (info == null)  {          // if not in cache
           info = NedImageGetter.queryNedImages(params, w);
           cache.put(params,info,SEC_IN_DAY);
       }
      return info;
   }


    public static FixedObjectGroup  queryIsoImages(IsoImageListParams params,
                                                   Window             w)
                                  throws FailedRequestException {
        FixedObjectGroup info;
        Cache cache= CacheHelper.getObjectCache();
        info= (FixedObjectGroup)cache.get(params);
        if (info == null)  {          // if not in cache
            info = IsoImageGetter.queryIsoImages(params, w);
            if (info!=null) {
                cache.put(params,info,SEC_IN_DAY);
            }
        }
        return info;
    }


    public static FixedObjectGroup getIspyData(HorizonsIspyParams params,
                                               Component          c)
                                           throws FailedRequestException,
                                                  ParseException,
                                                  IOException {
        FileOutputStream out   =null;
        FileInputStream  in    =null;
        FixedObjectGroup retval=null;
        byte data[];
        try {
            File dataFile= CacheHelper.getFile(params);
            if(dataFile==null) {        // if not in cache, then goto network
                data=IspyGetter.getIspy(params, c);
                String fileName=params.getUniqueString()+".txt";
                dataFile= CacheHelper.makeFile(fileName);
                out=new FileOutputStream(dataFile);
                out.write(data);
                CacheHelper.putFile(params, dataFile);
            }
            else {
                in= new FileInputStream(dataFile);
                int size= in.available();
                data= new byte[size];
                int readSize= in.read(data);
                if (readSize==-1 || readSize!=size) {
                    throw new IOException(_prop.getError("ispyRead"));
                }
            }
            retval= IspyGetter.parseIspyData(data);
        } finally {
            FileUtil.silentClose(out);
            FileUtil.silentClose(in);
        }
        return retval;
    }

}
/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313) 
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
