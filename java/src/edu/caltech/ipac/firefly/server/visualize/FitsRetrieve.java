package edu.caltech.ipac.firefly.server.visualize;

import edu.caltech.ipac.client.net.FailedRequestException;
import edu.caltech.ipac.client.net.FileData;
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
                FileData fd[]= VisNetwork.getImageSrv(params,null);
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
