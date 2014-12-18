package edu.caltech.ipac.firefly.server.visualize;

import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.FileData;
import edu.caltech.ipac.firefly.server.packagedata.FileInfo;
import edu.caltech.ipac.visualize.net.AnyFitsParams;
import edu.caltech.ipac.visualize.net.VisNetwork;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
/**
 * User: roby
 * Date: Nov 23, 2010
 * Time: 5:27:47 PM
 */


/**
 * @author Trey Roby
 * @deprecated use LockingVisNetwork
 * @see LockingVisNetwork
 */
public class FitsRetrieve {

    private static final Map<AnyFitsParams,ThreadContainer> _activeRequest= new HashMap<AnyFitsParams,ThreadContainer>();

    public static FileInfo getFile(URL url) throws FailedRequestException, SecurityException {
        FileInfo retval= null;
        AnyFitsParams params= null;
        try {
            params= new AnyFitsParams(url);

            ThreadContainer tc;
            synchronized (FitsRetrieve.class) {
                if (!_activeRequest.containsKey(params)) {
                    _activeRequest.put(params,new ThreadContainer(Thread.currentThread()));
                }
                else {
                    int i= 0;
                }
                tc= _activeRequest.get(params);
            }
            synchronized (tc._lock) {
                FileData fd[]= VisNetwork.getImage(params,null);
                retval = new FileInfo(fd[0].getFile().getPath(),
                                      fd[0].getSugestedExternalName(),
                                      fd[0].getFile().length());
            }

        }  catch (Exception e) {
            throw new FailedRequestException("No data",null,e);
        }
        finally {
            synchronized (FitsRetrieve.class) {
                if (params!=null) _activeRequest.remove(params);
            }
        }
        return retval;
    }

    private static class ThreadContainer {
        public final Thread _thread;
        public final Object _lock;
        public ThreadContainer(Thread t) {
            _thread= t;
            _lock= new Object();
        }
    }


}

