package edu.caltech.ipac.util;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import java.io.IOException;
import java.io.Reader;

/**
 * Date: Apr 28, 2011
 *
 * @author loi
 * @version $Id: HtmlParser.java,v 1.1 2011/04/30 00:01:46 loi Exp $
 */
public class HtmlParser {
    /**
     * A simple convenience method to parse the whole document for the value of a given tag.
     * @param r
     * @param tag
     * @return
     */
    public static String parse(Reader r, HTML.Tag tag) {

        HTMLEditorKit.Parser parser = new HEK().getParser();
        ValueListCallback cb = new ValueListCallback(tag);
        try {
            parser.parse(r, cb, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return cb.getValues();
    }

    public static void parse(Reader r, HTMLEditorKit.ParserCallback cb, boolean ignoreCharSet) throws IOException {
        HTMLEditorKit.Parser parser = new HEK().getParser();
        parser.parse(r, cb, ignoreCharSet);
    }

    private static class HEK extends HTMLEditorKit {
        @Override
        protected Parser getParser() {
            return super.getParser();
        }
    }

    public static class ValueListCallback extends HTMLEditorKit.ParserCallback {
        private StringBuffer values = new StringBuffer();
        private boolean doAcceptText = false;
        private HTML.Tag matchTag = null;

        public ValueListCallback(HTML.Tag matchTag) {
            this.matchTag = matchTag;
        }

        public String getValues() {
            return values.toString();
        }

        @Override
        public void handleText(char[] chars, int i) {
            if (chars != null) {
                if (doAcceptText) {
                    values.append(new String(chars).trim());
                }
            }
        }

        @Override
        public void handleStartTag(HTML.Tag tag, MutableAttributeSet mutableAttributeSet, int i) {
            if (tag.equals(matchTag)) {
                doAcceptText = true;
            }
        }

        @Override
        public void handleEndTag(HTML.Tag tag, int i) {
            if (tag.equals(matchTag)) {
                doAcceptText = false;
            }
        }
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
