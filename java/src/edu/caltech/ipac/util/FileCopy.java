package edu.caltech.ipac.util;

import java.io.*;


/**
 * File copy utilities
 * $Id: FileCopy.java,v 1.3 2005/12/08 22:31:13 tatianag Exp $
 */
public class FileCopy {

    private static boolean debug = Boolean.getBoolean("FileCopy_SHOW_DEBUG");


    /**
     * Copy given source file to given destination.
     * Exception is thrown if copy fails or destination file already exists.
     * @param sourceName string name of the source file
     * @param destName string name of the destination
     * @return java.io.File object for destination
     * @exception edu.caltech.ipac.util.FileCopyException if copy fails for some reason
     */
    public static File copy(String sourceName, String destName)
        throws IOException {
        return copy(sourceName, destName, false, null);
    }


    /**
     * Copy given source file to given destination.
     * If overwrite is true and destination file already exists, it will be overwritten.
     * @param sourceName string name of the source file
     * @param destName string name of the destination
     * @param overwrite true if it's OK to overwrite destination file
     * @return java.io.File object for destination
     * @exception edu.caltech.ipac.util.FileCopyException if copy fails for some reason
     */
    public static File copy(String sourceName, String destName, boolean overwrite)
        throws IOException {
        return copy(sourceName, destName, overwrite, null);
    }


    /**
     * Copy given source file to given destination.
     * If suffix is not null, the source is copied to a temporary file with
     * the given suffix first. After copy is completed, the file is renamed
     * to destName This ensures that destName refers only to completed files.
     * @param sourceName string name of the source file
     * @param destName string name of the destination
     * @param overwrite true if it's OK to overwrite destination file
     * @param suffix string suffix to be used when writing destination file
     * @return java.io.File object for destination
     * @exception edu.caltech.ipac.util.FileCopyException if copy fails for some reason
     */
    public static File copy(String sourceName, String destName, boolean overwrite, String suffix)
        throws IOException {

        File sourceFile = new File(sourceName);
        File destinationFile = new File((suffix==null)?destName:(destName+suffix));
        FileInputStream source = null;
        FileOutputStream destination = null;
        byte[] buffer;
        int bytesRead;

        if (debug) {
            System.out.println("Source: "+sourceFile.getAbsolutePath());
            System.out.println("Destination: "+destinationFile.getAbsolutePath());
        }

        try {
            // First make sure the specified source file
            // exists, is a file, and is readable.
            if (!sourceFile.exists() || !sourceFile.isFile())
                throw new FileCopyException("FileCopy: no such source file: " + sourceName);
            if (!sourceFile.canRead())
                throw new FileCopyException("FileCopy: source file " +
                                            "is unreadable: " + sourceName);
            verifyDestinationWritable(destinationFile,overwrite);

            // If we've gotten this far, then everything is okay; we can copy the file.
            source = new FileInputStream(sourceFile);
            destination = new FileOutputStream(destinationFile);
            buffer = new byte[1024];
            while(true) {
                bytesRead = source.read(buffer);
                if (bytesRead == -1) break;
                destination.write(buffer, 0, bytesRead);
            }
            destination.flush();
            // force all system buffers to syncronize with the underlying device
            destination.getFD().sync();
        }
        // No matter what happens, always close any streams we've opened.
        finally {
            if (source != null)
                try { source.close(); } catch (IOException e) { ; }
            if (destination != null)
                try { destination.close(); } catch (IOException e) { ; }
        }

        // If suffix for interidiate (partial) file was specified, rename file
        if (suffix != null) {
            File trueDestinationFile = new File(destName);
            if (debug) System.out.println("True Destination: "+trueDestinationFile.getAbsolutePath());
            try {
                verifyDestinationWritable(trueDestinationFile, overwrite);
                destinationFile.renameTo(trueDestinationFile);
                destinationFile = trueDestinationFile;

            } catch (FileCopyException fce) {
                destinationFile.delete();
                throw fce;
            }
        }
        return destinationFile;
    }

    /**
     * If the destination exists and overwrite is false, return false.
     * If the destination exists and overwrite is true, make sure it is a writable file.
     * If the destination doesn't exist, make sure the directory exists and is writable.
     */
    private static void verifyDestinationWritable(File destinationFile, boolean overwrite)
        throws FileCopyException {

        if (destinationFile.exists()) {
            if (destinationFile.isFile()) {
                if (!overwrite) {
                    throw new FileCopyException("FileCopy: can not overwrite "+
                                                destinationFile.getAbsolutePath());
                }

                if (!destinationFile.canWrite())
                    throw new FileCopyException("FileCopy: destination " +
                                                "file is unwritable: " +
                                                destinationFile.getAbsolutePath());
            }
            else
                throw new FileCopyException("FileCopy: destination "
                                            + "is not a file: " +
                                            destinationFile.getAbsolutePath());

        }
        else {
            File parentdir = parent(destinationFile);
            if (!parentdir.exists())
                throw new FileCopyException("FileCopy: destination "
                                            + "directory doesn't exist: " +
                                            destinationFile.getAbsolutePath());
            if (!parentdir.canWrite())
                throw new FileCopyException("FileCopy: destination "
                                            + "directory is unwritable: " +
                                            destinationFile.getAbsolutePath());
        }
    }

    // File.getParent() can return null when the file is specified without
    // a directory or is in the root directory.
    // This method handles those cases.
    private static File parent(File f) {
        String dirname = f.getParent();
        if (dirname == null) {
            if (f.isAbsolute()) return new File(File.separator);
            else return new File(System.getProperty("user.dir"));
        }
        return new File(dirname);
    }

    // java -DFileCopy_SHOW_DEBUG=true edu.caltech.ipac.util.FileCopy sourceName destName .locked
    public static void main(String[] args) {
        if (args.length != 3)
            System.err.println("Usage: java FileCopy " +
                               "<source file> <destination file> <partial file suffix>");
        else {
            try {
                File destFile = copy(args[0], args[1], false, args[2]);
                System.out.println("Copy completed. Written "+destFile.getAbsolutePath());
            }
            catch (Exception e) {
                System.err.println("ERROR: "+e.getMessage()); }
        }
    }
}

class FileCopyException extends IOException {
    public FileCopyException(String msg) { super(msg); }
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
