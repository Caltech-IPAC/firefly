/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


export const MenuItemKeys= {
    fitsDownload : 'fitsDownload',
    imageSelect : 'imageSelect',
    lockImage: 'lockImage',
    zoomUp : 'zoomUp',
    zoomDown : 'zoomDown',
    zoomOriginal : 'zoomOriginal',
    zoomFit : 'zoomFit',
    zoomFill : 'zoomFill',
    colorTable : 'colorTable',
    stretchQuick : 'stretchQuick',
    rotate: 'rotate',
    rotateNorth: 'rotateNorth',
    flipImage: 'flipImage',
    centerPlotOnQuery : 'centerPlotOnQuery',
    selectArea: 'selectArea',
    distanceTool: 'distanceTool',
    markerToolDD : 'markerToolDD',
    northArrow : 'northArrow',
    grid : 'grid',
    ds9Region : 'ds9Region',
    maskOverlay : 'maskOverlay',
    layer : 'layer',
    irsaCatalog : 'irsaCatalog',
    restore : 'restore',
    lockRelated: 'lockRelated',
    fitsHeader: 'fitsHeader'
};

const defaultOff = [MenuItemKeys.lockImage, MenuItemKeys.irsaCatalog];


export const defMenuItemKeys= Object.keys(MenuItemKeys).reduce((obj,k) => {
    obj[k]= !defaultOff.includes(k);
    return obj;
},{});

