import {get} from 'lodash';
import {transformToTreeNodes} from '../ui/TableTree.jsx';


describe('Test suite for TableTree function', () => {

    test('transformToTreeNodes', () => {
        const tableModel = {
            tableData: {
                columns: [ {name: 'schema'}, {name: 'table'}, {name: 'col'}],
                data: [
                    ['dev', 'dev_tbl1', 'tbl1_c1'],
                    ['dev', 'dev_tbl1', 'tbl1_c2'],
                    ['dev', 'dev_tbl2', 'tbl2_c1'],
                    ['dev', 'dev_tbl2', 'tbl2_c2'],
                    ['dev', 'dev_tbl3', 'tbl3_c1'],

                    ['test', 'test_tbl1', 'tbl1_c1'],
                    ['test', 'test_tbl1', 'tbl1_c2'],
                    ['test', 'test_tbl2', 'tbl2_c1'],
                    ['test', 'test_tbl2', 'tbl2_c2'],
                    ['test', 'test_tbl3', 'tbl3_c1'],

                    ['ops', 'ops_tbl1', 'tbl1_c1'],
                    ['ops', 'ops_tbl1', 'tbl1_c2'],
                    ['ops', 'ops_tbl2', 'tbl2_c1'],
                    ['ops', 'ops_tbl2', 'tbl2_c2'],
                    ['ops', 'ops_tbl3', 'tbl3_c1'],
                ]
            }
        };

        const treeNodes = transformToTreeNodes(tableModel, ['schema', 'table']);

        expect(treeNodes).toEqual(
            [
                { key: 'dev', title: 'dev', children: [
                        { key: 'dev--dev_tbl1', title: 'dev_tbl1', children:
                                [
                                    { key: 'dev--dev_tbl1--0', title: 'tbl1_c1'},
                                    { key: 'dev--dev_tbl1--1', title: 'tbl1_c2'},
                                ],
                        },
                        { key: 'dev--dev_tbl2', title: 'dev_tbl2', children:
                                [
                                    { key: 'dev--dev_tbl2--0', title: 'tbl2_c1'},
                                    { key: 'dev--dev_tbl2--1', title: 'tbl2_c2'},
                                 ],
                        },
                        { key: 'dev--dev_tbl3', title: 'dev_tbl3', children:
                                [
                                    { key: 'dev--dev_tbl3--0', title: 'tbl3_c1'},
                                ],
                        },
                    ]
                },
                { key: 'test', title: 'test', children: [
                        { key: 'test--test_tbl1', title: 'test_tbl1', children:
                                [
                                    { key: 'test--test_tbl1--0', title: 'tbl1_c1'},
                                    { key: 'test--test_tbl1--1', title: 'tbl1_c2'},
                                ],
                        },
                        { key: 'test--test_tbl2', title: 'test_tbl2', children:
                                [
                                    { key: 'test--test_tbl2--0', title: 'tbl2_c1'},
                                    { key: 'test--test_tbl2--1', title: 'tbl2_c2'},
                                ],
                        },
                        { key: 'test--test_tbl3', title: 'test_tbl3', children:
                                [
                                    { key: 'test--test_tbl3--0', title: 'tbl3_c1'},
                                ],
                        },
                    ]
                },
                { key: 'ops', title: 'ops', children: [
                        { key: 'ops--ops_tbl1', title: 'ops_tbl1', children:
                                [
                                    { key: 'ops--ops_tbl1--0', title: 'tbl1_c1'},
                                    { key: 'ops--ops_tbl1--1', title: 'tbl1_c2'},
                                ],
                        },
                        { key: 'ops--ops_tbl2', title: 'ops_tbl2', children:
                                [
                                    { key: 'ops--ops_tbl2--0', title: 'tbl2_c1'},
                                    { key: 'ops--ops_tbl2--1', title: 'tbl2_c2'},
                                ],
                        },
                        { key: 'ops--ops_tbl3', title: 'ops_tbl3', children:
                                [
                                    { key: 'ops--ops_tbl3--0', title: 'tbl3_c1'},
                                ],
                        },
                    ]
                },
            ]
        );
    });

});



