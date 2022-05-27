/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.server.visualize.fitseval;
/*
 * User: roby
 * Date: 7/5/18
 */


import edu.caltech.ipac.firefly.data.HasSizeOf;
import edu.caltech.ipac.firefly.data.RelatedData;
import edu.caltech.ipac.visualize.plot.plotdata.FitsRead;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Trey Roby
 */
public class FitsDataEval implements Serializable, HasSizeOf {

    private final FitsRead [] frAry;
    private final List<RelatedData>[] relatedDataAry;
    private final File hduUnCompressedFile;
    private long baseSize= 0;



    public FitsDataEval(FitsRead[] frAry, File hduCompressedFile) {
        this.frAry= frAry;
        relatedDataAry= new List[frAry.length];
        this.hduUnCompressedFile = hduCompressedFile;
    }

    public FitsRead[] getFitReadAry() { return frAry; }
    public List<RelatedData> getRelatedData(int imageIdx) { return relatedDataAry[imageIdx]; }
    public File getHduUnCompressedFile() { return hduUnCompressedFile; }

    public long getSizeOf() {
        long fitsTotal= Arrays.stream(this.frAry).map(FitsRead::getSizeOf).reduce(0L, Long::sum);
        if (baseSize==0) {
            baseSize= 8;
            if (hduUnCompressedFile!=null) baseSize+= hduUnCompressedFile.getAbsolutePath().length();
            baseSize+=Arrays.stream(relatedDataAry)
                    .map( rdList -> rdList==null ? 0 : rdList.stream().map(RelatedData::getSizeOf)
                            .reduce(0L, Long::sum))
                    .reduce(0L, Long::sum);

        }
        return baseSize+fitsTotal;
    }

    public void addRelatedData(int imageIdx, RelatedData rData) {
        if (relatedDataAry[imageIdx]==null) {
            relatedDataAry[imageIdx]= new ArrayList<>();
        }
        relatedDataAry[imageIdx].add(rData);
    }

    public void addAllRelatedData(int imageIdx, List<RelatedData> rDataList) {
        if (rDataList==null) return;
        for(RelatedData rd : rDataList) addRelatedData(imageIdx, rd);
    }

    public void addRelatedDataToAllImages(List<RelatedData> rDataList) {
        if (rDataList==null) return;
        for(int i=0; (i<relatedDataAry.length); i++) addAllRelatedData(i, rDataList);
    }

}
