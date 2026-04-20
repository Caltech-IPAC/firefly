/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.db.jdbc.exceptions;

import java.sql.SQLException;

/**
 * Wraps a SQLException caused by a data integrity constraint violation.
 * Message format is compatible with the regex patterns used in HsqlDbAdapter.handleSqlExp.
 */
public class DataIntegrityViolationException extends NestedRuntimeException {

    public DataIntegrityViolationException(String sql, SQLException cause) {
        super("data integrity violation [%s]; nested exception is %s: %s"
                .formatted(sql, cause.getClass().getName(), cause.getMessage()), cause);
    }
}
