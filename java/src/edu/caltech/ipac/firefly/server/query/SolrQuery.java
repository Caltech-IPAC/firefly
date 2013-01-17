package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.solrclient.QueryParams;
import edu.caltech.ipac.solrclient.SolrQueryExec;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.StringKey;
import org.apache.solr.client.solrj.SolrServerException;

import java.io.File;
import java.io.IOException;



/**
 * Date: Jun 5, 2009
 *
 * @author loi
 * @version $Id: SolrQuery.java,v 1.4 2010/04/13 16:59:25 roby Exp $
 */
abstract public class SolrQuery extends IpacTablePartProcessor {
    private static final String SOLR_SERVR_URL = AppProperties.getProperty("solr.server.url");

    protected File loadDataFile(TableServerRequest request) throws IOException, DataAccessException {

        Cache cache = CacheManager.getCache(Cache.TYPE_TEMP_FILE);
        StringKey key = new StringKey(SolrQuery.class.getSimpleName(), getUniqueID(request));
        File f = (File) cache.get(key);
        if (f == null) {
            QueryParams queryParams = getQueryParams(request);

            f = File.createTempFile(getFilePrefix(request), ".tbl", ServerContext.getTempWorkDir());
            SolrQueryExec query = new SolrQueryExec(SOLR_SERVR_URL);
            try {
                query.query(queryParams, f);
            } catch (SolrServerException e) {
                throw new DataAccessException(e);
            }

            cache.put(key, f);
        }
        return f;
    }

    abstract protected QueryParams getQueryParams(TableServerRequest request);

    public static QueryParams makeDefQueryParams(String queryString) {
        return new QueryParams(queryString);
    }
}

/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
* OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS,
* HOWEVER USED.
*
* IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE
* FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL
* OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO
* PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE
* ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
*
* RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE
* AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR
* ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE
* OF THE SOFTWARE.
*/