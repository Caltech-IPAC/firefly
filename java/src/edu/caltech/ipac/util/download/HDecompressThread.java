package edu.caltech.ipac.util.download;

import edu.caltech.ipac.util.hcompress.HDecompress;
import edu.caltech.ipac.util.hcompress.HDecompressException;

import java.io.OutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.channels.Pipe;
import java.nio.channels.Channels;

/**
 * Date: Aug 25, 2005
 *
 * @author Trey Roby
 * @version $id:$
 */
public class HDecompressThread implements Runnable {


//============================================================================
//---------------------------- Constructors ----------------------------------
//============================================================================
    private Thread       _thread= new Thread(this,"HDecompressThread");
    private InputStream  _in;
    private OutputStream _out;
    private Exception    _exception= null;
    private boolean      _done= false;


    HDecompressThread(OutputStream out) {
        _out= out;
    }

//============================================================================
//---------------------------- Public Methods --------------------------------
//============================================================================

    public OutputStream beginWithPipeOutputStream() throws IOException {
        Pipe pipe= Pipe.open();
        OutputStream osToPipe= Channels.newOutputStream(pipe.sink());
        _in= Channels.newInputStream( pipe.source());
        _thread.start();
        return osToPipe;
    }


    public boolean waitForDone() {
        try {
            _thread.join();
        } catch (InterruptedException ignore) {}

        return _done;
    }


    public boolean isDone() { return _done; }

    public boolean isSuccessful() { return _exception==null; }

    public IOException getException() {
        IOException e= null;
        if (_exception!=null) {
            e= new IOException("Exception durring compress");
            e.initCause(_exception);
        }
        return e;
    }

//============================================================================
//---------------------------- Methods from Runnable Interface ----------------
//============================================================================

    public void run() {
        try {
            new HDecompress().decompressFITS(_in,_out);
        } catch (HDecompressException e) {
            _exception= e;
        }
        finally {
            try { if (_in!=null) _in.close(); } catch (IOException ignore) {}
            try { if (_out!=null) _out.close(); } catch (IOException ignore) {}
        }
        synchronized (this) {
            _done= true;
            notifyAll();
        }
    }

//============================================================================
//---------------------------- Private / Protected Methods -------------------
//============================================================================

//============================================================================
//---------------------------- Factory Methods -------------------------------
//============================================================================


//============================================================================
//---------------------------- Inner Classes ---------------------------------
//============================================================================

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