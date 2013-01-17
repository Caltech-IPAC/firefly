package edu.caltech.ipac.vamp.server.persistence;

import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.query.SolrQuery;
import edu.caltech.ipac.solrclient.QueryParams;

import java.util.Arrays;

/**
 * Created by IntelliJ IDEA.
 * User: tlau
 * Date: Apr 20, 2010
 * Time: 12:29:20 PM
 * To change this template use File | Settings | File Templates.
 */
@SearchProcessorImpl(id ="avmSearch")
public class QueryAvmSearch extends SolrQuery {
    public static final String QUERY_STRING = "AvmSearch.field.search";


    protected QueryParams getQueryParams(TableServerRequest request) {
        String queryStr = request.getParam(QUERY_STRING);
        QueryParams params = SolrQuery.makeDefQueryParams(queryStr);
        params.setDoctype("vamp");
        params.setFieldBoostInfo(Arrays.asList("id^10", "avm_id^10", "headline^2.0",
                "title^3.0", "subject_name^2.0", "subject_cat^0.5", "description^1.0"));
        params.setQueryFields(Arrays.asList(
                "avm_id", "avm_type", "publisher", "publisher_id",
                "title", "headline", "subject_cat", "subject_name",
                "description", "image_product_quality",
                "instrument", "facility",
                "credit", "creator", "contact_name"));
        params.setHighlightFields(Arrays.asList(
                "avm_id", "avm_type", "publisher", "publisher_id",
                "title", "headline", "subject_cat", "subject_name",
                "description", "image_product_quality",
                "instrument", "facility",
                "credit", "creator", "contact_name"));
    //        params.setFragmentSize(200);
        return params;
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
