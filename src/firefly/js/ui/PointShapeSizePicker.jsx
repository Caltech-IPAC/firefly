/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Box, Stack, Typography} from '@mui/joy';
import React, {useEffect, useState} from 'react';
import PropTypes from 'prop-types';
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

const defaultGetColor= (drawLayer) => drawLayer?.drawingDef?.color;


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
        <PopupPanel title={'Symbol Picker'} >
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
        return getColor(dl);
    };

    const dlColor = useStoreConnector(() => storeUpdate());
    useEffect(() => {
        if (dlColor !== drawingDef.color) {
            setDrawingDef({...drawingDef, color: dlColor});
        }
    }, [dlColor]);

    let setDrawingDef = () => {};
    [drawingDef, setDrawingDef] = useState(() => drawingDef);
    const  {symbol} = drawingDef;
    const {width} = DrawUtil.getDrawingSize(drawingDef.size, symbol);
    const [size, setSize] = useState(() => `${width}`);
    const [validSize, setValidSize] = useState(() => (width >= MINSIZE) && (width <= MAXSIZE));

    const updateSymbol = (ev) => {
        const value = ev?.target?.value;
        const symbol = value&&DrawSymbol[value];

        const symbolSize = DrawUtil.getSymbolSizeBasedOn(symbol, drawingDef);

        const newDD = (symbolSize === drawingDef.size) ? {...drawingDef, symbol} :
                                                         {...drawingDef, symbol, size: symbolSize};

        const dl = getDrawLayersByDisplayGroup(getDlAry(), displayGroupId);
        update(displayGroupId, newDD, plotId, dl.titleMatching);
        setDrawingDef(newDD);

    };

    const updateSize = (ev) => {
        const size = ev?.target?.value;
        let validSize;
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
        if (!validSize) return;
        return (
            <SimpleCanvas width={canvasSize} height={canvasSize} backgroundColor={bkColor}
                          drawIt={(c)=>drawOnCanvas(c, df, canvasSize, canvasSize)}/>
        );
    };

    const PointOptions = [ DrawSymbol.CIRCLE, DrawSymbol.SQUARE, DrawSymbol.DIAMOND,
        DrawSymbol.CROSS, DrawSymbol.X, DrawSymbol.ARROW, DrawSymbol.POINT_MARKER,
        DrawSymbol.BOXCIRCLE, DrawSymbol.DOT];
    const df = validSize&&drawingDef;
    const bkColor = getBackgroundColor(drawingDef.color);
    const options = PointOptions.map((p) => {
                        return {value: p.key, label: drawShapeWithLabel(p, drawingDef, bkColor)};
                    });
    return (
        <Box sx={{width: 320}}>
            <Stack spacing={2}>
                <RadioGroupInputFieldView
                    sx={{alignSelf:'center'}}
                    onChange={updateSymbol}
                    tooltip='available symbol shapes'
                    options={options}
                    value={drawingDef.symbol.key}
                    orientation='vertical'/>
                <InputFieldView  label={'Symbol Size (px):'}
                                 valid={validSize}
                                 type='number'
                                 onChange={updateSize}
                                 onKeyDown={onArrowDown}
                                 onKeyUp={onArrowUp}
                                 endDecorator={validSize ? drawSymbol(df, validSize, size) : undefined}
                                 value={size}
                                 tooltip={'enter the symbol size or use the arrow up (or down) key in the field to increase (or decrease) the size number '}
                                 placeholder={`size 3 < ${MAXSIZE}`}
                                 size={16}
                                 message={`invalid data entry, size is within 3 & ${MAXSIZE}`}/>
                <Typography sx={{alignSelf:'center'}} >Try up/down arrow keys </Typography>
            </Stack>
           <Stack {...{direction:'row', m:1, justifyContent:'space-between'}}>
                <CompleteButton  dialogId={popupId} onSuccess={updateShape} text='Close'/>
                <HelpIcon helpId={'visualization.imageoptions'}/>
            </Stack>
        </Box>
    );
}

ShapePicker.propTypes= {
    drawingDef: PropTypes.object.isRequired,
    displayGroupId: PropTypes.string.isRequired,
    plotId: PropTypes.string.isRequired,
    update: PropTypes.func.isRequired,
    getColor: PropTypes.func.isRequired
};

function drawShapeWithLabel(pointObj, drawingDef, bkColor) {
    const size = 16;
    const canvasSize=size+2;
    let symbolSize = DrawUtil.getSymbolSize(size, size, pointObj);
    if (pointObj.key === 'DOT') symbolSize /= 2;
    const df = {...drawingDef, symbol: pointObj, size: symbolSize};

    return (
        <Stack {...{direction: 'row', spacing:1, alignItems:'center'}}>
            <SimpleCanvas width={canvasSize} height={canvasSize} backgroundColor={bkColor}
                          drawIt={(c)=> drawOnCanvas(c, df, canvasSize, canvasSize)}/>
            <Typography>{pointObj.key}</Typography>
        </Stack>
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


