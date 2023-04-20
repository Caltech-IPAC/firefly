/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot;

import edu.caltech.ipac.firefly.visualize.ClientFitsHeader;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class PixelValue {

	static public double pixelVal(File f, ImagePt pt, ClientFitsHeader header) {
		if (header == null || !f.canRead()) return Double.NaN;
		try (RandomAccessFile raf = new RandomAccessFile(f, "r")) {
			return PixelValue.pixelVal(raf, (int)pt.getX(), (int)pt.getY(), header);
		} catch (IOException e) {
			return Double.NaN;
		}
	}

	static public double pixelVal(RandomAccessFile fits_file, int x, int y, ClientFitsHeader header) throws IOException{
		int plane_number   = header.getPlaneNumber();
		int bitpix         = header.getBitpix();
		long naxis1        = header.getNaxis1();
		long naxis2        = header.getNaxis2();
		double cdelt2      = header.getCDelt2();
		double bscale      = header.getBScale();
		double bzero       = header.getBZero();
		double blank_value = header.getBlankValue();
		long data_offset   = header.getDataOffset();
		long yLong         = cdelt2 < 0 ?  naxis2 -1 -y : y;
		int plane_offset   = plane_number>-1 ? plane_number: 0;

		int bytes_per_pixel= getBytePerPixel(bitpix);
		long pixel_offset  = (naxis1 * naxis2 * plane_offset) + (yLong * naxis1 + x);
		long file_pointer  = data_offset + pixel_offset * bytes_per_pixel;
		double file_value  = readValue(fits_file,file_pointer, bitpix, blank_value);

		if (!isPalomar(header)) return file_value * bscale + bzero; // normal case

		 // If this is a Palomar Transient Factory single-epoch FITS image, then
		 // convert pixel values to magnitudes and apply photometric and airmass corrections.
		 // (200x-era request from PTF scientists)
		 // See other uses of PALOMAR_ID elsewhere in Firefly for other pieces of this.
		 // TODO- generalize or retire?
		double airmass= header.getDoubleHeader(ImageHeader.AIRMASS);
		double extinct= header.getDoubleHeader(ImageHeader.EXTINCT);
		double imagezpt= header.getDoubleHeader(ImageHeader.IMAGEZPT);
		double exptime= header.getDoubleHeader(ImageHeader.EXPTIME);
		return !Double.isNaN(file_value)?  -2.5 * .43429 * Math.log(file_value / exptime) +
				imagezpt + extinct * airmass:Double.NaN;
	}

	private static boolean isPalomar(ClientFitsHeader h) {
		// Identify Palomar Transient Factory single-epoch images based on FITS headers
		return
				h.getStringHeader(ImageHeader.ORIGIN).startsWith(ImageHeader.PALOMAR_ID)  &&
				h.containsKey(ImageHeader.AIRMASS) && h.containsKey(ImageHeader.EXTINCT) &&
				h.containsKey(ImageHeader.IMAGEZPT) && h.containsKey(ImageHeader.EXPTIME);
	}

	private static int getBytePerPixel(int bitpix) {
		return switch (bitpix) {
			case 8 -> 1;
			case 16 -> 2;
			case 32, -32 -> 4;
			case 64, -64 -> 8;
			default -> 0;
		};
	}

	private static double readValue(RandomAccessFile fits_file, long file_pointer, int bitpix, double blank_value)
			throws IOException{
		fits_file.seek(file_pointer);
		double value= switch (bitpix) {
			case 8 ->   fits_file.readUnsignedByte();
			case 16 ->  fits_file.readShort();
			case 32 ->  fits_file.readInt();
			case -32 -> fits_file.readFloat();
			case -64 -> fits_file.readDouble();
			case  64 -> fits_file.readLong();
			default ->  blank_value;
		};
		if (bitpix > 0 && value == blank_value) return Double.NaN;
		return value;
	}



	public static void main(String[] args) {
		if (args.length != 3) {
			System.out.println( "usage:  java edu.caltech.ipac.visualize.plot.PixelValue <FITSfilename> <x> <y>");
			System.out.println("   where the first pixel in the file is x=0 y=0");
			System.exit(1);
		}
		long HDU_offset;
		long header_size;
		Fits myFits;
		BasicHDU[] myHDUs = null;

		try {
			myFits = new Fits(args[0]);
			myHDUs = myFits.read();
		}
		catch (FitsException e) {
			System.out.println("Caught exception e: "+e);
			e.printStackTrace();
			System.exit(1);
		}
		if (myHDUs == null) {
			System.out.println("no HDUs in file - apparently not a FITS file");
			System.exit(1);
		}


		for (BasicHDU hdUs : myHDUs) {
			HDU_offset = hdUs.getFileOffset();
			System.out.println("getFileOffset = " + hdUs.getFileOffset());
			hdUs.info(System.out);
			Header header = hdUs.getHeader();
			header_size = header.getOriginalSize();
			System.out.println("header.getSize() = " + header.getSize());
			System.out.println("header.getOriginalSize() = " + header.getOriginalSize());

			long data_offset = HDU_offset + header_size;

			//data_offset = 23440;  // DEBUG ONLY

			System.out.println("data_offset = " + data_offset);
			//header.dumpHeader(System.out);
			int bitpix = header.getIntValue("BITPIX", 0);
			int naxis = header.getIntValue("NAXIS", 0);
			int naxis1 = header.getIntValue("NAXIS1", 0);
			int naxis2 = header.getIntValue("NAXIS2", 0);
			int naxis3 = header.getIntValue("NAXIS3", 1);
			double bscale = header.getDoubleValue("BSCALE", 1);
			double bzero = header.getDoubleValue("BZERO", 0);
			double cdelt2 = header.getDoubleValue("CDELT2", 0);
			double blank_value = bitpix > 0 ? header.getDoubleValue("BLANK", Double.NaN) : Double.NaN;
			System.out.println("naxis3 = " + naxis3);
			RandomAccessFile fits_file = null;

			int xcenter = Integer.parseInt(args[1]);
			int ycenter = Integer.parseInt(args[2]);
			int plane_number = 1;
			int x = xcenter;
			int y = ycenter;


			try {
				fits_file = new RandomAccessFile(args[0], "r");
			} catch (FileNotFoundException e) {
				System.out.println("Caught exception e: " + e);
				e.printStackTrace();
				System.exit(1);
			}

			double pixel_data = Double.NaN;
			System.out.println("Fetching value for x = " + x + "  y = " + y);
			try {
				ClientFitsHeader miniHeader = new ClientFitsHeader(plane_number, bitpix, naxis1, naxis2,
						cdelt2, bscale, bzero, blank_value, data_offset);
				pixel_data = PixelValue.pixelVal(fits_file, x, y, miniHeader);
				fits_file.close();
				if (!Double.isNaN(pixel_data)) {
					System.out.println("x = " + x + "  y = " + y + "  plane_number = " + plane_number +
							"   pixel_data = " + pixel_data);
				}
				else {
					System.out.println("no value at that pixel" );
				}
			} catch (IOException e) {
				System.out.println("Caught exception e: " + e);
				e.printStackTrace();
			}
		}
	}



}
