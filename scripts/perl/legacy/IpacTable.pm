#!/usr/bin/perl

package IpacTable;

use strict;

use IpacTableDefinition;
use IpacTableRow;

sub new {
    my $class = shift;
    my $self = {};
    my $filepath = shift;
    $self->{filepath} = $filepath;
    $self->{definitions} = [];
    $self->{rows} = [];

    my @ipacTableDefs = ();
    my @ipacTableRows = ();

    open FILE, "<", $filepath or die $!;
    while (<FILE>) {
        if (/\\/) {  }
        elsif (/\|/) {
            if ($#ipacTableDefs < 1) {
                # the line starts with the separator                                                                                         
                my $offset = 0;
                my $idx = 1;
                my $newidx = 0;
                my $n = 0;
                while ($idx < length($_)-1) {
                    $newidx = index($_, '|', $idx);
                    if ($newidx > 1) {
                        chomp;
                        my $offset = $idx;
                        my $width = $newidx - $idx;
                        my $def = substr($_, $offset, $width);
                        $def =~ s/^\s+//; $def =~ s/\s+$//;
                        my $ipacTableDef = new IpacTableDefinition($n, $def, $offset, $width);
                        push(@ipacTableDefs, $ipacTableDef);
                        #printf "%d %d |%s|\n", $ipacTableDef->offset(), $ipacTableDef->width(), $ipacTableDef->name();
                    }
                    $idx = $newidx+1;
                    $n++;
                }
            } else {
                chomp;
                my @types = split /\s*\|\s*/;
                #line starts and ends with "|") 
                my $n = 1;
                foreach my $ipacTableDef (@ipacTableDefs) {
                    $types[$n] =~ s/^\s+//;
                    $types[$n] =~ s/\s+$//;
                    $ipacTableDef->type($types[$n]);
                    $n++;
                }
            }
        } else {
            chomp;
            
            my @fields;
            foreach my $ipacTableDef (@ipacTableDefs) {
                my $fld = substr($_, $ipacTableDef->offset(), $ipacTableDef->width());
                $fld =~ s/^\s+//; $fld =~ s/\s+$//;
                push(@fields, $fld);
            }
            my $ipacTableRow = new IpacTableRow();
            $ipacTableRow->fields(@fields);
            push(@ipacTableRows, $ipacTableRow);
        }
    }
    push(@{$self->{definitions}}, @ipacTableDefs);
    push(@{$self->{rows}}, @ipacTableRows);
    
    close FILE or die $!;

    bless($self, $class);
    return $self;
}

sub filepath {
    my $self = shift;
    return $self->{filepath};
}

sub definitions {
    my $self = shift;
    return @{$self->{definitions}};
}

sub rows {
    my $self = shift;
    return @{$self->{rows}};
}

sub definitionIndex {
    my $self = shift;
    my $def = shift;
    foreach my $ipacTableDef (@{$self->{definitions}}) {
        if ($ipacTableDef->name() eq $def) {
            return $ipacTableDef->index();
        }
    }
    return -1;
}

1; 
