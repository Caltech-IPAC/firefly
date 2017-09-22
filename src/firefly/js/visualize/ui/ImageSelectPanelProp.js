/**
 * Created by cwang on 3/10/16.
 */

import {get} from 'lodash';
import {getAppOptions} from '../../core/AppDataCntlr.js';

export const initPanelCatalogs = [
    {
        'id':'iras',
        'Title':'IRAS',
        'Symbol': 'IRAS',
        'CatalogId': 0,
        'fields': ['types'],
        'types': {
            'Title': 'Choose IRAS ISSA/IRIS Image:',
            'Items': [
                {'item':'issa-12',  'id': 0, 'name': 'ISSA 12 microns'},
                {'item':'issa-25',  'id': 1, 'name': 'ISSA 25 microns'},
                {'item':'issa-60',  'id': 2, 'name': 'ISSA 60 microns'},
                {'item':'issa-100', 'id': 3, 'name': 'ISSA 100 microns'},
                {'item':'iris-12',  'id': 4, 'name': 'IRAS: IRIS 12 microns'},
                {'item':'iris-25',  'id': 5, 'name': 'IRAS: IRIS 25 microns'},
                {'item':'iris-60',  'id': 6, 'name': 'IRAS: IRIS 60 microns'},
                {'item':'iris-100', 'id': 7, 'name': 'IRAS: IRIS 100 microns'}],
            'Default': 'iris-25'
        },
        'range': {'min': 1, 'max': 12.5, 'unit': 'deg'},
        'size': 5
    },
    /*
    {
        'id':'atlas',
        'Title':'Atlas',
        'Symbol': 'ATLAS',
        'CatalogId': 8,
        'fields': ['types', 'bands'],
        'types': {
            'Title': 'Atlas:',
            'Items': [
                {'item':'spitzer.seip_science', 'id': 0, 'name': 'Spitzer SEIP'},
                {'item':'spitzer.fls_irac', 'id': 1, 'name': 'Spitzer FLS'}
                ],
            'Default': 'spitzer.seip_science'
        },
        'bands': {
            'Title': 'Choose Band:',
            'Items': [
                {'item':`'IRAC1'`, 'id': 0, 'name': 'IRAC 1'},
                {'item':`'IRAC2'`, 'id': 1, 'name': 'IRAC 2'},
                {'item':`'IRAC3'`, 'id': 2, 'name': 'IRAC 3'},
                {'item':`'IRAC4'`, 'id': 3, 'name': 'IRAC 4'},
                {'item':`'MIPS24'`, 'id': 4, 'name': 'MIPS 1'}],
            'Default': `'IRAC1'`
        },
        'range': {'min':.008, 'max':5, 'unit': 'deg'},
        'size':.5
    },
    */
    {
        'id':'2mass',
        'Title':'2MASS',
        'Symbol': 'TWOMASS',
        'CatalogId': 1,
        'fields': ['types'],
        'types': {
            'Title': 'Choose 2MASS Image:',
            'Items': [
                {'item':'j', 'id': 0, 'name': 'J (1.25 microns)'},
                {'item':'h', 'id': 1, 'name': 'H (1.65 microns)'},
                {'item':'k', 'id': 2, 'name': 'K (2.17 microns)'}],
            'Default': 'j'
        },
        'range': {'min':.008, 'max':.139, 'unit': 'deg'},
        'size':.139
    },
    {
        'id': 'wise',
        'Title': 'WISE',
        'Symbol': 'WISE',
        'CatalogId': 2,
        'fields': ['types', 'bands'],
        'types': {
            'Title': 'WISE Level:',
            'Items': [
                {'item':'3a', 'id': 1, 'name': '4 Band Atlas'},
                {'item':'1b', 'id': 0, 'name': '4 Band level 1'},],
            'Default': '3a'
        },
        'bands': {
            'Title': 'WISE Bands:',
            'Items': [
                {'item':'1', 'id': 0, 'name': 'Band 1'},
                {'item':'2', 'id': 1, 'name': 'Band 2'},
                {'item':'3', 'id': 0, 'name': 'Band 3'},
                {'item':'4', 'id': 1, 'name': 'Band 4'}],
            'Default': '1'
        },
        'range': {'min':.01, 'max': 2, 'unit': 'deg'},
        'size':.15
    },
    {
        'id': 'msx',
        'Title': 'MSX',
        'Symbol': 'MSX',
        'CatalogId': 3,
        'fields': ['types'],
        'types': {
            'Title': 'Choose MSX Image:',
            'Items': [
                {'item': '3', 'id': 0, 'name': 'A (8.28 microns)'},
                {'item': '4', 'id': 1, 'name': 'C (12.13 microns)'},
                {'item': '5', 'id': 2, 'name': 'D (14.65 microns)'},
                {'item': '6', 'id': 3, 'name': 'E (21.3 microns)'}
            ],
            'Default': '3'
        },
        'range': {'min': 0.1, 'max': 1.5, 'unit': 'deg'},
        'size': 1.0
    },
    {
        'id': 'dss',
        'Title': 'DSS',
        'Symbol': 'DSS',
        'CatalogId': 4,
        'fields': ['types'],
        'types': {
            'Title': 'DSS Survey Types:',
            'Items': [
                {'item':'poss2ukstu_red', 'id': 0, 'name': 'POSS2/UKSTU Red'},
                {'item':'poss2ukstu_ir', 'id': 1, 'name': 'POSS2/UKSTU Infrared'},
                {'item':'poss2ukstu_blue', 'id': 2, 'name': 'POSS2/UKSTU Blue'},
                {'item':'poss1_red', 'id': 3, 'name': 'POSS1 Red'},
                {'item':'poss1_blue', 'id': 4, 'name': 'POSS1 Blue'},
                {'item':'quickv', 'id': 5, 'name': 'Quick-V Survey'},
                {'item':'phase2_gsc2', 'id': 6, 'name': 'HST Phase 2 Target Positioning(GSC 2)'},
                {'item':'phase2_gsc1', 'id': 7, 'name': 'HST Phase 1 Target Positioning(GSC 1)'},
                {'item':'all', 'id': 8, 'name': 'The best of a combined list of all plates'}
            ],
            'Default': 'poss2ukstu_red'
        },
        'range': {'min':.016, 'max': 0.5, 'unit': 'deg'},
        'size':.25
    },
    {
        'id': 'sdss',
        'Title': 'SDSS',
        'Symbol': 'SDSS',
        'CatalogId': 5,
        'fields': ['types'],
        'types': {
            'Title': 'Sloan DSS Survey Types:',
            'Items': [
                {'item':'u', 'id': 0, 'name': 'U'},
                {'item':'g', 'id': 1, 'name': 'G'},
                {'item':'r', 'id': 2, 'name': 'R'},
                {'item':'i', 'id': 3, 'name': 'I'},
                {'item':'z', 'id': 4, 'name': 'Z'}
            ],
            'Default': 'u'
        },
        'range': {'min': 0.016, 'max': 0.5, 'unit': 'deg'},
        'size':.25
    },
    {
        'id': 'fileUpload',
        'Title': 'FITS File',
        'Symbol': 'FITS',
        'CatalogId': 6,
        'fields': ['upload', 'list', 'extinput'],
        'button': {
            'Title': 'Enter URL of a FITS File:',
            'nullallowed': false
        },
        'upload': {
            'Title': ''
        },
        'list': {
            'Title': 'If file contains multiple extensions:',
            'Items': [
                {'item':'loadAll', 'id': 0, 'name': 'Load all the extensions'},
                {'item':'loadOne', 'id': 1, 'name': 'Load only one'}
            ]
        },
        'extinput': {
            'Title': 'Extension:',
            'nullallowed': false,
            'dependon': {
                'list':'loadOne'
            }
        }
    },
    {
        'id': 'url',
        'Title': 'URL',
        'Symbol': 'URL',
        'CatalogId': 7,
        'fields': ['input', 'list', 'extinput'],
        'input': {
            'Title': 'Enter URL of a FITS File:',
            'nullallowed': false
        },
        'list': {
            'Title': 'If file contains multiple extensions:',
            'Items': [
                {'item':'loadAll', 'id': 0, 'name': 'Load all the extensions'},
                {'item':'loadOne', 'id': 1, 'name': 'Load only one'}
            ]
        },
        'extinput': {
            'Title': 'Extension:',
            'nullallowed': false,
            'dependon': {
                'list': 'loadOne'
            }
        }
    }
];


const defaultOrder= [
    'atlas',
    'iras',
    '2mass',
    'wise',
    'msx',
    'dss',
    'sdss',
    'fileUpload',
    'url'
];


export const getPanelCatalogs = () => get(getAppOptions(), 'imageTabs', defaultOrder)
    .map( (id) => initPanelCatalogs.find( (p) => p.id===id))
    .filter( (e) => e)
    .map ( (e,idx) => Object.assign({},e,{CatalogId:idx}));



/*
  blank
 {
 'Title': 'Blank Image',
 'Symbol': 'BLANK',
 'CatalogId': 8,
 'fields': ['input'],
 'input': {
 'Title': 'Pixel Size:',
 'nullallowed': false,
 'Default': 400,
 'range': {'min': 100, 'max': 800}
 },
 'range': {'min': 0.01, 'max': 30, 'unit': 'deg'},
 'size': 0.139
 }
*/
