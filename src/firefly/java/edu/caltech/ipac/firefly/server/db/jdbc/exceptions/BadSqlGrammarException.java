/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.db.jdbc.exceptions;

import java.sql.SQLException;

/**
 * Wraps a SQLException caused by bad SQL grammar (syntax errors, missing objects, etc.).
 * Message format is compatible with the regex patterns used in HsqlDbAdapter.handleSqlExp.
 */
public class BadSqlGrammarException extends NestedRuntimeException {

    public BadSqlGrammarException(String sql, SQLException cause) {
        super("bad SQL grammar [%s]; nested exception is %s: %s"
                .formatted(sql, cause.getClass().getName(), cause.getMessage()), cause);
    }
}
