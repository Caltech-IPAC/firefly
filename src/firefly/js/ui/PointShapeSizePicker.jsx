/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {useEffect, useState} from 'react';
import PropTypes from 'prop-types';
import {get} from 'lodash';
import {dispatchShowDialog} from '../core/ComponentCntlr.js';
import {PopupPanel} from './PopupPanel.jsx';
import {DrawSymbol} from '../visualize/draw/DrawSymbol.js';
import DrawUtil from '../visualize/draw/DrawUtil.js';
import {drawOnCanvas} from '../visualize/ui/DrawLayerUIComponents.jsx';
import {dispatchChangeDrawingDef, getDlAry} from '../visualize/DrawLayerCntlr.js';
import CompleteButton from './CompleteButton.jsx';
import DialogRootContainer from './DialogRootContainer.jsx';
import {RadioGroupInputFieldView} from './RadioGroupInputFieldView.jsx';
import {InputFieldView} from './InputFieldView.jsx';
import {SimpleCanvas} from '../visualize/draw/SimpleCanvas.jsx';
import {getDrawLayersByDisplayGroup} from '../visualize/PlotViewUtil.js';
import Color from '../util/Color.js';
import {HelpIcon} from '../ui/HelpIcon.jsx';
import {useStoreConnector} from 'firefly/ui/SimpleComponent';



const MINSIZE = 3;
const MAXSIZE = 100;
const popupIdBase = 'ShapePickerDialog';
const ARROW_UP = 38;
const ARROW_DOWN = 40;

let popupId;



function defaultUpdate(id, newDrawingDef, plotId, titleMatching) {
    dispatchChangeDrawingDef(id, newDrawingDef, plotId, titleMatching);
}

function defaultGetColor(drawLayer) {
    return get(drawLayer, ['drawingDef', 'color']);
}


/**
 * Show a dialog to change the shape of the symbol.  Most use cases which use normal draw layers
 * do not require the last 3 parameter. The last three are require if you are using this to modify a shape that is
 * not using the draw layers DrawingDef
 *
 * @param {DrawLayer} dl
 * @param {String} plotId
 * @param {DrawingDef} [drawingDef]
 * @param {Function} [update]
 * @param {Function} [getColor]
 */
export function showPointShapeSizePickerDialog(dl, plotId, drawingDef= undefined,
                                               update= defaultUpdate, getColor= defaultGetColor) {
    const {isPointData=false} = dl;

    popupId = popupIdBase; //keep one dialog
    const popup= isPointData ? (
        <PopupPanel title={'Symbol Picker - ' + dl.drawLayerId } >
            <ShapePicker
                drawingDef={drawingDef || dl.drawingDef}
                displayGroupId={dl.displayGroupId}
                plotId={plotId}
                update={update}
                getColor={getColor}
            />
        </PopupPanel>
    ) : null;

    if (popup) {
        DialogRootContainer.defineDialog(popupId, popup);
        setTimeout(() => dispatchShowDialog(popupId), 0);
    }
}

export function ShapePicker({drawingDef, displayGroupId, plotId, update, getColor}) {

    const storeUpdate = () => {   // color change externally
        const dl = getDrawLayersByDisplayGroup(getDlAry(), displayGroupId);
        const dlColor = getColor(dl);

        if (dlColor !== drawingDef.color) {
            setDrawingDef({...drawingDef, color: dlColor});
        }
    };

    useStoreConnector(() => storeUpdate()); //replaced flux.addListener()

    let setDrawingDef = () => {};
    [drawingDef, setDrawingDef] = useState(() => drawingDef);
    const  {symbol} = drawingDef;
    const {width} = DrawUtil.getDrawingSize(drawingDef.size, symbol);
    const [size, setSize] = useState(() => `${width}`);
    const [validSize, setValidSize] = useState(() => (width >= MINSIZE) && (width <= MAXSIZE));

    const updateSymbol = (ev) => {
        const value = get(ev, 'target.value');
        const symbol = value&&DrawSymbol[value];

        const symbolSize = DrawUtil.getSymbolSizeBasedOn(symbol, drawingDef);

        const newDD = (symbolSize === drawingDef.size) ? {...drawingDef, symbol} :
                                                         {...drawingDef, symbol, size: symbolSize};

        const dl = getDrawLayersByDisplayGroup(getDlAry(), displayGroupId);
        update(displayGroupId, newDD, plotId, dl.titleMatching);
        setDrawingDef(newDD);

    };

    const updateSize = (ev) => {
        const size = get(ev, 'target.value');
        let validSize = true;
        let isize;

        if (isNaN(parseFloat(size))) {
            validSize = false;
        } else {
            isize = Math.floor(parseFloat(size));
            validSize = size && (isize >= MINSIZE)&&(isize <= MAXSIZE);
        }

        if (!validSize) {
            setSize(size);
            setValidSize(validSize);
        } else {
            const symbolSize = DrawUtil.getSymbolSize(isize, isize, drawingDef.symbol);
            const newDD = {...drawingDef, size: symbolSize};

            setSize(isize);
            setValidSize(validSize);
            setDrawingDef(newDD);
            const dl = getDrawLayersByDisplayGroup(getDlAry(), displayGroupId);
            update(displayGroupId, newDD, plotId, dl.titleMatching);
        }
    };

    const onArrowDown = (ev) => {
        const isize = validSize && Math.floor(parseFloat(size));

        if (validSize) {
            if ((ev.keyCode === ARROW_UP) && (size < MAXSIZE)) { // arrow up
                setSize(`${isize + 1}`);
            } else if ((ev.keyCode === ARROW_DOWN) && (size > MINSIZE)) { // arrow down
                setSize(`${isize - 1}`);
            }
        }
    };

    const onArrowUp = (ev) => {

        if (validSize && ((ev.keyCode === ARROW_UP || ev.keyCode === ARROW_DOWN ))) { // arrow up or down
            const isize = Math.floor(parseFloat(size));
            const symbolSize = DrawUtil.getSymbolSize(isize, isize, drawingDef.symbol);
            const newDD = {...drawingDef, size: symbolSize};

            const dl = getDrawLayersByDisplayGroup(getDlAry(), displayGroupId);
            update(displayGroupId, newDD, plotId, dl.titleMatching);
            setDrawingDef(newDD);
        }
    };

    const updateShape = () => {

        if (!validSize) {
            return;
        }
        const isize = Math.floor(parseFloat(size));
        const symbolSize = DrawUtil.getSymbolSize(isize, isize, drawingDef.symbol);

        const dl = getDrawLayersByDisplayGroup(getDlAry(), displayGroupId);
        update(displayGroupId, {...drawingDef, symbol: drawingDef.symbol, size: symbolSize},
            plotId, dl.titleMatching);


    };

    const drawSymbol = (df, validSize, size) => {
        
        const maxSize = 30;
        let canvasSize = validSize && (Math.floor(parseFloat(size)) + 2);
        const bkColor = getBackgroundColor(df.color);
        if (size > maxSize) {
            canvasSize = validSize && Math.floor(parseFloat(maxSize)) + 2;
            const symbolSize = DrawUtil.getSymbolSize(maxSize, maxSize, drawingDef.symbol);
            df = {...drawingDef, size: symbolSize};
        }

        if (validSize) {

            if (size > maxSize) {

                return (
                    <div style={{display:'flex', alignItems:'center', width: canvasSize, height: canvasSize}}>
                        <SimpleCanvas width={canvasSize} height={canvasSize} backgroundColor={bkColor}
                                      drawIt={(c)=>drawOnCanvas(c, df, canvasSize, canvasSize)}/>
                        {/*<text style={{fontSize:`${10.5+parseInt(size/10)}px`}}>+</text>*/}
                    </div>);
            } else {
                return (

                    <div style={{display:'flex', width: canvasSize, height: canvasSize}}>

                        <SimpleCanvas width={canvasSize} height={canvasSize} backgroundColor={bkColor}
                                      drawIt={(c)=>drawOnCanvas(c, df, canvasSize, canvasSize)}/>
                    </div>);
            }
        }
    };

    const PointOptions = [ DrawSymbol.CIRCLE, DrawSymbol.SQUARE, DrawSymbol.DIAMOND,
        DrawSymbol.CROSS, DrawSymbol.X, DrawSymbol.ARROW, DrawSymbol.POINT_MARKER,
        DrawSymbol.BOXCIRCLE, DrawSymbol.DOT];
    const df = validSize&&drawingDef;
    const labelW = 70;
    const mLeft = 10;
    const bkColor = getBackgroundColor(drawingDef.color);
    const textColor = '#000000';
    const options = PointOptions.map((p) => {
                        return {value: p.key, label: drawShapeWithLabel(p, drawingDef, bkColor, textColor)};
                    });
    return (
        <div style={{width: 320}}>
            <div style={{margin: mLeft,
                         border: '1px solid rgba(0, 0, 0, 0.298039)',
                         borderRadius: 5,
                         padding: '10px 5px'
                         }}>
                <div style={{display: 'flex', marginLeft: mLeft}} >
                    <div style={{width: labelW, color: textColor}} title={'pick a symbol'}>Symbols:</div>
                    <RadioGroupInputFieldView
                                              onChange={updateSymbol}
                                              tooltip='available symbol shapes'
                                              options={options}
                                              value={drawingDef.symbol.key}
                                              alignment='vertical'/>
                </div>
                <div style={{marginLeft: mLeft, marginTop: mLeft, height: 26, display: 'flex', alignItems: 'center'}}>
                    <InputFieldView  label={'Symbol Size (px):'}
                                     labelStyle={{color: textColor}}
                                     labelWidth={labelW+30}
                                     valid={validSize}
                                     onChange={updateSize}
                                     onKeyDown={onArrowDown}
                                     onKeyUp={onArrowUp}
                                     value={size}
                                     tooltip={'enter the symbol size or use the arrow up (or down) key in the field to increase (or decrease) the size number '}
                                     type={'text'}
                                     placeholder={`size 3 < ${MAXSIZE}`}
                                     size={16}
                                     message={`invalid data entry, size is within 3 & ${MAXSIZE}`}/>
                    {validSize && drawSymbol(df, validSize, size)}
                </div>
                <div style={{marginLeft: mLeft, marginTop: mLeft, color: textColor}}>
                    <i>Try up/down arrow keys  </i>
                </div>
                <div style={{display:'flex'}}>
                    <HelpIcon
                        helpId={'visualization.imageoptions'}/>
                </div>
            </div>
            <div style={{marginBottom: 10, marginLeft: mLeft}} >
                <CompleteButton  dialogId={popupId}
                                 onSuccess={updateShape}
                                 text={'OK'}/>
            </div>
        </div>
    );
}

ShapePicker.propTypes= {
    drawingDef: PropTypes.object.isRequired,
    displayGroupId: PropTypes.string.isRequired,
    plotId: PropTypes.string.isRequired,
    update: PropTypes.func.isRequired,
    getColor: PropTypes.func.isRequired
};

function drawShapeWithLabel(pointObj, drawingDef, bkColor, textColor) {
    let   size = 16;
    let   symbolSize = DrawUtil.getSymbolSize(size, size, pointObj);
    const addSpace = 6;

    if (pointObj.key === 'DOT') {
        symbolSize /= 2;
    }

    const df = {...drawingDef, symbol: pointObj, size: symbolSize};

    size += 2;

    return (
            <div style={{display: 'inline-block', height: size+addSpace}}>
                <div style={{display: 'flex', position: 'relative', top: addSpace/2, alignItems: 'center'}}>
                    <div style={{height: size, width: size}}>
                        <SimpleCanvas width={size} height={size} backgroundColor={bkColor}
                                      drawIt={(c)=> drawOnCanvas(c, df, size, size)}/>
                    </div>
                    <div style={{height:size, marginLeft: 10, lineHeight: size+'px', textAlign: 'center', color: textColor}}>{pointObj.key}</div>
                </div>
            </div>
    );
}

/**
 * @summary get the background color which is either black/white or complementary color
 * @param color
 * @returns {*}
 */
function getBackgroundColor(color) {
    return Color.getBWBackground(color);
}


