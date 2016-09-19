/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.astro;

import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.IpacTableUtil;
import edu.caltech.ipac.util.action.ClassProperties;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.Locale;

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
    private final static ClassProperties _prop =
            new ClassProperties(IpacTableWriter.class);
    private static final String LINE_SEP = "\n";

    public final static String BACKSLASH = "\\";

    // constants
    private final static int COL_LENGTH = _prop.getIntValue("column.length");
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
            save(out, dataGroup);
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
        save(new PrintWriter(stream), dataGroup);
    }

    private static void save(PrintWriter out, DataGroup dataGroup)
        throws IOException {
        List<DataType> headers = Arrays.asList(dataGroup.getDataDefinitions());
        int totalRow = dataGroup.size();

        IpacTableUtil.writeAttributes(out, dataGroup.getKeywords());
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

