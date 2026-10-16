/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
/*eslint-env node, jest */

import React from 'react';
import '@testing-library/jest-dom';
import {act, fireEvent, render, screen} from '@testing-library/react';

import {createInputCell, inputColumnRenderer} from '../ui/TableRenderer.js';

const TIP = 'syntax help';

const cellBuilders = {
    createInputCell: () => createInputCell(TIP, 10, () => ({valid: true})),
    inputColumnRenderer: () => inputColumnRenderer({tbl_id: 't1', cname: 'c', tooltips: TIP}),
};

const hover = (el) => {
    fireEvent.mouseOver(el);
    act(() => jest.advanceTimersByTime(1000));
};

describe.each(Object.keys(cellBuilders))('%s syntax tooltip', (name) => {
    beforeEach(() => jest.useFakeTimers());
    afterEach(() => jest.useRealTimers());

    const data = [['a'], ['b']];

    test('hover alone shows no tooltip', () => {
        const Cell = cellBuilders[name]();
        render(<Cell rowIndex={0} data={data} colIdx={0}/>);
        hover(screen.getByRole('textbox'));
        expect(screen.queryByRole('tooltip')).toBeNull();
    });

    test('focus + hover shows the tooltip', () => {
        const Cell = cellBuilders[name]();
        render(<Cell rowIndex={0} data={data} colIdx={0}/>);
        const input = screen.getByRole('textbox');
        act(() => input.focus());
        hover(input);
        expect(screen.getByRole('tooltip')).toHaveTextContent(TIP);
    });

    test('a cell reused for another row forgets focus that was never blurred', () => {
        const Cell = cellBuilders[name]();
        const {rerender} = render(<Cell rowIndex={0} data={data} colIdx={0}/>);
        act(() => screen.getByRole('textbox').focus());
        rerender(<Cell rowIndex={1} data={data} colIdx={0}/>);   // table swaps the row; the focused input is removed, no blur
        hover(screen.getByRole('textbox'));
        expect(screen.queryByRole('tooltip')).toBeNull();
    });
});
