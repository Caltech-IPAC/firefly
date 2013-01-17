package edu.caltech.ipac.firefly.data;

import com.google.gwt.regexp.shared.SplitResult;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.util.MinMax;

import com.google.gwt.regexp.shared.RegExp;

import java.util.ArrayList;

/**
 * @author tatianag
 *         $Id: SpecificPoints.java,v 1.3 2011/12/02 17:57:54 tatianag Exp $
 */
public class SpecificPoints {

    public static final String SERIALIZATION_KEY = "SPECIFIC_POINTS";

    private static String POINT_FIELDS_SEPARATOR = ";";
    private static String POINTS_SEPARATOR ="|";
    private static RegExp pointFieldsSeparatorRegExp = RegExp.compile("\\;");
    private static RegExp pointsSeparatorRegExp = RegExp.compile("\\|");

    ArrayList<Point> specificPoints;
    String desc;

    public SpecificPoints() {
        specificPoints = new ArrayList<Point>();
    }

    public void addPoint(int id, String label, String desc, MinMax x, MinMax y) {
        specificPoints.add(new Point(id, label, desc, x, y));
    }

    public void setDescription(String desc) {
        this.desc = desc;
    }

    public int getNumPoints() {
        return specificPoints.size();
    }

    public Point getPoint(int idx) {
        return specificPoints.get(idx);
    }

    public String getDescription() {
        return desc;
    }

    public static class Point {
        int id;  // points with the same id are rendered the same way
        String label;
        String desc;
        MinMax xMinMax;
        MinMax yMinMax;

        public Point(int id, String label, String desc, MinMax xMinMax, MinMax yMinMax) {
            this.id = id;
            this.label = label;
            this.desc = desc;
            this.xMinMax = xMinMax;
            this.yMinMax = yMinMax;
        }

        public int getId() { return id; }
        public String getLabel() { return label; }
        public String getDesc() { return desc; }
        public MinMax getXMinMax() { return xMinMax; }
        public MinMax getYMinMax() { return yMinMax; }
    }

    public String toString() {
        String str = desc;

        for (Point p : specificPoints) {
            str += POINTS_SEPARATOR;

            // Not sure how to make format work on both client and server
            // String xFormat = MinMax.getFormatString(p.xMinMax, 3);
            // String yFormat = MinMax.getFormatString(p.yMinMax, 3);
            str += p.id+POINT_FIELDS_SEPARATOR+
                   p.label+POINT_FIELDS_SEPARATOR+
                   p.desc+POINT_FIELDS_SEPARATOR+
                   p.xMinMax.getMin()+POINT_FIELDS_SEPARATOR+
                   p.xMinMax.getMax()+POINT_FIELDS_SEPARATOR+
                   p.xMinMax.getReference()+POINT_FIELDS_SEPARATOR+
                   p.yMinMax.getMin()+POINT_FIELDS_SEPARATOR+
                   p.yMinMax.getMax()+POINT_FIELDS_SEPARATOR+
                   p.yMinMax.getReference();
        }
        return str;
    }

    public static SpecificPoints parse(String fromStr) {
        SpecificPoints points = new SpecificPoints();
        SplitResult ptStrings = pointsSeparatorRegExp.split(fromStr);
        if (ptStrings.length() < 2) { return points; }
        points.setDescription(ptStrings.get(0));
        for (int i=1; i<ptStrings.length(); i++) {
            String ptString = ptStrings.get(i);
            SplitResult fldStrings = pointFieldsSeparatorRegExp.split(ptString);
            if (fldStrings.length() != 9) {
                // TODO how to let user know that something is wrong?
                GwtUtil.showDebugMsg("Unable to parse static point "+ptString);
                continue;
            }
            int id;
            String label, desc;
            double minX, maxX, minY, maxY, refX, refY;
            try {
                id = Integer.parseInt(fldStrings.get(0));
                label = fldStrings.get(1);
                desc = fldStrings.get(2);
                minX = Double.parseDouble(fldStrings.get(3));
                maxX = Double.parseDouble(fldStrings.get(4));
                refX = Double.parseDouble(fldStrings.get(5));
                minY = Double.parseDouble(fldStrings.get(6));
                maxY = Double.parseDouble(fldStrings.get(7));
                refY = Double.parseDouble(fldStrings.get(8));
            } catch (Exception e) {
                GwtUtil.showDebugMsg("Unable to parse specific point "+ptString+": "+e.getMessage());
                continue;
            }
            points.addPoint(id, label, desc, new MinMax(minX, maxX, refX), new MinMax(minY, maxY, refY));
        }
        return points;
    }

}

