/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.astro;

import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.IpacTableUtil;

import java.io.BufferedOutputStream;
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
            out = new PrintWriter(new BufferedWriter(new FileWriter(file), IpacTableUtil.FILE_IO_BUFFER_SIZE));
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
        save(stream, dataGroup, false);
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
        save(new PrintWriter(new BufferedOutputStream(stream, IpacTableUtil.FILE_IO_BUFFER_SIZE)), dataGroup, ignoreSysMeta);
    }

    private static void save(PrintWriter out, DataGroup dataGroup, boolean ignoreSysMeta) throws IOException {
        dataGroup.shrinkToFitData();
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

