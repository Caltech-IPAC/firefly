package edu.caltech.ipac.firefly.visualize.graph;

import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.util.MinMax;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author tatianag
 *         $Id: $
 */
public class Sampler {

    static int NO_SAMPLE_LIMIT = 1000;

    int numPointsInSample;
    int numPointsRepresented;

    SamplePointGetter samplePointGetter;
    List<SamplePoint> sampledPoints;

    MinMax xMinMax;
    MinMax yMinMax;


    Sampler(SamplePointGetter samplePointGetter) {
        this.samplePointGetter = samplePointGetter;
    }

    public List<Sampler.SamplePoint> sample(List<TableData.Row> rows) {

        ArrayList<SamplePoint> pointsToSample = new ArrayList<SamplePoint>();

        double xMin=Double.POSITIVE_INFINITY, xMax=Double.NEGATIVE_INFINITY, yMin=Double.POSITIVE_INFINITY, yMax=Double.NEGATIVE_INFINITY;

        SamplePoint sp;
        for (TableData.Row row : rows) {
            sp = samplePointGetter.getValue(row);
            if (sp != null) {
                if (sp.x < xMin) { xMin = sp.x; }
                if (sp.x > xMax) { xMax = sp.x; }
                if (sp.y < yMin) { yMin = sp.y; }
                if (sp.y > yMax) { yMax = sp.y; }

                pointsToSample.add(sp);
            }
        }
        numPointsRepresented = pointsToSample.size();

        xMinMax = new MinMax(xMin, xMax);
        yMinMax = new MinMax(yMin, yMax);

        // 2000 cells nX=100, nY=20
        // 3600 cells nX=120, nY=30
        // 6400 cells nX =160, nY=40
        if (shouldSample(pointsToSample.size())) {
            CellsSampler cellsSampler = new CellsSampler(new MinMax(xMin, xMax), new MinMax(yMin, yMax),
                    120, 30, pointsToSample);
            sampledPoints = cellsSampler.getSamplePoints();
        } else {
            sampledPoints = pointsToSample;
        }
        numPointsInSample = sampledPoints.size();

        // sort sample points by row id
        Collections.sort(sampledPoints, new Comparator<SamplePoint>() {
            public int compare(Sampler.SamplePoint p1, Sampler.SamplePoint p2) {
                return new Integer(p1.getRow().getRowIdx()).compareTo(p2.getRow().getRowIdx());
            }
        });

        return sampledPoints;
    }

    public int getNumPointsInSample() { return sampledPoints.size(); }
    public int getNumPointsRepresented() { return numPointsRepresented; }

    public MinMax getXMinMax() { return xMinMax; }
    public MinMax getYMinMax() { return yMinMax; }

    public static boolean shouldSample(int numRows) {
        return (numRows > NO_SAMPLE_LIMIT);
    }

    public static class SamplePoint {
        double x;
        double y;
        TableData.Row row;
        List<Integer> representedRows; // indexes of represented rows

        SamplePoint(double x, double y, TableData.Row row) {
            this.x = x;
            this.y = y;
            this.row = row;
        }

        TableData.Row getRow() { return row; }
        double getX() { return x; }
        double getY() { return y; }
        void setRepresentedRows(List<Integer> representedRows) { this.representedRows = representedRows; }
        List<Integer> getRepresentedRows() { return representedRows; }
    }

    public static interface SamplePointGetter {
        SamplePoint getValue(TableData.Row row);
    }
}
