/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import numeral from 'numeral';
import {debounce, get} from 'lodash';

import {ValidationField} from '../../ui/ValidationField.jsx';
import {ListBoxInputField} from '../../ui/ListBoxInputField.jsx';
import {CheckboxGroupInputField} from '../../ui/CheckboxGroupInputField.jsx';
import {RangeSlider} from '../../ui/RangeSlider.jsx';
import {callGetColorHistogram} from '../../rpc/PlotServicesJson.js';
import {encodeServerUrl} from '../../util/WebUtil.js';
import {formatFlux} from '../VisUtil.js';
import {getRootURL} from '../../util/BrowserUtil.js';

import {
    PERCENTAGE,  ABSOLUTE,SIGMA,
    STRETCH_LINEAR, STRETCH_LOG, STRETCH_LOGLOG, STRETCH_EQUAL,
    STRETCH_SQUARED, STRETCH_SQRT, STRETCH_ASINH, STRETCH_POWERLAW_GAMMA} from '../RangeValues.js';

import {getFieldGroupResults, validateFieldGroup} from '../../fieldGroup/FieldGroupUtils.js';
import {dispatchStretchChange, visRoot} from '../ImagePlotCntlr.js';
import {getActivePlotView} from '../PlotViewUtil.js';
import {makeSerializedRv} from './ColorDialog.jsx';


const LABEL_WIDTH= 105;
const HIST_WIDTH= 340;
const HIST_HEIGHT= 55;
const cbarImStyle= {width:'100%', height:10, padding: '0 2px 0 2px', boxSizing:'border-box' };

const histImStyle= {
    width:HIST_WIDTH, 
    height:HIST_HEIGHT,
    margin: '2px auto 3px auto',
    boxSizing:'border-box',
    border:'1px solid black',
    display: 'block'
};


const maskWrapper= {
    position:'absolute',
    left:0,
    top:0,
    width:'100%',
    height:'100%'
};



const textPadding= {paddingBottom:3};

export class ColorBandPanel extends PureComponent {

    constructor(props) {
        super(props);
        this.state={exit:true};
        this.handleReadout= this.handleReadout.bind(this);
        this.mouseMove= this.mouseMove.bind(this);
        this.mouseLeave= this.mouseLeave.bind(this);
        this.bandReplot= debounce(getReplotFunc(props.groupKey, props.band), 600);
    }


    componentWillReceiveProps(nextProps) {
        const {plot:nPlot}= nextProps;
        const {plot}= this.props;
        if (nPlot.plotId!==plot.plotId || nPlot.plotState!==plot.plotState) {
            this.initImages(nPlot,nextProps.band);
        }
        if (nextProps.groupKey !== this.props.groupKey) {
            this.bandReplot= debounce(getReplotFunc(this.props.groupKey, this.props.band), 600);
        }
    }

    componentWillMount() {
        const {plot,band}= this.props;
        this.initImages(plot,band);
    }

    initImages(plot,band) {

        callGetColorHistogram(plot.plotState,band,HIST_WIDTH,HIST_HEIGHT)
            .then(  (result) => {
                const dataHistUrl= encodeServerUrl(getRootURL() + 'sticky/FireFly_ImageDownload',
                    { file: result.DataHistImageUrl, type: 'any' });

                const cbarUrl= encodeServerUrl(getRootURL() + 'sticky/FireFly_ImageDownload',
                    { file: result.CBarImageUrl, type: 'any' });
                const dataHistogram= result.DataHistogram;
                const dataBinMeanArray= result.DataBinMeanArray;
                this.setState({dataHistUrl,cbarUrl,dataHistogram,dataBinMeanArray});
            });
    }

    mouseMove(ev) {
        const {offsetX:x}= ev;
        const {dataHistogram,dataBinMeanArray}= this.state;
        if (dataHistogram && dataBinMeanArray) {
            const idx = Math.trunc((x * (dataHistogram.length / HIST_WIDTH)));
            const histValue = dataHistogram[idx];
            const histMean = dataBinMeanArray[idx];
            this.setState({histIdx: idx, histValue, histMean, exit: false});
        }

    }
    mouseLeave() { this.setState({exit:true}); }

    handleReadout(c) {
        if (!c) return;
        c.removeEventListener('mousemove', this.mouseMove);
        c.removeEventListener('mouseleave', this.mouseLeave);
        c.addEventListener('mousemove', this.mouseMove);
        c.addEventListener('mouseleave', this.mouseLeave);
    }

    render() {
        const {fields,plot,band}=this.props;
        const {dataHistUrl,cbarUrl, histIdx, histValue,histMean,exit, doMask}=  this.state;



        let panel;
        if (fields) {
            const {algorithm, zscale}=fields;
            const a= Number.parseInt(algorithm.value);
            if (a===STRETCH_ASINH) {
                const renderRange = (isZscale) => {
                    return isZscale ? renderZscale() : getUpperAndLowerFields();
                };
                panel= renderAsinH(fields, renderRange, this.bandReplot);
            }
            else if (a===STRETCH_POWERLAW_GAMMA) {
                panel= renderGamma(fields);
            }
            else if (zscale.value==='zscale') {
                panel= renderZscale();
            }
            else {
                panel= renderStandard();
            }
        }
        else {
            panel= renderStandard();
        }


        return (
                <div style={{minHeight:305, minWidth:360, padding:5, position:'relative'}}>
                    <img style={histImStyle} src={dataHistUrl} key={dataHistUrl} ref={this.handleReadout}/>
                    <ReadoutPanel 
                        width={HIST_WIDTH}
                        exit={exit}
                        idx={histIdx}
                        histValue={histValue}
                        histMean={histMean}
                        plot={plot}
                        band={band}
                    />
                    <div style={{display:'table', margin:'auto auto'}}>
                        {getStretchTypeField()}
                    </div>

                    {panel}

                    <div style={{position:'absolute', bottom:5, left:5, right:5}}>
                        <div>
                            {suggestedValuesPanel( plot,band )}
                        </div>
                        {getZscaleCheckbox()}
                        <img style={cbarImStyle} src={cbarUrl} key={cbarUrl}/>
                    </div>
                    {doMask && <div style={maskWrapper}> <div className='loading-mask'/> </div> }
                </div>
            );
    }
}

ColorBandPanel.propTypes= {
    groupKey : PropTypes.string.isRequired,
    band : PropTypes.object.isRequired,
    plot : PropTypes.object.isRequired,
    fields : PropTypes.object.isRequired
};



const readTopBaseStyle= { fontSize: '11px', paddingBottom:5, height:16 };
const dataStyle= { color: 'red' };

function suggestedValuesPanel( plot,band ) {

    const precision6Digit = '0.000000';
    const style= { fontSize: '11px', paddingBottom:5, height:16, whiteSpace: 'pre'};

    const  fitsData= plot.webFitsData[band.value];
    const {dataMin, dataMax} = fitsData;
    const dataMaxStr = `Data Max: ${numeral(dataMax).format(precision6Digit)} `;
    const dataMinStr = `Data Min: ${numeral(dataMin).format(precision6Digit)}`;

    return (
        <div style={style}>
                <span style={{float:'left', paddingRight:2, opacity:.5, marginLeft:40 }}>
                  {dataMinStr}            {dataMaxStr}
                </span>
        </div>
    );
}

function ReadoutPanel({exit, plot,band,idx,histValue,histMean,width}) {
    const topStyle= Object.assign({width},readTopBaseStyle);
    if (exit) {
        return (
            <div style={topStyle}>
                <span style={{float:'right', paddingRight:2, opacity:.4, textAlign: 'center' }}>
                Move mouse over graph to see values
                </span>
            </div>
        );
    }
    else {
        return (
            <div style={topStyle}> Histogram: index:
                <span style={dataStyle}>{idx}</span>, Size:
                <span style={dataStyle}>{histValue}</span>, Mean Value :
                <span style={dataStyle}> {formatFlux(histMean, plot, band)} </span>
            </div>
        );
    }
}

ReadoutPanel.propTypes= {
    exit : PropTypes.bool,
    band : PropTypes.object,
    plot : PropTypes.object,
    idx : PropTypes.number,
    histValue :PropTypes.number,
    histMean :PropTypes.number,
    width :PropTypes.number
};





//===============================================================================
//===============================================================================
//================ Helpers
//===============================================================================
//===============================================================================



export function getTypeMinField(lowerWhich='lowerWhich') {
    return (
        <ListBoxInputField fieldKey={lowerWhich} inline={true} labelWidth={0}
                           options={ [ {label: '%', value: PERCENTAGE},
                                       {label: 'Data', value: ABSOLUTE},
                                       {label: 'Sigma', value: SIGMA}
                                       ]}
                           multiple={false}
        />
    );
}

function getTypeMaxField() {
    return (
        <ListBoxInputField fieldKey='upperWhich' inline={true} labelWidth={0}
                           options={ [ {label: '%', value: PERCENTAGE},
                                       {label: 'Data', value: ABSOLUTE},
                                       {label: 'Sigma', value: SIGMA}
                                                  ]}
                           multiple={false}
        />
    );
}

export function getZscaleCheckbox() {
    return (
        <div style={{display:'table', margin:'auto auto', paddingBottom:5}}>
            <CheckboxGroupInputField
                options={ [ {label: 'Use ZScale for bounds', value: 'zscale'} ] }
                fieldKey='zscale'
                labelWidth={0} />
        </div>
    );
}

function renderZscale() {
    return (
        <div>
            <ValidationField wrapperStyle={textPadding} labelWidth={LABEL_WIDTH} fieldKey='zscaleContrast' />
            <ValidationField wrapperStyle={textPadding} labelWidth={LABEL_WIDTH} fieldKey='zscaleSamples' />
            <ValidationField wrapperStyle={textPadding} labelWidth={LABEL_WIDTH} fieldKey='zscaleSamplesPerLine' />
        </div>
    );
}

function renderStandard() { return  getUpperAndLowerFields(); }

function getUpperAndLowerFields() {
    return (
        <div>
            <div style={{ whiteSpace:'no-wrap'}}>
                <ValidationField wrapperStyle={textPadding} inline={true}
                                 labelWidth={LABEL_WIDTH}
                                 fieldKey='lowerRange'
                />
                {getTypeMinField()}
            </div>
            <div style={{ whiteSpace:'no-wrap'}}>
                <ValidationField  wrapperStyle={textPadding} labelWidth={LABEL_WIDTH}
                                  inline={true}
                                  fieldKey='upperRange'
                />
                {getTypeMaxField()}
            </div>
        </div>
    );
}

function getStretchTypeField() {
    return (
        <div style={{paddingBottom:12}}>
            <ListBoxInputField fieldKey='algorithm' inline={true} labelWidth={67}
                               options={ [
                                    {label: 'Linear',                 value: STRETCH_LINEAR},
                                    {label: 'Log',                    value: STRETCH_LOG},
                                    {label: 'Log-Log',                value: STRETCH_LOGLOG},
                                    {label: 'Histogram Equalization', value: STRETCH_EQUAL},
                                    {label: 'Squared',                value: STRETCH_SQUARED },
                                    {label: 'Sqrt',                   value: STRETCH_SQRT},
                                    {label: 'Asinh',                  value: STRETCH_ASINH},
                                    {label: 'Power Law Gamma',        value: STRETCH_POWERLAW_GAMMA}
                                 ]}
            />
        </div>
    );
}

function renderGamma(fields) {
    const {zscale}= fields;
    const range= (zscale.value==='zscale') ? renderZscale() : getUpperAndLowerFields();
    return (
        <div>
            {range}
            <div style={{paddingTop:10}}/>
            <ValidationField wrapperStyle={textPadding}  labelWidth={LABEL_WIDTH} fieldKey='gamma' />
        </div>
    );
}

const asinhSliderMarks = {
 0: '0', 5: '5', 10: '10', 15: '15', 20: '20'
};

const ASINH_Q_MAX_SLIDE_VAL = 20;

export function renderAsinH(fields, renderRange, replot) {
    const {zscale}= fields;
    const range= renderRange(zscale.value==='zscale');
    const qvalue = get(fields, ['asinhQ', 'value'], Number.NaN);
    const label = `Q: ${Number.parseFloat(qvalue).toFixed(1)} `;

    return (
        <div style={{paddingBottom: 60}}>
            {range}
            <div style={{paddingTop: 5, paddingRight: 15, opacity: .4, textAlign: 'center'}}>
                Q=0 for linear stretch;<br/> increase Q to make brighter features visible
            </div>
            <RangeSlider fieldKey='asinhQ'
                         min={0}
                         minStop={0}
                         max={ASINH_Q_MAX_SLIDE_VAL}
                         maxStop={ASINH_Q_MAX_SLIDE_VAL}
                         marks={asinhSliderMarks}
                         step={0.1}
                         slideValue={qvalue}
                         label={label}
                         labelWidth={60}
                         wrapperStyle={{marginTop: 10, marginBottom: 20, marginRight: 15}}
                         decimalDig={1}
                         onValueChange={replot}
            />
        </div>
    );
}


function getReplotFunc(groupKey, band) {

    return (val) => {
        validateFieldGroup(groupKey).then((valid) => {
            if (valid) {
                const request = getFieldGroupResults(groupKey);
                const serRv = makeSerializedRv(request);
                const stretchData = [{band: band.key, rv: serRv, bandVisible: true}];
                const pv = getActivePlotView(visRoot());
                if (pv) dispatchStretchChange({plotId: pv.plotId, stretchData});
            }
        });
    };
}