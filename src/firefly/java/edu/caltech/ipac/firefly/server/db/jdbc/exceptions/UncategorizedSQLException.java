/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.db.jdbc.exceptions;

import java.sql.SQLException;

/**
 * Wraps a SQLException that does not fit a more specific category.
 */
public class UncategorizedSQLException extends NestedRuntimeException {

    private final SQLException sqlException;

    public UncategorizedSQLException(String sql, SQLException cause) {
        super("Uncategorized SQL [%s]; nested exception is %s: %s"
                .formatted(sql, cause.getClass().getName(), cause.getMessage()), cause);
        this.sqlException = cause;
    }

    public SQLException getSQLException() {
        return sqlException;
    }
}
