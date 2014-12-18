package edu.caltech.ipac.util.download;

import edu.caltech.ipac.util.ClientLog;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.Assert;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Date: Aug 23, 2005
 *
 * @author Trey Roby
 * @version $id:$
 */
public class MultipartMimeOutputStream extends FilterOutputStream {

    /** default encoding */
    //private static final String DEFAULT_ENCODING = "ISO-8859-1";

    /** preferred encoding */
    //private String _encoding = DEFAULT_ENCODING;
    /**
     * MIME boundary that delimits parts
     */
    private String _boundary;
    private OutputStream _out= null;

    private boolean _endOfSection    = false;
    private boolean _preambleDone    = false;
    private boolean _buffStop        = true;

    private byte      _buffer[]      = new byte[10*1024];
    private byte      _saveBuff[]    = new byte[_buffer.length];
    private int       _saveLength    = 0;
    private int       _buffIdx       = 0;
    private FileNamer _fileNamer;
    private TextLine  _textLine      = new TextLine();
    private Headers   _headers       = new Headers();
    private boolean   _logEachFile;
    private List<FileData>  _fileNames= new ArrayList<FileData>(5);
    private HDecompressThread _hdThread= null;

//============================================================================
//---------------------------- Constructors ----------------------------------
//============================================================================
    /**
     * Creates an output stream filter built on top of the specified
     * underlying output stream.
     *
    */
    public MultipartMimeOutputStream(URLConnection conn,
                            boolean useOriginalFileName,
                            File    baseFileName,
                            boolean logEachFile)
                                                     throws IOException {
        super(null);
        String type = conn.getContentType();
        _logEachFile= logEachFile;
        _fileNamer= new FileNamer(useOriginalFileName, baseFileName);
        // If one value is null, choose the other value

        if (type == null ||
            type.toLowerCase().indexOf("multipart")<0) {
            throw new IOException("Posted content type isn't multipart/form-data");
        }

        _boundary = MultipartMimeParser.extractBoundary(type);
    }

    public FileData[] getFileNames() {
        return _fileNames.toArray(new FileData[_fileNames.size()]);
    }

    public Iterator<FileData> fileNameIterator() { return _fileNames.iterator(); }

//============================================================================
//---------------------------- Methods from FilteredOutputStream Class -------
//============================================================================

    public void write(int b) throws IOException {
        putByte((byte)b);
    }


    public void flush() throws IOException {
        if (_out!=null) _out.flush();
    }


    public void close() throws IOException {
        try {
            flush();
        } catch (IOException ignored) {
        }
        finishHDecompress();
        if (_out!=null) _out.close();
    }

//============================================================================
//---------------------------- Private / Protected Methods -------------------
//============================================================================


    private void putByte(byte b) throws IOException {
        if (_headers.getContainsSection()) {
            if (_preambleDone) {
                putSection(b);
            }
            else {
                processPreamble(b);
            }
        }
        else {
            throw new IOException(
                           "Undefined data at end of multipart mime file");
        }
    }


    private void processPreamble(byte b) throws IOException {
        boolean lineDone= _textLine.makeTextLine(b);
        if (lineDone) {
            if (_textLine.getTextLine().startsWith(_boundary)) {
                _preambleDone= true;
            }
        }
    }

    private void putSection(byte b) throws IOException {
        if (_headers.isPartHeaderDone()) {
            writeAttachment(b);
        }
        else {
            _headers.processHeader(b);
        }

    }


    private void finishHDecompress() throws IOException {
        HDecompressThread hdt= _hdThread;
        _hdThread= null;
        if (hdt!=null) {
            hdt.waitForDone();
            if (!hdt.isSuccessful()) {
                throw hdt.getException();
            }
        }
    }

    private void writeAttachment(byte b) throws IOException {
        if (_out==null) {
            _hdThread= null;
            File f= _fileNamer.makeFile(_headers);
            String sugested= _fileNamer.makeAdjustedDispositionFile(_headers);
            _fileNames.add(new FileData(f,sugested) );
            if (_logEachFile) {
                String hdr[]= _headers.getHeaders();
                String outStr[]= new String[hdr.length+1];
                outStr[0]= "Writing file: "+ f.getPath();
                for(int i=1; i<outStr.length; i++) {
                    outStr[i]= "header["+(i-1)+"]: "+hdr[i-1];
                }
                ClientLog.message(outStr);
            }

            _out=new BufferedOutputStream( new FileOutputStream(f), 16*1024);
            String contentEncoding= _headers.getContentEncoding();
            if (contentEncoding==null) {
                // do nothing
            }
            else if (contentEncoding.toLowerCase().equals("x-h")) {
                _hdThread= new HDecompressThread(_out);
                _out= _hdThread.beginWithPipeOutputStream();
            }
            else if (contentEncoding.toLowerCase().equals("x-gzip")) {
                Assert.tst("gzip not finished");
            }
            else {
                ClientLog.warning("unreconized Content-encoding: "+
                                  contentEncoding, "cannot uncompress");
            }
            _saveLength= 0;

        }
        boolean buffStop= makeDataLine(b);
        int boundaryLength= _boundary.length();
        if (buffStop) {
            if(_buffIdx>= boundaryLength) {
                _endOfSection=true;
                for(int i=0; i<boundaryLength; i++) {
                    if(_boundary.charAt(i)!=_buffer[i]) {
                        _endOfSection=false; // Not the boundary!
                        break;
                    }
                }
            }
            MultipartMimeParser.writeAttachmentBuff(_out,_saveBuff,
                                                    _saveLength, _endOfSection);
            if(_endOfSection)  {
                try {
                    _out.close();
                } finally {
                    _out=null;
                    //_partHeaderDone= false;
                    _headers= new Headers();
                    finishHDecompress();
                }
            }
            else {
                _saveLength= _buffIdx;
                System.arraycopy(_buffer,0,_saveBuff,0,_buffIdx);
            }

        }
    }


    private boolean makeDataLine(byte b) {
        if (_buffStop) {
            _buffStop= false;
            _buffIdx=0;
        }
        _buffStop= (b=='\n');
        _buffer[_buffIdx++]=b;

        if (_buffStop || _buffIdx==_buffer.length) {
            _buffStop= true;
        }
        return _buffStop;
    }




//============================================================================
//---------------------------- Inner Classes ---------------------------------
//============================================================================

    private static class FileNamer {
        private boolean _useOriginalFileName;
        private File    _baseFileName;
        private int     _fileCnt= 1;

        public FileNamer( boolean useOriginalFileName, File baseFileName) {
            _useOriginalFileName= useOriginalFileName;
            _baseFileName       = baseFileName;
        }

        public File makeFile(Headers headers) {
            File partFile;
            String dispositionFname= headers.getDispositionFilename();
            File dir= _baseFileName.isDirectory() ? _baseFileName :
                      _baseFileName.getParentFile();
            String realExt= getRealExtenstion(headers);
            if(dispositionFname!=null && !dispositionFname.equals("")) {
                if (_useOriginalFileName && !_baseFileName.isDirectory()) {
                    if (realExt!=null) {
                        String base= FileUtil.getBase(dispositionFname);
                        partFile= new File(dir, base+"."+realExt);
                    }
                    else {
                        partFile= new File(dir, dispositionFname);
                    }
                } // end if _useOriginalFileName
                else {
                    partFile= makePartFile(dir, dispositionFname, realExt);
                }
            }
            else {
                partFile= makePartFile(dir, null, realExt);
            }
            return partFile;
        }

        public String makeAdjustedDispositionFile(Headers headers) {
            String dispositionFname= headers.getDispositionFilename();
            String retval= dispositionFname;
            String realExt= getRealExtenstion(headers);
            if(dispositionFname!=null && !dispositionFname.equals("")) {
                if (realExt!=null) {
                    String base= FileUtil.getBase(dispositionFname);
                    retval= base+"."+realExt;
                }
            }
            return retval;
        }

        private File makePartFile(File dir,
                                  String dispFilename,
                                  String ext) {
            if (ext==null) {
                ext= (dispFilename==null) ?
                     FileUtil.getExtension(_baseFileName) :
                     FileUtil.getExtension(dispFilename);
            }

            String base= FileUtil.getBase(_baseFileName);
            File f= new File(dir, base+"-"+_fileCnt+"."+ext);
            _fileCnt++;
            return f;
        }

        private String getRealExtenstion(Headers headers) {
            return URLDownload.getContentTypeExtension(
                           headers.getContentType());
        }
        //public File getLastFile() { return _partFile; }
    }



    private static class TextLine {

        private static final String DEFAULT_ENCODING = "ISO-8859-1";
        private static final int BUFF_SIZE = 100;

        /** preferred encoding */
        private String _encoding = DEFAULT_ENCODING;

        private boolean _lineDone= true;
        private String  _line= null;
        private StringBuffer  _lineBuffer= null;
        private byte    _buffer[]  = new byte[BUFF_SIZE];
        private int     _buffIdx= 0;

        public TextLine() {}

        /**
         * @param b add a byte to the text line
         * @return true if line is complete, otherwise false
         * @throws IOException if the encoding is unknown
         */
        public boolean makeTextLine(byte b) throws IOException {
            if (_lineDone) {
                _lineBuffer= new StringBuffer(BUFF_SIZE+2);
                _lineDone= false;
                _buffIdx=0;
            }
            if (_buffIdx==_buffer.length) {
                _buffIdx=0;
            }
            if(b=='\n') {
                _lineDone=true;
                if(_buffer[_buffIdx-1]=='\r') {
                    _buffIdx--;
                }
            }
            else {
                _buffer[_buffIdx++]=b;
            }

            if (_lineDone || _buffIdx==_buffer.length) {
                _lineBuffer.append(new String(_buffer, 0, _buffIdx, _encoding));
            }
            if (_lineDone) _line= _lineBuffer.toString();
            return _lineDone;
        }

        public String getTextLine() {
            String retval= null;
            if (_lineDone) retval= _line;
            return retval;
        }
    }


    private static class Headers {
        private TextLine _textLine      = new TextLine();
        private List<String> _headers       = new ArrayList<String>(5);
        private boolean  _partHeaderDone= false;
        private String   _headerLine;
        private boolean  _foundHeaderLines= false;
        private boolean  _containsSection  = true;
        private String   _contentType  ="text/plain";  // rfc1867 says this is the default
        private String   _contentEncoding= null;
        private MultipartMimeParser.Disposition _disposition= null;

        public Headers() {

        }

        public void processHeader(byte b) throws IOException {
            boolean lineDone= _textLine.makeTextLine(b);
            if (lineDone) {
                String line= _textLine.getTextLine();
                if (line.length() == 0 && _foundHeaderLines) {
                    if (_foundHeaderLines) { // at end of headers
                        _partHeaderDone= true;
                        _headers.add(_headerLine);
                        parseHeaders();
                        _headerLine= "";
                        _foundHeaderLines=false;
                    }
                    else { // at end of file, anything after this is undefined
                        _containsSection= false;
                    }
                }
                else { // process a header line
                    _foundHeaderLines=true;
                    if ((line.startsWith(" ") || line.startsWith("\t"))) {
                        _headerLine = _headerLine + line;
                    }
                    else {
                        if (_headerLine!=null && _headerLine.length()>0) {
                            _headers.add(_headerLine);
                        }
                        _headerLine= line;
                    }
                }
            }
        }


        private void parseHeaders() throws IOException {

            for(String headerline: _headers) {
                if(headerline.toLowerCase().startsWith("content-disposition:")) {
                    // Parse the content-disposition line
                    _disposition= MultipartMimeParser.extractDispositionInfo(
                                   headerline);
                }
                else if(headerline.toLowerCase().startsWith("content-type:")) {
                    // Get the content type, or null if none specified
                    String type=MultipartMimeParser.extractContentType(headerline);
                    if(type!=null) {
                        _contentType=type;
                    }
                }
                else if(headerline.toLowerCase().startsWith(
                                                  "content-encoding:")) {
                    String line = headerline.toLowerCase();
                    int end = line.indexOf(";");
                    if (end == -1)  end = line.length();
                    _contentEncoding= line.substring(17, end).trim();  // "content-encoding:" is 13
                }
            } // end loop
        }


        public MultipartMimeParser.Disposition getDisposition() {
            return _disposition;
        }

        public boolean isPartHeaderDone() { return _partHeaderDone; }
        public boolean getContainsSection() { return _containsSection; }

        public String getDispositionFilename() {
            return (_disposition!=null) ? _disposition.getFilename() : null;
        }
        public String getContentType() { return _contentType; }
        public String getContentEncoding() { return _contentEncoding; }

        public String[] getHeaders() {
            return _headers.toArray(new String[_headers.size()]);
        }

    }

}

