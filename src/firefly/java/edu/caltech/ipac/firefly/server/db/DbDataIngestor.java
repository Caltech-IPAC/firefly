/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.server.db;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.TableUtil;
import edu.caltech.ipac.table.io.FITSTableReader;
import edu.caltech.ipac.table.io.IpacTableReader;
import edu.caltech.ipac.table.io.SpectrumMetaInspector;
import edu.caltech.ipac.table.io.TableParseHandler;
import edu.caltech.ipac.table.io.VoTableReader;

import java.io.File;
import java.io.IOException;

import static edu.caltech.ipac.firefly.data.TableServerRequest.TBL_INDEX;
import static edu.caltech.ipac.table.TableUtil.guessFormat;

/**
 * Date: 10/24/24
 *
 * @author loi
 * @version : $
 */
public class DbDataIngestor {

    public static FileInfo ingestData(TableServerRequest req, DbAdapter dbAdapter, File srcFile, DataGroup meta, DbAdapter.DataGroupSupplier dataGroupSupplier) throws DataAccessException {

        String source = srcFile.getAbsolutePath();
        boolean searchForSpectrum = SpectrumMetaInspector.hasSpectrumHint(req);

        TableUtil.Format format = null;
        try {
            format = DuckDbReadable.guessFileFormat(srcFile.getAbsolutePath());
            if (format == null) format = guessFormat(srcFile);
        } catch (IOException ignored) {}

        if (format == null) throw new DataAccessException("Unsupported format, file:" + source);

        if (!(dbAdapter instanceof DuckDbAdapter)) {
            // used to be the default, but is now only needed for testing
            return dataGroupSupplier != null ? dbAdapter.ingestData(dataGroupSupplier, dbAdapter.getDataTable()) : null;
        }

        int tblIdx = req.getIntParam(TBL_INDEX, 0);

        try {
            return switch (format) {
                case IPACTABLE -> ingestIpacTableDirectly(dbAdapter, srcFile, meta, searchForSpectrum);
                case VO_TABLE -> ingestVoTableDirectly(dbAdapter, source, meta, tblIdx, searchForSpectrum);
                case CSV, TSV, PARQUET -> DuckDbReadable.castInto(format, dbAdapter).ingestDataDirectly(source, meta);
                case FITS -> ingestFitsTable(req, dbAdapter, source, tblIdx);
                default -> throw new DataAccessException("Unsupported format, file:" + source);
            };
        } catch (IOException e) {
            throw new DataAccessException(e);
        }
    }

    static FileInfo ingestVoTableDirectly(DbAdapter dbAdapter, String source, DataGroup meta, int tblIdx, boolean searchForSpectrum) throws IOException {
        VoTableReader.parse(new TableParseHandler.DbIngest(dbAdapter, meta, searchForSpectrum), source, tblIdx);
        return new FileInfo(dbAdapter.getDbFile());
    }

    static FileInfo ingestIpacTableDirectly(DbAdapter dbAdapter, File source, DataGroup meta, boolean searchForSpectrum) throws IOException {
        IpacTableReader.parseTable(new TableParseHandler.DbIngest(dbAdapter, meta, searchForSpectrum), source);   // only the first table.
        return new FileInfo(dbAdapter.getDbFile());
    }

    static FileInfo ingestFitsTable(TableServerRequest req, DbAdapter dbAdapter, String source, int tableIndex) throws IOException, DataAccessException {
        var table = FITSTableReader.convertFitsToDataGroup(source, req, FITSTableReader.DEFAULT, tableIndex);
        dbAdapter.ingestData(() -> table, dbAdapter.getDataTable());
        return new FileInfo(dbAdapter.getDbFile());
    }


}
