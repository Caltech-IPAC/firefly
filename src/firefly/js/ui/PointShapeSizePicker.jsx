/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component, PropTypes} from 'react';
import {flux} from '../Firefly.js';
import sCompare from 'react-addons-shallow-compare';
import {dispatchShowDialog} from '../core/ComponentCntlr.js';
import {PopupPanel} from './PopupPanel.jsx';
import {DrawSymbol} from '../visualize/draw/PointDataObj.js';
import DrawUtil from '../visualize/draw/DrawUtil.js';
import {drawOnCanvas} from '../visualize/ui/DrawLayerItemView.jsx';
import {dispatchChangeDrawingDef, getDlAry} from '../visualize/DrawLayerCntlr.js';
import CompleteButton from './CompleteButton.jsx';
import DialogRootContainer from './DialogRootContainer.jsx';
import {CheckboxGroupInputFieldView} from './CheckboxGroupInputField.jsx';
import {InputFieldView} from './InputFieldView.jsx';
import {showInfoPopup} from './PopupUtil.jsx';
import {SimpleCanvas} from '../visualize/draw/SimpleCanvas.jsx';
import {getDrawLayersByDisplayGroup} from '../visualize/PlotViewUtil.js';
import {get, isNaN} from 'lodash';
import {clone} from '../util/WebUtil.js';
import validator from 'validator';


const PointOptions = [ DrawSymbol.CIRCLE, DrawSymbol.SQUARE, DrawSymbol.DIAMOND,
                       DrawSymbol.CROSS, DrawSymbol.X, DrawSymbol.ARROW,
                       DrawSymbol.BOXCIRCLE, DrawSymbol.DOT];

const MINSIZE = 3;
const MAXSIZE = 20;
const popupId = 'ShapePickerDialog';
const ARROW_UP = 38;
const ARROW_DOWN = 40;

export function showPointShapeSizePickerDialog(dl, plotId) {
    var {isPointData=false} = dl;
    const popup= isPointData ? (
        <PopupPanel title={'Symbol Picker'} >
            <ShapePickerWrapper drawingDef={dl.drawingDef} displayGroupId={dl.displayGroupId} plotId={plotId} />
        </PopupPanel>
    ) : null;

    if (popup) {
        DialogRootContainer.defineDialog(popupId, popup);
        setTimeout(() => dispatchShowDialog(popupId), 0);
    }
}

class ShapePickerWrapper extends Component {
    constructor(props) {
        super(props);

        var {size, symbol} = props.drawingDef;
        var {width} = DrawUtil.getDrawingSize(size, symbol);
        var validSize = (width >= MINSIZE) && (width <= MAXSIZE);

        this.state = {drawingDef: props.drawingDef, size: `${width}`, validSize}; // sizexsize: the overal size shown on UI
        this.displayGroupId = props.displayGroupId;
        this.plotId = props.plotId;
        this.updateSymbol = this.updateSymbol.bind(this);
        this.updateSize = this.updateSize.bind(this);
        this.updateShape = this.updateShape.bind(this);
        this.onArrowDown = this.onArrowDown.bind(this);
        this.onArrowUp = this.onArrowUp.bind(this);
    }

    shouldComponentUpdate(np,ns) { return sCompare(this,np,ns); }

    componentDidMount() {
        this.removeListener = flux.addListener(() => this.storeUpdate());
    }

    storeUpdate() {   // color change externally
        const {drawingDef} = this.state;
        const color = drawingDef.color;
        const dl = getDrawLayersByDisplayGroup(getDlAry(), this.props.displayGroupId);
        const dlColor = get(dl, ['drawingDef', 'color']);

        if (dlColor !== drawingDef.color) {
            this.setState({drawingDef: clone(drawingDef, {color: dlColor})});
        }
    }

    updateSymbol(ev) {
        const value = get(ev, 'target.value');
        const symbol = value&&DrawSymbol[value];

        const {drawingDef} = this.state;

        const symbolSize = DrawUtil.getSymbolSizeBasedOn(symbol, drawingDef);
        const newDD = (symbolSize === drawingDef.size) ? clone(drawingDef, {symbol}) :
                                                         clone(drawingDef, {symbol, size: symbolSize});

        dispatchChangeDrawingDef(this.displayGroupId, newDD, this.plotId);
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
            dispatchChangeDrawingDef(this.displayGroupId, newDD, this.plotId);
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

            dispatchChangeDrawingDef(this.displayGroupId, newDD, this.plotId);
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

        dispatchChangeDrawingDef(this.displayGroupId, clone(drawingDef, {symbol: drawingDef.symbol,
                                                                         size: symbolSize}), this.plotId);

    }


    render() {
        const {drawingDef, size, validSize} = this.state;
        const labelW = 70;
        const mLeft = 10;
        const df = validSize&&drawingDef;
        const canvasSize = validSize && (Math.floor(parseFloat(size)) + 2);
        const bkColor = getBackgroundColor(drawingDef.color);
        const textColor = '#000000';
        const options = PointOptions.map((p) => {
                            return {value: p.key, label: drawShapeWithLabel(p, drawingDef, bkColor, textColor)}
                        });

        return (
            <div style={{width: 300}}>
                <div style={{margin: mLeft,
                             border: '1px solid rgba(0, 0, 0, 0.298039)',
                             borderRadius: 5,
                             padding: '10px 5px'
                             }}>
                    <div style={{display: 'flex', marginLeft: mLeft}} >
                        <div style={{width: labelW, color: textColor}} title={'pick a symbol'}>Symbols:</div>
                        {CheckboxGroupInputFieldView({fieldKey: 'pointoptions',
                                                  onChange: this.updateSymbol,
                                                  tooltip: 'available symbol shapes',
                                                  options,
                                                  value: drawingDef.symbol.key,
                                                  alignment: 'vertical'})}
                    </div>
                    <div style={{marginLeft: mLeft, marginTop: mLeft, height: 26, display: 'flex', alignItems: 'center'}}>
                        <InputFieldView  label={'Symbol Size:'}
                                         labelStyle={{color: textColor}}
                                         labelWidth={labelW}
                                         valid={validSize}
                                         onChange={this.updateSize}
                                         onKeyDown={this.onArrowDown}
                                         onKeyUp={this.onArrowUp}
                                         value={size}
                                         tooltip={'enter the symbol size or use the arrow up (or down) key in the field to increase (or decrease) the size number '}
                                         type={'text'}
                                         placeholder={'size is between 3 and 20'}
                                         size={16}
                                         message={'invalid data entry, size is within 3 & 20'}/>
                        <div style={{width: canvasSize, height: canvasSize}}>
                            {validSize && <SimpleCanvas width={canvasSize} height={canvasSize} backgroundColor={bkColor}
                                                        drawIt={(c)=>drawOnCanvas(c, df, canvasSize, canvasSize)}/>}
                        </div>
                    </div>
                    <div style={{marginLeft: mLeft, marginTop: mLeft, color: textColor}}>
                        <i>enter the number or use the arrow up/down key to increase/decrease the size number </i>
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

const [R, G, B, A] = [0, 1, 2, 3];
const [LUMI, CONTRAST, HSP] = [0, 1, 2];

/**
 * @summary extract R,G,B amd alpha value from color string like 'rgba(...)', 'rgb(...)' or '#rrggbb'
 * @param color
 * @returns {*}
 */
function getRGBA(color) {
    const rgbKey = ['rgba(', 'rgb(', '#'];
    var rgbStr = rgbKey.find((k) => {
                    return color.includes(k);
                 });

    if (!rgbStr) {
        return null;
    }

    var rgba = [];
    if (rgbStr === '#') {
        for (let i = 1; i <= 5; i += 2) {
            rgba.push(parseInt(color.slice(i, i + 2), 16));
        }
    } else {
        rgba = color.replace(rgbStr, '').replace(')', '').replace(/ /g, '').split(',');
        rgba = rgba.map((v) => parseFloat(v));
    }

    if (rgbKey !== 'rgba') {
        rgba.push(1.0);
    }

    return rgba;
}


/**
 * @summary get the background color which is either black/white or complementary color
 * @param color
 * @returns {*}
 */
function getBackgroundColor(color) {
    return getBWBackground(color);
}

/**
 * @summary get background in terms of black or white, based on various algorithms, relative luminance, contrast, or HSP color model.
 * @param color
 * @returns {*}
 */
function getBWBackground(color, method = LUMI) {
    var weight;
    var th;
    var luma;

    const rgba = getRGBA(color);
    var linearLuma = (w) => {
                return [R, G, B].reduce((lum, c) => {
                    lum += w[c] * rgba[c];
                    return lum;
                }, 0.0);
            };
    var squareLuma = (w) => {
                return [R, G, B].reduce((lum, c) => {
                    lum += w[c] * rgba[c] * rgba[c];
                    return lum;
                }, 0.0);
            };


    switch (method) {
        case CONTRAST:
        case HSP:
            weight = [0.299, 0.587, 0.114];
            th = 128;

            if (method === CONTRAST) {
                luma =  linearLuma(weight);
            } else {
                luma = Math.sqrt(squareLuma(weight));

            }
            break;
        default:
            weight = [0.2126, 0.7152, 0.0722];
            th = 165;

            luma = linearLuma(weight);
            break;
    }
    const bkGrey = luma < th ? parseInt('ff', 16) : 0;  // dark color -> get bright, bright color -> get dark

    return `rgba(${bkGrey}, ${bkGrey}, ${bkGrey}, 1.0)`;
}

/**
 * @summary get complementary color
 * @param color
 * @returns {*}
 */
function getComplemetaryColor(color) {
   var rgba = getRGBA(color);

   if (!rgba)  return 'rgba(201, 201, 201, 1.0)';   //'#e3e3e3'
   var rgb = rgba.slice(0, 3);

   var RGB2HSV = (rgb) =>
    {
        var hsv = {};
        var maxV = Math.max(...rgb);
        var minV = Math.min(...rgb);

        var dif = maxV - minV;

        hsv.saturation = (maxV === 0.0) ? 0 : (100.0 * dif / maxV);
        if (hsv.saturation === 0) {
            hsv.hue = 0;
        } else if (rgb[R] === maxV) {
            hsv.hue = 60.0 * (rgb[G] - rgb[B]) / dif;
        } else if (rgb[G] === maxV) {
            hsv.hue = 120.0 + 60.0 * (rgb[B] - rgb[R]) / dif;
        } else if (rgb[B] === maxV) {
            hsv.hue = 240.0 + 60.0 * (rgb[R] - rgb[G]) / dif;
        }
        if (hsv.hue < 0.0) hsv.hue += 360.0;
        hsv.value = Math.round(maxV * 100 / 255);
        hsv.hue = Math.round(hsv.hue);
        hsv.saturation = Math.round(hsv.saturation);
        return hsv;
    };

    var HSV2RGB = (hsv) => {
        var rgb = [0, 0, 0];
        var toRGB = (rgb, r, g, b) => {
            rgb[R] = r;
            rgb[G] = g;
            rgb[B] = b;
            return rgb.map((v) => (Math.round(v*255)));
        };

        if (hsv.saturation === 0) {
            var v = Math.round(hsv.value*2.55);

            if (hsv.hue === 180.0) {
                v = 255 - v;
            }
            rgb = toRGB(rgb, v, v, v);
        } else {
            hsv.hue /= 60.0;
            hsv.saturation /= 100.0;
            hsv.value /= 100.0;


            let i = Math.floor(hsv.hue);
            let f = hsv.hue - i;
            let a = hsv.value * (1.0 - hsv.saturation);
            let b = hsv.value * (1.0 - hsv.saturation * f);
            let c = hsv.value * (1.0 - hsv.saturation * (1-f));
            let d = hsv.value;
            switch(i) {
                case 0:
                    rgb = toRGB(rgb, d, c, a);
                    break;
                case 1:
                    rgb = toRGB(rgb, b, d, a);
                    break;
                case 2:
                    rgb = toRGB(rgb, a, d, c);
                    break;
                case 3:
                    rgb = toRGB(rgb, a, b, d);
                    break;
                case 4:
                    rgb = toRGB(rgb, c, a, d);
                    break;
                case 5:
                    rgb = toRGB(rgb, d, a, b);
                    break;
                default:
                    rgb = toRGB(rgb, 0, 0, 0);
                    break;
            }
        }
        return rgb;
    };

    var HueShift  = (h, s)  => {
        h += s;

        while (h >= 360.0) {
            h -= 360.0;
        }
        while (h < 0.0) {
            h+= 360.0;
        }
        return h;
    };

    var tmphsv = RGB2HSV(rgb);
    tmphsv.hue = HueShift(tmphsv.hue, 180.0);
    rgb = HSV2RGB(tmphsv);

    return `rgba(${rgb[R]}, ${rgb[G]}, ${rgb[B]}, ${rgba[3]})`;
}

