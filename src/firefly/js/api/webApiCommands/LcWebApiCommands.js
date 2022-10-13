import {isEmpty} from 'lodash';
import {makeExamples} from '../WebApi.js';
import {dispatchShowDropDown} from '../../core/LayoutCntlr.js';


const lcPanelOverview= {
    overview: [
        'Load a time series'
    ],
    parameters: {
        url: {desc:'tap service url', isRequired:true},
        dataset : {desc:'a dataset, must be - neowise, ptf, ztf, lsst_sdss, or generic', isRequired:true},
        execute: 'true or false - if true execute the tap search'
    },
};

const lcPanelExamples= [
    {
        desc:'load a time series',
        params:{
            url: 'https://irsa.ipac.caltech.edu/cgi-bin/ZTF/nph_light_curves?collection=ztf_dr3&bad_catflags_mask=0&id=695211400134650',
            dataset: 'ztf',
            execute: 'true'
        }
    },
];


function validateLC(params) {
    return {valid:true};
}

function showLcPanel(cmd,params) {
    const {url, dataset, execute=true, ...rest}= params;

    const newUrl= new URL(url);
    if (!isEmpty(rest)) {
        Object.entries(rest).forEach( ([k,v]) => newUrl.searchParams.append(k,v));
    }
    dispatchShowDropDown({view:'LCUpload', initArgs:{urlApi:{dataset,execute, url: newUrl.toString()}}});
}

export function getLcCommands() {
    return [
        {
            cmd : 'lc',
            validate : validateLC,
            execute:  showLcPanel,
            allowAdditionalParameters: true,
            ...lcPanelOverview,
            examples: makeExamples('lc', lcPanelExamples),
        },
    ];
}