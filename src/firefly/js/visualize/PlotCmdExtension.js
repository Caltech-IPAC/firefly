/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/**
 * @author Trey Roby
 */

export const AREA_SELECT= 'AREA_SELECT';
export const LINE_SELECT= 'LINE_SELECT';
export const POINT= 'POINT';
export const NONE= 'NONE';

export class PlotCmdExtension {
    constructor(id, plotId, extType, imageUrl, title, toolTip, callback=null) {
        this.id = id;
        this.plotId = plotId;
        this.imageUrl = imageUrl;
        this.title = title;
        this.toolTip = toolTip;
        this.extType= extType;
        this.callback= callback;
    }

    //getId() { return this.id; }
    //getImageUrl() { return this.imageUrl; }
    //getTitle() { return this.title; }
    //getToolTip() { return this.toolTip; }
    //getExtType() { return extType; }
}
