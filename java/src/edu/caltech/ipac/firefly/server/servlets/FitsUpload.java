package edu.caltech.ipac.firefly.server.servlets;

import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.visualize.VisContext;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;


/**
 * Date: Feb 11, 2007
 *
 * @author Trey Roby
 * @version $Id: FitsUpload.java,v 1.19 2011/10/25 05:23:50 roby Exp $
 */
public class FitsUpload extends BaseHttpServlet {

    private static final String _nameBase="upload";
    private static final String _fitsNameExt=".fits";
    private static final String DEFAULT_ENCODING = "ISO-8859-1";

    protected void processRequest(HttpServletRequest req, HttpServletResponse res) throws Exception {

        OutputStream fileOut= null;
        FileWriter headWriter= null;
        File dir= VisContext.getVisUploadDir();
        File uploadedFile= getUniqueName(dir);
        try {




            BufferedInputStream in = new BufferedInputStream(req.getInputStream());

            String boundary= readLine(in);
            String inStr;
            int saveLength= 0;
            //TODO : determine if this file is a zip files - gzip .gz starts with 2 hex values: 1f 8b
            //                                               .zip file begin with  PK
            //                                               .Z file begin with  1f 9d
            headWriter= new FileWriter(new File(dir,"headers.dat"));
            for(inStr= readLine(in); (inStr.length()>0); inStr= readLine(in)) {
                headWriter.write(inStr + "\n");
            }

            byte[] buf=new byte[8*1024];
            byte[] saveBuff= new byte[buf.length];
            fileOut=new BufferedOutputStream(
                                          new FileOutputStream(uploadedFile), 16*1024);

            int read=0;
            int boundaryLength=boundary.length();
            boolean eof=false;

            while(!eof) {
                read=readLine(in, buf, 0, buf.length, true);
                // check for eof and boundary
                if(read==-1) {
                    throw new IOException("unexpected end of part");
                }
                else {
                    if(read>= boundaryLength) {
                        eof=true;
                        for(int i=0; i<boundaryLength; i++) {
                            if(boundary.charAt(i)!=buf[i]) {
                                eof=false; // Not the boundary!
                                break;
                            }
                        }
                    }
                    writeAttachmentBuff(fileOut,saveBuff, saveLength, eof);
                    if(!eof)  {
                        saveLength= read;
                        System.arraycopy(buf,0,saveBuff,0,read);
                    }
                }
            }

            FileUtil.silentClose(fileOut);

        } catch (EOFException e) {
        } finally {
            FileUtil.silentClose(fileOut);
            FileUtil.silentClose(headWriter);
        }

        if (FileUtil.isGZipFile(uploadedFile)) {
            File uploadedFileZiped= new File(uploadedFile.getPath() + "." + FileUtil.GZ);
            uploadedFile.renameTo(uploadedFileZiped);
            FileUtil.gUnzipFile(uploadedFileZiped, uploadedFile, (int)FileUtil.MEG);
        }

        PrintWriter resultOut = res.getWriter();
        String retval= VisContext.replaceWithPrefix(uploadedFile);
        resultOut.println(retval);
        String size= StringUtils.getSizeAsString(uploadedFile.length(),true);
        Logger.info("Successfully uploaded file: "+uploadedFile.getPath(),
                    "Size: "+ size);
        Logger.stats(Logger.VIS_LOGGER,"Fits Upload", "fsize", (double)uploadedFile.length()/StringUtils.MEG, "bytes", size);
    }


    /**
     * Read the next line of input.
     *
     * @return     a String containing the next line of input from the stream,
     *        or null to indicate the end of the stream.
     * @exception IOException	if an input or output exception has occurred.
     */
    /**
     * Read the next line of input.
     *
     * @return     a String containing the next line of input from the stream,
     *        or null to indicate the end of the stream.
     * @exception IOException	if an input or output exception has occurred.
     */
    private String readLine(BufferedInputStream in) throws IOException {
        StringBuffer sbuf = new StringBuffer();
        int result;
        byte[] buf=new byte[1024];
        String retval= null;

        do {
            result = readLine(in, buf, 0, buf.length, false);  // does +=
            if (result != -1) {
                sbuf.append(new String(buf, 0, result, DEFAULT_ENCODING));
            }
        } while (result == buf.length);  // loop only if the buffer was filled

        if (sbuf.length()==0 && result == -1 ) {
            retval= null;

        }
        else {
            retval= sbuf.toString();
        }

        return retval;
    }

    /**
     *
     * @param b byte array to read into
     * @param off offset into the array
     * @param len maximum length
     * @return number of bytes read
     */
    private  int readLine(BufferedInputStream in,
                          byte[] b,
                          int off,
                          int len,
                          boolean keepNL) throws IOException{
        int result= 0;
        boolean lineComplete= false;
        int buffIdx= 0;

        while(buffIdx<len && result!=-1 && !lineComplete) {
            result= in.read();
            if (result > -1) {
                if ((byte)result=='\n') {
                    lineComplete= true;
                    if (keepNL) {
                        b[off++]= (byte)result;
                        buffIdx++;
                    }
                }
                else if ((byte)result=='\r') {
                    lineComplete= true;
                    if (keepNL) {
                        b[off++]= (byte)result;
                        buffIdx++;
                    }
                    in.mark(100);
                    result= in.read();
                    if (result > -1) {
                        byte testChar= (byte)result;
                        if (testChar!='\n')  {
                            in.reset();
                        }
                        else if (keepNL) {
                            b[off++]= (byte)result;
                            buffIdx++;
                        }
                    }
                }
                else {
                    b[off++]= (byte)result;
                    buffIdx++;
                }
            }
        } // end for loop

        if (buffIdx==0 && result==-1) buffIdx= -1;

        return buffIdx;
    }

    public static void writeAttachmentBuff(OutputStream         out,
                                           byte                 outBuff[],
                                           int                  len,
                                           boolean              endOfSection)
                                  throws IOException {
        if(len>0) {
            if(endOfSection && outBuff.length>=2 &&
               (outBuff[len-2]=='\r' && outBuff[len-1]=='\n')) {
                len-=2;
            }
            out.write(outBuff, 0, len);
        }
    }



    private static File getUniqueName(File dir) {
        File f;
        try {
            f= File.createTempFile(_nameBase, _fitsNameExt, dir);
        } catch (IOException e) {
            f= null;
        }
        return f;
    }

}
/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED ?AS-IS? TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
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
