import {makeExamples} from '../WebApi.js';
import {dispatchShowDropDown} from '../../core/LayoutCntlr.js';

const alertViewerOverview = {
    overview: [
        'Open Alert Viewer and load an alert by alert id.'
    ],
    parameters: {
        id: {desc: 'Alert id to load into Alert Viewer', isRequired: true},
    },
};

const alertViewerExamples = [
    {
        desc: 'Open Alert Viewer and load an alert by id',
        params: {
            id: '170059278837088375'
        }
    },
];

function validateAlertViewer() {
    return {valid: true};
}

function showAlertViewer(cmd, params) {
    const {id} = params;
    dispatchShowDropDown({view: 'AlertUpload', initArgs: {urlApi: {id}}});
}

export function getAlertViewerCommands() {
    return [
        {
            cmd: 'alert',
            validate: validateAlertViewer,
            execute: showAlertViewer,
            ...alertViewerOverview,
            examples: makeExamples('alert', alertViewerExamples),
        },
    ];
}
