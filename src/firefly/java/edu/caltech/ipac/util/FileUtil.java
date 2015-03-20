/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
/*****************************************************************************
 * Copyright (C) 1999 California Institute of Technology. All rights reserved
 * US Government Sponsorship under NASA contract NAS7-918 is acknowledged
 ****************************************************************************/
package edu.caltech.ipac.util;

import javax.imageio.stream.ImageInputStream;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 *  A collection of utilities reltaed to File manipulations
 *  @author G. Turek
 *  @version $Id: FileUtil.java,v 1.61 2012/12/10 19:01:01 roby Exp $
 */
public class FileUtil 
{
    public final static String jpeg = "jpeg";
    public final static String jpg  = "jpg";
    public final static String png  = "png";
    public final static String bmp  = "bmp";
    public final static String gif  = "gif";
    public final static String tiff = "tiff";
    public final static String tif  = "tif";
    public final static String EXP  = "exp";
    public final static String SATF = "satf";
    public final static String SASF = "sasf";
    public final static String BSP = "bsp";
    public final static String FIT  = "fit";
    public final static String FITS = "fits";
    public final static String GZ   = "gz";
    public final static String PS   = "ps";
    public final static String PDF  = "pdf";
    public final static String HTML = "html";
    public final static String AOR  = "aor";
    public final static String TXT  = "txt";
    public final static String TGT  = "tgt";
    public final static String ZIP  = "zip";
    public final static String TBL  = "tbl";
    public final static String NL   = "nl";
    public final static String JAR   = "jar";
    public final static String CSH   = "csh";
    public final static String SH   = "sh";
    public final static String PL   = "pl";
    public final static String SO   = "so";
    public final static String REG  = "reg";

    public static final long MEG          = 1048576;
    public static final long GIG          = 1048576 * 1024;
    public static final long MEG_TENTH    = MEG / 10;
    public static final long GIG_HUNDREDTH= GIG / 100;
    public static final long K            = 1024;

    private static String[] CHARSETS_TO_BE_TESTED = {"ISO-8859-1","windows-1252", "windows-1253","UTF-8", "UTF-16",};

    public enum ConvertTo {MEG,GIG,K}

  private final static String  ADD_START= "---";

    /**
     * Get the extension of a filename.
     * @param  s a file name such as <code>a.dat</code>
     * @return String the extension of the file.
     *                A null is returned if there is no extension; 
     *                
     */
  public static String getExtension(String s) 
  {
    String ext = null;
    int i = s.lastIndexOf('.');
    if (i > 0 &&  i < s.length() - 1) 
    {
      ext = s.substring(i+1).toLowerCase();
    }
    return ext;
  }

    /**
     * Get the extension of a file.
     * @param  f a file such as <code>a.dat</code>
     * @return String the extension of the file.
     *                A null is returned if there is no extension
     *                
     */
  public static String getExtension(File f) 
  {
    return getExtension(f.getName());
  }

    public static boolean isExtension(File f, String ext) {
        return isExtension(f.getName(),ext);
    }

    public static boolean isExtension(String filename, String ext) {
        String testExt= getExtension(filename);
        return (testExt!=null && testExt.equalsIgnoreCase(ext));
    }

    public static boolean isGZExtension(File f) { return isExtension(f,FileUtil.GZ); }


    /**
     * Get the name of of a filename without the extension.
     * @param  s a file name such as <code>a.dat</code>
     * @return String the base name of the file. i.e. if "abc.dat" is
     *                passed to this method it will return "abc"
     *                A null is returned if there is no base;
     *                
     */
  public static String getBase(String s) {
    String base;
    int i = s.lastIndexOf('.');
    if (i==-1 || i==0) {
       base= s;
    }
    else {
       base = s.substring(0, i);
    }
    return base;
  }

    /**
     * Get the name of of a filename without the extension. 
     * @param  f a file name such as <code>a.dat</code>
     * @return String the base name of the file. i.e. if "abc.dat" is
     *                passed to this method it will return "abc"
     *                A null is returned if there is no base;
     *                
     */
  public static String getBase(File f) {
    return getBase(f.getName());
  }

    /**
     * Set the extension of a filename.
     * @param  extensionName the extension to be replaced or added.
     * @param  fileName a file name such as <code>xxxx.dat</code> or
                      <code>xxx</code>
     * @param  replace if true replace the filename extension (if it
                       has one) with the specified extension, otherwise
                       just add the extension to the filename.
     * @return String The file with the specified extension.
     */
    public static String setExtension (String  extensionName, 
                                       String  fileName,
                                       boolean replace){
        int dotPosition = fileName.lastIndexOf(".");
        String newFileName;
        //AR253: TLau 08/02/10 no extension to set if extensionName is null.
        if (extensionName == null) return fileName;

        if (dotPosition == -1) {
            newFileName = fileName + "." + extensionName;
        }
        else if (replace) {
            newFileName = fileName.substring(0,dotPosition+1) + extensionName;
        }
        else { // has extension && !replace
            newFileName = fileName + "." + extensionName;
        }

        return newFileName;
    }


    public static File setExtension (String  extensionName,
                                     File   fileName,
                                     boolean replace){
        return new File(setExtension(extensionName,fileName.getPath(),replace));
    }



    /**
     * Set the extension of a filename.  If the file already has
     * an extension the replace the extension with the specified
     * one.
     * @param  extensionName a file name such as <code>xxxx.dat</code> or
                      <code>xxx</code>
     * @param  fileName the extension to be replaced or added.
     * @return String The file with the specified extension.
     */
    public static String setExtension (String  extensionName, 
                                       String  fileName){
       return setExtension (extensionName,fileName,true);
    }

    /**
     * This method replaces a file extension or adds one if it
     * is not already there.
     * @param  f the input file
     * @param  extension the file extension that should be there
     * @return File the modified file with the correct extension
     */
    public static File modifyFile (File f, String extension) {
        String path= f.getPath();
        String currExt= getExtension(path);
        File   retval;
        if ( currExt != null && currExt.equals(extension))  {
                                  //if extesion is correct
            retval= f;
        }
        else {
            retval= new File(setExtension(extension, path, false));
        }
        return retval;
    }


    public static File createUniqueFileFromFile(File f) {
        return createUniqueFileFromFile(f,false);
    }


    public static File createUniqueFileFromFile(File    f, 
                                                boolean alreadyModified) {
       File    dir    = f.getParentFile();
       String  base   = getBase(f);
       String  ext    = getExtension(f);
       int     index  = 1;
       boolean done   = false;
       File    outFile= null;

       if (alreadyModified) { // remove the modification and set the index
           String str= ADD_START + "[0-9]*";
           Pattern p= Pattern.compile(str);
           Matcher m= p.matcher(base);
           if (m.find()) {
              int i= m.start();
              String idxStr= base.substring(i+ADD_START.length());
              base= base.substring(0,i);
              try {
                  index= Integer.parseInt(idxStr) + 1;
              } catch (NumberFormatException e) {
                  index=1;
              }
           }
       }

       String newFileStr;
       while(index<20000 && !done) {
          if (ext==null) {
              newFileStr= base+ADD_START+index;
          }
          else {
              newFileStr= setExtension(ext,base+ADD_START+index,false);
          }
          outFile= new File(dir, newFileStr);
          done= !outFile.exists();
          index++;
       }
       if (!done) outFile= null;
       return outFile;
    }

    public static void copyFile(File fromFile, File toFile) throws IOException {
        DataInputStream in=null;
        DataOutputStream out=null;
        try {
            in=new DataInputStream(new BufferedInputStream(
                                new FileInputStream(fromFile), 4096));
            out=new DataOutputStream(
                  new BufferedOutputStream( new FileOutputStream(toFile),4096));
            while(true) {
                out.writeByte(in.readByte());
            }
        } catch (EOFException e) {
            // do nothing we are done
        } finally {
            silentClose(in);
            silentClose(out);
        }
    }

    public static String readFile(File file) throws IOException {
        DataInputStream in=null;
        DataOutputStream out=null;
        if (!file.canRead()) {
            throw new IOException("Cannot read file");
        }
        ByteArrayOutputStream byteAry= new ByteArrayOutputStream( (int)file.length()+200);

        int buffsize = (int)Math.min(file.length() + 400, 20000);

        try {
            in=new DataInputStream(new BufferedInputStream( new FileInputStream(file), buffsize));
            out=new DataOutputStream(byteAry);
            while(true) {
                out.writeByte(in.readByte());
            }

        } catch (EOFException e) {
            // do nothing we are done
        } finally {
            silentClose(in);
            silentClose(out);
        }
        return byteAry.toString();
    }



    public static boolean writeStringToFile(File f, String s) {
        BufferedWriter out=null;
        boolean success= false;
        try {
            out= new BufferedWriter(new FileWriter(f), s.getBytes().length+20);
            out.write(s);
            success= true;
        } catch (IOException e) {
            // do nothing we are done
        } finally {
            silentClose(out);
        }
        return success;
    }

    public static File computeUnzipFileName(File f) throws IOException {
        File toFile= f;
        String ext= FileUtil.getExtension(f);
        if (ext!=null && ext.equalsIgnoreCase(FileUtil.GZ)) {
            toFile= new File(FileUtil.getBase(f.getPath()));
        }
        return toFile;
    }


    public static File gUnzipFile(File f) throws IOException {
        return gUnzipFile(f,10000);
    }

    public static File gUnzipFile(File f, int bufferSize) throws IOException {
        String ext= FileUtil.getExtension(f);
        File toFile= f;
        if (ext!=null && ext.equalsIgnoreCase(FileUtil.GZ)) {
            toFile= new File(FileUtil.getBase(f.getPath()));
            gUnzipFile(f,toFile,bufferSize);
        }
        else {
           throw new IOException(f.getName() +" does not end with "+ FileUtil.GZ);
        }
        return toFile;
    }


    public static File gUnzipFile(File f, File toFile, int bufferSize) throws IOException {
        String ext= FileUtil.getExtension(f);
        File retval= f;
        if (ext!=null && ext.equalsIgnoreCase(FileUtil.GZ)) {
            GZIPInputStream in= null;
            DataOutputStream out=null;
            try {
                in= new GZIPInputStream(new FileInputStream(f),bufferSize);
                out=new DataOutputStream(
                        new BufferedOutputStream( new FileOutputStream(toFile),bufferSize));
                byte buffer[]= new byte[bufferSize];
                for(int size= in.read(buffer); (size>-1); size= in.read(buffer)) {
                    out.write(buffer,0,size);
                }
                retval= toFile;


            } finally {
                FileUtil.silentClose(in);
                FileUtil.silentClose(out);
            }
        }
        else {
            throw new IOException(f.getName() +" does not end with "+ FileUtil.GZ);
        }
        return retval;
    }



    public static boolean isGZipFile(File f) {
        boolean retval;
        DataInputStream in=null;
        try {
            in=new DataInputStream( new FileInputStream(f));
            int b0 = in.read();
            int b1 = in.read();
            if (b0 == -1 || b1==-1) throw new EOFException();
            int value=  (b1 << 8) | b0;
            retval= (value == GZIPInputStream.GZIP_MAGIC);
        } catch (IOException e) {
            retval= false;
        } finally {
            FileUtil.silentClose(in);
        }
        return retval;
    }

    /**
     * Write a file to an output stream
     * @param f input file
     * @param oStream stream to copy to
     * @throws IOException if any problem occures
     */
    public static void writeFileToStream(File f, OutputStream oStream) throws IOException {
        DataInputStream in= new DataInputStream(new BufferedInputStream(new FileInputStream(f),(int)MEG));
        try {
            while(true) {
                byte ch = in.readByte();
                oStream.write(ch);
            }
        } catch (EOFException e) {
            // do nothing
        } finally {
            FileUtil.silentClose(in);
        }
    }

    /**
     * Write a file to an output stream
     * @param s String to output
     * @param oStream stream to copy to
     * @throws IOException if any problem occures
     */
    public static void writeStringToStream(String s, OutputStream oStream) throws IOException {
        DataInputStream in= new DataInputStream(new ByteArrayInputStream(s.getBytes()));
        try {
            while(true) {
                byte ch = in.readByte();
                oStream.write(ch);
            }
        } catch (EOFException e) {
            // do nothing
        } finally {
            FileUtil.silentClose(in);
        }
    }



    /**
     * TOTALLY untested, only deals with files and directories
     * TODO: test
     * TODO: what about links?
     * @param fromDir directory to copy from
     * @param toDir directory to copy to
     * @throws IOException if the copy of any file failes
     */
    public static void recusiveCopyDir(File fromDir, File toDir) throws IOException {
        Assert.argTst(fromDir.isDirectory(), "the from file is not a directory");
        Assert.argTst((!toDir.exists() || toDir.exists() && toDir.isDirectory()),
                      "toDir is not a directory, it must either be a " +
                      "directory or not exist on the disk");
        if ( !(toDir.exists() && toDir.isDirectory()) ) {
            boolean success= toDir.mkdirs();
            if (!success) {
                throw new IOException("could not create directory: "+ toDir.getPath());
            }
        }

        File fileAry[]= listFiles(fromDir);
        for(File f : fileAry) {
            if (f.isDirectory()) {
                recusiveCopyDir(f,new File(toDir, f.getName()));
            }
            else {
                copyFile(f,new File(toDir, f.getName()));
            }
        }
    }



    /**
     * Replaces each file name of this array of files that matches the given regular expression with the given replacement.
     * Returns an array of the new files.
     * @param files  files array to replace name
     * @param regex  a regular expression
     * @param replacement  replacement when matches
     * @return an array of files
     */
    public static File[] replaceAllFiles(File[] files, String regex, String replacement) {
        if (files == null) return null;
        File[] retfiles = new File[files.length];
        for (int i = 0; i < files.length; i++) {
            retfiles[i] = files[i] == null ? null :
                          new File(files[i].toString().replaceAll(regex, replacement));
        }
        return retfiles;
    }

    public static File addPrefixToBase(File f, String addition) {
        File dir= f.getParentFile();
        return addPrefixToBase(f,addition,dir);
    }
    public static File addPrefixToBase(File f, String addition, File newDir) {
        Assert.argTst(newDir.isDirectory(),
                      "parameter newDir must be a directory");
        String name= f.getName();
        String base= getBase(name);
        String ext= getExtension(name);
        return new File(newDir,addition+base+"."+ext);
    }

    public static File addStringToBase(File f, String addition) {
        File dir= f.getParentFile();
        return addStringToBase(f,addition,dir);
    }
    public static File addStringToBase(File f, String addition, File newDir) {
        Assert.argTst(newDir.isDirectory(),
                      "parameter newDir must be a directory");
        String name= f.getName();
        String base= getBase(name);
        String ext= getExtension(name);
        return new File(newDir,base+addition+"."+ext);
    }

    public static File[] addStringToBase(File[] files, String addition, File newDir) {
        if (files == null) return null;

        File[] retfiles = new File[files.length];
        for(int i = 0; i < files.length; i++) {
            retfiles[i] = newDir == null ? addStringToBase(files[i], addition) :
                            addStringToBase(files[i], addition, newDir);
        }
        return retfiles;
    }

    public static File[] listFiles(final File dir) {
        return listFilesWithExtension(dir,null);
    }

    public static File[] listJarFiles(File dir) {
        return listFilesWithExtension(dir,JAR);
    }

    public static File[] listFilesWithExtension(final File dir,
                                                final String ext) {
        return dir.listFiles( new FilenameFilter() {
            public boolean accept( File dir, String name ) {
                return (ext!=null) ? name.endsWith( "."+ ext) : true;
            }
        } );
    }

    public static File[] listDirectories(final File dir) {
        return dir.listFiles( new FilenameFilter() {
            public boolean accept( File dir, String name ) {
                return new File(dir,name).isDirectory();
            }
        } );
    }


    public static boolean silentClose(Reader reader) {
        boolean success= true;
        if (reader!=null) {
            try { reader.close();
            } catch (IOException ignore) {
               success= false;
            }
        }
        return success;
    }


    public static boolean silentClose(Writer writer) {
        boolean success= true;
        if (writer!=null) {
            try { writer.close();
            } catch (IOException ignore) {
               success= false;
            }
        }
        return success;
    }

    public static boolean silentClose(InputStream stream) {
        boolean success= true;
        if (stream!=null) {
            try { stream.close();
            } catch (IOException ignore) {
               success= false;
            }
        }
        return success;
    }

    public static boolean silentClose(ImageInputStream stream) {
        boolean success= true;
        if (stream!=null) {
            try { stream.close();
            } catch (IOException ignore) {
                success= false;
            }
        }
        return success;
    }

    public static boolean silentClose(Socket stream) {
        boolean success= true;
        if (stream!=null) {
            try { stream.close();
            } catch (IOException ignore) {
               success= false;
            }
        }
        return success;
    }



    public static boolean silentClose(OutputStream stream) {
        boolean success= true;
        if (stream!=null) {
            try { stream.close();
            } catch (IOException ignore) {
               success= false;
            }
        }
        return success;
    }
    public static boolean silentClose(JarFile jar) {
        boolean success= true;
        if (jar!=null) {
            try { jar.close();
            } catch (IOException ignore) {
               success= false;
            }
        }
        return success;
    }


    public static boolean silentClose(RandomAccessFile raf) {
        boolean success= true;
        if (raf!=null) {
            try { raf.close();
            } catch (IOException ignore) {
                // ignore results
            }
        }
        return success;
    }



    public static void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            for(File f : dir.listFiles()) {
                if (f.isDirectory())  deleteDirectory(f);
                else  f.delete();
            }
            dir.delete();
        }
    }



    public static String getSizeAsString(long size) {
        return getSizeAsString(size,false);
    }

    public static String getSizeAsString(long size, boolean verbose) {
        String retval= "0";
        String kStr= "K";
        String mStr= "M";
        String gStr= "G";
        if (verbose) {
            kStr= " K";
            mStr= " Megs";
            gStr= " Gigs";
        }

        if (size > 0 && size < (1*MEG)) {
            retval= ((size / K) + 1) + kStr;
        }
        else if (size >= (1*MEG) && size <  (2*GIG) ) {
            long megs = size / MEG;
            long remain= size % MEG;
            long decimal = remain / MEG_TENTH;
            retval= megs +"."+ decimal + mStr;
        }
        else if (size >= (2*GIG) ) {
            long gigs = size / GIG;
            long remain= size % GIG;
            long decimal = remain / GIG_HUNDREDTH;
            retval= gigs +"."+ decimal + gStr;
        }
        return retval;
    }


    /**
     * return filename without the path.
     *
     * @param file is a file such as <code>/home/data/a.dat</code>
     * @return File the filename only. i.e. if "/home/data/abc.dat" is
     *         passed to this method it will return "abc.dat"
     */
    public static String getFilename(File file){
        String ext = getExtension(file);
        String base = getBase(file);
        return setExtension(ext, base);
    }

    public static String absolutePathForWindows(String filename) {
        if (OSInfo.isPlatform(OSInfo.ANY_WINDOWS))
        {
            String backSlash = "" + (File.separatorChar);
            String doubleBackSlash = backSlash + backSlash;
            String pathSet[] = filename.split(doubleBackSlash);
            String newPath = pathSet[0];

            for (int i=1; i<pathSet.length; i++) {
                newPath += doubleBackSlash + pathSet[i];
            }
            return newPath;
        } else
            return filename;
    }

    public static String getHostname() {
        String retHost= "UNKNOWN_HOST";
        try {
            String host= InetAddress.getLocalHost().getCanonicalHostName();
            if (host!=null) {
                int idx= host.indexOf(".");
                if (idx>-1) {
                    retHost= host.substring(0,idx);
                    try {
                        int v= Integer.parseInt(retHost);
                        retHost= host;
                    } catch (NumberFormatException e) {
                        // do nothing, we found a host name
                    }

                }
                else {
                    retHost= host;
                }
            }
        } catch (UnknownHostException e) {
            retHost= "UNKNOWN_HOST";
        }
        return retHost;
    }

    //============================================================================
    //                       File Directory Utilities
    //============================================================================


    public static List<URL> getJarsWithManifestEntry(String entry) {
        String urlStr;
        List<URL> matchingJars = new ArrayList<URL>();
        try {
            urlStr = URLDecoder.decode(getThisClassURL().toString(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return matchingJars;
        }
        int start= urlStr.indexOf('/');
        int end= urlStr.lastIndexOf('!');
        String fileStr;
        if (start < end) {
            fileStr= urlStr.substring(start, end);
            File jarFile= new File(fileStr);
            File jarsDir= jarFile.getParentFile();
            File jarFiles[]= FileUtil.listJarFiles(jarsDir);
            JarFile jf;
            for(File jarF : jarFiles) {
                try {
                    jf = new JarFile(jarF);
                    Attributes att= jf.getManifest().getAttributes(entry);
                    if (att!=null) {
                        matchingJars.add(jarF.toURI().toURL());
                    }
                } catch (IOException e) {
                    System.out.println("Could not open: "+jarF.getPath());
                }
            }

        }
        else {
            System.out.println("FileUtil: could not find the list of jar files.");
            System.out.println("           "+ getThisClassFileName() +
                               " is not in a jar file.");
        }
        return matchingJars;
    }

    /**
     * Determine the file is binary or not
     * @param file File to check
     * @return true if the file is binary
     */
    public static boolean isBinary(File file) throws IOException{
        final int BUFFER_SIZE = 4;
        byte[] buffer = new byte[BUFFER_SIZE];
        DataInputStream in = new DataInputStream (new FileInputStream(file.getAbsolutePath()));
        int n = in.read(buffer, 0, BUFFER_SIZE);
        char c;
        boolean isBinary = true;

        if (getExtension(file).matches("(bmp|dmg|exe|fits|gif|gz|gzip|jpg|jpeg|pdf|png|tar|tgz|tif|tiff|zip)"))
            return isBinary;
        else
            isBinary = false;

        if (!getExtension(file).matches("(txt|tbl)")) {
            for (byte b: buffer) {
                c = (char)b;
                if ( !Character.isWhitespace(c) && (c < 32 || c > 127) ) {
                    isBinary = true;
                    break;
                }
            }
        }

        return isBinary;
    }

    public static String guessCharset(File file) throws Exception {
        return detectCharset(file, CHARSETS_TO_BE_TESTED).displayName();
    }

    public static Charset detectCharset(File f, String[] charsets) throws Exception {
        Charset charset = null;
        BufferedInputStream input = new BufferedInputStream(new FileInputStream(f));
        byte[] buffer = new byte[5120];
        input.read(buffer);
        for (String charsetName : charsets) {
            charset = detectCharset(buffer, Charset.forName(charsetName));
            if (charset != null) {
                break;
            }
        }
        input.close();
        return charset;
    }

    private static Charset detectCharset(byte[] buffer, Charset charset) {
        try {
            CharsetDecoder decoder = charset.newDecoder();
            decoder.reset();

            boolean identified = identify(buffer, decoder);

            if (identified) {
                return charset;
            } else {
                return null;
            }

        } catch (Exception e) {
            return null;
        }
    }

    private static boolean identify(byte[] bytes, CharsetDecoder decoder) {
        try {
            decoder.decode(ByteBuffer.wrap(bytes));
        } catch (CharacterCodingException e) {
            return false;
        }
        return true;
    }

    /**
     * Guess the file's character set and create BufferedReader
     * @param file input text file
     * @return BufferedReader using guessed character set
     * @throws IOException any IOException during guessing character set or creating FileInputStream
     */
    public static BufferedReader createBufferedReaderWithGuessedCharset(File file) throws IOException {
        return createBufferedReaderWithGuessedCharset(file, "UTF-8") ;
    }

    /**
     * Guess the file's character set and create BufferedReader
     * @param file input text file
     * @param defaultCharset Default character set: US-ASCII, ISO-8859-1, UTF-8, UTF-16BE, UTF-16LE, UTF-16
     * @return BufferedReader using guessed character set
     * @throws IOException any IOException during guessing character set or creating FileInputStream
     */
    public static BufferedReader createBufferedReaderWithGuessedCharset(File file, String defaultCharset)
            throws IOException {
        String charset= null;
        try {
            charset= guessCharset(file);
        } catch (Exception e) {
            IOException ioe= new IOException ();
            ioe.initCause(e);
        }
        InputStreamReader isr = new InputStreamReader(new FileInputStream(file), charset);
        return new BufferedReader(isr);
    }

    private static URL getThisClassURL() {
        return FileUtil.class.getClassLoader().getResource(getThisClassFileName());
    }

    private static String getThisClassFileName() {
        String cName= FileUtil.class.getName();
        return cName.replace(".", "/") + ".class";
    }
}

