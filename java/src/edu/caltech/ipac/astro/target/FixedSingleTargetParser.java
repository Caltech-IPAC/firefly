package edu.caltech.ipac.astro.target;

import edu.caltech.ipac.astro.net.NedParams;
import edu.caltech.ipac.astro.net.SimbadParams;
import edu.caltech.ipac.astro.net.TargetNetwork;
import edu.caltech.ipac.util.FileReadStatusException;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.download.FailedRequestException;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;


/**
 * This class was extracted from OpenFixedSingleTarget to remove its dependencies on Java Swing.
 *
 * @author Carlos Campos, Xiuqin Wu, Trey Roby
 */

public class FixedSingleTargetParser {

    public static final int MAX_NUMBER_OF_ERRORS = 20;
    public static final String LINE_FEED = "\r\n";
    public static String FORMAT_ERROR = "Format error in the following line:" +
            LINE_FEED;

    public static final String COORD_SYSTEM = "COORD_SYSTEM";
     public static final String EQUINOX = "EQUINOX";
     public static final String RESOLVER = "NAME-RESOLVER";

     public static final String NED_RESOLVER = "NED";
     public static final String SIMBAD_RESOLVER = "Simbad";
     public static final String ATTRIB_SEP = "=";


    private String _coord_system;
    private String _equinox;
    private String _resolver;

    private int _errorsFound = 0;
    private StringBuffer _errorBuffer;
    private SimpleParser _parser;

    public FixedSingleTargetParser() { }


    public void parseTargets(BufferedReader in, TargetList targets)
            throws TargetParseException {

        String[] elements = null;
        String nextTarget = null;

        TargetFixedSingle target;
        PositionJ2000 tgtPosition;
        UserPosition userPosition;
        ProperMotion pm = null;
        float lonpm = 0.0F, latpm = 0.0F;
        CoordinateSys coordSys = CoordinateSys.EQ_J2000;
        float epoch = 2000.0F;

        _errorsFound = 0;
        _errorBuffer = new StringBuffer();
        _parser = new SimpleParser(in);
        _coord_system = CoordinateSys.EQUATORIAL_NAME;
        _equinox = "J2000";
        _resolver = SIMBAD_RESOLVER;


        getHeader();

        if (_errorsFound > 0) {
            throw new TargetParseException(_errorBuffer.toString());
        } else {
            //System.out.println("coord: " + coord_system + ", equinox" + equinox);
            try {
                nextTarget = _parser.getNewLine();
            } catch (IOException e) {
            }

            // when nextTarget == null, it reaches the end of file
            while (nextTarget != null && _errorsFound < MAX_NUMBER_OF_ERRORS) {
                if (nextTarget.length() >= 2) {
                    try {
                        nextTarget= StringUtils.polishString(nextTarget);
                        elements = getElements(nextTarget);
                        target = new TargetFixedSingle();
                        target.setName(elements[0]);
                        coordSys = TargetUtil.makeCoordSys(_coord_system,
                                _equinox);

                        boolean correctFloatFormat = true;
                        Float fl = null;

                        if (isValidData(elements[3])) {
                            try {
                                fl = new Float(elements[3]);
                            } catch (NumberFormatException nfe) {
                                correctFloatFormat = false;
                            }
                        }

                        if (correctFloatFormat && isValidData(elements[3]) &&
                                (fl.floatValue() > 1899)) {
                            epoch = fl.floatValue();
                            //don't do anything else
                        } else {

                            if (isValidData(elements[3]) && isValidData(elements[4])) {
                                lonpm = Float.parseFloat(elements[3]);
                                latpm = Float.parseFloat(elements[4]);
                                pm = new ProperMotion(lonpm, latpm);
                            }
                            if (isValidData(elements[5]))
                                epoch = _parser.getFloat(elements[5]);
                        }

                        if (isValidData(elements[0]) && !isValidData(elements[2])) {
                            String resolveType = _resolver;
                            if (isValidData(elements[1])) resolveType = elements[1];
                            PositionAttributePair pap = resolveName(elements[0], resolveType);
                            tgtPosition = pap.position;

                        } else {
                            userPosition = new UserPosition(elements[1],
                                    elements[2],
                                    pm, coordSys, epoch);
                            tgtPosition = new PositionJ2000(userPosition);
                        }


                        target.setPosition(tgtPosition);


                        targets.addTarget(target);
                    } catch (Exception e) {
                        addError(e.getMessage());
                    }
                    if (_errorsFound > MAX_NUMBER_OF_ERRORS) {
                        addError("Too many errors: " + MAX_NUMBER_OF_ERRORS +
                                " errors reached.");
                        break;
                    }
                }

                try {
                    nextTarget = _parser.getNewLine();
                } catch (IOException e) {
                }
                ;
            }

            if (_errorsFound > 0) {
                throw new TargetParseException(_errorBuffer.toString());
            }
        }
    }

    protected void getHeader() {
        try {
            _coord_system = _parser.GetKeywordValue(COORD_SYSTEM);
            if (!_coord_system.equalsIgnoreCase(CoordinateSys.EQUATORIAL_NAME) &&
                    !_coord_system.equalsIgnoreCase(CoordinateSys.ECLIPTIC_NAME) &&
                    !_coord_system.equalsIgnoreCase(CoordinateSys.GALACTIC_NAME)) {
                String err = "The keyword: " + COORD_SYSTEM + " may have one of" +
                        " three values: " +
                        CoordinateSys.EQUATORIAL_NAME + ", " +
                        CoordinateSys.ECLIPTIC_NAME + ", or " +
                        CoordinateSys.GALACTIC_NAME + ".";
                addError(err);
            }
        } catch (IOException per) { /* do nothing - optional line */ }

        try {
            _equinox = _parser.GetKeywordValue(EQUINOX);
            if (!_equinox.equalsIgnoreCase(CoordinateSys.J2000_DESC) &&
                    !_equinox.equalsIgnoreCase(CoordinateSys.B1950_DESC)) {
                String err = "The keyword: " + EQUINOX + " may have one of" +
                        " two values: " +
                        CoordinateSys.J2000_DESC + " or " +
                        CoordinateSys.B1950_DESC;
                addError(err);
            }
        } catch (IOException per) { /* do nothing - optional line */ }

        try {
            _resolver = _parser.GetKeywordValue(RESOLVER);
            if (!_resolver.equalsIgnoreCase(NED_RESOLVER) &&
                    !_resolver.equalsIgnoreCase(SIMBAD_RESOLVER)) {
                String err = "The keyword: " + RESOLVER + " may have one of" +
                        " two values: " +
                        NED_RESOLVER + " or " + SIMBAD_RESOLVER;
                addError(err);
            }
        } catch (IOException per) { /* do nothing - optional line */ }
    }

    protected void addError(String err) {
        _errorBuffer.append("-Line number: " + _parser.getLineCount() +
                LINE_FEED + " Message: " +
                err + LINE_FEED + LINE_FEED);
        _errorsFound++;

    }

    private boolean isValidData(String s) {
        return s != null && !isAnAttribute(s);
    }

    private boolean isAnAttribute(String s) {
        return s.indexOf(ATTRIB_SEP) > 0;
    }

    private PositionAttributePair resolveName(String name, String resolver)
            throws IOException,
                   FailedRequestException {
        PositionAttributePair retval = new PositionAttributePair();
        if (resolver.equalsIgnoreCase(NED_RESOLVER)) {
            NedParams params = new NedParams(name);
            NedAttribute na;
            na = TargetNetwork.getNedPosition(params);
            retval.position = na.getPosition();
            retval.attribute = na;
        } else if (resolver.equalsIgnoreCase(SIMBAD_RESOLVER)) {
            SimbadParams params = new SimbadParams(name);
            SimbadAttribute na;
            na = TargetNetwork.getSimbadPosition(params);
            retval.position = na.getPosition();
            retval.attribute = na;
        } else {
            throw new IOException("No name resolver of type:  " + resolver +
                    ". Name must be of type NED or Simbad");
        }
        return retval;
    }


    private class PositionAttributePair {
        public PositionJ2000 position;
        public TargetAttribute attribute;
    }


//====================================================================
//
//====================================================================

    protected int getNumberOfQuotes(String str) {
        int counter = 0;
        String sub = str;
        while (sub.indexOf("\"") != -1 && counter < 8) {
            counter++;
            sub = sub.substring(sub.indexOf("\"") + 1);
        }
        return counter;
    }

    protected String cleanUp(String line) {
        System.out.println("cleanUp.line: " + line);
        if (line == null)
            return line;

        int index1 = line.indexOf("#");
        int index2 = line.indexOf("!");
        int index;
        String clean = null;

        if (index1 == -1 && index2 == -1) {
            clean = line.trim();
        } else {
            index = index2;
            if (index1 != -1 && index2 != -1) {
                if (index1 < index2) {
                    index = index1;
                }
            } else if (index1 == -1) {
                index = index2;
            } else {
                index = index1;
            }
            clean = line.substring(0, index).trim();
        }
        return clean;
    }

    /**
     * parse the input line and put all elements in a array of String.
     * <p/>
     * To keep this method backward compatible, it will return an array of at least 8 elements.  If there is more than 8, it
     * will return an array of n elements.
     *
     * @param nextTarget the input line to parse
     * @throws edu.caltech.ipac.util.FileReadStatusException
     *
     */
    public String[] getElements(String nextTarget)
            throws FileReadStatusException {

        int minArySize = 8;  // set the minimum array size to return.

        if (nextTarget == null) {
            throw new FileReadStatusException("String is null");
        }
        ArrayList<String> strings = new ArrayList<String>();

        StringTokenizer st = new StringTokenizer(nextTarget);
        String buffer = null;
        int stringsCounter = 0;
        try {
            String chunk;
            int numberOfQuotes;
            while (st.hasMoreTokens() && stringsCounter < 8) {
                chunk = st.nextToken();
                if (chunk.indexOf("\"") == -1) {
                    strings.add(chunk);
/*
System.out.println("chunck: " + chunk);
System.out.println("getStrings: " + strings[0]);
System.out.println("stringsCounter: " + stringsCounter);
*/
                    continue;   // finished one element
                }

                numberOfQuotes = getNumberOfQuotes(chunk);
                if (numberOfQuotes > 2) {
                    throw new FileReadStatusException(FORMAT_ERROR + nextTarget);
                }
                // no more than 2 quotes
                if (!(chunk.startsWith("\""))) {
                    throw new FileReadStatusException(FORMAT_ERROR + nextTarget);
                }
                // the first character is first quote
                if (numberOfQuotes == 2) {
                    if (!chunk.endsWith("\"")) {
                        throw new FileReadStatusException(FORMAT_ERROR + nextTarget);
                    } else {
                        strings.add(chunk.substring(1, chunk.length() - 1));
                    }
                } else { // one quote
                    buffer = chunk.substring(1);
                    int panic = 0;
                    boolean completedQuotedToken = false;

                    while (st.hasMoreTokens() && !completedQuotedToken) {
                        panic++;
                        if (panic > 40) {
                            throw new FileReadStatusException(
                                    FORMAT_ERROR + nextTarget);
                        }
                        String chunk2 = st.nextToken();

                        if (chunk2.indexOf("\"") == -1) {
                            buffer = buffer + " " + chunk2;
                        } else {
                            if (getNumberOfQuotes(chunk2) > 1) {
                                throw new FileReadStatusException(
                                        FORMAT_ERROR + nextTarget);
                            }
                            // only one quote in chunk2
                            if (!chunk2.endsWith("\"")) {
                                throw new FileReadStatusException("numberof quotes: " +
                                        getNumberOfQuotes(chunk2) + ",chunk=" + chunk2 + ", "
                                        + FORMAT_ERROR + nextTarget);
                            }
                            // chunk2 ends with quote
                            buffer = buffer + " " +
                                    chunk2.substring(0, chunk2.length() - 1);
                            strings.add(buffer);
                            completedQuotedToken = true;
                        }
                    }  // while
                } // one quote
            }

            if (strings.size() < minArySize) {
                // if the return array size is less than the mininum, fill it with null
                for (int i = strings.size(); i < minArySize; i++) {
                    strings.add(null);
                }
            }

            return strings.toArray(new String[0]);
        } catch (NoSuchElementException e) {
            throw new FileReadStatusException("Target data is missing in line: " +
                    nextTarget);
        }
    }

    public static class TargetParseException extends Exception {

        public TargetParseException() {}

        public TargetParseException(String s) {
            super(s);
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

