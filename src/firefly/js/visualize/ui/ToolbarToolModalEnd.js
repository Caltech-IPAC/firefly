import {dispatchComponentStateChange, getComponentState} from '../../core/ComponentCntlr.js';

/**
 * @typedef {Object} ModalEndInfo
 * @prop {Function} closeLayer
 * @prop {String} closeText
 * @prop {String} key
 * @prop {boolean} offOnNewPlot
 * @prop {boolean} callIfReplaced
 * @prop {Array.<String>|undefined} plotidAry - array of ids this ModalEndInfo applies to, if undefined then all
 */


export const emptyModalEndInfo = {
    closeLayer: undefined,
    closeText: undefined,
    key: undefined,
    offOnNewPlot: false,
    callIfReplaced: false,
    plotIdAry: undefined
};

/**
 * close any toolbar layers
 * @param exceptionKeys
 */
export function closeToolbarModalLayers(exceptionKeys = []) {
    const modalEndInfo = getModalEndInfo();
    if (!modalEndInfo) return;
    const {key, closeLayer} = modalEndInfo;
    if (exceptionKeys.find((eK) => eK === key)) return;
    closeLayer?.();
}


export function createModalEndUI(modalEndInfo, plotId) {
    const {closeLayer, closeText, plotIdAry}= modalEndInfo ?? {};
    if (!closeLayer || !closeText || !plotId) return false;
    return (!plotIdAry?.length || plotIdAry.includes(plotId));
}

export const getModalEndInfo= () => getComponentState('ModalEndInfo', emptyModalEndInfo);

export const clearModalEndInfo= () => setModalEndInfo({});

export function setModalEndInfo(info) {
    const oldInfo= getModalEndInfo();
    dispatchComponentStateChange('ModalEndInfo',  {...emptyModalEndInfo, ...info});
    if ((!oldInfo.key && !info.key) || oldInfo.key!==info.key) return;
    if (oldInfo?.callIfReplaced && info.key!==oldInfo.key) oldInfo?.closeLayer?.(info.key);
}
