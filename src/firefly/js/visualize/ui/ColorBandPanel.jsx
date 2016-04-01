/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component,PropTypes} from 'react';

import {ValidationField} from '../../ui/ValidationField.jsx';
import {ListBoxInputField} from '../../ui/ListBoxInputField.jsx';
import {CheckboxGroupInputField} from '../../ui/CheckboxGroupInputField.jsx';
import {callGetColorHistogram} from '../../rpc/PlotServicesJson.js';
import {encodeServerUrl} from '../../util/WebUtil.js';
import {formatFlux} from '../VisUtil.js';
import {getRootURL} from '../../util/BrowserUtil.js';

import {
    PERCENTAGE, MAXMIN, ABSOLUTE,SIGMA,
    STRETCH_LINEAR, STRETCH_LOG, STRETCH_LOGLOG, STRETCH_EQUAL,
    STRETCH_SQUARED, STRETCH_SQRT, STRETCH_ASINH, STRETCH_POWERLAW_GAMMA} from '../RangeValues.js';




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

const textPadding= {paddingBottom:3};

export class ColorBandPanel extends Component {

    constructor(props) {
        super(props);
        this.state={exit:true};
        this.handleReadout= this.handleReadout.bind(this);
        this.mouseMove= this.mouseMove.bind(this);
        this.mouseLeave= this.mouseLeave.bind(this);
    }


    componentWillReceiveProps(nextProps) {
        const {plot:nPlot}= nextProps;
        const {plot}= this.props;
        if (nPlot.plotId!==plot.plotId || nPlot.plotState!==plot.plotState) {
            this.initImages(nPlot,nextProps.band);
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
        var idx= Math.trunc((x *(dataHistogram.length/HIST_WIDTH)));
        const histValue= dataHistogram[idx];
        const histMean= dataBinMeanArray[idx];
        this.setState({histIdx:idx,histValue,histMean,exit:false});

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
        var {fields,plot,band}=this.props;
        const {dataHistUrl,cbarUrl, histIdx, histValue,histMean,exit}=  this.state;
        var panel;
        if (fields) {
            const {algorithm, zscale}=fields;
            var a= Number.parseInt(algorithm.value);
            if (a===STRETCH_ASINH) {
                panel= renderAsinH(fields);
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
                        <div style={{display:'table', margin:'auto auto', paddingBottom:5}}>
                            <CheckboxGroupInputField
                                options={ [ {label: 'Use ZScale for bounds', value: 'zscale'} ] }
                                fieldKey='zscale'
                                labelWidth={0} />
                        </div>
                        <img style={cbarImStyle} src={cbarUrl} key={cbarUrl}/>
                    </div>
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

function ReadoutPanel({exit, plot,band,idx,histValue,histMean,width}) {
    var topStyle= Object.assign({width},readTopBaseStyle);
    if (exit) {
       return (
           <div style={topStyle}>
                <span style={{float:'right', paddingRight:2, opacity:.4 }}>
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



function getTypeMinField() {
    return (
        <ListBoxInputField fieldKey={'lowerWhich'} inline={true} labelWidth={0}
                           options={ [ {label: '%', value: PERCENTAGE},
                                       {label: 'Data', value: ABSOLUTE},
                                       {label: 'Data Min', value: MAXMIN},
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
                                       {label: 'Data Max', value: MAXMIN},
                                       {label: 'Sigma', value: SIGMA}
                                                  ]}
                           multiple={false}
        />
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
    var {zscale}= fields;
    var range= (zscale.value==='zscale') ? renderZscale() : getUpperAndLowerFields();
    return (
        <div>
            {range}
            <div style={{paddingTop:10}}/>
            <ValidationField wrapperStyle={textPadding}  labelWidth={LABEL_WIDTH} fieldKey='gamma' />
        </div>
    );
}

function renderAsinH(fields) {
    var {zscale}= fields;
    var range= (zscale.value==='zscale') ? renderZscale() : getUpperAndLowerFields();
    return (
        <div>
            {range}
            <div style={{paddingTop:10}}/>
            <ValidationField  wrapperStyle={textPadding} labelWidth={LABEL_WIDTH} fieldKey='DR' />
            <ValidationField  wrapperStyle={textPadding} labelWidth={LABEL_WIDTH} fieldKey='BP' />
            <ValidationField  wrapperStyle={textPadding} labelWidth={LABEL_WIDTH} fieldKey='WP' />
        </div>
    );
}


