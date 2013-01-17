#!/usr/bin/perl

package IpacTableDefinition;

use strict;


sub new {
    my $class = shift;
    my $self = {};
    $self->{index} = shift;
    $self->{name} = shift;
    $self->{offset} = shift;
    $self->{width} = shift;
    $self->{type} = undef;
    bless($self, $class);
    return $self;
}

sub index {
    my $self = shift;
    if (@_) { 
	$self->{index} = shift;
    }
    return $self->{index};
}


sub name {
    my $self = shift;
    if (@_) { 
	$self->{name} = shift;
    }
    return $self->{name};
}


sub offset {
    my $self = shift;
    if (@_) {
	$self->{offset} = shift;
    }
    return $self->{offset};
}

sub width {
    my $self = shift;
    if (@_) {
	$self->{width} = shift;
    }
    return $self->{width};
}

sub type {
    my $self = shift;
    if (@_) {
	$self->{type} = shift;
    }
    return $self->{type};
}


1; 
