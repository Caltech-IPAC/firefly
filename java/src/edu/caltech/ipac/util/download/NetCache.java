/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util.download;

import edu.caltech.ipac.util.ClientLog;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheKey;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NetCache implements Serializable {

    enum ExpireType { ABS_DATE, OFFSET_DATE, NONE}

    private static final String CACHE_WRITE_DATE_PREF= "NetCache.writeDate";
    private static transient NetCache _theInstance;
    private transient File   _cacheFile;
    private transient long _lastAccessDate= 1L;
    private transient ObjectCache _objCacheInterface= null;
    private transient FileCache _fileCacheInterface= null;
    private transient long     _maxCacheSize= 5000 * FileUtil.MEG;
    private transient int _totalFileSize;
    private transient boolean _enabled= true;
    private File    _dir;
    private Map<String,CacheInfo>     _cache= new HashMap<String,CacheInfo>(213);

   private NetCache() { }

    /**
     * Return the only instance of Platform
     * @return singleton instance of NetCache
     */
   public static NetCache getInstance() {
      if (_theInstance == null) _theInstance= new NetCache();
      return _theInstance;
   }

    public static Cache getFileCache() {
        NetCache instance= getInstance();
        if (instance._fileCacheInterface==null) {
            instance._fileCacheInterface= instance.makeFileCache();
        }
        return instance._fileCacheInterface;
    }

    public static Cache getObjectCache() {
        NetCache instance= getInstance();
        if (instance._objCacheInterface==null) {
            instance._objCacheInterface= instance.makeObjCache();
        }
        return instance._objCacheInterface;
    }




   static private void setInstance(NetCache netCache) {
      _theInstance= netCache;
   }

   public static void initializeCache(File f) throws IOException{
      NetCache netCache= null;
      try {
         FileInputStream fis= new FileInputStream(f);
         ObjectInputStream objIS= new ObjectInputStream(
                                         new BufferedInputStream(fis));
         try {
            ClientLog.message("Cache File: "+ f.getPath());
            netCache= (NetCache)objIS.readObject();
            setInstance(netCache);
            netCache.setCacheFile(f);
             netCache._lastAccessDate= System.currentTimeMillis();
         } catch (ClassNotFoundException e) {
            NetCache.getInstance().setCacheFile(f);
            throw new IOException("class not found " + e.getMessage());
         }
         finally {
             FileUtil.silentClose(objIS);
         }
         netCache.syncCache();
      }
      catch (IOException ioe) {
         NetCache.getInstance().setCacheFile(f);
         NetCache.getInstance()._lastAccessDate= 1L;
         if (ioe instanceof FileNotFoundException) {
             ClientLog.message("A cache file does not exist- " +
                               "initializing a new cache");
         }
         else {
             throw ioe;
         }
      }
   }

   private void reloadCacheIfStale() {
       long saveDate= AppProperties.getLongPreference(CACHE_WRITE_DATE_PREF,0);
       if (_lastAccessDate< saveDate || _lastAccessDate< _cacheFile.lastModified()) {
           ObjectInputStream objIS= null;
           try {
               FileInputStream fis= new FileInputStream(this._cacheFile);
               objIS= new ObjectInputStream( new BufferedInputStream(fis));
               NetCache netCache= (NetCache)objIS.readObject();
               _cache= netCache._cache;
               objIS.close();
               _lastAccessDate= System.currentTimeMillis();
               ClientLog.message("reloading cache");
               syncCache();
           }
           catch (Exception e) {
               ClientLog.warning("Error reloading cache", e.toString());
           }
           finally {
               FileUtil.silentClose(objIS);
           }
       }
   }

   protected void saveCache()  {
      NetCache netCache= getInstance();
      File f= netCache.getCacheFile();
      File dir= f.getParentFile();
      if (!dir.exists()) dir.mkdirs();
      try {
         if ( f != null) {
            saveCache(f);
         }
         else {
            throw new FileNotFoundException("No cache file has need set.");
         }
      } catch (IOException e) {
          ClientLog.warning(true,"Cache IO problem with saving cache" ,
                            e.toString());
       }
   }

   protected void saveCache(File f) throws IOException {
      NetCache netCache= getInstance();
      netCache.setCacheFile(f);
      FileOutputStream fos= new FileOutputStream(f);
      ObjectOutputStream objOS= new ObjectOutputStream(
                                      new BufferedOutputStream(fos));
      objOS.writeObject(netCache);
      objOS.flush();
      objOS.close();
      _lastAccessDate= System.currentTimeMillis();
      AppProperties.setPreference(CACHE_WRITE_DATE_PREF,
                                   _lastAccessDate+"");
   }

   void setCacheFile(File f) { _cacheFile= f; }
   File getCacheFile()       { return _cacheFile; }

   public void setCacheEnabled(boolean enabled) { _enabled= enabled; }

   private void syncCache() {
      _totalFileSize= 0;
      Set<String> keys= _cache.keySet();
      String key;
      CacheInfo info;
      File tstFile;
       boolean removed;
      //System.out.println("begin syncing files: ");
      for(Iterator i= keys.iterator(); (i.hasNext());) {
          key= (String)i.next();
          info= _cache.get(key);
          if (info.isExpired()) {
              i.remove();
          }
         else {
             if (info.isFileCache()) {
                 //System.out.println("NetCache: syncing: "+ info.getCacheFile());
                 FileData fDataAry[]= info.getCacheFile();
                 removed= false;
                 for(FileData fData : fDataAry) {
                     tstFile= makeFullPathFile(fData);
                     if (!tstFile.exists()) {
                         if (!removed) i.remove();
                         removed= true;
                     } // end if tstFile.exist
                     else {
                         _totalFileSize+= tstFile.length();
                     } // end else
                 }
             } // end if isFileCache
         } // end else isExpired
      } // end loop
   }

   public void setCacheDir(File dir) {
      _dir= dir;
      if (_cacheFile != null) {
         File newCacheFile= new File( _dir, _cacheFile.getName());
         setCacheFile(newCacheFile);
         syncCache();
      }
   }

   public File getCacheDir() {
      return _dir;
   }

    /**
     * set the maximum cache size for the file cache
     * @param sizeInK in Kbytes
     */
   public void setMaxCacheSize(int sizeInK) {
      _maxCacheSize= sizeInK * 1024;
   }

   public long getMaxCacheSize() {
      return _maxCacheSize/1024;
   }

    private void addObjectToCache(CacheKey params,
                                  Object o,
                                  ExpireType expireType,
                                  Date expireDate,
                                  int lifespanInSecs) {
        if (_enabled) {
            if (o instanceof Serializable) {
                reloadCacheIfStale();
                String uniqueStr=params.getUniqueString();
                if (expireType==ExpireType.OFFSET_DATE) {
                    _cache.put(uniqueStr, new CacheInfo(o, lifespanInSecs) );

                }
                else {
                    _cache.put(uniqueStr, new CacheInfo(o, expireDate) );
                }
                saveCache();
            }
            else {
                ClientLog.warning(true, "Attempting to add a " +
                                        "non-serializable object:" + o.toString());
            }
        }
    }

    public void addObjectToCache(CacheKey params, Object o, int lifespanInSecs) {
        addObjectToCache(params,o,ExpireType.OFFSET_DATE,null,lifespanInSecs);
    }


   public void addObjectToCache(CacheKey params, Object o, Date expireDate) {
       ExpireType expireType= (expireDate==null) ? ExpireType.NONE : ExpireType.ABS_DATE;
       addObjectToCache(params,o,expireType,expireDate,-1);
   }

    public void addObjectToCache(CacheKey params, Object o) {
        addObjectToCache(params,o,null);
    }



   public void addFileToCache(CacheKey params, String fileName) {
       addFileToCache(params,fileName,null,
                        FileData.FileType.UNKNOWN  );
   }
    public void addFileToCache(CacheKey params,
                               String    fileName,
                               String    sugestedUserFilename) {
        addFileToCache(params,fileName,sugestedUserFilename,
                       FileData.FileType.UNKNOWN  );
    }

   public void addFileToCache(CacheKey params,
                              String    fileName,
                              String    sugestedUserFilename,
                              FileData.FileType fileType) {
       addFileToCache(params,
                      new FileData(new File(fileName),
                        sugestedUserFilename,fileType));

   }


    public void addFileToCache(CacheKey params,
                               FileData  fileData) {

      if (_enabled) {
         reloadCacheIfStale();
         String uniqueStr=params.getUniqueString();
         File workFile= makeFullPathFile(fileData);
         _totalFileSize+= workFile.length();
         FileData fData= makeRelativeFileData(fileData);
         _cache.put(uniqueStr, new CacheInfo(fData) );
         if (_totalFileSize > _maxCacheSize) purgeCache(workFile);
         saveCache();
      }
   }


    public void addFilesToCache(CacheKey params, FileData fileData[]) {
        if (_enabled) {
            reloadCacheIfStale();
            String uniqueStr=params.getUniqueString();
            File workFiles[]= new File[fileData.length];
            FileData outFileData[]= new FileData[fileData.length];
            for(int i=0; i<fileData.length; i++) {
                workFiles[i]= makeFullPathFile(fileData[i]);
                outFileData[i]= makeRelativeFileData(fileData[i]);
                _totalFileSize+= workFiles[i].length();
            }
            _cache.put(uniqueStr, new CacheInfo( outFileData) );
            if (_totalFileSize > _maxCacheSize) purgeCache(workFiles);
            saveCache();
        }
    }

   public void removeFromCache(CacheKey params) {
      reloadCacheIfStale();
      String uniqueStr= params.getUniqueString();
      if (_cache.containsKey(uniqueStr)) {
           CacheInfo info= _cache.get(uniqueStr);
           if (info.isFileCache()) {
               File f;
               for(FileData fData : info.getCacheFile()) {
                   f= makeFullPathFile(fData);
                   if (f.exists() && f.canWrite()) f.delete();
               }
           }
           _cache.remove(uniqueStr);
           saveCache();
      }
   }

   private void purgeCache(File exemptFile) {
       File f[]= {exemptFile};
       purgeCache(f);
   }


   private void purgeCache(File exemptFiles[]) {
      //System.out.println("NetCach.purgeCache");
      Map.Entry<String,CacheInfo> entry;
      Set<Map.Entry<String,CacheInfo>> allEntries= _cache.entrySet();
      Iterator<Map.Entry<String,CacheInfo>> i;
      List<Map.Entry<String,CacheInfo>> mapList=
                      new ArrayList<Map.Entry<String,CacheInfo>>(_cache.size());
       CacheInfo info;
       File f;

      for(i= allEntries.iterator(); (i.hasNext());) {
          entry= i.next();
          info= entry.getValue();
          if (info.isExpired()) {
             i.remove();
          }
          else {
             if (info.isFileCache()) {
                 mapList.add(entry); 
             }
          }
      }


       // sort the cache by cache date
       // then starting with the old start deleting file until the total size
       // is down to the target size

      Collections.sort(mapList, new CompareDate() );
      for(i= mapList.iterator(); (i.hasNext()); ) {
          entry= i.next();
          info= entry.getValue();
          FileData fNameAry[]= info.getCacheFile();
          for(FileData fData : fNameAry) {
              f= makeFullPathFile(fData);
              if ( !containsFile(f,exemptFiles)) {
                  _totalFileSize-= f.length();
                  f.delete();
                  if (_totalFileSize < _maxCacheSize) break;
              }
          }
      }
   }


   public boolean containsFile(File f, File fAry[]) {
       boolean found= false;
       for(int i=0; (i<fAry.length && !found); i++) {
           found= f.equals(fAry[i]);
       }
       return found;
   }

   public FileData[] checkCacheForFile(CacheKey params) {
      FileData retFileAry[]= null;
      if (_enabled) {
         reloadCacheIfStale();
         String uniqueStr=params.getUniqueString();
         if (_cache.containsKey(uniqueStr)) {
            CacheInfo info= _cache.get(uniqueStr);
            FileData cFile[]= info.getCacheFile();
            retFileAry= new FileData[cFile.length];
            boolean removed= false;
            for(int i=0; i<cFile.length; i++) {
                retFileAry[i]= new FileData( makeFullPathFile(cFile[i]),
                                          cFile[i].getSugestedExternalName(),
                                          cFile[i].getFileType());
                if (!retFileAry[i].getFile().exists()) {
                    if (!removed)  _cache.remove(uniqueStr);
                    removed= true;
                    retFileAry[i]= null;
                }
            }
            if (removed) {
                for(FileData retFile : retFileAry) {
                    if (retFile != null && retFile.getFile().exists()) {
                        retFile.getFile().delete();
                    }

                }
                retFileAry= null;
            }
         }
      }
      return retFileAry;
   }

   public Object checkCacheForObject(CacheKey params) {
      Object retObject= null;
      if (_enabled) {
         reloadCacheIfStale();
         String uniqueStr=params.getUniqueString();
         if (_cache.containsKey(uniqueStr)) {
            CacheInfo info= _cache.get(uniqueStr);
            if (info.isExpired()) {
                retObject= null;
                _cache.remove(uniqueStr);
            }
            else {
                info.accessed();
                retObject= info.getCacheObject();
            }
         }
      }
      return retObject;
   }

    private File makeFullPathFile(FileData fData) {
        return makeFullPathFile(fData.getFile());
    }

    private File makeFullPathFile(File f) {
        return new File(_dir,f.getName());
    }


    private FileData makeRelativeFileData(FileData fData) {
        return new FileData(new File(fData.getFile().getName()),
                            fData.getSugestedExternalName(),
                            fData.getFileType());
    }


    private ObjectCache makeObjCache() { return new ObjectCache(); }
    private FileCache makeFileCache() { return new FileCache(); }


    // ===================================================================
   // ------------------  Private Inner Classes    ----------------------
   // ===================================================================


    private class ObjectCache implements Cache {
        public void put(CacheKey key, Object value) {
            addObjectToCache(key,value);
        }

        public void put(CacheKey key, Object value, int lifespanInSecs) {
            addObjectToCache(key,value,lifespanInSecs);
        }

        public void put(CacheKey key, Object value, Date expireDate) {
            addObjectToCache(key,value,expireDate);
        }

        public Object get(CacheKey key) { return checkCacheForObject(key); }

        public boolean isCached(CacheKey key) { // check the file cache
            return (checkCacheForObject(key) != null);
        }

        public int getSize() { return _cache.size(); } // this is more than just object data

        public List<String> getKeys() { return new ArrayList<String>(_cache.keySet()); } // this is more keys than just oject data
    }


    private class FileCache implements Cache {
        public void put(CacheKey key, Object value) {
            if (value instanceof String) { //interpreted as a file name
                addFileToCache(key,(String)value);
            } else if (value instanceof File) {
                addFileToCache(key,((File)value).getName());
            } else if (value instanceof FileData) {
                addFileToCache(key,(FileData)value);
            } else if (value instanceof FileData[]) {
                addFilesToCache(key,(FileData[])value);
            } else {
                throw new IllegalArgumentException("value must be a string " +
                                                   "(relative path file name) or " +
                                                   "a FileData Object");
            }
        }

        public void put(CacheKey key, Object value, int lifespanInSecs) {
            put(key,value); // file data cache does not support life span
        }
        public void put(CacheKey key, Object value, Date expireDate) {
            put(key,value); // file data cache does not support life span
        }

        public Object get(CacheKey key) { return checkCacheForFile(key); }

        public boolean isCached(CacheKey key) {
            return (checkCacheForFile(key)!=null);
        }

        public int getSize() { return _cache.size(); } // this is more than just file data

        public List<String> getKeys() { return new ArrayList<String>(_cache.keySet()); } // this is more keys than just file data
    }



   public class CacheInfo implements Serializable {
       private final ExpireType _expireType;
       private final FileData _cacheFile[];
       private final Object   _cacheObj;
       private final boolean  _isFile;
       private final Date     _cacheDate;
       private final int _lifespanInSecs;
       private Date     _expireDate;


      CacheInfo(FileData cacheFile) {
          this(new FileData[] {cacheFile});
      }

       CacheInfo(FileData cacheFile[]) {
           _cacheDate= new Date();
           _cacheFile= cacheFile;
           _isFile   = true;
          _expireType= ExpireType.NONE;
          _lifespanInSecs= -1;
          _cacheObj=  null;
      }

      CacheInfo(Object o, Date expireDate) {
          if (expireDate!=null) {
              _expireType= ExpireType.ABS_DATE;
          }
          else {
              _expireType= ExpireType.NONE;
          }
          _cacheDate= new Date();
          _cacheFile= null;
          _lifespanInSecs= -1;
          _expireDate= expireDate;
          _cacheObj=  o;
          _isFile   = false;
      }

       CacheInfo(Object o,  int lifespanInSecs) {
           if (lifespanInSecs>0) {
               _expireType= ExpireType.OFFSET_DATE;
           }
           else {
               _expireType= ExpireType.NONE;
           }
           _cacheDate= new Date();
           _cacheFile= null;
           _lifespanInSecs= lifespanInSecs;
           _expireDate= computeLifespanDate();
           _cacheObj=  o;
           _isFile   = false;

       }

       public void accessed() {
           if (_expireType==ExpireType.OFFSET_DATE) {
               _expireDate= computeLifespanDate();
           }
       }

      public boolean isFileCache() { return _isFile; }
      public Date getCacheDate()   { return _cacheDate; }

      public FileData[] getCacheFile() {
         Assert.tst(_isFile);
         return _cacheFile;
      }

      public Object getCacheObject() { 
         Assert.tst(!_isFile);
         return _cacheObj; 
      }

      public boolean isExpired() {
         boolean retval= false;
         if (_expireDate != null)
               retval=  new Date().after(_expireDate);
         return retval;
      }

       private Date computeLifespanDate() {
           Date retval= null;
           if (_lifespanInSecs>0)  {
               long mills= System.currentTimeMillis() + _lifespanInSecs*1000;
               retval= new Date(mills);
           }
           return retval;
       }



   }


   private class CompareDate implements Comparator<Map.Entry> {
      public int compare(Map.Entry e1, Map.Entry e2) {
        CacheInfo info1= (CacheInfo)e1.getValue();
        CacheInfo info2= (CacheInfo)e2.getValue();
        return info1.getCacheDate().compareTo(info2.getCacheDate());
      }
   }
}
