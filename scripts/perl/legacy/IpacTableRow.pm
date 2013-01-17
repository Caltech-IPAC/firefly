#!/usr/bin/perl

package IpacTableRow;

use strict;

sub new {
    my $class = shift;
    my $self = {};
    $self->{fields} = [];
    bless($self, $class);
    return $self;
}


sub fields {
    my $self = shift;
    if (@_) {
	@{ $self->{fields} } = @_;
    }
    return @{$self->{fields}};
}

sub value {
    my $self = shift;
    my $idx = shift;
    my @flds = @{$self->{fields}}; 
    return $flds[$idx];
}

1; 
