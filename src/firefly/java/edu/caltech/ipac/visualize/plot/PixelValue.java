/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot;

import edu.caltech.ipac.firefly.visualize.DirectFitsAccessData;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class PixelValue {

	/**
	 * Read a FITS pixel value directly from the file on disk. That is, open the file and read one value and close.
	 * @param f - the FITS file
	 * @param pt - point, the x,y of the location to read
	 * @param h - the direct file access data, mostly from the fits header
	 * @return - the read result
	 */
	public static Result getPixelValue(File f, ImagePt pt, DirectFitsAccessData h) {
		if (h == null || !f.canRead()) return Result.makeUnavailable();

		try (RandomAccessFile raf = new RandomAccessFile(f, "r")) {
			if (h.bitpix()==64 && ((h.bZero()==0 && h.bScale()==1))) {
				return pixelValLongResult(raf,pt,h);
			}
			else {
				return pixelValDoubleResult(raf,pt,h);
			}
		} catch (IOException e) {
			return Result.makeUnavailable();
		}
	}

	private static Result pixelValDoubleResult(RandomAccessFile raf, ImagePt pt, DirectFitsAccessData header) throws IOException {
		double v= pixelValDouble(raf,pt,header);
		int bitpix= header.bitpix();

		if (bitpix<64) {
			String base16= switch (bitpix) {
				case 8, 16, 32 -> getIntBase16String((int)v, bitpix);
				case -32 -> getIntBase16String(Float.floatToIntBits((float)v),bitpix);
				case -64 -> getLongBase16String(Double.doubleToLongBits(v));
				default -> "";
			};
			String resultType= isInt(header) ? Result.TYPE_DECIMAL_INT : Result.TYPE_FLOAT;
			String status= getValStatus(v,isInt(header));
			return new Result(status,resultType,scaleValue(v,header)+"",base16);
		}
		else {
			return !Double.isNaN(v) ?
					getLongResult((long)v,Result.STATUS_VALUE) : getLongResult((long)v,Result.STATUS_UNDEFINED);
		}
	}

	private static String getValStatus(double v, boolean isInt) {
		if (isInt) return !Double.isNaN(v) ? Result.STATUS_VALUE : Result.STATUS_UNDEFINED;
		else return !Double.isNaN(v) ? Result.STATUS_VALUE : Result.STATUS_NAN;
	}

	private static Result pixelValLongResult(RandomAccessFile raf, ImagePt pt, DirectFitsAccessData header) throws IOException {
		long v= pixelValLong(raf, pt, header);
		String vUnsignedStr= Long.toUnsignedString(v);
		try {
			String bV= Long.toUnsignedString(Long.parseUnsignedLong(header.blankValue()));
			String status= !vUnsignedStr.equals(bV) ? Result.STATUS_VALUE : Result.STATUS_UNDEFINED;
			return getLongResult(v,status);
		} catch (NumberFormatException ignore) {
			return getLongResult(v,Result.STATUS_VALUE);
		}
	}

	private static String getIntBase16String(int v, int bitpix) {
		return switch (bitpix) {
			case 8 -> String.format("0x%02x",(byte)v);
			case 16 ->String.format("0x%04x",(short)v);
			default ->String.format("0x%08x",v);
		};
	}

	private static String getLongBase16String(long v) { return String.format("0x%016x",v); }

	private static Result getLongResult(long v, String status) {
		return new Result(status, Result.TYPE_DECIMAL_INT, Long.toString(v,10), getLongBase16String(v));
	}

	private static long pixelValLong(RandomAccessFile fits_file, ImagePt pt, DirectFitsAccessData header) throws IOException {
		return readValueLong(fits_file, getFitsFilePointer(header,pt));
	}

	private static long getFitsFilePointer(DirectFitsAccessData h, ImagePt pt) {
		int x= (int)pt.getX();
		int y= (int)pt.getY();
		double cdelt2    = h.cDelt2();
		long naxis1      = h.naxis1();
		long naxis2      = h.naxis2();
		long data_offset = h.dataOffset();
		int plane_number = h.planeNumber();
		int bytesPerPixel= getBytePerPixel(h.bitpix());

		long yLong = cdelt2 < 0 ? naxis2 - 1 - y : y;
		int plane_offset = plane_number > -1 ? plane_number : 0;
		long pixel_offset  = (naxis1 * naxis2 * plane_offset) + (yLong * naxis1 + x);
		return data_offset + pixel_offset * bytesPerPixel;
	}

	private static double pixelValDouble(RandomAccessFile fits_file, ImagePt pt, DirectFitsAccessData header)
			throws IOException{
		double blankValueDouble= Double.NaN;
		try {
			blankValueDouble = Double.parseDouble(header.blankValue());
		}
		catch (NumberFormatException ignore) { }

		return readValue(fits_file,getFitsFilePointer(header,pt), header, blankValueDouble);
	}

	private static double scaleValue(double v, DirectFitsAccessData h) {
		return !isPalomar(h) ? v * h.bScale() + h.bZero() : convertToPolomar(v,h);
	}

	private static double convertToPolomar(double fileValue, DirectFitsAccessData header) {
		// todo- this code should never have been here, but we are stuck with it for now
		// If this is a Palomar Transient Factory single-epoch FITS image, then
		// convert pixel values to magnitudes and apply photometric and airMass corrections.
		// (200x-era request from PTF scientists)
		// See other uses of PALOMAR_ID elsewhere in Firefly for other pieces of this.
		double airMass= header.getDoubleHeader(ImageHeader.AIRMASS);
		double extinct= header.getDoubleHeader(ImageHeader.EXTINCT);
		double imageZpt= header.getDoubleHeader(ImageHeader.IMAGEZPT);
		double expTime= header.getDoubleHeader(ImageHeader.EXPTIME);
		return !Double.isNaN(fileValue)?  -2.5 * .43429 * Math.log(fileValue/ expTime) +
				imageZpt + extinct * airMass:Double.NaN;
	}

	private static boolean isPalomar(DirectFitsAccessData h) {
		// Identify Palomar Transient Factory single-epoch images based on FITS headers
		return h.getStringHeader(ImageHeader.ORIGIN,"").startsWith(ImageHeader.PALOMAR_ID)  &&
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

	private static boolean isInt(DirectFitsAccessData h) { return h.bitpix()>0;}

	private static double readValue(RandomAccessFile fits_file, long file_pointer, DirectFitsAccessData h, double blankValueDouble)
			throws IOException{
		fits_file.seek(file_pointer);
		double value= switch (h.bitpix()) {
			case 8 ->   fits_file.readUnsignedByte();
			case 16 ->  fits_file.readShort();
			case 32 ->  fits_file.readInt();
			case -32 -> fits_file.readFloat();
			case -64 -> fits_file.readDouble();
			case  64 -> fits_file.readLong();
			default ->  blankValueDouble;
		};
		return (isInt(h) && value == blankValueDouble) ? Double.NaN : value;
	}

	private static long readValueLong(RandomAccessFile fits_file, long file_pointer) throws IOException{
		fits_file.seek(file_pointer);
		return fits_file.readLong();
	}

	public record Result(String status, String type, String valueBase10, String valueBase16) {
		public static final String STATUS_UNAVAILABLE= "UNAVAILABLE";
		public static final String STATUS_NAN= "NaN";
		public static final String STATUS_UNDEFINED= "UNDEFINED";
		public static final String STATUS_VALUE= "VALUE";
		public static final String TYPE_EMPTY= "EMPTY";
		public static final String TYPE_DECIMAL_INT = "DECIMAL_INT";
		public static final String TYPE_FLOAT= "FLOAT";
		private static final Result unavailable= new Result(STATUS_UNAVAILABLE,TYPE_EMPTY,"","");
		public static Result makeUnavailable() {return unavailable;}
	};
}
