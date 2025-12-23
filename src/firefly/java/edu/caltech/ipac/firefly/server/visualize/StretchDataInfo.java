package edu.caltech.ipac.firefly.server.visualize;

import edu.caltech.ipac.firefly.data.HasSizeOf;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.visualize.plot.RangeValues;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static edu.caltech.ipac.firefly.visualize.Band.BLUE;
import static edu.caltech.ipac.firefly.visualize.Band.GREEN;
import static edu.caltech.ipac.firefly.visualize.Band.RED;

/**
 *
 * Container for the byte stretch tile. Holds standard, 3-color, or mask tiles
 */
public class StretchDataInfo implements Serializable, HasSizeOf {
    private final Map<Band, List<byte[]>> byte1d;
    private final Map<Band, List<byte[]>> byte1dHalf;
    private final Map<Band, List<byte[]>> byte1dQuarter;
    private final long sizeByteFull;
    private final long sizeByteHalf;
    private final long sizeByteQuarter;
    private final long totalTiles;
    private final RangeValues[] rvAry;

    public StretchDataInfo(List<byte[]> byte1d, List<byte[]> byte1dHalf, List<byte[]> byte1dQuarter, RangeValues[] rvAry) {
        this(
                Collections.singletonMap(Band.NO_BAND, byte1d),
                Collections.singletonMap(Band.NO_BAND, byte1dHalf),
                Collections.singletonMap(Band.NO_BAND, byte1dQuarter),
                rvAry
        );
    }

    public StretchDataInfo(Map<Band, List<byte[]>> byte1d, Map<Band, List<byte[]>> byte1dHalf, Map<Band, List<byte[]>> byte1dQuarter, RangeValues[] rvAry) {
        this.byte1d = byte1d;
        this.byte1dHalf = byte1dHalf;
        this.byte1dQuarter = byte1dQuarter;
        this.rvAry = rvAry;
        sizeByteFull = getSizeOfAryList(byte1d);
        sizeByteHalf = getSizeOfAryList(byte1dHalf);
        sizeByteQuarter = getSizeOfAryList(byte1dQuarter);
        if (byte1d != null) totalTiles = byte1d.values().iterator().next().size();
        else if (byte1dHalf != null) totalTiles = byte1dHalf.values().iterator().next().size();
        else if (byte1dQuarter != null) totalTiles = byte1dQuarter.values().iterator().next().size();
        else totalTiles = 0;
    }

    private static long getSizeOfAryList(Map<Band, List<byte[]>> aryListMap) {
        if (aryListMap == null || aryListMap.isEmpty()) return 0;

        long sum = 0;
        for (var arrayList : aryListMap.values()) {
            if (arrayList != null && !arrayList.isEmpty()) {
                sum += arrayList.stream().map((a) -> a.length).mapToLong(Long::valueOf).sum();
            }
        }
        return sum;
    }

    public Map<Band, List<byte[]>> getFullData(DirectStretchUtils.CompressType ct) {
        return switch (ct) {
            case FULL -> byte1d;
            case QUARTER, QUARTER_HALF_FULL, QUARTER_HALF -> byte1dQuarter;
            case HALF, HALF_FULL -> byte1dHalf;
        };
    }

    public byte[] findData(DirectStretchUtils.CompressType ct, int tileNumber, Band band) {
        return switch (ct) {
            case FULL -> byte1d.get(band).get(tileNumber);
            case QUARTER, QUARTER_HALF_FULL, QUARTER_HALF -> byte1dQuarter.get(band).get(tileNumber);
            case HALF, HALF_FULL -> byte1dHalf.get(band).get(tileNumber);
        };
    }

    public boolean hasCompressType(DirectStretchUtils.CompressType ct) {
        return switch (ct) {
            case FULL -> byte1d != null;
            case QUARTER, QUARTER_HALF_FULL, QUARTER_HALF -> byte1dQuarter != null;
            case HALF, HALF_FULL -> byte1dHalf != null;
        };
    }

    public static String getMostCompressedDescription(DirectStretchUtils.CompressType ct) {
        return switch (ct) {
            case FULL -> "Full";
            case QUARTER, QUARTER_HALF_FULL, QUARTER_HALF -> "Quarter";
            case HALF, HALF_FULL -> "Half";
        };
    }

    public boolean isRangeValuesMatching(PlotState state) {
        if (!state.isThreeColor()) {
            return rvAry.length == 1 && rvAry[0].toString().equals(state.getRangeValues().toString());
        }
        for (Band band : new Band[]{RED, GREEN, BLUE}) {
            if (state.isBandUsed(band)) {
                int idx = band.getIdx();
                if (rvAry[idx] == null || !rvAry[idx].toString().equals(state.getRangeValues(band).toString())) {
                    return false;
                }
            }
        }
        return true;
    }

    public RangeValues[] getRangeValues() {
        return rvAry;
    }

    public long getTotalTiles() {
        return totalTiles;
    }

    @Override
    public long getSizeOf() {
        return (rvAry.length * 80L) + sizeByteFull + sizeByteHalf + sizeByteQuarter;
    }
}
