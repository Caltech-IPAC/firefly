import {Button, Stack} from '@mui/joy';
import React from 'react';
import {array, object, string} from 'prop-types';
import {
    dispatchActivateFileMenuItem, dispatchActivateMenuItem,
    dispatchSetSearchParams, dispatchUpdateDataProducts, getActiveFileMenuKey, getActiveMenuKey
} from '../../../metaConvert/DataProductsCntlr.js';
import {DPtypes} from '../../../metaConvert/DataProductsType.js';
import {SingleColumnMenu} from '../../../ui/DropDownMenu.jsx';
import {DropDownToolbarButton} from '../../../ui/DropDownToolbarButton.jsx';
import {TextButton} from '../../../ui/TextButton.jsx';
import {ToolbarButton} from '../../../ui/ToolbarButton.jsx';

/**
 *
 * @param  {{}} obj
 * @param {String} obj.dpId
 * @param {DataProductsDisplayType} obj.dataProductsState
 * @param {boolean} obj.showMenu - true to show the Menu
 * @param {boolean} obj.showRedoSearchButton - true to show the button
 * @param {Function} obj.extraction a function to extract the data product into somewhere else
 * @return {function} function to create the drap down menu
 */
export function createMakeDropdownFunc({ dpId, dataProductsState, showMenu, extraction, showRedoSearchButton} ) {
    const {menu, fileMenu, activeMenuLookupKey, menuKey, analysisActivateFunc,
        originalTitle, extractionText} = dataProductsState;
    const hasFileMenu = (fileMenu?.menu?.length ?? 0) > 1;
    const hasMenu = showMenu && menu && menu.length > 0;
    if (!hasMenu && !hasFileMenu && !showRedoSearchButton && !extraction) return undefined;
    return () => (
        <DropDown {...{
            dataProductsState, originalTitle, menuKey, hasMenu, menu, dpId, hasFileMenu, fileMenu, analysisActivateFunc,
            showRedoSearchButton, activeMenuLookupKey, extraction, extractionText}}/>
    );
}

function DropDown({dataProductsState, menuKey, originalTitle, hasMenu, menu, dpId, hasFileMenu, fileMenu,
                      analysisActivateFunc, showRedoSearchButton, activeMenuLookupKey,
                      extraction, extractionText}) {
    return (
        <Stack {...{direction:'row', alignItems:'center', height: 30}}>
            {!hasMenu ?
                <div style={{width: 50, height: 1}}/> :
                <DropDownToolbarButton
                    text='More' tip='Other data to display' useDropDownIndicator={true}
                    style={{paddingRight: 20}}
                    dropDown={<OtherOptionsDropDown {...{menu, dpId, activeMenuLookupKey}} />}
                />}

            {hasFileMenu &&
                <DropDownToolbarButton
                    text='File Contents' tip='Other data in file' useDropDownIndicator={true}
                    style={{paddingRight: 20}}
                    dropDown={<FileMenuDropDown {...{fileMenu, dpId}} />}/>
            }
            {extraction &&
                <Button onClick={() => extraction()} title={extractionText || 'Pin'}
                        sx={{whiteSpace:'nowrap', minHeight:10, py:.25}}>
                    {extractionText || 'Pin'}
                </Button>}
            {showRedoSearchButton && analysisActivateFunc &&
                <ToolbarButton
                    text='Redo Search' tip='Redo Search'
                    onClick={() => {
                        dispatchSetSearchParams({dpId, activeMenuLookupKey, menuKey, params: undefined});
                        dispatchUpdateDataProducts(dpId, {
                            ...dataProductsState, allowsInput: true, name: originalTitle,
                            displayType: DPtypes.ANALYZE, activate: analysisActivateFunc
                        });
                    }}/>
            }
        </Stack>
    );

}

const OtherOptionsDropDown= ({menu, dpId, activeMenuLookupKey}) => {
    return (
        <SingleColumnMenu>
            {menu.map( (menuItem, idx) => (
                <ToolbarButton text={menuItem.name} tip={`${menuItem.semantics} - ${menuItem.url}`}
                               horizontal={false} key={'otherOptions-'+idx} hasCheckBox={true}
                               checkBoxOn={menuItem.menuKey===getActiveMenuKey(dpId, activeMenuLookupKey)}
                               onClick={() => {
                                   dispatchActivateMenuItem(dpId,menuItem.menuKey);
                               } }/> )
            )}
        </SingleColumnMenu> );
};

OtherOptionsDropDown.propTypes= { dpId : string, menu : array, activeMenuLookupKey : string, };

const FileMenuDropDown= ({fileMenu, dpId}) => (
    <SingleColumnMenu>
        {fileMenu.menu.map( (fileMenuItem, idx) => (
            <ToolbarButton text={fileMenuItem.name} tip={fileMenuItem.name}
                           style={fileMenuItem.interpretedData ? {paddingLeft: 30} : {}}
                           horizontal={false} key={'fileMenuOptions-'+idx} hasCheckBox={true}
                           checkBoxOn={fileMenuItem.menuKey===getActiveFileMenuKey(dpId,fileMenu)}
                           onClick={() => dispatchActivateFileMenuItem({dpId,fileMenu,newActiveFileMenuKey:fileMenuItem.menuKey})}/> )
        )}
    </SingleColumnMenu> );

FileMenuDropDown.propTypes= { dpId : string, fileMenu : object};