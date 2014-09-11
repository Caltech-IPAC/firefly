package edu.caltech.ipac.firefly.visualize.graph;

import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.util.MinMax;

import java.util.ArrayList;
import java.util.List;

/**
 * @author tatianag
 *         $Id: $
 */
public class Sampler {

    //static int NO_SAMPLE_LIMIT = 1000;
    static int NO_SAMPLE_LIMIT = 10000;

    float xyRatio = 1;
    int maxPoints = 3600;

    int numPointsInSample;
    int numPointsRepresented = 0;

    SamplePointGetter samplePointGetter;
    List<SamplePoint> sampledPoints;

    int minWeight=1, maxWeight=1;

    MinMax xMinMax;
    MinMax yMinMax;

    int xSampleBins = 0, ySampleBins = 0;  // will stay 0, if not sampled
    double xSampleBinSize = 0d, ySampleBinSize = 0d; // will stay 0d if not sampled

    Sampler(SamplePointGetter samplePointGetter) {
        this.samplePointGetter = samplePointGetter;
    }

    public void setXYRatio(float xyRatio) {
        if (xyRatio > 0) {
            this.xyRatio = xyRatio;
        }
    }

    public void setMaxPoints(int maxPoints) {
        this.maxPoints = maxPoints;
    }


    public List<Sampler.SamplePoint> sample(List<TableData.Row> rows) {

        ArrayList<SamplePoint> pointsToSample = new ArrayList<SamplePoint>();

        double xMin=Double.POSITIVE_INFINITY, xMax=Double.NEGATIVE_INFINITY, yMin=Double.POSITIVE_INFINITY, yMax=Double.NEGATIVE_INFINITY;

        SamplePoint sp;
        int rowIdx = 0;
        int weight;
        for (TableData.Row row : rows) {
            sp = samplePointGetter.getValue(rowIdx, row);
            if (sp != null) {
                if (sp.x < xMin) { xMin = sp.x; }
                if (sp.x > xMax) { xMax = sp.x; }
                if (sp.y < yMin) { yMin = sp.y; }
                if (sp.y > yMax) { yMax = sp.y; }

                pointsToSample.add(sp);

                weight = sp.getWeight();
                if (weight < minWeight) minWeight = weight;
                if (weight > maxWeight) maxWeight = weight;

                numPointsRepresented += weight;
            }
            rowIdx++;
        }
        // numPointsRepresented = pointsToSample.size();

        xMinMax = new MinMax(xMin, xMax);
        yMinMax = new MinMax(yMin, yMax);

        // 2000 cells nX=100, nY=20
        // 3600 cells nX=120, nY=30
        // 6400 cells nX =160, nY=40
        if (shouldSample(pointsToSample.size())) {
            CellsSampler cellsSampler = new CellsSampler(new MinMax(xMin, xMax), new MinMax(yMin, yMax),
                    xyRatio, maxPoints, pointsToSample);
            xSampleBins = cellsSampler.getNumXCells();
            ySampleBins = cellsSampler.getNumYCells();
            xSampleBinSize = cellsSampler.getXCellSize();
            ySampleBinSize = cellsSampler.getYCellSize();
            // when sampled, each point will represent more points than originally,
            // and the weight will change
            minWeight = cellsSampler.getMinWeight();
            maxWeight = cellsSampler.getMaxWeight();
            sampledPoints = cellsSampler.getSamplePoints();
        } else {
            sampledPoints = pointsToSample;
        }
        numPointsInSample = sampledPoints.size();

        // no need to sort, since we don't search sample points anymore
        // sort sample points by row id
        //Collections.sort(sampledPoints, new Comparator<SamplePoint>() {
        //    public int compare(Sampler.SamplePoint p1, Sampler.SamplePoint p2) {
        //        return new Integer(p1.getRowIdx()).compareTo(p2.getRowIdx());
        //    }
        //});

        return sampledPoints;
    }

    public int getNumPointsInSample() { return sampledPoints.size(); }
    public int getNumPointsRepresented() { return numPointsRepresented; }

    public MinMax getXMinMax() { return xMinMax; }
    public MinMax getYMinMax() { return yMinMax; }

    public int getMinWeight() { return minWeight; }
    public int getMaxWeight() { return maxWeight; }

    public int getXSampleBins() { return xSampleBins; }
    public int getYSampleBins() { return ySampleBins; }

    public double getXSampleBinSize() { return xSampleBinSize; }
    public double getYSampleBinSize() { return ySampleBinSize; }


    public static boolean shouldSample(int numRows) {
        return (numRows > NO_SAMPLE_LIMIT);
    }

    public static class SamplePoint {
        double x;
        double y;
        int rowIdx;
        List<Integer> representedRows; // indexes of represented rows

        public SamplePoint(double x, double y, int rowIdx) {
            this.x = x;
            this.y = y;
            this.rowIdx = rowIdx;
        }

        public int getRowIdx() { return rowIdx; }
        public double getX() { return x; }
        public double getY() { return y; }

        public void setRepresentedRows(List<Integer> representedRows) {
            this.representedRows = representedRows;
        }

        public List<Integer> getRepresentedRows() { return representedRows; }

        // index of the representative point row in the full table
        public int getFullTableRowIdx() { return rowIdx; }

        // weight of point represented by rowIdx
        public int getWeight() { return representedRows == null ? 1 : representedRows.size(); }

        public void adjustXY(double newX, double newY) {
            x = newX;
            y = newY;
        }
    }

    public static class SamplePointInDecimatedTable extends SamplePoint {

        int fullTableRowIdx;
        int weight;

        public SamplePointInDecimatedTable(double x, double y, int rowIdx, int fullTableRowIdx, int weight) {
            super(x, y, rowIdx);
            this.fullTableRowIdx = fullTableRowIdx;
            this.weight = weight;
        }

        /**
         * For decimated tables when setting represented rows,
         * we need to provide new weight
         * @param representedRows represented rows
         * @param newWeight combined weight of all represented rows
         */
        public void setRepresentedRows(List<Integer> representedRows, int newWeight) {
            setRepresentedRows(representedRows);
            weight=newWeight;
        }


        @Override
        public int getFullTableRowIdx() {
            return fullTableRowIdx;
        }

        @Override
        public int getWeight() {
            // the weight includes the weights of all represented points
            return weight;
        }

    }

    public static interface SamplePointGetter {
        SamplePoint getValue(int rowIdx, TableData.Row row);
    }
}
