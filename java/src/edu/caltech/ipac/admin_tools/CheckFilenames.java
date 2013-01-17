package edu.caltech.ipac.admin_tools;

import java.lang.reflect.*;
import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.BufferedReader;

import nom.tam.fits.*;
import nom.tam.util.*;

public class CheckFilenames
{

static int files_checked = 0;
static int fits_files_checked = 0;
static int tbl_files_checked = 0;

static public void main(String args[])
{
    if (args.length != 0)
    {
	System.out.println(
	    "usage: java CheckFilenames");
	System.exit(1);
    }

    CheckFilenames fits_check_filename = new CheckFilenames();

    fits_check_filename.process_dir(".");
    System.out.println("\n" + files_checked + " total files checked");
    System.out.println("  " + fits_files_checked + " fits files checked");
    System.out.println("  " + tbl_files_checked + " tblfiles checked");



    }
    void process_dir(String this_dirname)
    {
	//System.out.println("Entering process_dir with this_dirname = " + this_dirname);
	File this_dir = new File(this_dirname);
	String dir_list[] = this_dir.list();
	//System.out.println("dir_list.length = " + dir_list.length);
	for (int i = 0; i < dir_list.length; i++)
	{
	    File file_item = new File(this_dirname + "/" + dir_list[i]);
	    if (file_item.isDirectory())
	    {
		//System.out.println("a dir = " + dir_list[i]);
		String next_dirname = this_dirname.concat("/");
		next_dirname = next_dirname.concat(dir_list[i]);
		this.process_dir(next_dirname);
	    }
	    else if (file_item.isFile())
	    {
		//System.out.println("a file = " + dir_list[i]);
		if (dir_list[i].endsWith("fits"))
		{
		    check_fits(this_dirname, dir_list[i]);
		    fits_files_checked++;
		    files_checked++;
		}
		else if (dir_list[i].endsWith("tbl"))
		{
		    check_tbl(this_dirname, dir_list[i]);
		    tbl_files_checked++;
		    files_checked++;
		}
		if ((files_checked % 1000) == 0)
		{
		    System.out.println(files_checked + " files checked so far");
		}
	    }
	    else
	    {
		System.out.println("not a file or directory = " + dir_list[i]);
	    }
	}
	//System.out.println("Leaving process_dir");
    }

    void check_fits(String input_dirname, String input_filename)
    {
	String full_filename = input_dirname.concat("/");
	full_filename = full_filename.concat(input_filename);
        Fits fits = null;
        Header header = null;

	try
	{
	    fits = new Fits(full_filename);   //open the file
	    BasicHDU[] myHDUs = fits.read();     // get all of the header-data units
						   // usually just one primary HDU
	    header = myHDUs[0].getHeader();  // get the header

	    String filename = header.getStringValue("FILENAME");
	    String dirname = header.getStringValue("DIRNAME");
	    if (filename == null)
	    {
		System.out.println("\nBAD: no FILENAME in the FITS header");
		System.out.println("  File: " + 
		    input_dirname.substring(2) + "/" + input_filename);
	    }
	    else if (dirname == null)
	    {
		System.out.println("\nBAD: no DIRNAME in the FITS header");
		System.out.println("  File: " + 
		    input_dirname.substring(2) + "/" + input_filename);
	    }
	    else if ((filename.equals(input_filename)) && 
		(dirname.equals(input_dirname.substring(2))))
	    {
		//System.out.println("\nOK");
	    }
	    else
	    {
		System.out.println(
		"\nWRONG FILENAME - header says\n    FILENAME = " + filename +
		"\n    DIRNAME = " + dirname);
		System.out.println("  File is actually " + 
		    input_dirname.substring(2) + "/" + input_filename);
	    }


	}
	catch (FitsException e)
	{
	    e.printStackTrace();
	}

    }
    void check_tbl(String input_dirname, String input_filename)
    {
	try
	{
	    String filename = input_dirname.concat("/");
	    filename = filename.concat(input_filename);
	    //System.out.println("filename = " + filename);
	    FileReader fr = new FileReader(filename);
	    BufferedReader in = new BufferedReader(fr);
	    String str;
	    String filename_in_header = null;
	    String dirname_in_header = null;

	    while ((str = in.readLine()) != null)
	    {
		//System.out.println("str = " + str);
		int filename_index = str.indexOf("FILENAME");
		if (filename_index >= 0)
		{
		    filename_index = str.indexOf("'") + 1;
		    int end_filename_index = str.indexOf("'", filename_index);
		    filename_in_header = str.substring(filename_index, end_filename_index);
		    //System.out.println("filename_in_header = " + filename_in_header);
		}
		int dirname_index = str.indexOf("DIRNAME");
		if (dirname_index >= 0)
		{
		    dirname_index = str.indexOf("'") + 1;
		    int end_dirname_index = str.indexOf("'", dirname_index);
		    dirname_in_header = str.substring(dirname_index, end_dirname_index);
		    //System.out.println("dirname_in_header = " + dirname_in_header);
		}
		if ((filename_in_header != null) && (dirname_in_header != null))
		{
		    break;
		}
	    }
	    in.close();
	    if (filename_in_header == null)
	    {
		System.out.println("\nBAD: no FILENAME in the tbl header");
		System.out.println("  File: " + filename);
	    }
	    else if (dirname_in_header == null)
	    {
		System.out.println("\nBAD: no DIRNAME in the tbl header");
		System.out.println("  File: " + filename);
	    }
	    else
	    {
		String pathname_in_header = "./" + dirname_in_header + "/" + 
		    filename_in_header;
		if (!pathname_in_header.equals(filename))
		{
		    System.out.println(
		    "\nWRONG FILENAME - header says\n    FILENAME = " + 
		    filename_in_header +
		    "\n    DIRNAME = " + dirname_in_header);
		    System.out.println("  File is actually " + filename);
		}
	    }
	}
	catch (IOException ioe)
	{
	    System.err.println("Wierd - can't read file: " + ioe);
	}
    }
}


