import {get} from 'lodash';
import {add, remove, replace} from '../toc';

const toc_test = [
    {
        id: '0',
        items: [
            {
                id: '0-0',
                items: [
                    {id: '0-0-0'},
                    {id: '0-0-1'},
                    {id: '0-0-2'},
                    {id: '0-0-3'},
                ]
            },
            {
                id: '0-1',
                items: [
                    {id: '0-1-0'},
                    {id: '0-1-1'},
                    {id: '0-1-2'},
                ]
            },
            {
                id: '0-2',
                items: [
                    {id: '0-2-0'},
                    {id: '0-2-1'},
                    {id: '0-2-2'},
                ]
            },
        ]
    },
    {id: '1'},
    {id: '2'}
];



it('Add', () => {
    let toc = add(toc_test, '0', {id: 'new'});  // add to the beginning
    expect(toc.length).toBe(4);
    expect(get(toc, '0')).toEqual({id: 'new'});

    toc = add(toc_test, '1', {id: 'new'});      // add to the middle
    expect(toc.length).toBe(4);
    expect(get(toc, '1')).toEqual({id: 'new'});

    toc = add(toc_test, '3', {id: 'new'});      // add to the end
    expect(toc.length).toBe(4);
    expect(get(toc, '3')).toEqual({id: 'new'});

    toc = add(toc_test, '0.items.0.items.2', {id: 'new'});      // add to deep item
    expect(get(toc, '0.items.0.items.length')).toBe(5);
    expect(get(toc, '0.items.0.items')).toEqual([
        {id: '0-0-0'},
        {id: '0-0-1'},
        {id: 'new'},
        {id: '0-0-2'},
        {id: '0-0-3'},
    ]);

});

it('Remove', () => {
    let toc = remove(toc_test, '0');  // remove from the top
    expect(toc.length).toBe(2);
    expect(get(toc, '0')).toEqual({id: '1'});

    toc = remove(toc_test, '1');      // remove from the middle
    expect(toc.length).toBe(2);
    expect(get(toc, '1')).toEqual({id: '2'});

    toc = remove(toc_test, '2');      // remove from the end
    expect(toc.length).toBe(2);
    expect(get(toc, '2')).toBeUndefined();

    toc = remove(toc_test, '0', '1', '2', '3');     // remove multiple including non-existing keys.
    expect(toc.length).toBe(0);
});


it('Replace', () => {
    let toc = replace(toc_test, '0', {id: 'new'});      // replace existing item
    expect(toc.length).toBe(3);
    expect(get(toc, '0')).toEqual({id: 'new'});

    toc = replace(toc_test, '0.items.2.items.1', {id: 'new'});      // replace deep item
    expect(toc.length).toBe(3);
    expect(get(toc, '0.items.2.items')).toEqual( [
        {id: '0-2-0'},
        {id: 'new'},
        {id: '0-2-2'},
    ]
);

});

