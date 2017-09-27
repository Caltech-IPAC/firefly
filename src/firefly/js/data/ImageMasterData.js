/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {clone} from '../util/WebUtil.js';


/**
 * @global
 * @public
 * @typedef {Object} ImageMasterData
 *
 * @summary  need summary
 *
 * @prop {string} project - name of project or mission
 * @prop {string} subProject - subproject name or null
 * @prop {string} title - description of the image
 * @prop {number} wavelength - the actual wavelength value
 * @prop {string} wavelengthDesc - description of the wavelength
 * @prop {string} helpUrl - full url to help for this data
 * @prop {string} tooltip - one line of helpful text
 * @prop {string} imageId - a unique id for the list of image data
 * @prop {string} projectTypeKey - as of key such as 'galactic', 'extra-galactic, solar-system'
 * @prop {string} projectTypeDesc - description of the project type
 * @prop {number} minRangeDeg  - for radius search, smallest radius accepted
 * @prop {number} maxRangeDeg  - for radius search, largest radius accepted
 * @prop {object} plotRequestParams  - an object for a WebPlotRequest
 *
 */



const template = {
    project: '',
    subProject: '',
    title : '',
    wavelength: 0,
    wavelengthDesc: '0 micron',
    helpUrl: 'http://irsa.ipac.caltech.edu',
    tooltip: 'Add help here',
    imageId : 'XXXXXX-need-id',
    projectTypeKey: '',
    projectTypeDesc: '',
    minRangeDeg: .1,
    maxRangeDeg: .2,
    plotRequestParams  : {
        title: 'need title',
    }

};


const irasIssaTemplate= clone( template,
    {
        project: 'IRAS',
        subProject: 'issa',
        helpUrl: 'http://irsa.ipac.caltech.edu/stuff',
        projectTypeKey: 'galactic',
        projectTypeDesc: 'Galactic',
        minRangeDeg: 1,
        maxRangeDeg:12.5,
    });

const irasIrisTemplate= clone( template,
    {
        project: 'IRAS',
        subProject: 'iris',
        helpUrl: 'http://irsa.ipac.caltech.edu/stuff',
        projectTypeKey: 'galactic',
        projectTypeDesc: 'Galactic',
        minRangeDeg: 1,
        maxRangeDeg:12.5,
    });

const wiseAtlasTemplate= clone( template,
           {
               project: 'WISE',
               subProject: 'Atlas',
               helpUrl: 'http://irsa.ipac.caltech.edu/stuff',
               projectTypeKey: 'galactic',
               projectTypeDesc: 'Galactic',
               minRangeDeg: .01,
               maxRangeDeg: .2
           });

const wiseLevel1Template= clone( template,
    {
        project: 'WISE',
        subProject: '4 Band Level 1',
        helpUrl: 'http://irsa.ipac.caltech.edu/stuff',
        projectTypeKey: 'galactic',
        projectTypeDesc: 'Galactic',
        minRangeDeg: .01,
        maxRangeDeg: .2
    });


const twoMastsTemplate= clone( template,
    {
        project: '2MASS',
        subProject: null,
        helpUrl: 'http://irsa.ipac.caltech.edu/stuff',
        projectTypeKey: 'galactic',
        projectTypeDesc: 'Galactic',
        minRangeDeg: .008,
        maxRangeDeg: .139
    });

const dssTemplate= clone( template,
    {
        project: 'DSS',
        subProject: null,
        helpUrl: 'http://irsa.ipac.caltech.edu/stuff',
        projectTypeKey: 'galactic',
        projectTypeDesc: 'Galactic',
        minRangeDeg: .016,
        maxRangeDeg: .5
    });

const msxTemplate= clone( template,
    {
        project: 'MSX',
        subProject: null,
        helpUrl: 'http://irsa.ipac.caltech.edu/stuff',
        projectTypeKey: 'galactic',
        projectTypeDesc: 'Galactic',
        minRangeDeg: .01,
        maxRangeDeg: 1.5
    });

const sdssTemplate= clone( template,
    {
        project: 'SDSS',
        subProject: null,
        helpUrl: 'http://irsa.ipac.caltech.edu/stuff',
        projectTypeKey: 'galactic',
        projectTypeDesc: 'Galactic',
        minRangeDeg: .016,
        maxRangeDeg: .5
    });

const wiseMasterData = [
    clone (wiseAtlasTemplate, {
        title : 'band 1',
        wavelength: 41,
        wavelengthDesc: '41 micron',
        tooltip: 'This is wise band 1',
        imageId : 'wiseAtlasBand1',
        plotRequestParams  : {
            Service   : 'WISE',
            SurveyKey:  '3a',
            SurveyKeyBand: '1',
            title: 'WISE Atlas 1',
            drawingSubgroupID: 'wise',
        }
    }),
    clone (wiseAtlasTemplate, {
        title : 'band 2',
        wavelength: 42,
        wavelengthDesc: '42 micron',
        tooltip: 'This is wise band 2',
        imageId : 'wiseAtlasBand2',
        plotRequestParams  : {
            Service   : 'WISE',
            SurveyKey:  '3a',
            SurveyKeyBand: '2',
            title: 'WISE Atlas 2',
            drawingSubgroupID: 'wise',
        }
    }),
    clone (wiseAtlasTemplate, {
        title : 'band 3',
        wavelength: 43,
        wavelengthDesc: '43 micron',
        tooltip: 'This is wise band 3',
        imageId : 'wiseAtlasBand3',
        plotRequestParams  : {
            Service   : 'WISE',
            SurveyKey:  '3a',
            SurveyKeyBand: '3',
            title: 'WISE Atlas 3',
            drawingSubgroupID: 'wise',
        }
    }),
    clone (wiseAtlasTemplate, {
        title : 'band 4',
        wavelength: 44,
        wavelengthDesc: '44 micron',
        tooltip: 'This is wise band 4',
        imageId : 'wiseAtlasBand4',
        plotRequestParams  : {
            Service   : 'WISE',
            SurveyKey:  '3a',
            SurveyKeyBand: '4',
            title: 'WISE Atlas 4',
            drawingSubgroupID: 'wise',
        }
    }),
    clone (wiseLevel1Template, {
        title : 'Level 1 band 1',
        wavelength: 41,
        wavelengthDesc: '41 micron',
        tooltip: 'This is wise band 1',
        imageId : 'wiseLevel1Band1',
        plotRequestParams  : {
            Service   : 'WISE',
            SurveyKey:  '1b',
            SurveyKeyBand: '1',
            title: 'WISE 1b 1',
            drawingSubgroupID: 'wise',
        }
    }),
    clone (wiseLevel1Template, {
        title : 'Level 1 band 2',
        wavelength: 42,
        wavelengthDesc: '42 micron',
        tooltip: 'This is wise band 2',
        imageId : 'wiseLevel1Band2',
        plotRequestParams  : {
            Service   : 'WISE',
            SurveyKey:  '1b',
            SurveyKeyBand: '2',
            title: 'WISE 1b 2',
            drawingSubgroupID: 'wise',
        }
    }),
    clone (wiseLevel1Template, {
        title : 'Level 1 band 3',
        wavelength: 43,
        wavelengthDesc: '43 micron',
        tooltip: 'This is wise band 3',
        imageId : 'wiseLevel1Band3',
        plotRequestParams  : {
            Service   : 'WISE',
            SurveyKey:  '1b',
            SurveyKeyBand: '3',
            title: 'WISE 1b 3',
            drawingSubgroupID: 'wise',
        }
    }),
    clone (wiseLevel1Template, {
        title : 'Level 1 band 4',
        wavelength: 44,
        wavelengthDesc: '44 micron',
        tooltip: 'This is wise band 4',
        imageId : 'wiseLevel1Band4',
        plotRequestParams  : {
            Service   : 'WISE',
            SurveyKey:  '1b',
            SurveyKeyBand: '4',
            title: 'WISE 1b 4',
            drawingSubgroupID: 'wise',
        }
    }),
];


const twoMastMasterData = [
    clone (twoMastsTemplate, {
        title : 'J',
        wavelength: 1.25,
        wavelengthDesc: '1.25 micron',
        tooltip: 'This is 2MASS J',
        imageId : '2massJ',
        plotRequestParams  : {
            Service   : 'TWOMASS',
            SurveyKey:  'j',
            title: '2MASS J',
            drawingSubgroupID: '2mass',
        }
    }),
    clone (twoMastsTemplate, {
        title : 'H',
        wavelength: 1.65,
        wavelengthDesc: '1.65 micron',
        tooltip: 'This is 2MASS H',
        imageId : '2massH',
        plotRequestParams  : {
            Service   : 'TWOMASS',
            SurveyKey:  'h',
            title: '2MASS H',
            drawingSubgroupID: '2mass',
        }
    }),
    clone (twoMastsTemplate, {
        title : 'K',
        wavelength: 2.17,
        wavelengthDesc: '2.17 micron',
        tooltip: 'This is 2MASS K',
        imageId : '2massK',
        plotRequestParams  : {
            Service   : 'TWOMASS',
            SurveyKey:  'k',
            title: '2MASS K',
            drawingSubgroupID: '2mass',
        }
    }),
];


const msxMasterData = [
    clone (msxTemplate, {
        title : 'A (8.28 microns)',
        wavelength: 8.28,
        wavelengthDesc: '8.28 micron',
        tooltip: 'This is MSX A tip',
        imageId : 'msxA3',

        plotRequestParams  : {
            Service   : 'MSX',
            SurveyKey:  '3',
            title: 'MSX A',
            drawingSubgroupID: 'msx',
        }
    }),
    clone (msxTemplate, {
        title : 'C (12.13 microns)',
        wavelength: 12.13,
        wavelengthDesc: '12.13 micron',
        tooltip: 'This is MSX C tip',
        imageId : 'msxC4',

        plotRequestParams  : {
            Service   : 'MSX',
            SurveyKey:  '4',
            title: 'MSX C',
            drawingSubgroupID: 'msx',
        }
    }),
    clone (msxTemplate, {
        title : 'D (14.65 microns)',
        wavelength: 14.65,
        wavelengthDesc: '14.65 micron',
        tooltip: 'This is MSX D tip',
        imageId : 'msxD5',

        plotRequestParams  : {
            Service   : 'MSX',
            SurveyKey:  '5',
            title: 'MSX D',
            drawingSubgroupID: 'msx',
        }
    }),
    clone (msxTemplate, {
        title : 'E (21.3 microns)',
        wavelength: 21.3,
        wavelengthDesc: '21.3 micron',
        tooltip: 'This is MSX E tip',
        imageId : 'msxE6',

        plotRequestParams  : {
            Service   : 'MSX',
            SurveyKey:  '6',
            title: 'MSX E',
            drawingSubgroupID: 'msx',
        }
    }),
];

const dssMastMasterData = [
    clone (dssTemplate, {
        title : 'POSS2/UKSTU Red',
        wavelength: 0,
        wavelengthDesc: '0 micron',
        tooltip: 'This is DSS poss2ukstu_red',
        imageId : 'dss-poss2ukstu_red',
        plotRequestParams  : {
            Service   : 'DSS',
            SurveyKey:  'poss2ukstu_red',
            title: 'DSS poss2ukstu_red',
            drawingSubgroupID: 'dss',
        }
    }),
    clone (dssTemplate, {
        title : 'POSS2/UKSTU Infrared',
        wavelength: 0,
        wavelengthDesc: '0 micron',
        tooltip: 'This is DSS poss2ukstu_ir',
        imageId : 'dss-poss2ukstu_ir',
        plotRequestParams  : {
            Service   : 'DSS',
            SurveyKey:  'poss2ukstu_ir',
            title: 'DSS poss2ukstu_ir',
            drawingSubgroupID: 'dss',
        }
    }),
    clone (dssTemplate, {
        title : 'POSS2/UKSTU Blue',
        wavelength: 0,
        wavelengthDesc: '0 micron',
        tooltip: 'This is DSS poss2ukstu_blue',
        imageId : 'dss-poss2ukstu_blue',
        plotRequestParams  : {
            Service   : 'DSS',
            SurveyKey:  'poss2ukstu_blue',
            title: 'DSS poss2ukstu_blue',
            drawingSubgroupID: 'dss',
        }
    }),
    clone (dssTemplate, {
        title : 'POSS1 Red',
        wavelength: 0,
        wavelengthDesc: '0 micron',
        tooltip: 'This is DSS poss1_red',
        imageId : 'dss-poss1_red',
        plotRequestParams  : {
            Service   : 'DSS',
            SurveyKey:  'poss1_red',
            title: 'DSS poss1_red',
            drawingSubgroupID: 'dss',
        }
    }),
    clone (dssTemplate, {
        title : 'POSS1 Blue',
        wavelength: 0,
        wavelengthDesc: '0 micron',
        tooltip: 'This is DSS poss1_blue',
        imageId : 'dss-poss1_blue',
        plotRequestParams  : {
            Service   : 'DSS',
            SurveyKey:  'poss1_blue',
            title: 'DSS poss1_blue',
            drawingSubgroupID: 'dss',
        }
    }),
    clone (dssTemplate, {
        title : 'Quick-V Survey',
        wavelength: 0,
        wavelengthDesc: '0 micron',
        tooltip: 'This is DSS quickv',
        imageId : 'dss-quickv',
        plotRequestParams  : {
            Service   : 'DSS',
            SurveyKey:  'quickv',
            title: 'DSS quickv',
            drawingSubgroupID: 'dss',
        }
    }),
    clone (dssTemplate, {
        title : 'HST Phase 2 (GSC 2)',
        wavelength: 0,
        wavelengthDesc: '0 micron',
        tooltip: 'This is DSS phase2_gsc2',
        imageId : 'dss-phase2_gsc2',
        plotRequestParams  : {
            Service   : 'DSS',
            SurveyKey:  'phase2_gsc2',
            title: 'DSS phase2_gsc2',
            drawingSubgroupID: 'dss',
        }
    }),
    clone (dssTemplate, {
        title : 'HST Phase 1 (GSC 1)',
        wavelength: 0,
        wavelengthDesc: '0 micron',
        tooltip: 'This is DSS phase2_gsc1',
        imageId : 'dss-phase2_gsc1',
        plotRequestParams  : {
            Service   : 'DSS',
            SurveyKey:  'phase2_gsc1',
            title: 'DSS phase2_gsc1',
            drawingSubgroupID: 'dss',
        }
    }),
    clone (dssTemplate, {
        title : 'The best of a combined list',
        wavelength: 0,
        wavelengthDesc: '0 micron',
        tooltip: 'This is DSS phase2_gsc1',
        imageId : 'dss-all',
        plotRequestParams  : {
            Service   : 'DSS',
            SurveyKey:  'phase2_gsc1',
            title: 'DSS phase2_gsc1',
            drawingSubgroupID: 'dss',
        }
    }),
];


const sdssMasterData = [
    clone (sdssTemplate, {
        title : 'u',
        wavelength: 0,
        wavelengthDesc: '5 micron',
        tooltip: 'This is SDSS u',
        imageId : 'sdss-u',
        plotRequestParams  : {
            Service   : 'SDSS',
            SurveyKey:  'u',
            title: 'SDSS u',
            drawingSubgroupID: 'sdss',
        }
    }),
    clone (sdssTemplate, {
        title : 'g',
        wavelength: 0,
        wavelengthDesc: '0 micron',
        tooltip: 'This is SDSS g',
        imageId : 'sdss-g',
        plotRequestParams  : {
            Service   : 'SDSS',
            SurveyKey:  'g',
            title: 'SDSS g',
            drawingSubgroupID: 'sdss',
        }
    }),
    clone (sdssTemplate, {
        title : 'r',
        wavelength: 0,
        wavelengthDesc: '0 micron',
        tooltip: 'This is SDSS r',
        imageId : 'sdss-r',
        plotRequestParams  : {
            Service   : 'SDSS',
            SurveyKey:  'r',
            title: 'SDSS r',
            drawingSubgroupID: 'sdss',
        }
    }),
    clone (sdssTemplate, {
        title : 'i',
        wavelength: 0,
        wavelengthDesc: '0 micron',
        tooltip: 'This is SDSS i',
        imageId : 'sdss-i',
        plotRequestParams  : {
            Service   : 'SDSS',
            SurveyKey:  'i',
            title: 'SDSS i',
            drawingSubgroupID: 'sdss',
        }
    }),
    clone (sdssTemplate, {
        title : 'z',
        wavelength: 0,
        wavelengthDesc: '0 micron',
        tooltip: 'This is SDSS z',
        imageId : 'sdss-z',
        plotRequestParams  : {
            Service   : 'SDSS',
            SurveyKey:  'z',
            title: 'SDSS z',
            drawingSubgroupID: 'sdss',
        }
    }),
];


const irasMasterData = [
    clone (irasIssaTemplate, {
        title : '12 microns',
        wavelength: 12,
        wavelengthDesc: '12 microns',
        tooltip: 'This is ISSA 12',
        imageId : 'iras-issa-12',
        plotRequestParams  : {
            Service   : 'ISSA',
            SurveyKey:  '12',
            title: 'ISSA 12',
            drawingSubgroupID: 'iras',
        }
    }),
    clone (irasIssaTemplate, {
        title : '25 microns',
        wavelength: 25,
        wavelengthDesc: '25 microns',
        tooltip: 'This is ISSA 25',
        imageId : 'iras-issa-25',
        plotRequestParams  : {
            Service   : 'ISSA',
            SurveyKey:  '25',
            title: 'ISSA 25',
            drawingSubgroupID: 'iras',
        }
    }),
    clone (irasIssaTemplate, {
        title : '60 microns',
        wavelength: 60,
        wavelengthDesc: '60 microns',
        tooltip: 'This is ISSA 60',
        imageId : 'iras-issa-60',
        plotRequestParams  : {
            Service   : 'ISSA',
            SurveyKey:  '60',
            title: 'ISSA 60',
            drawingSubgroupID: 'iras',
        }
    }),
    clone (irasIssaTemplate, {
        title : '100 microns',
        wavelength: 100,
        wavelengthDesc: '100 microns',
        tooltip: 'This is ISSA 100',
        imageId : 'iras-issa-100',
        plotRequestParams  : {
            Service   : 'ISSA',
            SurveyKey:  '100',
            title: 'ISSA 100',
            drawingSubgroupID: 'iras',
        }
    }),
    clone (irasIrisTemplate, {
        title : '12 microns',
        wavelength: 12,
        wavelengthDesc: '12 microns',
        tooltip: 'This is IRIS 12',
        imageId : 'iras-iris-12',
        plotRequestParams  : {
            Service   : 'IRIS',
            SurveyKey:  '12',
            title: 'IRIS 12',
            drawingSubgroupID: 'iras',
        }
    }),
    clone (irasIrisTemplate, {
        title : '25 microns',
        wavelength: 25,
        wavelengthDesc: '25 microns',
        tooltip: 'This is IRIS 25',
        imageId : 'iras-iris-25',
        plotRequestParams  : {
            Service   : 'IRIS',
            SurveyKey:  '25',
            title: 'IRIS 25',
            drawingSubgroupID: 'iras',
        }
    }),
    clone (irasIrisTemplate, {
        title : '60 microns',
        wavelength: 60,
        wavelengthDesc: '60 microns',
        tooltip: 'This is IRIS 60',
        imageId : 'iras-iris-60',
        plotRequestParams  : {
            Service   : 'IRIS',
            SurveyKey:  '60',
            title: 'IRIS 60',
            drawingSubgroupID: 'iras',
        }
    }),
    clone (irasIrisTemplate, {
        title : '100 microns',
        wavelength: 100,
        wavelengthDesc: '100 microns',
        tooltip: 'This is IRIS 100',
        imageId : 'iras-iris-100',
        plotRequestParams  : {
            Service   : 'IRIS',
            SurveyKey:  '100',
            title: 'IRIS 100',
            drawingSubgroupID: 'iras',
        }
    }),
];


export const imageMasterData = [
    ...wiseMasterData, ...twoMastMasterData, ...dssMastMasterData,
    ...msxMasterData, ...sdssMasterData, ...irasMasterData
];
