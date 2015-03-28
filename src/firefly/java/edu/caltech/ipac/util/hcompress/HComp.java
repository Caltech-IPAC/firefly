/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util.hcompress;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * TODO Write Javadocs
 *
 * @version $Id: HComp.java,v 1.2 2005/12/01 00:56:21 roby Exp $
 *          <hr>
 *          Represents the data contained in an Observation Request. <BR>
 *          Copyright (C) 1999-2005 California Institute of Technology. All rights reserved.<BR>
 *          US Government Sponsorship under NASA contract NAS7-918 is acknowledged. <BR>
 */
public class HComp {

    public static final int LINE_BUFF_SIZE = 80;

    public static final String FMT_RAW = "raw";
    public static final String FMT_NET = "net";
    public static final String FMT_FITS = "fits";
    public static final String FMT_HHH = "hhh";
    public static final String FMT_UNK = "unknown";

    boolean verbose = false;

    File srcFilename;
    File destFileName;
    String format = FMT_UNK;

    int[] data, shuffle_temp;
    int nx = -1;
    int ny = -1;

    int scale = 1024;
    private FileInputStream fis;
    private FileChannel ic;
    private FileOutputStream fos;
    private FileChannel oc;
    private boolean passFitsHeader;
    private final double LOG2 = Math.log(2.0);
    private static int MAGIC_CODE = 0xDD99;


    public HComp(File srcFileName, File destFileName) {
        // TODO determine the format
        this.srcFilename = srcFileName;
        this.destFileName = destFileName;
    }

    public HComp(String srcFileName, String destFileName) {
        // TODO determine the format
        this.srcFilename = new File(srcFileName);
        this.destFileName = new File(destFileName);
    }

    public void hCompresss(String srcFileName, String destFileName, String format) throws HCompException {
        this.format = format;
        this.srcFilename = new File(srcFileName);
        this.destFileName = new File(destFileName);
        hCompress();
    }

    public void hCompress(File srcFileName, File destFileName, String format) throws HCompException {
        this.format = format;
        this.srcFilename = srcFileName;
        this.destFileName = destFileName;
        hCompress();
    }

    public void hCompress(String srcFileName, String destFileName) throws HCompException {
        hCompress(new File(srcFileName), new File(destFileName));
    }

    public void hCompress(File srcFileName, File destFileName) throws HCompException {
        // this.format = format;   TODO Determine the format;
        this.srcFilename = srcFileName;
        this.destFileName = destFileName;
        hCompress();
    }


    public void setPassFitsHeader(boolean passFitsHeader) {
        this.passFitsHeader = passFitsHeader;
    }

    public void hCompress() throws HCompException {


        /*
         * Read data, return address & size
         * FITS header will get written to stdout
         get_data(infile, inname, stdout, &a, &nx, &ny, format);
         if (verbose) {
             fprintf(stderr, "Image size (%d,%d)  Scale factor %d\n",
                 ny,nx,scale);
         }
         */
        getData();

        /*
         * H-transform
         htrans(a,nx,ny);
         */
        hTransform();

        /*
         * Digitize
         digitize(a,nx,ny,scale);
         */
        digitize();

        /*
         * Encode and write to stdout
         encode(stdout,a,nx,ny,scale);
         */
        encode();

    }

    private void getData() throws HCompException {
        assert (format.equalsIgnoreCase(HComp.FMT_UNK) == false);

        if (format.equalsIgnoreCase(HComp.FMT_RAW)) {
            getRAW();
        } else if (format.equalsIgnoreCase(HComp.FMT_NET)) {
            getRAW();
        } else if (format.equalsIgnoreCase(HComp.FMT_FITS)) {
            getFITS();
        } else if (format.equalsIgnoreCase(HComp.FMT_HHH)) {
            getHHH();
        }
    }

    private void getRAW() throws HCompException {

        FileInputStream fis = null;
        FileChannel fc = null;

        try {

            fis = new FileInputStream(this.srcFilename);
            fc = fis.getChannel();
            MappedByteBuffer mappedBuffer = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
            data = mappedBuffer.asIntBuffer().array();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new HCompException(e.getMessage(), e);
        } catch (IOException e) {
            e.printStackTrace();
            throw new HCompException(e.getMessage(), e);
        } finally {
            if (fis != null) {
                if (fc != null) {
                    try {
                        fc.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        throw new HCompException(e.getMessage(), e);
                    }
                }
            }
        }
    }

    private void getFITS() throws HCompException {
        passFitsHeader = true;
        readFITS();
    }

    /**
     * Reads the FITS file including the headers. The headers are written to the destination file if the
     * field passFitsHeader is true. The buffersize depends on the the presence of line terminators in the
     * source file.
     * <p/>
     * The resulting data is placed in an int[] that is stored in the class field data
     *
     * @throws HCompException
     */
    private void readFITS() throws HCompException {
        fis = null;
        ic = null;

        try {

            fis = new FileInputStream(this.srcFilename);
            ic = fis.getChannel();

            if (passFitsHeader) {
                fos = new FileOutputStream(this.destFileName);
                oc = fos.getChannel();
            }

            ByteBuffer bb = ByteBuffer.allocate(LINE_BUFF_SIZE);

            int bbRead = -1, bbReadTotal = 0;
            while ((bbRead = ic.read(bb)) != -1) {
                if (passFitsHeader) {
                    oc.write(bb);
                }


                // TODO scan the buffer or NAXIS1, NAXIS2 and store
                // TODO Verify NAXIS, DATATPE, PSIZE and GCOUNT

                bbReadTotal += bbRead;
            }

            MappedByteBuffer mappedBuffer = ic.map(FileChannel.MapMode.READ_ONLY, bbReadTotal, ic.size());
            data = mappedBuffer.asIntBuffer().array();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new HCompException(e.getMessage(), e);
        } catch (IOException e) {
            e.printStackTrace();
            throw new HCompException(e.getMessage(), e);
        }

    }

    private void getHHH() throws HCompException {
        passFitsHeader = true;
        readFITS();
    }

    private void hTransform() {
        int nmax, log2n, h0, hx, hy, hc, nxtop, nytop, i, j, k;
        int oddx, oddy;
        int shift, mask, mask2, prnd, prnd2, nrnd2;
        int s10, s00;

        /*
         * log2n is log2 of max(nx,ny) rounded up to next power of 2
         */
        nmax = (nx > ny) ? nx : ny;
        log2n = (int) (Math.log((float) nmax) / LOG2 + 0.5);
        if (nmax > (1 << log2n)) {
            log2n++;
        }


        /*
         * get temporary storage for shuffling elements
         */
        shuffle_temp = new int[((nmax + 1) / 2)];

        /*
         * set up rounding and shifting masks
         */
        shift = 0;
        mask = -2;
        mask2 = mask << 1;
        prnd = 1;
        prnd2 = prnd << 1;
        nrnd2 = prnd2 - 1;


        /*
         * do log2n reductions
         *
         * We're indexing a as a 2-D array with dimensions (nx,ny).
         */

        nxtop = nx;
        nytop = ny;

        for (k = 0; k < log2n; k++) {
            oddx = nxtop % 2;
            oddy = nytop % 2;
            for (i = 0; i < nxtop - oddx; i += 2) {
                s00 = i * ny;				/* s00 is index of a[i,j]	*/
                s10 = s00 + ny;			/* s10 is index of a[i+1,j]	*/
                for (j = 0; j < nytop - oddy; j += 2) {


                    /*
                     * Divide h0,hx,hy,hc by 2 (1 the first time through).
                     */
                    h0 = (data[s10 + 1] + data[s10] + data[s00 + 1] + data[s00]) >> shift;
                    hx = (data[s10 + 1] + data[s10] - data[s00 + 1] - data[s00]) >> shift;
                    hy = (data[s10 + 1] - data[s10] + data[s00 + 1] - data[s00]) >> shift;
                    hc = (data[s10 + 1] - data[s10] - data[s00 + 1] + data[s00]) >> shift;


                    /*
                     * Throw away the 2 bottom bits of h0, bottom bit of hx,hy.
                     * To get rounding to be same for positive and negative
                     * numbers, nrnd2 = prnd2 - 1.
                     */
                    data[s10 + 1] = hc;
                    data[s10] = ((hx >= 0) ? (hx + prnd) : hx) & mask;
                    data[s00 + 1] = ((hy >= 0) ? (hy + prnd) : hy) & mask;
                    data[s00] = ((h0 >= 0) ? (h0 + prnd2) : (h0 + nrnd2)) & mask2;
                    s00 += 2;
                    s10 += 2;
                }
                if (oddy != 0) {
                    /*
                     * do last element in row if row length is odd
                     * s00+1, s10+1 are off edge
                     */
                    h0 = (data[s10] + data[s00]) << (1 - shift);
                    hx = (data[s10] - data[s00]) << (1 - shift);
                    data[s10] = ((hx >= 0) ? (hx + prnd) : hx) & mask;
                    data[s00] = ((h0 >= 0) ? (h0 + prnd2) : (h0 + nrnd2)) & mask2;
                    s00 += 1;
                    s10 += 1;
                }
            }
            if (oddx != 0) {
                /*
                 * do last row if column length is odd
                 * s10, s10+1 are off edge
                 */
                s00 = i * ny;
                for (j = 0; j < nytop - oddy; j += 2) {
                    h0 = (data[s00 + 1] + data[s00]) << (1 - shift);
                    hy = (data[s00 + 1] - data[s00]) << (1 - shift);
                    data[s00 + 1] = ((hy >= 0) ? (hy + prnd) : hy) & mask;
                    data[s00] = ((h0 >= 0) ? (h0 + prnd2) : (h0 + nrnd2)) & mask2;
                    s00 += 2;
                }
                if (oddy != 0) {
                    /*
                     * do corner element if both row and column lengths are odd
                     * s00+1, s10, s10+1 are off edge
                     */
                    h0 = data[s00] << (2 - shift);
                    data[s00] = ((h0 >= 0) ? (h0 + prnd2) : (h0 + nrnd2)) & mask2;
                }
            }


            /*
             * now shuffle in each dimension to group coefficients by order
             */
            for (i = 0; i < nxtop; i++) {
                shuffle(data[ny * i], nytop, 1);
            }
            for (j = 0; j < nytop; j++) {
                shuffle(data[j], nxtop, ny);
            }


            /*
             * image size reduced by 2 (round up if odd)
             */
            nxtop = (nxtop + 1) >> 1;
            nytop = (nytop + 1) >> 1;


            /*
             * divisor doubles after first reduction
             */
            shift = 1;


            /*
             * masks, rounding values double after each iteration
             */
            mask = mask2;
            prnd = prnd2;
            mask2 = mask2 << 1;
            prnd2 = prnd2 << 1;
            nrnd2 = prnd2 - 1;
        }


    }

    /**
     * Adapted from http://simbad.u-strasbg.fr/public/uncompression/sources/Hdecomp.java
     *
     * @param off offset in the data[] array to shuffle
     * @param n   number of elements to shuffle
     * @param n2  second dimension
     */
    private void unshuffle(int off, int n, int n2) {
        int i;
        int nhalf;
        int p1, p2, pt;
        int n22 = n2 << 1;

        /* copy 2nd half of array to tmp */
        nhalf = (n + 1) >> 1;
        pt = 0;
        p1 = off + n2 * nhalf;
        for (i = nhalf; i < n; i++) {
            shuffle_temp[pt++] = data[p1];
            p1 += n2;
        }

        /* distribute 1st half of array to even elements */
        p2 = off + (n2 * (nhalf - 1));
        p1 = off + ((n2 * (nhalf - 1)) << 1);
        for (i = nhalf - 1; i >= 0; i--) {
            data[p1] = data[p2];
            p2 -= n2;
            p1 -= n22;
        }

        /* now distribute 2nd half of array (in tmp) to odd elements */
        pt = 0;
        p1 = off + n2;
        for (i = 1; i < n; i += 2) {
            data[p1] = shuffle_temp[pt++];
            p1 += n22;
        }
    }


    /**
     * @param off offset in the data[] array to shuffle
     * @param n   number of elements to shuffle
     * @param n2  second dimension
     */
    private void shuffle(int off, int n, int n2) {
        int i;
        int nhalf;
        int p1, p2, pt;
        int n22 = n2 << 1;

        // midpoint of the array
        nhalf = (n + 1) >> 1;

        // copy the odd elements to temp
        pt = 0;
        p1 = off + n2;
        for (i = 1; i < n; i += 2) {
            shuffle_temp[pt++] = data[p1];
            p1 += n22;
        }

        // move to even elements to bottom half of the array
        p1 = off + ((n2 * (nhalf - 1)) << 1);
        p2 = off + (n2 * (nhalf - 1));
        for (i = 2; i < n; i += 2) {
            data[p1] = data[p2];
            p1 += n2;
            p2 += n22;
        }

        // move the odd elements from temp to top half of the array
        pt = 0;
        p1 = off + n2;
        for (i = 1; i < n; i++) {
            data[p1] = shuffle_temp[pt++];
            p1 += n2;
        }
    }

    private void digitize() {
        int d;

        /*
         * round to multiple of scale
         */
        if (scale <= 1) {
            return;
        }
        d = (scale + 1) / 2 - 1;

// 	    for (p=a; p <= &a[nx*ny-1]; p++)
//        *p = ((*p>0) ? (*p+d) : (*p-d))/scale;

        for (int i = 0; i <= nx * ny - 1; i++) {
            data[i] = ((data[i] > 0) ? (data[i] + d) : (data[i] - d)) / scale;
        }

    }

    private void encode() throws HCompException {
        int nel, nx2, ny2, i, j, k, q;
        int vmax[] = new int[3];
        int nsign, bits_to_go;
        char nbitplanes[] = new char[3];
        char signbits[];

        try {
            nel = nx * ny;

            MappedByteBuffer bb = oc.map(FileChannel.MapMode.READ_WRITE, 0, nx * ny + 1);

            IntBuffer ib = bb.asIntBuffer();

            ib.put(MAGIC_CODE);

            ib.put(nx);
            ib.put(ny);
            ib.put(scale);

            ib.put(data[0]);


            if (ic != null) {
                ic.close();
            }
            if (oc != null) {
                oc.close();
            }
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            throw new HCompException(e.getMessage(), e);
        } finally {
            if (ic != null) {
                try {
                    ic.close();
                    ic = null;
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new HCompException(e.getMessage(), e);
                }
            }
            if (oc != null) {
                try {
                    oc.close();
                    oc = null;
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new HCompException(e.getMessage(), e);
                }
            }
        }

    }

    public static void main(String[] args) {
        /*
         * Get command line arguments, open input file(s) if necessary
         hcinit(argc, argv);
         */

    }

}
