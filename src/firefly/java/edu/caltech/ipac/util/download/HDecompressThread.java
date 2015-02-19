/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
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

