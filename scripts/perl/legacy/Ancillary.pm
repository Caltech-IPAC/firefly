#!/usr/bin/perl

package Ancillary;

use strict;


sub new {
    my $class = shift;
    my $self = {};
    $self->{fullname} = shift;
    $self->{row} = shift;
    $self->{basename} = shift;
    $self->{matched} = 0;
    $self->{width} = length($self->{fullname});
    bless($self, $class);
    return $self;
}

sub fullname {
    my $self = shift;
    if (@_) { 
	$self->{fullname} = shift;
        $self->{width} = strlen($self->{fullname});
    }
    return $self->{fullname};
}

sub basename {
    my $self = shift;
    if (@_) { 
	$self->{basename} = shift;
    }
    return $self->{basename};
}


sub row {
    my $self = shift;
    if (@_) {
	$self->{row} = shift;
    }
    return $self->{row};
}

sub matched {
    my $self = shift;
    if (@_) {
	$self->{matched} = shift;
    }
    return $self->{matched};
}

sub width {
    my $self = shift;
    return $self->{width};
}

1; 
