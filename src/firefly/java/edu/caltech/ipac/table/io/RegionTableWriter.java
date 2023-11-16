package edu.caltech.ipac.table.io;

import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataObject;
import edu.caltech.ipac.table.IpacTableUtil;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;

public class RegionTableWriter {
    private static Logger.LoggerImpl LOG = Logger.getLogger();
    public static void write(Writer writer, DataGroup data, String centerCols) throws IOException {

        BufferedWriter outf = new BufferedWriter(writer, IpacTableUtil.FILE_IO_BUFFER_SIZE);
        if (centerCols == null) {
            throw new IOException("Unable to find center columns");
        }
        outf.write("J2000; color=blue");
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
