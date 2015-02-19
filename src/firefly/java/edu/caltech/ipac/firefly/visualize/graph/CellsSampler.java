/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize.graph;

import com.google.gwt.user.client.Window;
import edu.caltech.ipac.firefly.util.MinMax;
import edu.caltech.ipac.util.decimate.DecimateKey;

import java.util.ArrayList;
import java.util.List;

/**
 * @author tatianag
 *         $Id: $
 */
public class CellsSampler {
    double xMin;
    double xMax;
    double yMin;
    double yMax;

    int nX;
    int nY;
    double xCellSize;
    double yCellSize;
    Cell[] cells;
    List<Sampler.SamplePoint> samplePoints;
    int minWeight=Integer.MAX_VALUE, maxWeight=Integer.MIN_VALUE;

    public CellsSampler (MinMax xMinMax, MinMax yMinMax, float xyRatio, int maxPoints, List<Sampler.SamplePoint> points) {
        this(xMinMax, yMinMax, (int)Math.round(Math.sqrt(maxPoints * xyRatio)),
                (int)Math.round(Math.sqrt(maxPoints/xyRatio)), points);

    }

    public CellsSampler (MinMax xMinMax, MinMax yMinMax, int nX, int nY, List<Sampler.SamplePoint> points) {
        xMin = xMinMax.getMin();
        xMax = xMinMax.getMax();
        yMin = yMinMax.getMin();
        yMax = yMinMax.getMax();
        this.nX = nX;
        this.nY = nY;
        // increase cell size a bit to include max values into grid
        xCellSize = Math.abs((xMax - xMin) / nX);
        xCellSize += xCellSize/1000.0/nX;
        yCellSize = Math.abs((yMax - yMin) / nY);
        yCellSize += yCellSize/1000.0/nY;
        DecimateKey decimateKey = new DecimateKey(xMin, yMin, nX, nY, xCellSize, yCellSize);
        computeSample(points, decimateKey);
    }

    public int getNumXCells() { return nX; }
    public int getNumYCells() { return nY; }

    public double getXCellSize() { return xCellSize; }
    public double getYCellSize() { return yCellSize; }

    // for now b  one point per cell
    void computeSample (List<Sampler.SamplePoint> points, DecimateKey decimateKey) {
        int cellIdx;
        cells = new Cell[(nX)*(nY)];
        for (Sampler.SamplePoint p : points) {
            double xval = p.getX();
            double yval = p.getY();
            cellIdx = decimateKey.getXIdx(xval)+nX*decimateKey.getYIdx(yval);
            // TODO remove this check
            if (cellIdx >= (nX)*(nY)) {
                Window.alert("Error During Sampling: cellIdx is " + cellIdx);
                samplePoints = points;
                return;
            }
            if (cells[cellIdx] == null) {
                cells[cellIdx] = new Cell(decimateKey.getCenterX(xval), decimateKey.getCenterY(yval));
            }
            cells[cellIdx].addPoint(p);
        }
        samplePoints = new ArrayList<Sampler.SamplePoint>();
        Sampler.SamplePoint sp;
        for (Cell c : cells) {
            if (c != null) {
                sp = c.getSamplePoint();
                int weight = c.getWeight();
                if (weight < minWeight) minWeight = weight;
                if (weight > maxWeight) maxWeight = weight;

                if (sp instanceof Sampler.SamplePointInDecimatedTable) {
                    ((Sampler.SamplePointInDecimatedTable)sp).setRepresentedRows(c.getRepresentedRowIdx(),weight);
                } else {
                    sp.setRepresentedRows(c.getRepresentedRowIdx());
                }

                samplePoints.add(sp);
            }
        }
    }

    int getMinWeight() {return minWeight; }
    int getMaxWeight() {return maxWeight; }


    List<Sampler.SamplePoint> getSamplePoints() {
        return samplePoints;
    }

    private static class Cell {
        //ArrayList<Sampler.SamplePoint> cellPoints;
        ArrayList<Integer> cellPointsRowsIdx;
        Sampler.SamplePoint randomPoint = null;
        Sampler.SamplePoint samplePoint = null;
        int weight = 0; // for decimated tables only (tells how many rows in full table point represents)
        double centerX, centerY;

        public Cell(double centerX, double centerY) {
            //cellPoints = new ArrayList<Sampler.SamplePoint>();
            this.cellPointsRowsIdx = new ArrayList<Integer>();
            this.centerX = centerX;
            this.centerY = centerY;
        }

        public void addPoint(Sampler.SamplePoint point) {
            //cellPoints.add(point);
            cellPointsRowsIdx.add(point.getRowIdx());
            if (randomPoint == null) {
                randomPoint = point;
            } else {
                if (Math.random() < 1d/(double)cellPointsRowsIdx.size()) {
                    randomPoint = point;
                }
            }
            weight += point.getWeight();
        }

        public Sampler.SamplePoint getSamplePoint() {
            if (samplePoint == null) {
                //samplePoint = cellPoints.get((int)(cellPoints.size()*Math.random()));
                samplePoint = randomPoint;
                if (cellPointsRowsIdx.size() > 1) {
                    // sample point now will have coordinates of the center
                    samplePoint.adjustXY(centerX, centerY);
                }
            }
            return samplePoint;
        }

        public ArrayList<Integer> getRepresentedRowIdx() {
            // not using binary search - no need to sort
            //Collections.sort(cellPointsRowsIdx);
            return cellPointsRowsIdx;
        }

        public int getWeight() { return weight; }
    }

}
