/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.server.db;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.TableUtil;
import edu.caltech.ipac.table.io.VoTableHandler;
import edu.caltech.ipac.table.io.VoTableReader;

import java.io.IOException;
import java.util.Arrays;

import static edu.caltech.ipac.table.TableUtil.Format.*;
import static edu.caltech.ipac.table.TableUtil.Format.VO_TABLE;

/**
 * Date: 10/24/24
 *
 * @author loi
 * @version : $
 */
public class DbDataIngestor {

    public static FileInfo ingestData(TableUtil.Format format, DbAdapter dbAdapter, String source, DataGroup meta) throws DataAccessException {
        if (format == null) throw new DataAccessException("Unsupported format, file:" + source);

        return switch (format) {
            case VO_TABLE -> ingestVoTable(dbAdapter, source, meta);
            case CSV, TSV, PARQUET -> DuckDbReadable.castInto(format, dbAdapter).ingestDataDirectly(source, meta);
            default -> throw new DataAccessException("Unsupported format, file:" + source);
        };
    }

    public static FileInfo ingestVoTable(DbAdapter dbAdapter, String source, DataGroup meta) throws DataAccessException {
        try {
            VoTableReader.parse(new VoTableHandler.DbIngest(dbAdapter, meta), source, 0);   // only the first table.
        } catch (IOException e) {
            throw new DataAccessException(e);
        }
        return new FileInfo(dbAdapter.getDbFile());
    }

}
