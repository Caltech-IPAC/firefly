package edu.caltech.ipac.util;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;




public class JarExpander {

   public static void expand(File inJarFile) throws IOException {
       expand(inJarFile, null, false);
   }


   public static void expand(File    inJarFile,
                             File    targetDir,
                             boolean expandMetaDir)
                                  throws IOException {
       expand(inJarFile, targetDir, expandMetaDir, false,null);
   }

    public static void expand(File    inJarFile,
                              File    targetDir,
                              boolean expandMetaDir,
                              boolean onUnixChmodExe)  throws IOException {
        expand(inJarFile, targetDir, expandMetaDir, onUnixChmodExe,null);
    }

   public static void expand(File    inJarFile, 
                             File    targetDir, 
                             boolean expandMetaDir,
                             boolean onUnixChmodExe,
                             ExpandStatus status)  throws IOException {
       ZipEntry        ze;
       InputStream     is;
       DataInputStream in;
       OutputStream    out;
       File            dir;
       File            outFile;
       byte            ch;
       if (targetDir != null && !targetDir.exists()) {
           throw new IOException("target directory does not exist");
       }
       if (targetDir != null && !targetDir.canWrite()) {
           throw new IOException("target directory is not writeable");
       }
       JarFile     jf     = new JarFile(inJarFile);
       Enumeration entries= jf.entries();
       String name;
       int idx= 0;
       while (entries.hasMoreElements()) {
           ze= (ZipEntry)entries.nextElement();
           name= ze.getName();
           if (!expandMetaDir && name.startsWith("META-INF")) {
                continue;
           }
           if (ze.isDirectory()) {
              System.out.println("dir: "+name);
              dir= new File( targetDir, name);
              dir.mkdirs();
           }
           else {
              System.out.println("file: "+name);
              outFile= new File( targetDir, name);
              dir= outFile.getParentFile();
              dir.mkdirs();
              is= jf.getInputStream(ze);
              in   = new DataInputStream(new BufferedInputStream(is));
              out  = new BufferedOutputStream(
                                 new FileOutputStream(outFile), 4096);
              try {
                  while(true) {
                      ch = in.readByte();
                      out.write(ch);
                  }
               } catch (EOFException e) {
                  out.close();
               }
               if (onUnixChmodExe) chmodIfNecessary(outFile);
               if (status!=null) status.completedFile(outFile,++idx);
           }
       }
       try {
          jf.close();
       } catch (EOFException e) {
          // do nothing
       }
   }



    private static void chmodIfNecessary(File f) {
        if (OSInfo.isPlatform(OSInfo.ANY_UNIX_OR_MAC)) {
            chmodIfNecessaryOnUnix(f);
        }
    }

    private static void chmodIfNecessaryOnUnix(File f) {
        boolean doChmod= false;
        String name= f.getName();
        String ext= FileUtil.getExtension(f);
        if (ext==null) ext= "";
        if (name.indexOf(".so")> -1 ||
            name.indexOf(".dylib")> -1 ||
            ext.equals(FileUtil.PL) ||
            ext.equals(FileUtil.CSH) ||
            ext.equals(FileUtil.SH) ) {
            doChmod= true;
        }

        try {
            if (!doChmod) {
                String fileCmd= "file " + f.getPath();
                Process p= Runtime.getRuntime().exec( fileCmd );
                InputStream processOutput= p.getInputStream();
                DataInputStream in = new DataInputStream(
                                              new BufferedInputStream(processOutput));
                StringBuffer sb= new StringBuffer(200);


                BufferedReader reader= new BufferedReader(
                                   new InputStreamReader(in), 100);
                try {
                    for(String line= reader.readLine(); (line!=null);
                        line= reader.readLine()) {
                        sb.append(line);
                    }
                } catch (IOException e) {
                    // ignore - if I fail, I just will end up with empty results
                } finally {
                    FileUtil.silentClose(reader);
                    p.destroy();
                }
                String results= sb.toString().toLowerCase();
                //System.out.printf("results= %s%n", results);
                if (results.indexOf("executable")>-1 ||
                    results.indexOf("shared")>-1    ||
                    results.indexOf("commands")>-1    ||
                    results.indexOf("dynamic")>-1    ||
                    results.indexOf("script")>-1) {
                    doChmod= true;
                }

                // the following is a cluge to find perl files on linux
                if (!doChmod && 
                    OSInfo.isPlatform(OSInfo.LINUX) &&
                    f.getParentFile().getName().equalsIgnoreCase("bin") &&
                    results.indexOf("text")>-1 )  {
                    doChmod= true;
                }

            }
            if (doChmod) {
                String chmodCmd= "chmod +x " + f.getPath();
                Runtime.getRuntime().exec( chmodCmd );
                System.out.println("     File : "+
                                   f.getName() + " : chmod +x");
            }
        } catch (IOException e) {
            System.out.println("JarExpander.chmodIfNecessary: " +
                               e.toString());
        }
    }

    
    public static interface ExpandStatus   {
        public void completedFile(File f, int idx);
    }

  public static void main( String[] args ) {
     try {
        if (args.length == 1) {
           expand(new File(args[0]));
        }
        else {
           expand(new File(args[0]), new File(args[1]), false );
        }
     } catch (IOException e) {
           System.out.println("JarExpander: " + e);
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
