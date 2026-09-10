#!/usr/bin/env python3
"""Summarize JUnit XML reports as GitHub-flavored markdown on stdout.

Usage: summarize-junit.py <dir-of-junit-xml>
Exits 0 even when tests failed; this is a reporting tool, not a gate.
"""

import glob
import os
import sys
import xml.etree.ElementTree as ET


def main(xml_dir):
    paths = sorted(glob.glob(os.path.join(xml_dir, '*.xml')))
    if not paths:
        print(f'No JUnit XML found in `{xml_dir}`.')
        return 0

    tests = failures = skipped = 0
    seconds = 0.0
    failed_names = []

    for path in paths:
        root = ET.parse(path).getroot()
        tests += int(root.get('tests', 0))
        failures += int(root.get('failures', 0)) + int(root.get('errors', 0))
        skipped += int(root.get('skipped', 0))
        seconds += float(root.get('time', 0))
        for case in root.iter('testcase'):
            # An <failure> element with text but no children is falsy in
            # ElementTree, so compare against None explicitly.
            bad = next(case.iter('failure'), None)
            if bad is None:
                bad = next(case.iter('error'), None)
            if bad is not None:
                cls = (case.get('classname') or '').split('.')[-1]
                failed_names.append(f'{cls}.{case.get("name")}')

    print(f'Java: **{tests}** tests, **{failures}** failed, '
          f'**{skipped}** skipped, {seconds:.0f}s')
    print()

    if failed_names:
        print('<details><summary>Failed Java tests</summary>')
        print()
        for name in sorted(failed_names):
            print(f'- `{name}`')
        print()
        print('</details>')

    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1] if len(sys.argv) > 1 else '.'))
