package edu.caltech.ipac.table.io;

import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataObject;
import edu.caltech.ipac.table.IpacTableUtil;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

public class RegionTableWriter {
    private static Logger.LoggerImpl LOG = Logger.getLogger();
    /** the keyword written when the request names no coordinate system */
    public static final String DEFAULT_COORD_SYS = "J2000";
    /**
     * The DS9 region coordinate system keywords the browser's table save dialog (TableSave.jsx) sends.
     * @see <a href="https://ds9.si.edu/doc/ref/region.html">DS9 regions</a>
     */
    private static final List<String> COORD_SYS_KEYWORDS = List.of(DEFAULT_COORD_SYS, "B1950", "GALACTIC", "ECLIPTIC");

    /**
     * Write each row's position as a point of a DS9 region file.
     * @param writer      where the region file is written
     * @param data        the table rows
     * @param centerCols  the longitude and latitude column names, as "s_ra,s_dec"
     * @param coordSys    the DS9 keyword of the coordinate system the two columns are in, such as "GALACTIC".
     *                    The positions are written as they are in the table, so the keyword is what makes a reader
     *                    of the region file place them correctly. null means J2000.
     * @throws IOException if centerCols is missing or coordSys is not a keyword of COORD_SYS_KEYWORDS
     */
    public static void write(Writer writer, DataGroup data, String centerCols, String coordSys) throws IOException {

        BufferedWriter outf = new BufferedWriter(writer, IpacTableUtil.FILE_IO_BUFFER_SIZE);
        if (centerCols == null) {
            throw new IOException("Unable to find center columns");
        }
        String coordSysKeyword = coordSys == null ? DEFAULT_COORD_SYS : coordSys.trim().toUpperCase();
        // the keyword comes from the request and goes into the file: accept only the known ones
        if (!COORD_SYS_KEYWORDS.contains(coordSysKeyword)) {
            throw new IOException("Unsupported region coordinate system: " + coordSys);
        }
        outf.write(coordSysKeyword + "; color=blue");
        String[] cols = centerCols.split(","); //center cols is of format "s_ra,s_dec"
        try {
            if (data != null && data.size() > 0) {
                for (DataObject row : data.values()) {
                    outf.newLine();
                    Object ra = row.getDataElement(cols[0]);
                    Object dec = row.getDataElement(cols[1]);
                    if (cols[0].equals(cols[1])) {//single column with an array entry for RA and DEC
                        Object singleCol = row.getDataElement(cols[0]);
                        if (singleCol == null) continue;
                        double[] entries = (double[]) singleCol;
                        ra = entries[0];
                        dec = entries[1];
                    }
                    if (ra == null || dec == null) continue;
                    outf.write("point    " + ra + "    " + dec);
                    outf.write(" # 10 point=circle");
                }
            }
            outf.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException(e);
        } finally {
            outf.close();
        }
    }
}
