/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {uniqBy, get, countBy} from 'lodash';


import {CheckboxGroupInputField} from './CheckboxGroupInputField.jsx';
import {CollapsiblePanel} from '../ui/panel/CollapsiblePanel.jsx';
import FieldGroupUtils, {getFieldVal} from '../fieldGroup/FieldGroupUtils.js';
import {dispatchComponentStateChange} from '../core/ComponentCntlr.js';

import './ImageSelect.css';


export class ImageSelect extends PureComponent {

    constructor(props) {
        super(props);
        this.state= {lastMod:new Date().getTime(), filteredImageData:undefined};   // can be open, close, or auto.
    }

    render() {
        const {style, imageMasterData, groupKey} = this.props;
        var {filteredImageData=imageMasterData} = this.state;
        const filterMission = toFilterSelectAry(groupKey, 'mission');
        const filterProjectType = toFilterSelectAry(groupKey, 'projectType');
        const filterwaveBand = toFilterSelectAry(groupKey, 'waveBand');

        filteredImageData = filterMission.length > 0 ? filteredImageData.filter( (d) => filterMission.includes(d.project)) : filteredImageData;
        filteredImageData = filterProjectType.length > 0 ? filteredImageData.filter( (d) => filterProjectType.includes(d.projectTypeKey)) : filteredImageData;
        filteredImageData = filterwaveBand.length > 0 ? filteredImageData.filter( (d) => filterwaveBand.includes(d.wavelength)) : filteredImageData;

        return (
            <div style={style} className='ImageSelect'>
                <FilterPanel {...{imageMasterData}}/>
                <DataProductList {...{filteredImageData, groupKey, onChange: () => this.setState({lastMod:new Date().getTime()})}}/>
            </div>
        );
    }

}

ImageSelect.propTypes = {
    imageMasterData: PropTypes.arrayOf(PropTypes.object),
    groupKey: PropTypes.string,
    style: PropTypes.object,
    title: PropTypes.string
};

const toFilterSelectAry = (groupKey, s) => getFieldVal(groupKey, `Filter_${s}`, '').split(',').map((d) => d.trim()).filter((d) => d);


/*--------------------------- Filter Panel ---------------------------------------------*/

// eslint-disable-next-line
function FilterPanel({imageMasterData, title='4. Select Data Set', onChange}) {
    return(
        <div className='FilterPanel'>
            <div className='FilterPanel__toolbar'>{title}</div>
            <FilterPanelView {...{onChange, imageMasterData}}/>
        </div>
    );
}

// eslint-disable-next-line
function FilterPanelView({onChange, imageMasterData}) {
    const missions = toCatSummary(imageMasterData, 'project', 'project');
    const projectTypes = toCatSummary(imageMasterData, 'projectTypeKey', 'projectTypeDesc');
    const waveBands = toCatSummary(imageMasterData, 'wavelength', 'wavelengthDesc');

    return (
        <div className='FilterPanel__view'>
            <CollapsiblePanel componentKey='missionFilter' header='MISSION:' isOpen={true}>
                <FilterSelect {...{onChange, type:'mission', dataList: missions}}/>
            </CollapsiblePanel>
            <CollapsiblePanel componentKey='projectTypesFilter' header='PROJECT TYPE:' isOpen={true}>
                <FilterSelect {...{onChange, type:'projectType', dataList: projectTypes}}/>
            </CollapsiblePanel>
            <CollapsiblePanel componentKey='waveBandsFilter' header='WAVEBAND' isOpen={true}>
                <FilterSelect {...{onChange, type:'waveBand', dataList: waveBands}}/>
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
        // eslint-disable-next-line
        const {type, dataList, onChange, maxShown=3} = this.props;
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
                    initialState={{
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
// eslint-disable-next-line
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
const toCatSummary = (master, key, desc) => Object.entries(countBy(master, (d) => `${get(d, key)};${get(d, desc)}`))
                                                    .map( ([d, c]) => ({name: d.split(';')[0], label: d.split(';')[1], count: c}));


/*--------------------------- Data Product List ---------------------------------------*/

// eslint-disable-next-line
function DataProductList({filteredImageData, groupKey, onChange}) {
    const projects= uniqBy(filteredImageData, 'project').map( (d) => d.project);
    const setDSListMode = (flg) => {
                                projects.forEach((k) => dispatchComponentStateChange(k, {isOpen:flg}));
                                onChange && onChange();
                            };

    return (
        <div className='DataProductList'>
            <div className='DataProductList__toolbar'>
                &bull;<a style={{marginRight: 7}} className='ff-href' onClick={() => setDSListMode(true)}>Expand All</a>
                &bull;<a className='ff-href' onClick={() => setDSListMode(false)}>Collapse All</a>
            </div>
            <div className='DataProductList__view'>
                {
                    projects.map((p) =>
                        <DataProduct key={p} {...{groupKey, project:p, filteredImageData}}/>
                    )
                }
            </div>
        </div>
    );
}

// eslint-disable-next-line
function DataProduct({groupKey, project, filteredImageData}) {

    // filter projects ... projects is like dataproduct or dataset.. i.e SEIP
    const projectData= filteredImageData.filter((d) => d.project === project);
    const subProjects= uniqBy(projectData, 'subProject').map( (d) => d.subProject);
    const labelMaxWidth = subProjects.filter((s) => s).reduce( (rval, s) => (s.length > rval ? s.length : rval), 0);
    const isOpen = hasImageSelection(groupKey, project);

    return (
        <div className='DataProductList__item'>
            <CollapsiblePanel componentKey={project} header={project} isOpen={isOpen}>
                <div className='DataProductList__item--details'>
                    {
                        subProjects.map((sp) =>
                            <BandSelect key={'sub_' + sp} {...{subProject:sp, projectData, labelMaxWidth}}/>
                        )
                    }
                </div>
            </CollapsiblePanel>
        </div>
    );

}

const hasImageSelection = (groupKey, proj) => {
    const fields = FieldGroupUtils.getGroupFields(groupKey) || {};
    return Object.values(fields).some((fld) => fld.fieldKey.startsWith(`IMAGES_${proj}`) && fld.value);
};


// eslint-disable-next-line
function BandSelect({subProject, projectData, labelMaxWidth}) {
    const fieldKey= `IMAGES_${get(projectData, [0, 'project'])}_${subProject}`;
    const options = toImageOptions(projectData.filter( (p) => p.subProject === subProject));
    const label = subProject && <div style={{minWidth: labelMaxWidth+'ch'}} className='DataProductList__item--bandLabel'>{subProject + ':'}</div>;
    return (
        <div className='DataProductList__item--band'>
            {label}
            <CheckboxGroupInputField
                key={fieldKey}
                fieldKey={fieldKey}
                initialState={{
                        value: '',   // workaround for _all_ for now
                        tooltip: 'Please select some boxes',
                        label : '' }}
                options={options}
                alignment='horizontal'
                labelWidth={35}
                wrapperStyle={{whiteSpace: 'normal'}}
            />
        </div>
    );
}

const toImageOptions= (a) => a.map ( (d) => ({label: d.title, value: d.imageId}));
