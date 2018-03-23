/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.astro;

import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.IpacTableUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class handles an action to save a catalog in IPAC table format to local file.
 *
 * @author Xiuqin Wu
 * @see DataGroup
 * @see edu.caltech.ipac.util.DataObject
 * @see DataType
 * @version $Id: IpacTableWriter.java,v 1.11 2012/08/10 20:58:28 tatianag Exp $
 */
public class IpacTableWriter {
    private static final String LINE_SEP = "\n";

    public final static String BACKSLASH = "\\";

    // constants
    private final static int COL_LENGTH = 30;
    // header names & constants
    private final static String NULL_STRING = "null";
    private final static String SEPARATOR = "|";
    private final static String EMPTY_SEPARATOR = " ";
    private final static String NOTHING = " ";
    //private static final NumberFormat _decimal = new DecimalFormat();
    //private static final String _maxColumn;

    static {
        char[] maxColumn = new char[COL_LENGTH];
        //_decimal.setMaximumFractionDigits(DECIMAL_MAX);
        //_decimal.setMinimumFractionDigits(DECIMAL_MIN);
        Arrays.fill(maxColumn, ' ');
        //_maxColumn = new String(maxColumn);
    }

    /**
     * constructor
     */
    private IpacTableWriter() { /*is never called*/
    }

    /**
     * save the catalogs to a file
     *
     * @param file the file name to be saved
     * @param dataGroup data group
     * @throws IOException on error
     */
    public static void save(File file, DataGroup dataGroup)
        throws IOException {
        PrintWriter out = null;
        try {
            out = new PrintWriter(new BufferedWriter(new FileWriter(file)));
            save(out, dataGroup, false);
        } finally {
            if (out != null) out.close();
        }
    }

    /**
     * save the catalogs to a stream, stream is not closed
     *
     * @param stream the output stream to write to
     * @param dataGroup data group
     * @throws IOException on error
     */
    public static void save(OutputStream stream, DataGroup dataGroup)
            throws IOException {
        save(new PrintWriter(stream), dataGroup, false);
    }

    /**
     * save the catalogs to a stream, stream is not closed
     *
     * @param stream the output stream to write to
     * @param dataGroup data group
     * @param ignoreSysMeta ignore meta use by system.
     * @throws IOException on error
     */
    public static void save(OutputStream stream, DataGroup dataGroup, boolean ignoreSysMeta)
            throws IOException {
        save(new PrintWriter(stream), dataGroup, ignoreSysMeta);
    }

    private static void save(PrintWriter out, DataGroup dataGroup, boolean ignoreSysMeta) throws IOException {
        List<DataType> headers = Arrays.asList(dataGroup.getDataDefinitions());
        int totalRow = dataGroup.size();

        if (ignoreSysMeta) {
            // this should return only visible columns
            headers = headers.stream().filter((dt) -> IpacTableUtil.isVisible(dataGroup, dt)).collect(Collectors.toList());
        }

        IpacTableUtil.writeAttributes(out, dataGroup.getKeywords(), ignoreSysMeta);
        IpacTableUtil.writeHeader(out, headers);

        for (int i = 0; i < totalRow; i++) {
            IpacTableUtil.writeRow(out, headers, dataGroup.get(i));
        }
        out.flush();
    }


    // ============================================================
    // ----------------------------------- Private Methods ---------------------------------------
    // ============================================================

}

