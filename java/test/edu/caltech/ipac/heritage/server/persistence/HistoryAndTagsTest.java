package edu.caltech.ipac.heritage.server.persistence;

import edu.caltech.ipac.firefly.data.SearchInfo;
import edu.caltech.ipac.firefly.data.TagInfo;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.persistence.HistoryAndTagsDao;

import java.text.SimpleDateFormat;
import java.util.List;

/**
 * @author tatianag
 *         $Id: HistoryAndTagsTest.java,v 1.4 2010/11/24 00:17:24 tatianag Exp $
 */
public class HistoryAndTagsTest {

    public static void main(String [] args) {

        // loads App Properties
        System.out.println("Working dir: "+ ServerContext.getWorkingDir());

        try {
            HistoryAndTagsDao dao = new HistoryAndTagsDao();
            TagInfo ti = dao.addTag("6", "queryString", "query description");
            printTagInfo("After Insert", ti);
            TagInfo ti1 = dao.getTag(ti.getTagName());
            printTagInfo("After Get", ti1);
            List<TagInfo> lst = dao.getTags("6");
            for (TagInfo t : lst) {
                printTagInfo("List item: "+t.getTagName(), t);
            }

            dao.removeTag("6", ti1.getTagName());            

            SearchInfo s1 = dao.addSearchHistory("tatiana", "query string (1)", "description (1)", false);
            SearchInfo s2 = dao.addSearchHistory("tatiana", "query string (2)", "description (2)", true);
            List<SearchInfo> list = dao.getSearchHistory("tatiana");
            System.out.println("SEARCH HISTORY AFTER TWO ITEMS ADDED");
            for (SearchInfo s : list) {
                printSearchInfo("List item: "+s.getQueryID(), s);
            }
            dao.updateSearch(s1.getQueryID(), true, "new desc");
            dao.updateSearch(s2.getQueryID(), false, "new desc");
            printSearchInfo("first: ",dao.getSearch(s1.getQueryID()));
            printSearchInfo( "second: ", dao.getSearch(s2.getQueryID()));

            dao.removeSearch("tatiana", s1.getQueryID());
            list = dao.getSearchHistory("tatiana");
            System.out.println("SEARCH HISTORY AFTER FIRST QUERY REMOVED");
            for (SearchInfo s : list) {
                printSearchInfo("List item: "+s.getQueryID(), s);
            }

            System.exit(0);

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void printTagInfo(String preText, TagInfo ti) {
        SimpleDateFormat df = new SimpleDateFormat();
        StringBuffer sb = new StringBuffer();
        sb.append("\n").append(preText).append("\n");
        sb.append("  tag id : ").append(ti.getTagID()).append("\n");
        sb.append("  tag name : ").append(ti.getTagName()).append("\n");
        sb.append("  history token: ").append(ti.getHistoryToken()).append("\n");
        sb.append("  description : ").append(ti.getDescription()).append("\n");
        sb.append("  num hits : ").append(ti.getNumHits()).append("\n");
        sb.append("  time created : ").append(df.format(ti.getTimeCreated())).append("\n");
        if (ti.getTimeUsed()!=null) {
            sb.append("  time used : ").append(df.format(ti.getTimeUsed())).append("\n");
        }
        System.out.println(sb.toString());

    }


    public static void printSearchInfo(String preText, SearchInfo si) {
        SimpleDateFormat df = new SimpleDateFormat();
        StringBuffer sb = new StringBuffer();
        sb.append("\n").append(preText).append("\n");
        sb.append("  query id : ").append(si.getQueryID()).append("\n");
        sb.append("  login id : ").append(si.getLoginID()).append("\n");
        sb.append("  history token : ").append(si.getHistoryToken()).append("\n");
        sb.append("  description : ").append(si.getDescription()).append("\n");
        sb.append("  favorite : ").append(si.isFavorite()).append("\n");
        sb.append("  time added : ").append(df.format(si.getTimeAdded())).append("\n");
        System.out.println(sb.toString());

    }
}
