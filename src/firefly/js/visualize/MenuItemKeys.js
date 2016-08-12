/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {get} from 'lodash';

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
    flipImageY: 'flipImageY',
    recenter : 'recenter',
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

const defaultOff = [MenuItemKeys.lockImage, MenuItemKeys.irsaCatalog, MenuItemKeys.maskOverlay];


const tempMiKeys= Object.keys(MenuItemKeys).reduce((obj,k) => {
    obj[k]= !defaultOff.includes(k);
    return obj;
},{});


export const defMenuItemKeys= Object.assign({}, tempMiKeys, get(window.firefly, 'MenuItemKeys', {}));

