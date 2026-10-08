/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
/*eslint-env node, jest */

import React from 'react';
import '@testing-library/jest-dom';
import {render as rtlRender, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {CssVarsProvider} from '@mui/joy';

import {SelectableHeader} from '../TableRenderer.js';

const noop = () => undefined;
// HelpIcon reads the color scheme, which MUI only provides under a CssVarsProvider; jsdom has no matchMedia
window.matchMedia ??= (media) => ({matches: false, media, addEventListener: noop, removeEventListener: noop, addListener: noop, removeListener: noop});
const render = (ui) => rtlRender(ui, {wrapper: CssVarsProvider});
const checkbox = () => screen.queryByRole('checkbox');
const help = () => screen.queryByTestId('HelpOutlineIcon');
const funnel = () => screen.queryByRole('button', {name: /^(Filter on selected rows|Check rows to filter on them)$/});

describe('SelectableHeader', () => {

    test('nothing checked: the funnel is disabled and says why', () => {
        render(<SelectableHeader selectable={true} hasSelected={false} checked={false} showFilters={true} onSelectAll={noop}/>);
        expect(checkbox()).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Check rows to filter on them'})).toBeDisabled();
        expect(help()).toBeInTheDocument();
    });

    test('rows checked: the funnel is enabled and filters', async () => {
        const onFilterSelected = jest.fn();
        render(<SelectableHeader selectable={true} hasSelected={true} checked={false} showFilters={true} onSelectAll={noop} onFilterSelected={onFilterSelected}/>);
        const btn = screen.getByRole('button', {name: 'Filter on selected rows'});
        expect(btn).toBeEnabled();
        await userEvent.click(btn);
        expect(onFilterSelected).toHaveBeenCalledTimes(1);
    });

    test('not selectable: only the help icon', () => {
        render(<SelectableHeader selectable={false} showFilters={true}/>);
        expect(checkbox()).toBeNull();
        expect(funnel()).toBeNull();
        expect(help()).toBeInTheDocument();
    });

    test('filters hidden: checkbox only', () => {
        render(<SelectableHeader selectable={true} hasSelected={true} showFilters={false} onSelectAll={noop}/>);
        expect(checkbox()).toBeInTheDocument();
        expect(funnel()).toBeNull();
        expect(help()).toBeNull();
    });

    test('showSelectRowFilter off: help icon, no funnel', () => {
        render(<SelectableHeader selectable={true} hasSelected={true} showFilters={true} showSelectRowFilter={false} onSelectAll={noop}/>);
        expect(funnel()).toBeNull();
        expect(help()).toBeInTheDocument();
    });
});
