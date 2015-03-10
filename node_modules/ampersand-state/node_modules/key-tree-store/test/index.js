var test = require('tape');
var KeyTree = require('../key-tree-store');


test('`add` should store objects', function (t) {
    var tree = new KeyTree();
    var one = {id: 'one'};
    var two = {id: 'two'};
    var three = {id: 'three'};
    var four = {id: 'four'};

    tree.add('first', one);
    tree.add('first.second', two);
    tree.add('first.second', three);
    tree.add('first.second.third', four);

    t.equal(Object.keys(tree.storage).length, 3);

    t.equal(tree.storage['first.second'].length, 2, 'should be two for `first.second` key');
    t.equal(tree.get('first').length, 4, 'should be 4 that match');
    t.equal(tree.get('first.second').length, 3, 'should be 3 that match');
    t.equal(tree.get('first.second.third').length, 1, 'should be 1 that match');

    t.equal(tree.get('second.third').length, 0, 'keypaths should start at the start');

    t.equal(tree.get('first.seco').length, 0, 'keypaths must be the full path, or end in a . to match');

    tree.remove(two);
    t.equal(tree.get('first').length, 3, 'should be 3 that match after removal');

    t.end();
});
