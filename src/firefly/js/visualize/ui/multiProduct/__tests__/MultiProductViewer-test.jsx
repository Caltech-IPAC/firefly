/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
/*eslint-env node, jest */

import React from 'react';
import {render} from '@testing-library/react';
import {getDataProducts} from '../../../../metaConvert/DataProductsCntlr.js';
import {DPtypes} from '../../../../metaConvert/DataProductsType.js';
import {MultiProductViewer} from '../MultiProductViewer.jsx';

// the data products store holds one viewer; each test sets its product with getDataProducts
jest.mock('../../../../metaConvert/DataProductsCntlr.js', () => ({
    dataProductRoot: () => [{dpId: 'dp-viewer'}],
    dispatchInitDataProducts: jest.fn(),
    dispatchSetSearchParams: jest.fn(),
    dispatchUpdateActiveKey: jest.fn(),
    getActivateParams: () => ({imageViewerId: 'image-viewer', chartViewerId: 'chart-viewer'}),
    getActiveFileMenuKey: jest.fn(),
    getActiveFileMenuKeyByKey: jest.fn(),
    getDataProducts: jest.fn(),
    getSearchParams: jest.fn(),
    getServiceParamsAry: jest.fn(),
    isServiceDescriptorActivated: jest.fn(),
}));
jest.mock('../../../../ui/SimpleComponent.jsx', () => ({
    useStoreConnector: (getState) => getState(),
}));
jest.mock('../../../MultiViewCntlr.js', () => ({
    dispatchAddViewer: jest.fn(),
    dispatchViewerUnmounted: jest.fn(),
    getLayoutDetails: jest.fn(),
    getMultiViewRoot: jest.fn(),
}));
// the product views themselves are not under test
jest.mock('../MultiProductChoice.jsx', () => ({MultiProductChoice: () => null}));
jest.mock('../MPMessages.jsx', () => ({
    AdvancedMessage: () => null, ProductDownload: () => null, ProductMessage: () => null, TextFileViewer: () => null,
}));
jest.mock('../DPDropdown.jsx', () => ({createMakeDropdownFunc: jest.fn()}));
jest.mock('../../../../ui/dynamic/ServiceDescriptorPanel.jsx', () => ({ServiceDescriptorPanel: () => null}));

const DP_ID = 'dp-viewer';

beforeEach(() => {
    jest.clearAllMocks();
});

describe('MultiProductViewer', () => {
    test('the cleanup of a product gets the product that replaces it', () => {
        const imageCleanup = jest.fn();
        getDataProducts.mockReturnValue({displayType: DPtypes.IMAGE, activate: () => imageCleanup});
        const {rerender} = render(<MultiProductViewer viewerId={DP_ID} metaDataTableId='tbl-1'/>);
        getDataProducts.mockReturnValue({displayType: DPtypes.TABLE, activate: jest.fn()});
        rerender(<MultiProductViewer viewerId={DP_ID} metaDataTableId='tbl-1'/>);
        expect(imageCleanup).toHaveBeenCalledWith({nextDisplayType: DPtypes.TABLE, nextMetaDataTableId: 'tbl-1'});
    });

    test('the cleanup of a product gets the table of the product that replaces it', () => {
        const imageCleanup = jest.fn();
        getDataProducts.mockReturnValue({displayType: DPtypes.IMAGE, activate: () => imageCleanup});
        const {rerender} = render(<MultiProductViewer viewerId={DP_ID} metaDataTableId='tbl-1'/>);
        getDataProducts.mockReturnValue({displayType: DPtypes.IMAGE, activate: jest.fn()});
        rerender(<MultiProductViewer viewerId={DP_ID} metaDataTableId='tbl-2'/>);
        expect(imageCleanup).toHaveBeenCalledWith({nextDisplayType: DPtypes.IMAGE, nextMetaDataTableId: 'tbl-2'});
    });

    test('a working state is not taken as the next product, so the cleanup gets the product being left', () => {
        const imageCleanup = jest.fn();
        getDataProducts.mockReturnValue({displayType: DPtypes.IMAGE, activate: () => imageCleanup});
        const {rerender} = render(<MultiProductViewer viewerId={DP_ID} metaDataTableId='tbl-1'/>);
        getDataProducts.mockReturnValue({displayType: DPtypes.MESSAGE, message: 'Loading...', isWorkingState: true});
        rerender(<MultiProductViewer viewerId={DP_ID} metaDataTableId='tbl-1'/>);
        expect(imageCleanup).toHaveBeenCalledWith({nextDisplayType: DPtypes.IMAGE, nextMetaDataTableId: 'tbl-1'});
    });
});
