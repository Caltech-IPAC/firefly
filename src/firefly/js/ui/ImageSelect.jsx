/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent, useEffect} from 'react';

import PropTypes from 'prop-types';
import {uniqBy, get, countBy, remove, sortBy, isNil} from 'lodash';


import {CheckboxGroupInputField} from './CheckboxGroupInputField.jsx';
import {RadioGroupInputField} from './RadioGroupInputField.jsx';
import {CollapsiblePanel} from '../ui/panel/CollapsiblePanel.jsx';
import FieldGroupUtils, {getFieldVal} from '../fieldGroup/FieldGroupUtils.js';
import {dispatchMultiValueChange} from '../fieldGroup/FieldGroupCntlr.js';
import {dispatchComponentStateChange} from '../core/ComponentCntlr.js';
import {updateSet} from '../util/WebUtil.js';
import {useStoreConnector} from './SimpleComponent';
import shallowequal from 'shallowequal';

import './ImageSelect.css';
import infoIcon from 'html/images/info-icon.png';


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

export function ImageSelect({style, imageMasterData, groupKey, multiSelect=true, addChangeListener}) {

    useEffect(() => {
        addChangeListener && addChangeListener('ImageSelect', fieldsReducer(imageMasterData, groupKey));
    }, [addChangeListener, imageMasterData, groupKey]);

    imageMasterData.forEach((d)=> {
        ['missionId', 'project', 'subProject'].forEach((k) => d[k] = d[k] || '');
    });

    const [filteredImageData] = useStoreConnector.bind({comparator: shallowequal})(() => getFilteredImageData(imageMasterData, groupKey));

    return (
        <div style={style} className='ImageSelect'>
            <ToolBar className='ImageSelect__toolbar' {...{filteredImageData, groupKey, onChange: () => this.setState({lastMod:new Date().getTime()})}}/>
            <div style={{flexGrow: 1, display: 'flex', height: 1}}>
                <div className='ImageSelect__panels' style={{marginRight: 3, flexGrow: 0}}>
                    <FilterPanel {...{imageMasterData, groupKey}}/>
                </div>
                <div className='ImageSelect__panels' style={{flexGrow: 1}}>
                    <DataProductList {...{filteredImageData, groupKey, multiSelect}}/>
                </div>
            </div>
        </div>
    );
}

ImageSelect.propTypes = {
    imageMasterData: PropTypes.arrayOf(PropTypes.object).isRequired,
    groupKey: PropTypes.string.isRequired,
    // this component needs to be wrapped by a FieldGroup.  User of this component need to provide
    // a function so this component can handle field change event from reducing function.
    addChangeListener: PropTypes.func.isRequired,
    style: PropTypes.object,
    multiSelect: PropTypes.bool
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
                if (value === '_all_' && !valAry.includes(imageId)) {
                    valAry.push(imageId);
                    inFields = updateSet(inFields, [fieldKey, 'value'], valAry.join(','));
                } else if (valAry.includes(imageId)) {
                    remove(valAry, (v) => v === imageId);
                    inFields = updateSet(inFields, [fieldKey, 'value'], valAry.join(','));
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

function ToolBar({filteredImageData, groupKey, onChange}) {
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

    return (
        <div className='ImageSelect__toolbar'>
            <div>
                &bull;<a style={{marginRight: 7}} className='ff-href' onClick={() => clearFields([FILTER_PREFIX])}>Clear Filters</a>
            </div>
            <div>
                &bull;<a style={{marginRight: 7}} className='ff-href' onClick={() => clearFields([IMG_PREFIX, PROJ_PREFIX])}>Clear Selections</a>
                &bull;<a style={{marginRight: 7}} className='ff-href' onClick={() => setDSListMode(true)}>Expand All</a>
                &bull;<a className='ff-href' onClick={() => setDSListMode(false)}>Collapse All</a>
            </div>
        </div>
    );
}

/*--------------------------- Filter Panel ---------------------------------------------*/
function FilterPanel({imageMasterData, groupKey, onChange}) {
    const allSelect = Object.values(FieldGroupUtils.getGroupFields(groupKey))
        .filter( (f) => get(f, 'fieldKey','').startsWith(FILTER_PREFIX))
        .filter( (f) => get(f, 'value'))                                    // look for only selected fields
        .map( (f) => f.value).join();                                       // get the value of the selected fields

    return(
        <div className='FilterPanel'>
            <div className='ImageSelect__title'>Filter By:</div>
            <div className='ImageSelect__info'>{pretty(allSelect, 25)}</div>
            <FilterPanelView {...{onChange, imageMasterData}}/>
        </div>
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
function FilterPanelView({onChange, imageMasterData}) {

    const forSummary = uniqBy(imageMasterData, (t) => `${t.missionId};${t.project}`);
    const missions = toFilterSummary(forSummary, 'missionId', 'missionId');
    const projectTypes = toFilterSummary(forSummary, 'projectTypeKey', 'projectTypeDesc');

    //order by wavelength
    const waveType=getBandList(imageMasterData);

    return (
        <div className='FilterPanel__view'>
            <CollapsiblePanel componentKey='missionFilter' header='MISSION:' isOpen={true}>
                <FilterSelect {...{onChange, type:'mission', dataList: missions}}/>
            </CollapsiblePanel>
            <CollapsiblePanel componentKey='projectTypesFilter' header='PROJECT TYPE:' isOpen={true}>
                <FilterSelect {...{onChange, type:'projectType', dataList: projectTypes}}/>
            </CollapsiblePanel>
            <CollapsiblePanel componentKey='waveTypesFilter' header='BAND:' isOpen={true}>
                <FilterSelect {...{onChange, type:'waveType', dataList: waveType}}/>
            </CollapsiblePanel>
        </div>
    );
}

/**
 *
 * @param {function} onChange
 * @param {string}   type
 * @param {object[]} dataList
 * @param {string}   dataList.name  ID
 * @param {string}   dataList.label desc
 * @param {number}   dataList.count
 */
class FilterSelect extends PureComponent {
    constructor(props) {
        super(props);
        this.state = {showExpanded: false};
    }

    render() {
        const {type, dataList, onChange, maxShown=6} = this.props;
        const {showExpanded} = this.state;
        const fieldKey= `Filter_${type}`;
        const options = toFilterOptions(dataList);
        const hasMore = maxShown < options.length && !showExpanded;
        const dispOptions = showExpanded ? options : options.slice(0, maxShown);
        const hasLess = dispOptions.length > maxShown;

        return (
            <div className='FilterPanel__item--std'>
                <CheckboxGroupInputField
                    key={fieldKey}
                    fieldKey={fieldKey}
                    initialState={{ options: dispOptions,
                                    value: '',   // workaround for _all_ for now
                                    tooltip: 'Please select some boxes',
                                    label : '' }}
                    options={dispOptions}
                    alignment='vertical'
                    labelWidth={35}
                    wrapperStyle={{whiteSpace: 'nowrap'}}
                />

                { hasMore && <a className='ff-href' style={{paddingLeft: 20, fontWeight: 'bold'}}
                                onClick={() => this.setState({showExpanded: true})}>more</a>
                }
                { hasLess && <a className='ff-href' style={{paddingLeft: 20, fontWeight: 'bold'}}
                                onClick={() => this.setState({showExpanded: false})}>less</a>
                }
            </div>
        );
    }
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

function DataProductList({filteredImageData, groupKey, multiSelect, onChange}) {
    const projects= uniqBy(filteredImageData, 'project').map( (d) => d.project);

    const allSelect = Object.values(FieldGroupUtils.getGroupFields(groupKey))
                            .filter( (f) => get(f, 'fieldKey','').startsWith(IMG_PREFIX))
                            .filter( (f) => get(f, 'value'))                                    // look for only selected fields
                            .map( (f) => f.options.filter( (o) => f.value.includes(o.value))
                                                  .map((o) => o.label).join() )                 // takes the label of the selected field
                            .join();
    let content;
    if (projects.length > 0) {
        content = projects.map((p) => <DataProduct key={p} {...{groupKey, project:p, filteredImageData, multiSelect}}/>);
    } else {
        content = (
            <div style={{display:'flex', justifyContent:'center', marginTop: 40}}>
                <div>No data match these criteria</div>
            </div>
        );
    }

    return (
        <div className='DataProductList'>
            <div className='ImageSelect__title'>Selection:</div>
            <div className='ImageSelect__info'>{pretty(allSelect, 100)}</div>
            <div className='DataProductList__view'>{content}</div>
        </div>
    );
}

function DataProduct({groupKey, project, filteredImageData, multiSelect}) {

    // filter projects ... projects is like dataproduct or dataset.. i.e SEIP
    const projectData= filteredImageData.filter((d) => d.project === project);
    const subProjects= uniqBy(projectData, 'subProject').map( (d) => d.subProject);
    const helpUrl = uniqBy(projectData, 'helpUrl').map( (d) => d.helpUrl);
    const labelMaxWidth = subProjects.filter((s) => s).reduce( (rval, s) => (s.length > rval ? s.length : rval), 0);
    const isOpen = hasImageSelection(groupKey, project);

    return (
        <div className='DataProductList__item'>
            <CollapsiblePanel componentKey={project} header={<Header {...{project, hrefInfo:helpUrl, multiSelect}}/>} isOpen={isOpen}>
                <div className='DataProductList__item--details'>
                    {
                        subProjects.map((sp) =>
                            <BandSelect key={'sub_' + sp} {...{groupKey, subProject:sp, projectData, labelMaxWidth, multiSelect}}/>
                        )
                    }
                </div>
            </CollapsiblePanel>
        </div>
    );

}

function Header({project, hrefInfo='', multiSelect}) {
    const fieldKey= `${PROJ_PREFIX}${project}`;

    const InfoIcon = () => hrefInfo && (
        <div>
            <a onClick={(e) => e.stopPropagation()} target='_blank' href={hrefInfo}>
                <img style={{width:'14px'}} src={infoIcon} alt='info'/></a>
        </div>
    );

    if (multiSelect) {
        return (
            <div className='DataProductList__item--header' >
                <div onClick={(e) => e.stopPropagation()}>
                    <CheckboxGroupInputField
                        key={fieldKey}
                        fieldKey={fieldKey}
                        initialState={{
                            value: '',   // workaround for _all_ for now
                            tooltip: 'Please select some boxes',
                            label : '' }}
                        options={[{label:project, value:'_all_'}]}
                        alignment='horizontal'
                        labelWidth={35}
                        wrapperStyle={{whiteSpace: 'normal' /*cursor:'pointer'*/}}
                    />
                </div>
                <div style={{marginLeft: -5}}>
                    <InfoIcon/>
                </div>
            </div>
        );
    } else {
        return (
            <div style={{display: 'inline-flex', alignItems: 'center'}}>
                <div style={{marginRight:5}}>{project}</div>
                <InfoIcon/>
            </div>
        );
    }

}

const hasImageSelection = (groupKey, proj) => {
    const fields = FieldGroupUtils.getGroupFields(groupKey) || {};
    return Object.values(fields).some((fld) => get(fld, 'fieldKey', '').startsWith(`${IMG_PREFIX}${proj}`) && fld.value);
};


function BandSelect({groupKey, subProject, projectData, labelMaxWidth, multiSelect}) {
    const fieldKey= IMG_PREFIX + get(projectData, [0, 'project']) + (subProject ? '||' + subProject : '');
    const options = toImageOptions(projectData.filter( (p) => p.subProject === subProject));
    const label = subProject && (
                    <div style={{display: 'inline-flex'}}>
                        <div style={{width: labelMaxWidth+1+'ch'}} title={subProject}
                             className='DataProductList__item--bandLabel'>{subProject}</div>
                        <span>:</span>
                    </div>);
    if (multiSelect) {
        return (
            <div className='DataProductList__item--band'>
                {label}
                <CheckboxGroupInputField
                    key={fieldKey}
                    fieldKey={fieldKey}
                    initialState={{
                        options,        // Note: values in initialState are saved into fieldgroup.  options are used in the reducer above to determine what 'all' means.
                        value: '',
                        tooltip: 'Please select some boxes',
                        label : '' }}
                    options={options}
                    alignment='horizontal'
                    labelWidth={35}
                    wrapperStyle={{whiteSpace: 'normal'}}
                />
            </div>
        );
    } else {
        return (             
            <div className='DataProductList__item--band'>
                {label}
                <RadioGroupInputField
                    key={`${groupKey}_${fieldKey}`}
                    fieldKey={`${IMG_PREFIX}${groupKey}`}
                    isGrouped={true}
                    initialState={{
                            options,        // Note: values in initialState are saved into fieldgroup.  options are used in the reducer above to determine what 'all' means.
                            value: '',
                            tooltip: 'Please select some boxes',
                            label : '' }}
                    options={options}
                    defaultValue=''
                    alignment='horizontal'
                    labelWidth={35}
                    wrapperStyle={{whiteSpace: 'normal'}}
                />
            </div>
        );
    }
}

const toImageOptions= (a) => a.map ( (d) => ({label: d.title, value: d.imageId}));

function pretty(str, max) {
    const words = str.split(',');
    let pretty = '';
    for(var i=0; i< words.length; i++) {
        if (pretty.length + words[i].length > max) {
            pretty += ` (${words.length-i} more)`;
            break;
        }
        pretty += words[i] + (i === words.length-1 ? '' : ',');
    }
    return pretty;
}