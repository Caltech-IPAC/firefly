#!/usr/bin/env python
#
# Find two available ports. There's still an (unavoidable) race condition
# here, ad the ports we return may have been taken by someone else by the
# time whoever is calling us gets to using them.
#

import socket

def find_free_ports(port = 0, nports = 1):
    # Inspired by http://unix.stackexchange.com/a/132524
    #
    ports = set()
    while len(ports) != nports:
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.bind(('', port))
        _, p = s.getsockname()
        ports.add(p)
        s.close()

    return tuple(sorted(ports))

if __name__ == "__main__":
    print "%d %d" % find_free_ports(0, 2)
