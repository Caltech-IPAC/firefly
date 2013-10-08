package edu.caltech.ipac.firefly.visualize.graph;

import com.google.gwt.user.client.Window;
import edu.caltech.ipac.firefly.util.MinMax;

import java.util.ArrayList;
import java.util.Collections;
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
    ArrayList<Sampler.SamplePoint> samplePoints;

    public CellsSampler (MinMax xMinMax, MinMax yMinMax, int nX, int nY, List<Sampler.SamplePoint> points) {
        xMin = xMinMax.getMin();
        xMax = xMinMax.getMax();
        yMin = yMinMax.getMin();
        yMax = yMinMax.getMax();
        this.nX = nX;
        this.nY = nY;
        // increase cell size a bit to include max values into grid
        xCellSize = Math.abs((xMax - xMin) / nX);
        xCellSize += xCellSize/100.0/nX;
        yCellSize = Math.abs((yMax - yMin) / nY);
        yCellSize += yCellSize/100.0/nY;
        computeSample(points);
    }

    int getCellIdx(Sampler.SamplePoint p) {
        int xIdx = (int)(Math.abs(p.getX() - xMin) / xCellSize);
        int yIdx = (int)(Math.abs(p.getY() - yMin) / yCellSize);
        return yIdx*nX+xIdx;
    }

    // for now – one point per cell
    void computeSample (List<Sampler.SamplePoint> points) {
        cells = new Cell[(nX)*(nY)];
        int cellIdx;
        for (Sampler.SamplePoint p : points) {
            cellIdx = getCellIdx(p);
            if (cellIdx >= (nX)*(nY)) {
                Window.alert("Error During Sampling: cellIdx is " + cellIdx);
            }
            if (cells[cellIdx] == null) { cells[cellIdx] = new Cell(); }
            cells[cellIdx].addPoint(p);
        }
        samplePoints = new ArrayList<Sampler.SamplePoint>();
        Sampler.SamplePoint sp;
        for (Cell c : cells) {
            if (c != null) {
                sp = c.getSamplePoint();
                sp.setRepresentedRows(c.getRepresentedRowIdx());
                samplePoints.add(sp);
            }
        }
    }


    List<Sampler.SamplePoint> getSamplePoints() {
        return samplePoints;
    }

    private static class Cell {
        ArrayList<Sampler.SamplePoint> cellPoints;
        ArrayList<Integer> cellPointsRowsIdx;
        Sampler.SamplePoint samplePoint;

        public Cell() {
            cellPoints = new ArrayList<Sampler.SamplePoint>();
            cellPointsRowsIdx = new ArrayList<Integer>();
        }

        public void addPoint(Sampler.SamplePoint point) {
            cellPoints.add(point);
            cellPointsRowsIdx.add(point.getRow().getRowIdx());
        }

        public Sampler.SamplePoint getSamplePoint() {
            if (samplePoint == null) {
                samplePoint = cellPoints.get((int)(cellPoints.size()*Math.random()));
            }
            return samplePoint;
        }

        public ArrayList<Integer> getRepresentedRowIdx() {
            Collections.sort(cellPointsRowsIdx);
            return cellPointsRowsIdx;
        }
    }

}
