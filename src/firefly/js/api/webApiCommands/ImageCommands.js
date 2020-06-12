import WebPlotRequest, {findInvalidWPRKeys, WPConst} from '../../visualize/WebPlotRequest';
import {isEmpty,isArray} from 'lodash';
import {dispatchPlotHiPS, dispatchPlotImage} from '../../visualize/ImagePlotCntlr';
import {RequestType} from '../../visualize/RequestType';
import {DEFAULT_FITS_VIEWER_ID} from '../../visualize/MultiViewCntlr';
import {makeExamples, ReservedParams} from '../WebApi';





const imageOverview= {
    overview: [
        'Load and image to firefly either from URL or from a service'
    ],
    allowAdditionalParameters: true,
    parameters: {
        url : 'URL of the FITS image',
        title : 'title to show',
        service : 'one of IRIS, ISSA, DSS, SDSS, TWOMASS, MSX-ATLAS, WISE, ATLAS,ZTF, PTF',
        SurveyKey : [
            'wise: Atlas',
            '2mass: asky,',
            'DSS: poss2ukstu_red poss2ukstu_ir poss2ukstu_blue poss1_red poss1_blue quickv phase2_gsc2 phase2_gsc1',
            'msx:  msx.msx_images',
            'SDSS- u,g,r,i,z,'
        ],
        SurveyKeyBand : [
            'IRIS, ISSA: 12,25,60,100',
            'TWOMASS: j,h,k',
            'MSX: A,C,D,E',
            'WISE: 1,2,3,4'
        ],
        [ReservedParams.POSITION.name]: ['coordinates of the image',...ReservedParams.POSITION.desc],
        [ReservedParams.SR.name]: ['size of image',...ReservedParams.SR.desc],
        colorTable : 'the color table to use: 0 - 21',
        MultiImageExts: 'a comma separated list of image extensions to load from the file'

    },
};
const imageExamples= [
    {
        sectionDesc: 'Create image using url',
        examples: [
            {
                desc:'Load from URL',
                params:{url: 'http://web.ipac.caltech.edu/staff/roby/data-products-test/1904-66_SFL.fits'}
                },
            {
                desc:'Load from URL all extensions',
                params:{url: 'http://web.ipac.caltech.edu/staff/roby/data-products-test/j8zs05yxq_flt.fits'}
            },
            {
                desc:'Load from URL extension 0 and 2',
                params:{url: 'http://web.ipac.caltech.edu/staff/roby/data-products-test/j8zs05yxq_flt.fits', MultiImageExts: '1,3' }
            }
        ] ,
    },
    {
        sectionDesc: 'Create images using a service',
        examples:[
            {
                desc:'Load from Wise',
                params:{service:'WISE', SurveyKey:'Atlas', SurveyKeyBand :'2', WorldPt :'10.68479;41.26906;EQ_J2000', sr  : '.12'}
            },
            {
                desc:'Load from MSX',
                params:{service:'ATLAS', SurveyKey:'msx.msx_images', SurveyKeyBand :'D', WorldPt :'10.68479;41.26906;EQ_J2000', sr  : '430s'}
            },
            {
                desc:'Load from SDSS',
                params: {service:'SDSS', SurveyKey:'u', WorldPt :'148.88822;69.06529;EQ_J2000', sr  : '8.4m'}
            },
            {
                desc:'Load from 2MASS',
                params: {service:'TWOMASS', SurveyKey:'asky', SurveyKeyBand:'j', WorldPt :'148.88822;69.06529;EQ_J2000', sr  : '.12d'}
            },
            {
                desc:'Load from DSS',
                params: {service:'DSS', SurveyKey:'poss2ukstu_red', WorldPt :'148.88822;69.06529;EQ_J2000', sr  : '600s'}
            },
        ]
    },
];


const hipsOverview= {
    overview: [
        'Load and HiPS to firefly from a service'
    ],
    parameters: {
        uri: {desc:'uri of the HiPS repository, you may have multiple uri parameters for mulitple HiPS', isRequired:true},
        [ReservedParams.SR.name]: ['field of view of the HiPS image to show (optional)',...ReservedParams.SR.desc],
        [ReservedParams.POSITION.name]: ['Point to center HiPS on (optional)',...ReservedParams.POSITION.desc],
    },
};
const hipsExamples= [
    {desc:'ALLWISE', params:{uri: 'ivo://CDS/P/allWISE/color'}},
    {desc:'ALLWISE- m51, 3 deg ', params:{uri: 'ivo://CDS/P/allWISE/color', sr:3, WorldPt:'202.48417;47.23056;EQ_J2000'}},
    {desc:'2MASS', params:{uri: 'ivo://CDS/P/2MASS/color'}},
    {desc:'DSS', params:{uri: 'ivo://CDS/P/DSS2/color'}},
    {desc:'SDSS', params:{uri: 'ivo://CDS/P/SDSS9/color'}},
    {desc:'SDSS- m81, 60 arcmin', params:{uri: 'ivo://CDS/P/SDSS9/color', ra: '148.88822', dec: '69.06529', sr:'40m'}},
    {desc:'2 hips', params:{uri: ['ivo://CDS/P/DSS2/color','ivo://CDS/P/2MASS/color'], ra: '148.88822', dec: '69.06529', sr:'40m'}},
];

const imageRootStr= 'API_plotId';
let nextId= 1;

function validateImage(inParams) {
    const params= {...inParams};
    if (params[ReservedParams.POSITION.name]) {
        params[WPConst.WORLD_PT]= params[ReservedParams.POSITION.name];
        Reflect.deleteProperty(params, ReservedParams.POSITION.name);
    }
    if (params[ReservedParams.SR.name]) {
        params[WPConst.SIZE_IN_DEG]= params[ReservedParams.SR.name];
        Reflect.deleteProperty(params, ReservedParams.SR.name);
    }
    const badParams= findInvalidWPRKeys(params);
    if (badParams.length || isEmpty(params)) {
        return {valid:false, msg:`url contains unsupported params: ${badParams.join()}`, badParams};
    }
    return {valid:true};
}

function showImage(cmd,inParams) {
    const params= {...inParams};
    if (params[ReservedParams.POSITION.name]) {
        params[WPConst.WORLD_PT]= params[ReservedParams.POSITION.name];
        Reflect.deleteProperty(params, ReservedParams.POSITION.name);
    }
    if (params[ReservedParams.SR.name]) {
        params[WPConst.SIZE_IN_DEG]= params[ReservedParams.SR.name];
        Reflect.deleteProperty(params, ReservedParams.SR.name);
    }
    const r= WebPlotRequest.makeFromObj(params);
    if (!params[WPConst.PLOT_GROUP_ID]) r.setPlotGroupId('webApiGroup');
    const plotId= `${imageRootStr}-${nextId++}`;
    dispatchPlotImage({plotId, wpRequest:r});
}


function validateHiPS(params) {
    return {valid:true};
}

function showHiPs(cmd,inParams) {
    const params= {...inParams};
    if (params[ReservedParams.POSITION.name]) {
        params[WPConst.WORLD_PT]= params[ReservedParams.POSITION.name];
        Reflect.deleteProperty(params, ReservedParams.POSITION.name);
    }
    if (params[ReservedParams.SR.name]) {
        params[WPConst.SIZE_IN_DEG]= params[ReservedParams.SR.name];
        Reflect.deleteProperty(params, ReservedParams.SR.name);
    }
    const uriAry= isArray(params.uri) ? params.uri : [params.uri];
    Reflect.deleteProperty(params, 'uri');

    uriAry
        .map( (entry) => ({...params,[WPConst.HIPS_ROOT_URL]:entry}))
        .forEach( (p) => {
            const r= WebPlotRequest.makeFromObj(p);
            if (!p[WPConst.PLOT_GROUP_ID]) r.setPlotGroupId('webApiGroup');
            const plotId= `${imageRootStr}-${nextId++}`;
            r.setPlotId(plotId);
            r.setRequestType(RequestType.HiPS);
            dispatchPlotHiPS({plotId, wpRequest:r, viewerId:DEFAULT_FITS_VIEWER_ID });
        });

}



/**
 * @return Array.<WebApiCommand>
 */
export function getImageCommands() {
    return [
        {
            cmd: 'image',
            validate: validateImage,
            execute: showImage,
            ...imageOverview,
            examples: makeExamples('image', imageExamples),
        },
        {
            cmd : 'hips',
            validate : validateHiPS,
            execute: showHiPs,
            ...hipsOverview,
            examples: makeExamples('hips', hipsExamples),
        },
    ];
}

