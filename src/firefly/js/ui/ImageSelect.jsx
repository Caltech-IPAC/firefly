/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {ChipDelete, Link, Stack, Tooltip, Typography} from '@mui/joy';
import React, {useContext, useEffect, useState} from 'react';

import PropTypes from 'prop-types';
import {uniqBy, get, countBy, remove, sortBy, isNil} from 'lodash';


import {CheckboxGroupInputField} from './CheckboxGroupInputField.jsx';
import {RadioGroupInputField} from './RadioGroupInputField.jsx';
import {CollapsibleGroup, CollapsibleItem} from '../ui/panel/CollapsiblePanel.jsx';
import FieldGroupUtils, {getFieldVal, getGroupFields, setFieldValue} from '../fieldGroup/FieldGroupUtils.js';
import {dispatchMultiValueChange} from '../fieldGroup/FieldGroupCntlr.js';
import {dispatchComponentStateChange} from '../core/ComponentCntlr.js';
import {updateSet} from '../util/WebUtil.js';
import {useFieldGroupValue, useFieldGroupWatch, useStoreConnector} from './SimpleComponent';
import {FD_KEYS, FG_KEYS} from 'firefly/visualize/ui/UIConst';
import {FieldGroupCtx} from 'firefly/ui/FieldGroup';

import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
import {CollapseAll, ExpandAll} from 'firefly/visualize/ui/Buttons.jsx';


const IMG_PREFIX = 'IMAGES_';
const FILTER_PREFIX = 'Filter_';
const PROJ_PREFIX = 'PROJ_ALL_';

/**
 * There are several important key related encoding that will help in understanding this file
 * There are 3 groups of checkboxes: filters, collapsible(project) boxes, and images
 * filters boxes are keyed(fieldkey) by FILTER_PREFIX + type
 *      - currently, there are 3 types: mission, projectType, and waveType
 *      - the view of this panel changes as these boxes are modified
 *      - it only affect the view, not what has already been selected.
 * collapsible boxes are keyed by PROJ_PREFIX + project
 *      - logically, they represent the state of its current/filtered checkboxes
 *      - when filter changes, this state will change
 * images boxes are keyed by IMG_PREFIX + project + [ || + subProject]
 *      - its option values contains the imageId found in imageMasterData
 *      - the state of these boxes are persistent and is not affected by filtering
 */

export function ImageSelect({imageMasterData, header, groupKey, multiSelect=true, addChangeListener, scrollDivId}) {
    const [isToolbarSticky, setIsToolbarSticky] = useState(false);

    useEffect(() => {
        addChangeListener && addChangeListener('ImageSelect', fieldsReducer(imageMasterData, groupKey));
        if (scrollDivId) {
            document.getElementById(scrollDivId).onscroll = (e) => {
                setIsToolbarSticky(e.target.scrollTop > 260);
            };
        }
    }, [addChangeListener, imageMasterData, groupKey, scrollDivId]);

    imageMasterData.forEach((d)=> {
        ['missionId', 'project', 'subProject'].forEach((k) => d[k] = d[k] || '');
    });

    const filteredImageData = useStoreConnector(() => getFilteredImageData(imageMasterData, groupKey));
    const [, setLastMod] = useState(new Date().getTime());

    const toolbarSx = isToolbarSticky
        ? {
            zIndex: 1,
            backgroundColor: 'background.surface',
            position: 'absolute',
            top: 0,
            left: 8,
            right: 8,
            borderBottom: 1,
            borderColor: 'neutral.outlinedBorder',
            pb: 1,
        }
        : {};

    return (
        <Stack spacing={1}>
            <ToolBar {...{filteredImageData, header, groupKey, onChange: () => setLastMod(new Date().getTime()),
                sx: toolbarSx}}/>
            <Stack spacing={1} direction='row'>
                <FilterPanel {...{imageMasterData, groupKey}}/>
                <DataProductList {...{filteredImageData, groupKey, multiSelect}}/>
            </Stack>
        </Stack>
    );
}

ImageSelect.propTypes = {
    imageMasterData: PropTypes.arrayOf(PropTypes.object).isRequired,
    groupKey: PropTypes.string.isRequired,
    // this component needs to be wrapped by a FieldGroup.  User of this component need to provide
    // a function so this component can handle field change event from reducing function.
    addChangeListener: PropTypes.func.isRequired,
    style: PropTypes.object,
    multiSelect: PropTypes.bool,
    scrollDivId: PropTypes.string           // this component is inside a scroll div
};

const toFilterSelectAry = (groupKey, s) => getFieldVal(groupKey, `Filter_${s}`, '').split(',').map((d) => d.trim()).filter((d) => d);

function getFilteredImageData(imageMasterData, groupKey, defaults={}) {

    const filterMission = isNil(defaults.mission) ? toFilterSelectAry(groupKey, 'mission') : defaults.mission;
    const filterProjectType = isNil(defaults.projectType) ? toFilterSelectAry(groupKey, 'projectType') : defaults.projectType;
    // const filterwaveBand = isNil(defaults.waveBand) ? toFilterSelectAry(groupKey, 'waveBand') : defaults.waveBand;
    const filterWaveType = isNil(defaults.waveType) ? toFilterSelectAry(groupKey, 'waveType') : defaults.waveType;

    let filteredImageData = imageMasterData;
    filteredImageData = filterMission.length > 0 ? filteredImageData.filter( (d) => filterMission.includes(d.missionId)) : filteredImageData;
    filteredImageData = filterProjectType.length > 0 ? filteredImageData.filter( (d) => filterProjectType.includes(d.projectTypeKey)) : filteredImageData;
    filteredImageData = filterWaveType.length > 0 ? filteredImageData.filter( (d) => filterWaveType.includes(d.waveType)) : filteredImageData;
    return filteredImageData;
}

function fieldsReducer(imageMasterData, groupKey) {

    return (inFields, action) => {
        const {fieldKey='', value=''} = action.payload;
        if (fieldKey.startsWith(PROJ_PREFIX)) {
            // a project checkbox is clicked.. only act on the currently filtered view
            const proj = fieldKey.replace(PROJ_PREFIX, '');
            const filteredImages = getFilteredImageData(imageMasterData, groupKey).filter((d) => d.project === proj);
            filteredImages.forEach(({project, subProject, imageId}) => {
                const fieldKey= IMG_PREFIX + project + (subProject ? '||' + subProject : '');
                const valAry = get(inFields, [fieldKey, 'value'], '').split(',').filter((v) => v);
                if (value === '_all_') {
                    if (!valAry.includes(imageId)) {    // add imageId is not already selected
                        valAry.push(imageId);
                        inFields = updateSet(inFields, [fieldKey, 'value'], valAry.join(','));
                    }
                } else {
                    if (valAry.includes(imageId)) {     // remove imageId if it was selected
                        remove(valAry, (v) => v === imageId);
                        inFields = updateSet(inFields, [fieldKey, 'value'], valAry.join(','));
                    }
                }
            });
        } else if (fieldKey.startsWith(IMG_PREFIX)) {
            // one item changed, update project selectAll checkbox
            const proj = fieldKey.replace(IMG_PREFIX, '').split('||')[0];
            const value = isAllSelected(getFilteredImageData(imageMasterData, groupKey), inFields, proj) ? '_all_' : '';
            inFields = updateSet(inFields, [PROJ_PREFIX + proj, 'value'], value);
        } else if (fieldKey.startsWith(FILTER_PREFIX)) {
            const type = fieldKey.replace(FILTER_PREFIX, '');
            // re-eval PROJECT level selections
            const filteredImageData = getFilteredImageData(imageMasterData, groupKey, {[type]: value});
            const projects= uniqBy(filteredImageData, 'project').map( (d) => d.project);
            projects.forEach((proj) =>  {
                const value = isAllSelected(filteredImageData, inFields, proj) ? '_all_' : '';
                inFields = updateSet(inFields, [PROJ_PREFIX+proj, 'value'], value);

            });
        }
        return inFields;

    };
}

function isAllSelected(filteredImageData, inFields, proj) {
    const availImageIds = filteredImageData.filter((d) => d.project === proj).map((d) => d.imageId);
    const selImageIds = Object.values(inFields)
        .filter((f) => get(f, 'fieldKey','').startsWith(IMG_PREFIX+proj))
        .map((f) => f.value).join(',').split(',')
        .filter((v) => v);
    const imgNotSel = availImageIds.filter((id) => !selImageIds.includes(id));
    return imgNotSel.length === 0;

}

function ToolBar({sx, header, filteredImageData, groupKey, onChange}) {
    const projects= uniqBy(filteredImageData, 'project').map( (d) => d.project);
    const setDSListMode = (flg) => {
        projects.forEach((k) => dispatchComponentStateChange(k, {isOpen:flg}));
        onChange && onChange();
    };
    const clearFields = (types) => {
        const fields = Object.values(FieldGroupUtils.getGroupFields(groupKey))
                            .filter( (f) => types.some( (t) => get(f, 'fieldKey','').startsWith(t)))
                            .filter( (f) => get(f, 'value'));        // look for all selected filters
        if (fields.length > 0) {
            fields.forEach((f) => f.value = '');
            dispatchMultiValueChange(groupKey, fields);
        }
    };
    const calcFilter = () => Object.values(FieldGroupUtils.getGroupFields(groupKey))
        .filter( (f) => get(f, 'fieldKey','').startsWith(FILTER_PREFIX))
        .filter( (f) => get(f, 'value'))                                    // look for only selected fields
        .map( (f) => f.value).join();                                       // get the value of the selected fields

    const calcSelect = () => Object.values(FieldGroupUtils.getGroupFields(groupKey))
        .filter( (f) => get(f, 'fieldKey','').startsWith(IMG_PREFIX))
        .filter( (f) => get(f, 'value'))                                    // look for only selected fields
        .map( (f) => f.options.filter( (o) => f.value.includes(o.value))
            .map((o) => o.label).join() )                 // takes the label of the selected field
        .join();

    const allFilter = useStoreConnector(calcFilter);
    const allSelect = useStoreConnector(calcSelect);
    const clrFilter = allFilter && (
        <Tooltip title='Clear Filters'>
            <ChipDelete onClick={() => clearFields([FILTER_PREFIX])} sx={{'--Chip-deleteSize': '1.2em'}}/>
        </Tooltip>
    );
    const clrSelect = allSelect && (
        <Tooltip title='Clear Selections'>
            <ChipDelete onClick={() => clearFields([IMG_PREFIX, PROJ_PREFIX])} sx={{'--Chip-deleteSize': '1.2em'}}/>
        </Tooltip>
    );

    return (
        <Stack direction='row' sx={sx}>
            {header?.()}
            <Stack direction='row' justifyContent='space-between' flex={1} alignItems='start' overflow='hidden' spacing={2}>
                <Stack flexGrow={1} overflow='hidden'>
                    <StatusLine label='Filter By:' msg={allFilter} endDecorator={clrFilter}/>
                    <StatusLine label='Selection:' msg={allSelect} endDecorator={clrSelect}/>
                </Stack>
                <Stack direction='row' >
                    <ExpandAll onClick={() => setDSListMode(true)} iconButtonSize='32px' tip='Expand All'/>
                    <CollapseAll onClick={() => setDSListMode(false)} iconButtonSize='32px' tip='Collapse All'/>
                </Stack>
            </Stack>
        </Stack>
    );
}

/*--------------------------- Filter Panel ---------------------------------------------*/
function FilterPanel({imageMasterData}) {
    return(
        <Stack flexGrow={0}>
            <FilterPanelView {...{imageMasterData}}/>
        </Stack>
    );
}

function getBandList(imageMasterData){

  const forSummary = uniqBy(imageMasterData, (t) => `${t.missionId};${t.project};${t.waveType}`);
  const waveInfo = uniqBy(forSummary.map ( (entry)=> {
      return {name:entry.waveType, label:entry.waveType,wavelength:parseFloat(entry.wavelength), wavelengthDesc:entry.wavelengthDesc };

  }), 'name');

  const waveType = toFilterSummary(forSummary , 'waveType', 'waveType');

  for (let i=0; i<waveInfo.length; i++){
      switch (waveInfo[i].waveUnit){
          case 'angstroms':
              waveInfo[i].wavelength = waveInfo[i].wavelength*0.0001;
              break;
          case 'mm':
              waveInfo[i].wavelength = waveInfo[i].wavelength*1000;
              break;
          case 'cm':
              waveInfo[i].wavelength = waveInfo[i].wavelength*10000;
              break;
          case 'GHz':
              waveInfo[i].wavelength = waveInfo[i].wavelength*299792.458;
              break;

      }
      for (let j=0; j<waveType.length; j++){

          if (waveInfo[i].name===waveType[j].name){
              waveInfo[i]['count'] = waveType[j].count;
              break;
          }
      }
  }
  return  sortBy(waveInfo, 'wavelength');

}
function FilterPanelView({imageMasterData}) {

    const forSummary = uniqBy(imageMasterData, (t) => `${t.missionId};${t.project}`);
    const missions = toFilterSummary(forSummary, 'missionId', 'missionId');
    const projectTypes = toFilterSummary(forSummary, 'projectTypeKey', 'projectTypeDesc');

    //order by wavelength
    const waveType=getBandList(imageMasterData);

    return (
        <Stack>
            <CollapsibleGroup>
                <CollapsibleItem componentKey='missionFilter' header='MISSION:' isOpen={true} title='Please select some boxes'>
                    <FilterSelect {...{type:'mission', dataList: missions}}/>
                </CollapsibleItem>
                <CollapsibleItem componentKey='projectTypesFilter' header='PROJECT TYPE:' isOpen={true} title='Please select some boxes'>
                    <FilterSelect {...{type:'projectType', dataList: projectTypes}}/>
                </CollapsibleItem>
                <CollapsibleItem componentKey='waveTypesFilter' header='BAND:' isOpen={true} title='Please select some boxes'>
                    <FilterSelect {...{type:'waveType', dataList: waveType}}/>
                </CollapsibleItem>
            </CollapsibleGroup>
        </Stack>

    );
}

/**
 * @param {object}   p
 * @param {string}   p.type
 * @param {object[]} p.dataList
 * @param {number}   p.maxShown
 */
function FilterSelect ({type, dataList, maxShown=6}) {
    const [showExpanded, setShowExpanded] = useState(false);
    const {groupKey} = useContext(FieldGroupCtx);

    const fieldKey= `Filter_${type}`;
    const options = toFilterOptions(dataList);
    const hasMore = maxShown < options.length && !showExpanded;
    const dispOptions = showExpanded ? options : options.slice(0, maxShown);
    const hasLess = dispOptions.length > maxShown;

    // get value from the filter field if it has been set before filter component is mounted, otherwise set '' for all options
    const initialVal = getFieldVal(groupKey, fieldKey, '');

    return (
        <Stack>
            <CheckboxGroupInputField
                key={fieldKey}
                fieldKey={fieldKey}
                initialState={{ options: dispOptions,
                                value: initialVal,
                                label : '' }}
                options={dispOptions}
                alignment='vertical'
            />

            { hasMore && <Link onClick={() => setShowExpanded(true)}>more</Link>
            }
            { hasLess && <Link onClick={() => setShowExpanded(false)}>less</Link>
            }
        </Stack>
    );
}

// save in case we want to do it this way
function FilterSelectExpanded({selections, onClose}) {
    return (
        <div className='FilterPanel__item--exp'>
            <div style={{width: '100%', height: 15, backgroundColor: '#f3f3f3'}}>
                <div style={{float: 'right', top: 1}}
                     className='btn-close'
                     title='Close popup'
                     onClick={onClose}
                />
            </div>
            <div style={{padding: '0 10px'}}>
                {selections}
            </div>
        </div>

    );
}

const toFilterOptions = (a) => a.map ( (d) => ({label: `${d.label}  (${d.count})`, value: d.name}));
const toFilterSummary = (master, key, desc) => Object.entries(countBy(master, (d) => `${get(d, key)};${get(d, desc)}`))
                                                    .map( ([d, c]) => ({name: d.split(';')[0], label: d.split(';')[1], count: c}));


/*--------------------------- Data Product List ---------------------------------------*/

function DataProductList({filteredImageData, groupKey, multiSelect}) {
    // Create a field for storing default images in rgb field group
    const [getDefaultImages, setDefaultImages] = useFieldGroupValue('defaultImages', FG_KEYS.rgb);

    const updateDefaultImages = (color) => ([coloredImageVal]) => {
        // if it's a colored (red, green, or blue) DataProductList and if image for that color is selected for the first time
        if (groupKey===FG_KEYS[color] && (coloredImageVal && !getDefaultImages()?.[color])) {
            const selectedColoredImage = filteredImageData.find((d) => d.imageId === coloredImageVal);

            // if defaultColor field (default images for rgb) is defined and default image specified for that color is same as selected image
            if (selectedColoredImage?.defaultColor && selectedColoredImage.defaultColor[color]===coloredImageVal) {
                setDefaultImages(selectedColoredImage.defaultColor);

                // also change image source of rest of colored images to archive
                ['red', 'green', 'blue'].forEach((c)=> setFieldValue(FG_KEYS[c], FD_KEYS.source, 'archive'));

                // also set filters on rest of colored images, if present on this one
                const filterFieldKeys = Object.keys(getGroupFields(FG_KEYS[color])).filter((fld)=>fld.startsWith('Filter'));
                filterFieldKeys.forEach((filterFieldKey)=>{
                    const filterVal = getFieldVal(FG_KEYS[color], filterFieldKey, '');
                    if(filterVal){
                        ['red', 'green', 'blue'].forEach((c)=> setFieldValue(FG_KEYS[c], filterFieldKey, filterVal));
                    }
                });
            }
        }
    };

    // Update default images if image selection for any of 3 colors is changed
    useFieldGroupWatch([IMG_PREFIX+FG_KEYS.red], updateDefaultImages('red'), [], FG_KEYS.red);
    useFieldGroupWatch([IMG_PREFIX+FG_KEYS.green], updateDefaultImages('green'), [], FG_KEYS.green);
    useFieldGroupWatch([IMG_PREFIX+FG_KEYS.blue], updateDefaultImages('blue'), [], FG_KEYS.blue);

    let defaultImage = '';
    if (groupKey===FG_KEYS.red) defaultImage = getDefaultImages()?.red;
    else if (groupKey===FG_KEYS.green) defaultImage = getDefaultImages()?.green;
    else if (groupKey===FG_KEYS.blue) defaultImage = getDefaultImages()?.blue;

    const projects= uniqBy(filteredImageData, 'project').map( (d) => d.project);

    let content;
    if (projects.length > 0) {
        content = projects.map((p) => <DataProduct key={p} {...{groupKey, project:p, filteredImageData, multiSelect, defaultImage}}/>);
    } else {
        content = (
            <div style={{display:'flex', justifyContent:'center', marginTop: 40}}>
                <div>No data match these criteria</div>
            </div>
        );
    }

    return (
        <Stack flexGrow={1}>
            <CollapsibleGroup disableDivider={true} sx={{'& button': {justifyContent: 'start'}}}>
                {content}
            </CollapsibleGroup>
        </Stack>
    );
}

function DataProduct({groupKey, project, filteredImageData, multiSelect, defaultImage}) {

    // filter projects ... projects is like dataproduct or dataset.. i.e SEIP
    const projectData= filteredImageData.filter((d) => d.project === project);
    const subProjects= uniqBy(projectData, 'subProject').map( (d) => d.subProject);
    const helpUrl = uniqBy(projectData, 'helpUrl').map( (d) => d.helpUrl);
    const labelMaxWidth = subProjects.filter((s) => s).reduce( (rval, s) => (s.length > rval ? s.length : rval), 0);
    const isOpen = hasImageSelection(groupKey, project);

    return (
        <Stack>
            <CollapsibleItem componentKey={project} isOpen={isOpen}
                             slotProps={{content:{variant:'soft'}}}
                             header={<Header {...{project, hrefInfo:helpUrl, multiSelect}}/>}>
                <Stack>
                    {
                        subProjects.map((sp) =>
                            <BandSelect key={'sub_' + sp} {...{groupKey, subProject:sp, projectData, labelMaxWidth, multiSelect, defaultImage}}/>
                        )
                    }
                </Stack>
            </CollapsibleItem>
        </Stack>
    );

}

function Header({project, hrefInfo='', multiSelect}) {
    const fieldKey= `${PROJ_PREFIX}${project}`;

    const InfoIcon = () => hrefInfo && (
        <a onClick={(e) => e.stopPropagation()} target='_blank' href={hrefInfo}>
            <InfoOutlinedIcon sx={{width:'1.2rem', verticalAlign:'middle'}}/>
        </a>
    );

    if (multiSelect) {
        return (
            <Stack spacing={1} direction='row' alignItems='center'>
                <div onClick={(e) => e.stopPropagation()}>
                    <CheckboxGroupInputField
                        key={fieldKey}
                        fieldKey={fieldKey}
                        initialState={{
                            value: '',   // workaround for _all_ for now
                            label : '' }}
                        options={[{label:project, value:'_all_'}]}
                        alignment='horizontal'
                        sx={{fontWeight: 'normal'}}
                    />
                </div>
                <InfoIcon/>
            </Stack>
        );
    } else {
        return (
            <Stack spacing={1} direction='row' alignItems='center'>
                <Typography sx={{fontWeight:'normal'}} level='body-sm'>{project}</Typography>
                <InfoIcon/>
            </Stack>
        );
    }

}

const hasImageSelection = (groupKey, proj) => {
    const fields = FieldGroupUtils.getGroupFields(groupKey) || {};
    return Object.values(fields).some((fld) => get(fld, 'fieldKey', '').startsWith(`${IMG_PREFIX}${proj}`) && fld.value);
};


function BandSelect({groupKey, subProject, projectData, labelMaxWidth, multiSelect, defaultImage}) {
    const fieldKey= IMG_PREFIX + get(projectData, [0, 'project']) + (subProject ? '||' + subProject : '');
    const options = toImageOptions(projectData.filter( (p) => p.subProject === subProject));
    const label = subProject && (
                    <Stack {...{direction: 'row', alignItems:'center'}}>
                        <Typography title={subProject}
                                    width={labelMaxWidth+1+'ch'}
                                    textAlign='right'
                                    noWrap={true}
                                    overflow='hidden'
                                    textOverflow='ellipsis'
                                    maxWidth={100}
                        >
                            {subProject}
                        </Typography>
                        <span>:</span>
                    </Stack>);
    if (multiSelect) {
        return (
            <Stack direction='row' alignItems='center'>
                {label}
                <CheckboxGroupInputField
                    sx={{
                        '& .ff-Checkbox-container':{flexWrap:'wrap'},
                        '& .ff-Checkbox-item:first-of-type':{ml:2}
                    }}
                    key={fieldKey}
                    fieldKey={fieldKey}
                    initialState={{
                        options,        // Note: values in initialState are saved into fieldgroup.  options are used in the reducer above to determine what 'all' means.
                        value: '',
                         }}
                    options={options}
                    orientation='horizontal'
                />
            </Stack>
        );
    } else {
        return (             
            <Stack direction='row'>
                {label}
                <RadioGroupInputField
                    sx={{
                        '& .ff-RadioGroup-container':{flexWrap:'wrap'},
                        '& .ff-RadioGroup-item:first-of-type':{ml:1.5},
                        '& .ff-Checkbox-item':{mt:1/4}
                    }}
                    key={`${groupKey}_${fieldKey}`}
                    fieldKey={`${IMG_PREFIX}${groupKey}`}
                    isGrouped={true}
                    initialState={{
                            options,        // Note: values in initialState are saved into fieldgroup.  options are used in the reducer above to determine what 'all' means.
                            value: defaultImage,
                            }}
                    options={options}
                    defaultValue=''
                    orientation='horizontal'
                />
            </Stack>
        );
    }
}

const toImageOptions= (a) => a.map ( (d) => ({label: d.title, value: d.imageId}));

function StatusLine ({label, msg, max=100000, endDecorator}) {
    msg = msg || 'none';
    const words = msg.split(',');
    let pretty = ' ';
    for(var i=0; i< words.length; i++) {
        if (pretty.length + words[i].length > max) {
            pretty += ` (${words.length-i} more)`;
            break;
        }
        pretty += words[i] + (i === words.length-1 ? '' : ', ');
    }
    return (
        <Stack direction='row' overflow='hidden' spacing={1}>
            <Typography {...{level:'body-xs', color:'neutral'}} whiteSpace='nowrap'>{label}</Typography>
            <Stack direction='row' overflow='hidden' alignItems='start'>
                <Typography {...{level:'body-xs', color:'warning', title:msg}}
                            sx={{
                                overflow: 'hidden',
                                textOverflow: 'ellipsis',
                                whiteSpace: 'nowrap'
                            }}>
                    {pretty}
                </Typography>
                {endDecorator}
            </Stack>
        </Stack>
    );
}


