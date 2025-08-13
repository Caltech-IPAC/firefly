import {makeExamples} from 'firefly/api/WebApi';
import {EXTERNAL_UPLOAD} from '../../core/AppDataCntlr';
import {flux} from '../../core/ReduxFlux';


const anyFileOverview= {
    overview: [
        'Load any file the Firefly can recognize'
    ],
    allowAdditionalParameters: false,
    parameters: {
        execute: 'true or false - if true, fully load the file',
        url : 'URL to the file',
        displayName : 'name to show as this file',
    }
};

const anyFileExample= [
    {
        desc:'Load a image file',
        params:{ url : 'http://web.ipac.caltech.edu/staff/roby/data-products-test/1904-66_SFL.fits' }
    },
    {
        desc:'Load a table file',
        params:{ url : 'https://web.ipac.caltech.edu/staff/roby/demo/WiseDemoTable.tbl' }
    },
    {
        desc:'Load a table file',
        params:{ url : 'https://web.ipac.caltech.edu/staff/roby/demo/fp_2mass.fp_psc29179.tbl' }
    },
    {
        desc:'Same as a above, specify and load immediately',
        params:{ url : 'https://web.ipac.caltech.edu/staff/roby/demo/fp_2mass.fp_psc29179.tbl', execute:true }
    },
];

function enableFileLoad(cmd,inParams) {

    const params= {...inParams};
    setTimeout(() => {
        const {url, execute=false}= params;
        flux.process({type:EXTERNAL_UPLOAD, payload:{url, displayName:'some name', immediate:execute}});
    },10);
}

export function getAnyFileLoadCommands() {
    return [
        {
            cmd: 'load',
            validate: () => ({valid:true}),
            execute: enableFileLoad,
            ...anyFileOverview,
            examples: makeExamples('load', anyFileExample),
        },
    ];
}