/**
 * Created by cwang on 3/10/16.
 */
const panelCatalogs = [
    {
        'Title':'IRSA',
        'Symbol': 'IRSA',
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
        'range': {'min': 3600, 'max': 45000, 'unit': 'arcsec'},
        'size': 18000
    },
    {
        'Title':'2MASS',
        'Symbol': '2MASS',
        'CatalogId': 1,
        'fields': ['types'],
        'types': {
            'Title': 'Choose 2MASS Image:',
            'Items': [
                {'item':'j', 'id': 0, 'name': 'J (1.25 microns)'},
                {'item':'h', 'id': 1, 'name': 'H (1.65 microns)'},
                {'item':'k', 'id': 2, 'name': 'K (2.17 microns)'}],
            'Default': 'j',
        },
        'range': {'min': 29, 'max': 500, 'unit': 'arcsec'},
        'size': 500
    },
    {
        'Title': 'WISE',
        'Symbol': 'WISE',
        'CatalogId': 2,
        'fields': ['types', 'bands'],
        'types': {
            'Title': 'WISE Level:',
            'Items': [
                {'item':'1b', 'id': 0, 'name': '4 Band level 1'},
                {'item':'3a', 'id': 1, 'name': '4 Band Atlas'}],
            'Default': '1b',
        },
        'bands': {
            'Title': 'WISE Bands:',
            'Items': [
                {'item':'1', 'id': 0, 'name': 'Band 1'},
                {'item':'2', 'id': 1, 'name': 'Band 2'},
                {'item':'3', 'id': 0, 'name': 'Band 3'},
                {'item':'4', 'id': 1, 'name': 'Band 4'}],
            'Default': '1',
        },
        'range': {'min': 36, 'max': 10800, 'unit': 'arcsec'},
        'size': 500
    }
];

export default panelCatalogs;