package edu.caltech.ipac.admin_tools;

import java.lang.reflect.*;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.StringTokenizer;
import java.util.ArrayList;
import java.net.URL;

import nom.tam.fits.*;
import nom.tam.util.*;

public class CreateFiles
{

static public void main(String args[])
{
    if (args.length != 1)
    {
	System.err.println(
	    "usage: java CreateFiles <file_list>");
	System.exit(1);
    }
    String input_filename = args[0];
    String output_filename = null ;
    String str;
    String size_string;
    String full_filename;
    int filesize;
    long total_filesize = 0;
    int longest_filename = 0;
    int longest_dirname = 0;




    /* get template tbl file */
    URL url = ClassLoader.getSystemResource(
	"edu/caltech/ipac/admin_tools/resources/heritage.tbl");
    ArrayList<String> tbl_template_contents = new ArrayList<String>();
    try
    {
	InputStream tbl_template_stream = url.openStream();
	InputStreamReader isr = new InputStreamReader(tbl_template_stream);
	BufferedReader in = new BufferedReader(isr);

	while ((str = in.readLine()) != null)
	{
	    tbl_template_contents.add(str);
	}
	in.close();
    }
    catch (IOException ioe)
    {
	System.err.println("ABORT: " + ioe);
	System.exit(1);
    }




    /* get template fits file */
    Fits template_fits = null;
    BasicHDU[] myHDUs = null;
    Header header = null;
    int[][] data32 = null;
    url = ClassLoader.getSystemResource(
	"edu/caltech/ipac/admin_tools/resources/heritage.fits");
    try
    {
	InputStream fits_template_stream = url.openStream();
	template_fits = new Fits(fits_template_stream);   //open the file
	myHDUs = template_fits.read(); // get all of the header-data units
	header = myHDUs[0].getHeader();  // get the header
	data32 = (int[][]) myHDUs[0].getData().getData();  // get the data
    }
    catch (IOException ioe)
    {
	System.err.println("ABORT: " + ioe);
	System.exit(1);
    }
    catch (FitsException fe)
    {
	System.err.println("ABORT: " + fe);
	System.exit(1);
    }




    CreateFiles loi = new CreateFiles();

    File infile = new File(input_filename);
    File outfile = null;

    try
    {
    FileReader fr = new FileReader(infile);
    BufferedReader in = new BufferedReader(fr);
    int new_files_created = 0;
    int fits_files_created = 0;
    int tbl_files_created = 0;
    int duplicate_count = 0;

    while ((str = in.readLine()) != null)
    {
	//System.out.println(str);
	StringTokenizer st = new StringTokenizer(str);
	while (st.hasMoreTokens())
	{
	    full_filename = st.nextToken();
	    outfile = new File(full_filename);
	    output_filename = outfile.getName();
	    if (!st.hasMoreTokens())
	    {
		break;
	    }
	    size_string = st.nextToken();
	    filesize = Integer.parseInt(size_string);
	    //System.out.println("filesize = " + filesize);
	    total_filesize += filesize;
	    //System.out.println("FILENAME = " + output_filename);
	    String parent = outfile.getParent();
	    File parent_dir = new File(parent);
	    parent_dir.mkdirs();

	    if (longest_filename < output_filename.length())
		longest_filename = output_filename.length();
	    if (longest_dirname < parent.length())
		longest_dirname = parent.length();

	    if (output_filename.endsWith("fits"))
	    {
		//System.out.println("it's a fits file");
		if (outfile.exists())
		{
		    System.out.println("Warning: fits file already exists: " + 
			output_filename);
		    new_files_created--;
		    fits_files_created--;
		    duplicate_count++;
		}
		loi.addFitsHeader(template_fits, header, data32, outfile,  
		    output_filename, parent, filesize);
		new_files_created++;
		fits_files_created++;
	    }
	    else if (output_filename.endsWith("tbl"))
	    {
		//System.out.println("it's a tbl file");
		if (outfile.exists())
		{
		    System.out.println("Warning: fits file already exists: " + 
			output_filename);
		    new_files_created--;
		    tbl_files_created--;
		    duplicate_count++;
		}
		loi.addTblHeader(tbl_template_contents, outfile,  
		    output_filename, parent, filesize);
		new_files_created++;
		tbl_files_created++;
	    }
	    else
	    {
		System.out.println("ERROR: file is not fits or tbl. ");
		System.out.println("filename = " + full_filename);
	    }

	    if ((new_files_created % 1000) == 0)
	    {
		System.err.println(new_files_created + " files created so far");
	    }
	}
    }
    System.err.println("\nTotal number of new files created = " + 
	new_files_created);
    System.err.println("Total new fits files created = " + fits_files_created);
    System.err.println("Total new tbl files created =  " + tbl_files_created);
    System.err.println("Duplicates =  " + duplicate_count);
    System.err.println("total_filesize = " + total_filesize);
    System.err.println("longest_filename = " + longest_filename +
	    "  longest_dirname = " + longest_dirname);
    }
    catch (FileNotFoundException fe)
    {
	System.err.println("ABORT: " + fe.getMessage());
    }
    catch (IOException ioe)
    {
	System.err.println("ABORT: " + ioe.getMessage());
    }
}


    private void addTblHeader(ArrayList<String> tbl_template_contents, 
	File outfile, 
	String output_filename, String parent, int desired_filesize) 
    {
	String file_string = "\\char FILENAME= '" + output_filename + "'";
	String parent_string = "\\char DIRNAME= '" + parent + "'";
	String str;
	try
	{
	    FileWriter fw = new FileWriter(outfile);
	    BufferedWriter out = new BufferedWriter(fw);

	    out.write(file_string);
	    out.newLine();
	    out.write(parent_string);
	    out.newLine();
	    int output_length = 
		file_string.length() + parent_string.length() + 2;

	    //while ((str = in.readLine()) != null)
	    int i = 0;
	    for (;;)
	    {
		String output_string = tbl_template_contents.get(i);
		out.write(output_string);
		out.newLine();
		output_length += output_string.length() + 1;
		if (output_length > desired_filesize)
		{
		    break;
		}
		i++;
		if (i >= tbl_template_contents.size())
		{
		    i = 0;
		}
	    }
	    out.close();
	}
	catch (FileNotFoundException fe)
	{
	    System.err.println("ABORT: " + fe.getMessage());
	}
	catch (IOException ioe)
	{
	    System.err.println("ABORT: " + ioe.getMessage());
	}
    }


    private void addFitsHeader(Fits fits, Header header, int[][] data32, 
	File outfile, 
	String output_filename, String parent, int filesize)
    {
	/* compute number of lines in image for a given file size */
	int image_bytes = ((filesize / 2880) -8) * 2880;  //header = 8 blocks
	int lines = image_bytes / 2048; //heritage.fits has 2048 bytes per line
	//System.out.println("filesize = " + filesize + "  lines = " + lines);
	int min_x = 0;
	int min_y = 0;
	int max_x = 512 - 1;
	int max_y = lines - 1;

	/* do the crop - works only for this exact image (heritage.fits) */
	int x,y; 
	int x_out, y_out;
	ImageData new_image_data = null;
	int naxis = 2;
	int naxis1 = 512;
	int naxis2 = 1024;
	float crpix1 = 257.0F;
	float crpix2 = 513.0F;

	int new_naxis1 = max_x - min_x + 1;
	int new_naxis2 = max_y - min_y + 1;
	BasicHDU retval ;
	Header new_header = clone_header(header);
	try
	{
	    new_header.addValue("NAXIS1" , new_naxis1, null);
	    new_header.addValue("NAXIS2" , new_naxis2, null);

	    float new_crpix1 = crpix1 - min_x;
	    new_header.addValue("CRPIX1" , crpix1 - min_x, null);
	    new_header.addValue("CRPIX2" , crpix2 - min_y, null);

	    int[][] new_data32 = new int[new_naxis2][new_naxis1];

	    y = 0;
	    for (y_out = min_y; y_out <= max_y; y_out++)
	    {
		for (x = 0; x <= max_x; x++)
		{
		    new_data32[y_out][x] = data32[y][x];
		}
		y++;
		if (y >= 1024)
		{
		    y = 0;
		}
	    }
	    new_image_data = new ImageData(new_data32);
	    BasicHDU new_HDU = new ImageHDU(new_header, new_image_data);
	    Fits new_fits = new Fits();
	    new_fits.addHDU(new_HDU);

	    new_header.addValue("FILENAME", output_filename, null);
	    new_header.addValue("DIRNAME", parent, null);

	    FileOutputStream fo = new FileOutputStream(outfile);
	    BufferedDataOutputStream o = new BufferedDataOutputStream(fo);
	    new_fits.write(o);
	}
	catch (FitsException e)
	{
	    e.printStackTrace();
	} catch (FileNotFoundException e) {
	    e.printStackTrace();
	}

    }


    static Header clone_header(Header header)
    {
	// first collect cards from old header
	Cursor iter = header.iterator();
	String cards[] = new String[header.getNumberOfCards()];
	int i = 0;
	while (iter.hasNext())
	{
	    HeaderCard card = (HeaderCard) iter.next();
	    //System.out.println("RBH card.toString() = " + card.toString());
	    cards[i] = card.toString();
	    i++;
	}
	return(new Header(cards));
    }

}
