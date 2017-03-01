/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {Component, PropTypes} from 'react';
import {get} from 'lodash';
import FieldGroupUtils from '../../fieldGroup/FieldGroupUtils.js';
import {InputGroup} from '../../ui/InputGroup.jsx';
import {RadioGroupInputField} from '../../ui/RadioGroupInputField.jsx';
import {ListBoxInputField} from '../../ui/ListBoxInputField.jsx';
import {SizeInputFields} from '../../ui/SizeInputField.jsx';
import {TargetPanel} from '../../ui/TargetPanel.jsx';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import {SpatialMethod} from '../../ui/CatalogSearchMethodType.jsx';


const mcenTip= `Specifies whether to return only the most centered (in pixel space)
 image-set for the given input position.  In multi-input mode, this
 flag applies only for input tables without a best column.`;

export class LSSTImageSpatialType extends Component {

    constructor(props) {
        super(props);
        this.state= {fields:null};
    }

    componentWillUnmount() {
        if (this.removeListener) this.removeListener();
        this.iAmMounted= false;
    }

    componentDidMount() {
        this.iAmMounted= true;
        this.removeListener= FieldGroupUtils.bindToStore(this.props.groupKey, (fields) => {
            if (this.iAmMounted) this.setState({fields});
        });
    }

    render() {
        const {fields}= this.state;
        const searchType= get(fields, 'intersect.value', 'CENTER');
        const {groupKey} = this.props;


        return (
            <FieldGroup groupKey={groupKey}
                        keepState={true}
                        reducerFunc={imageSearchReducer}
                        style={{display:'flex', flexDirection:'column', alignItems:'center'}}>
                {renderTargetPanel(groupKey, searchType)}
                <div style={{padding: 3}}>
                    <InputGroup labelWidth={270}>
                        <ListBoxInputField
                            fieldKey='intersect'
                            initialState= {{
                                          tooltip: 'Please select an option',
                                          label : 'Search Type (Region Intersection):',
                                          value:'CENTER'
                                      }}
                            options={ [
                                        {value: 'CENTER', label: 'Image contains target' },
                                        {value: 'COVERS', label: 'Image covers entire search region' },
                                        {value: 'ENCLOSED', label: 'Image is entirely enclosed by search region' },
                                        {value: 'OVERLAPS', label: 'Any pixel overlaps search region', disabled: true},
                                        {value: 'ALLSKY', label: 'All Sky'}] }
                            multiple={false}
                        />
                        {searchType === 'ALLSKY' && renderAllSkyNote()}
                        {searchType !== 'ALLSKY' && renderSearchRegion(searchType !== 'CENTER')}
                        {searchType !== 'ALLSKY' && renderImageSize(searchType === 'CENTER' || searchType === 'COVERS', false)}
                        {searchType !== 'ALLSKY' && renderMostCenter(searchType ==='CENTER' || searchType === 'COVERS', true)}
                    </InputGroup>
                </div>
            </FieldGroup>
        );
    }
}

function renderTargetPanel(groupKey, searchType) {
    return (
        <div className='intarget'>
            {(searchType !== 'ALLSKY') && <TargetPanel labelWidth={100} groupKey={groupKey}/>}
        </div> 
    );
}


function renderSearchRegion(visible) {
    
    return (
        <SizeInputFields fieldKey='size'
                         wrapperStyle={{visibility:visible?'visible':'hidden', marginTop: 5}}
                         initialState= {{
                                           value: '.1',
                                           tooltip: 'Please select an option',
                                           unit: 'arcsec',
                                           min:  1/3600,
                                           max:  43200/3600
                                 }}
                         label='Search Region (Square) Size:' />
    );
}

function renderImageSize(visible, disable) {

   var disableWrapper = disable ? {pointerEvents: 'none', opacity: 0.5} : {};
   var wrapper = {visibility:visible?'visible':'hidden', marginTop: 5,...disableWrapper};


   return (
       <SizeInputFields fieldKey='subsize'
                        wrapperStyle={wrapper}
                        initialState= {{
                                           value: '', // default 0.1?
                                           tooltip: 'Please select an option',
                                           unit: 'arcsec',
                                           min:  1/3600,
                                           max:  7200/3600
                                        }}
                        label='Return Image Size (leave blank for full images):'
                        />
   ) ;
}

function renderMostCenter(visible, disable) {
    var disableWrapper = disable ? {pointerEvents: 'none', opacity: 0.5} : {};
    var wrapper = {visibility:visible?'visible':'hidden', marginTop: 5,...disableWrapper};

    return (
        <RadioGroupInputField fieldKey='mcenter'
                              wrapperStyle={wrapper}
                              inline={true}
                              alignment='horizontal'
                              initialState={{
                                        tooltip: mcenTip,
                                        value: 'mcen',
                                        label: 'Return the most centered image containing the target:'
                                   }}
                              options={[
                                      {label: 'Yes', value: 'mcen'},
                                      {label: 'No', value: 'all'}
                                      ]}
        />
    );
}

function renderAllSkyNote() {
    return (
        <div style={{display:'flex', flexDirection:'column', alignItems:'center'}} >
            <div style={{marginTop: 10,  border: '1px solid #a3aeb9', padding:'30px 30px'}}>
                Search the catalog with no spatial constraints
            </div>
        </div>
    );
}


LSSTImageSpatialType.propTypes = { groupKey: PropTypes.string };


function imageSearchReducer(inFields, action) {
    if (!inFields) {
        return fieldInit();
    } else {
        return inFields;
    }
}

function fieldInit() {
    return (
    {
        spatial: {
            fieldKey: 'spatial',
            value: SpatialMethod.Box.value
        }
    }
    );
}