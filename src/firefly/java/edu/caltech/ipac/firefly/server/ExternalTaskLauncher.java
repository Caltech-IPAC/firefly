package edu.caltech.ipac.firefly.server;

import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.AppProperties;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 *
 * @author tatianag
 */
public class ExternalTaskLauncher {
    public static final int NORMAL_EXIT = 0; // by convention.
    public static final int ABORTED_BY_INTERRUPT = 556;

    private ExternalTaskHandler _handler;
    private String _exe = null;
    private List<String> _params= new ArrayList<String>(20);
    private ProcessBuilder _procBuilder = null;

    public static final Logger.LoggerImpl LOGGER = Logger.getLogger();


//============================================================================
//---------------------------- Constructors ----------------------------------
//============================================================================

    public ExternalTaskLauncher(String launcher) {

        String exe = AppProperties.getProperty(launcher + ".exe", null);
        if (exe != null) {
            exe = exe.trim();
            String[] exeparts = exe.split("\\s+");
            // the first keyword must be an executable, the rest are parameters
            Path exePath  = (new File(exeparts[0])).toPath();
            if (Files.isExecutable(exePath)) {
                _exe = exePath.toString();
                _procBuilder = new ProcessBuilder(_exe);
                if (exeparts.length > 1) {
                    for (int i=1; i<exeparts.length; i++) {
                        _params.add(exeparts[i]);
                    }
                }
            } else {
                LOGGER.error("The "+launcher+" launcher path is not an executable: "+exe);
                throw new IllegalArgumentException("The "+launcher+" launcher path is not an executable: "+exe);
            }

        } else {
            LOGGER.error(launcher + ".exe property is not defined");
            throw new IllegalArgumentException(launcher+".exe property is not defined");
        }
    }




//============================================================================
//---------------------------- Public Methods --------------------------------
//============================================================================

    public void setWorkDir(File dir) {
        _procBuilder.directory(dir);
    }

    public File getWorkDir() {
        return _procBuilder.directory();
    }

    public ExternalTaskHandler getHandler() {
        return _handler;
    }

    public void setHandler(ExternalTaskHandler handler) {
        _handler = handler;
    }

    public void addParams(Map<String,Object> params) {
        for(String key : params.keySet()) {
            Object val = params.get(key);
            if ( val == null ) {
                this.addParam(key);
            } else {
                this.addParam(key, val.toString());
            }
        }
    }

    public void addParam(String param) {
        _params.add(param);
    }

    public void addParam(File f) {
        addParam( f.getPath() );
    }
    public void addParam(int param) { addParam(param+""); }
    public void addParam(float param) { addParam(param+""); }
    public void addParam(double param) { addParam(param+""); }

    public void addParam(String param1, String param2) {
        addParam(param1);
        addParam(param2);
    }
    public void addParam(String param1, File param2) {
        addParam(param1);
        addParam(param2);
    }
    public void addParam(String param1, int param2) {
        addParam(param1);
        addParam(param2);
    }

    public void addParam(String param1, float param2) {
        addParam(param1);
        addParam(param2);
    }
    public void addParam(String param1, double param2) {
        addParam(param1);
        addParam(param2);
    }

    public Iterator entryIterator() {
        return _params.iterator();
    }

    private String getCommand() {
        return _exe;
    }

    public String getFullCommand() {
        return getFullCommand(false);
    }

    public String getFullCommand(boolean prettyPrint) {
        StringBuilder sb = new StringBuilder(300);
        sb.append(_exe);
        if (prettyPrint) {
            for (String param : _params) {
                if (param.startsWith("-")) {
                    sb.append(String.format("%n     "));
                } else {
                    sb.append(" ");
                }
                sb.append(param);
            }
            sb.append(String.format("%n"));
        } else {
            sb.append(_exe);
            for (String param : _params) {
                sb.append(" ");
                sb.append(param);
            }
        }
        return sb.toString();
    }

    public int execute() throws IOException {

        int status = -1;
        Process process=null;
        if ( getHandler() != null && !getHandler().abortExecution()) {
            try {

                if ( getHandler() != null ) {
                    getHandler().setup(this, _procBuilder.environment());
                }

                List<String> command= new ArrayList<String>(_params.size()+1);

                command.add(getCommand());
                command.addAll(_params);

                _procBuilder.command(command);

                process=_procBuilder.start();

                // Without error stream redirect, you need to have two separate Threads,
                // one reading from stdout and one reading from stderr,
                // to avoid the standard error buffer filling
                // while the standard output buffer was empty
                // (causing the child process to hang), or vice versa.
                Thread outHandler = new Thread(new StreamReader(StreamReader.STDOUT_TYPE, process.getInputStream(), process));
                Thread errHandler = new Thread(new StreamReader(StreamReader.STDERR_TYPE, process.getErrorStream(), process));
                outHandler.start();
                errHandler.start();

                status = process.waitFor();  // synchronous call, returns when process is done
                errHandler.join();
                outHandler.join();

            } catch( InterruptedException e ) {
                status = ABORTED_BY_INTERRUPT;
                if (process!=null) process.destroy();
                IOException ioe=  new IOException("Aborted by interrupt");
                ioe.initCause(e);
                throw ioe;
            }
            finally {
                if ( getHandler() != null ) {
                    getHandler().finish(status);
                }
            }
        }
        else {
            IOException ioe=  new IOException("Aborted by interrupt");
            ioe.initCause(new InterruptedException());
            throw ioe;
        }
        return status;
    }

//============================================================================
//---------------------------- Private / Protected Methods -------------------
//============================================================================


//============================================================================
//---------------------------- Inner Classes ---------------------------------
//============================================================================

    private class StreamReader implements Runnable {

        public static final int STDERR_TYPE = 0;
        public static final int STDOUT_TYPE = 1;

        private int _type;
        private InputStream _is;
        private Process _process;

        public StreamReader(int type, InputStream is, Process process) {
            _type = type;
            _is = is;
            _process = process;
        }

        public void run() {
            try {
                if ( _type == STDERR_TYPE ) {
                    getHandler().handleError(_is);
                } else {
                    getHandler().handleOut(_is);
                }
            } catch (InterruptedException e) {
                _process.destroy();
            }
        }
    }
}
