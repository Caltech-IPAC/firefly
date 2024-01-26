/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {useState} from 'react';
import PropTypes from 'prop-types';
import {get, isUndefined} from 'lodash';

import {FilterEditor} from '../../tables/ui/FilterEditor.jsx';
import {dispatchTableFilter} from '../../tables/TablesCntlr.js';
import {getTblById} from '../../tables/TableUtil.js';
import {useStoreConnector} from '../../ui/SimpleComponent.jsx';
import {Stack} from '@mui/joy';

export const FilterEditorWrapper = React.memo(({tbl_id}) => {
    const [sortInfo, setSortInfo] = useState('');
    useStoreConnector(() => get(getTblById(tbl_id), 'request.filters'));
    const tableModel = getTblById(tbl_id);
    return (
        <Stack height={450} width={650} overflow='hidden' position='relative' sx={{resize:'both', minWidth:550, minHeight:200}}>
          <Stack position='absolute' sx={{inset:'5px'}}>
                 <FilterEditor
                     tbl_id={tbl_id}
                     columns={get(tableModel, 'tableData.columns', [])}
                     selectable={false}
                     filterInfo={get(tableModel, 'request.filters')}
                     sortInfo={sortInfo}
                     onChange={(obj) => {
                         const fi = obj.filterInfo;
                         const {request} = getTblById(tbl_id);
                         if (!isUndefined(fi) && (fi !== get(request, 'filters'))) {
                             const newRequest = Object.assign({}, request, {filters: obj.filterInfo});
                             dispatchTableFilter(newRequest);
                         } else if (!isUndefined(obj.sortInfo)) {
                             setSortInfo(obj.sortInfo);
                         }
                     } }/>
             </Stack>
        </Stack>
     );
});


FilterEditorWrapper.propTypes = {
    tbl_id : PropTypes.string
};


