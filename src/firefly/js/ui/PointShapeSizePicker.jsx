/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {flux} from '../Firefly.js';
import {dispatchShowDialog} from '../core/ComponentCntlr.js';
import {PopupPanel} from './PopupPanel.jsx';
import {DrawSymbol} from '../visualize/draw/PointDataObj.js';
import DrawUtil from '../visualize/draw/DrawUtil.js';
import {drawOnCanvas} from '../visualize/ui/DrawLayerItemView.jsx';
import {dispatchChangeDrawingDef, getDlAry} from '../visualize/DrawLayerCntlr.js';
import CompleteButton from './CompleteButton.jsx';
import DialogRootContainer from './DialogRootContainer.jsx';
import {RadioGroupInputFieldView} from './RadioGroupInputFieldView.jsx';
import {InputFieldView} from './InputFieldView.jsx';
import {showInfoPopup} from './PopupUtil.jsx';
import {SimpleCanvas} from '../visualize/draw/SimpleCanvas.jsx';
import {getDrawLayersByDisplayGroup} from '../visualize/PlotViewUtil.js';
import {get, isNaN} from 'lodash';
import {clone} from '../util/WebUtil.js';
import Color from '../util/Color.js';
import validator from 'validator';
import {HelpIcon} from '../ui/HelpIcon.jsx';


const PointOptions = [ DrawSymbol.CIRCLE, DrawSymbol.SQUARE, DrawSymbol.DIAMOND,
                       DrawSymbol.CROSS, DrawSymbol.X, DrawSymbol.ARROW,
                       DrawSymbol.BOXCIRCLE, DrawSymbol.DOT];

const MINSIZE = 3;
const MAXSIZE = 100;
const popupIdBase = 'ShapePickerDialog';
const ARROW_UP = 38;
const ARROW_DOWN = 40;

var popupId;

export function showPointShapeSizePickerDialog(dl, plotId) {
    var {isPointData=false} = dl;

    popupId = popupIdBase; //keep one dialog
    const popup= isPointData ? (
        <PopupPanel title={'Symbol Picker - ' + dl.drawLayerId } >
            <ShapePickerWrapper drawingDef={dl.drawingDef} displayGroupId={dl.displayGroupId} plotId={plotId} />
        </PopupPanel>
    ) : null;

    if (popup) {
        DialogRootContainer.defineDialog(popupId, popup);
        setTimeout(() => dispatchShowDialog(popupId), 0);
    }
}

class ShapePickerWrapper extends PureComponent {
    constructor(props) {
        super(props);

        var {size, symbol} = props.drawingDef;
        var {width} = DrawUtil.getDrawingSize(size, symbol);
        var validSize = (width >= MINSIZE) && (width <= MAXSIZE);

        this.state = {drawingDef: props.drawingDef, size: `${width}`, validSize}; // sizexsize: the overal size shown on UI
        this.displayGroupId = props.displayGroupId;
        this.plotId = props.plotId;
        this.updateSymbol = this.updateSymbol.bind(this);
        this.drawSymbol = this.drawSymbol.bind(this);
        this.updateSize = this.updateSize.bind(this);
        this.updateShape = this.updateShape.bind(this);
        this.onArrowDown = this.onArrowDown.bind(this);
        this.onArrowUp = this.onArrowUp.bind(this);
    }


    componentWillReceiveProps(nextProps) {
        var {size, symbol} = nextProps.drawingDef;
        var {width} = DrawUtil.getDrawingSize(size, symbol);
        var validSize = (width >= MINSIZE) && (width <= MAXSIZE);

        this.setState({drawingDef: nextProps.drawingDef, size: `${width}`, validSize}); // sizexsize: the overal size shown on UI
        this.displayGroupId = nextProps.displayGroupId;
        this.plotId = nextProps.plotId;
    }

    componentDidMount() {
        this.iAmMounted = true;
        this.removeListener = flux.addListener(() => this.storeUpdate());
    }

    componentWillUnmount() {
        this.iAmMounted = false;
        this.removeListener && this.removeListener();
    }

    storeUpdate() {   // color change externally
        if (this.iAmMounted) {
            const {drawingDef} = this.state;
            const color = drawingDef.color;
            const dl = getDrawLayersByDisplayGroup(getDlAry(), this.props.displayGroupId);
            const dlColor = get(dl, ['drawingDef', 'color']);

            if (dlColor !== drawingDef.color) {
                this.setState({drawingDef: clone(drawingDef, {color: dlColor})});
            }
        }
    }

    updateSymbol(ev) {
        const value = get(ev, 'target.value');
        const symbol = value&&DrawSymbol[value];

        const {drawingDef} = this.state;

        const symbolSize = DrawUtil.getSymbolSizeBasedOn(symbol, drawingDef);
        const newDD = (symbolSize === drawingDef.size) ? clone(drawingDef, {symbol}) :
                                                         clone(drawingDef, {symbol, size: symbolSize});

        const dl = getDrawLayersByDisplayGroup(getDlAry(), this.props.displayGroupId);
        dispatchChangeDrawingDef(this.displayGroupId, newDD, this.plotId, dl.titleMatching);
        this.setState({drawingDef: newDD});

    }

    updateSize(ev) {
        const size = get(ev, 'target.value');
        var validSize = true;
        var isize;

        if (!size || !validator.isFloat(size)) {
            validSize = false;
        } else {
            isize = Math.floor(parseFloat(size));
            validSize = size && (isize >= MINSIZE)&&(isize <= MAXSIZE);
        }

        if (!validSize) {
            this.setState({size, validSize});
        } else {
            const {drawingDef} = this.state;
            const symbolSize = DrawUtil.getSymbolSize(isize, isize, drawingDef.symbol);
            const newDD = clone(drawingDef, {size: symbolSize});

            this.setState({size, validSize, drawingDef: newDD});
            const dl = getDrawLayersByDisplayGroup(getDlAry(), this.props.displayGroupId);
            dispatchChangeDrawingDef(this.displayGroupId, newDD, this.plotId, dl.titleMatching);
        }
    }

    onArrowDown(ev) {
        const {size, validSize} = this.state;
        const isize = validSize && Math.floor(parseFloat(size));

        if (validSize) {
            if ((ev.keyCode === ARROW_UP) && (size < MAXSIZE)) { // arrow up
                this.setState({size: `${isize + 1}`});
            } else if ((ev.keyCode === ARROW_DOWN) && (size > MINSIZE)) { // arrow down
                this.setState({size: `${isize - 1}`});
            }
        }
    }

    onArrowUp(ev) {
        var {size, validSize, drawingDef} = this.state;

        if (validSize && ((ev.keyCode === ARROW_UP || ev.keyCode === ARROW_DOWN ))) { // arrow up or down
            const isize = Math.floor(parseFloat(size));
            const symbolSize = DrawUtil.getSymbolSize(isize, isize, drawingDef.symbol);
            const newDD = clone(drawingDef, {size: symbolSize});

            const dl = getDrawLayersByDisplayGroup(getDlAry(), this.props.displayGroupId);
            dispatchChangeDrawingDef(this.displayGroupId, newDD, this.plotId, dl.titleMatching);
            this.setState({drawingDef: newDD});
        }
    }

    updateShape() {
        const {size, drawingDef, validSize} = this.state;

        if (!validSize) {
            return;
        }
        const isize = Math.floor(parseFloat(size));
        const symbolSize = DrawUtil.getSymbolSize(isize, isize, drawingDef.symbol);

        const dl = getDrawLayersByDisplayGroup(getDlAry(), this.props.displayGroupId);
        dispatchChangeDrawingDef(this.displayGroupId, clone(drawingDef, {symbol: drawingDef.symbol, size: symbolSize}),
                                                             this.plotId, dl.titleMatching);

    }

    drawSymbol(df, validSize, size){
        const {drawingDef} = this.state;

        const maxSize = 30;
        var canvasSize = validSize && (Math.floor(parseFloat(size)) + 2);
        const bkColor = getBackgroundColor(df.color);
        if (size > maxSize) {
            canvasSize = validSize && Math.floor(parseFloat(maxSize)) + 2;
            const symbolSize = DrawUtil.getSymbolSize(maxSize, maxSize, drawingDef.symbol);
            df = clone(drawingDef, {size: symbolSize})
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
    }

    render() {
        const {drawingDef, size, validSize} = this.state;
        const df = validSize&&drawingDef;
        const labelW = 70;
        const mLeft = 10;
        const bkColor = getBackgroundColor(drawingDef.color);
        const textColor = '#000000';
        const options = PointOptions.map((p) => {
                            return {value: p.key, label: drawShapeWithLabel(p, drawingDef, bkColor, textColor)}
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
                                                  onChange={this.updateSymbol}
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
                                         onChange={this.updateSize}
                                         onKeyDown={this.onArrowDown}
                                         onKeyUp={this.onArrowUp}
                                         value={size}
                                         tooltip={'enter the symbol size or use the arrow up (or down) key in the field to increase (or decrease) the size number '}
                                         type={'text'}
                                         placeholder={`size 3 < ${MAXSIZE}`}
                                         size={16}
                                         message={`invalid data entry, size is within 3 & ${MAXSIZE}`}/>
                        {validSize && this.drawSymbol(df, validSize, size)}
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
                                     onSuccess={this.updateShape}
                                     text={'OK'}/>
                </div>
            </div>
        );
    }
}

ShapePickerWrapper.propTypes= {
    drawingDef: PropTypes.object.isRequired,
    displayGroupId: PropTypes.string.isRequired,
    plotId: PropTypes.string.isRequired
};

function drawShapeWithLabel(pointObj, drawingDef, bkColor, textColor) {
    var   size = 16;
    var   symbolSize = DrawUtil.getSymbolSize(size, size, pointObj);
    const addSpace = 6;

    if (pointObj.key === 'DOT') {
        symbolSize /= 2;
    }

    const df = clone(drawingDef, {symbol: pointObj, size: symbolSize});
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


