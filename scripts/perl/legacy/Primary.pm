#!/usr/bin/perl

package Primary;

use strict;

sub new {
    my $class = shift;
    my $self = {};
    $self->{source} = shift;
    $self->{fullname} = shift;
    $self->{row} = shift;
    $self->{basename} = shift;
#    $self->{rowcontent} = shift;
    $self->{fields} = [];
    $self->{coverage} = undef;
    $self->{uncertainty} = undef;
    $self->{mask} = undef;
    bless($self, $class);
    return $self;
}

sub source {
    my $self = shift;
    if (@_) { 
	$self->{source} = shift;
    }
    return $self->{source};
}


sub fullname {
    my $self = shift;
    if (@_) { 
	$self->{fullname} = shift;
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

#sub rowcontent {
#    my $self = shift;
#    if (@_) { 
#	$self->{rowcontent} = shift;
#    }
#    return $self->{rowcontent};
#}

sub coverage {
    my $self = shift;
    if (@_) { 
	$self->{coverage} = shift;
    }
    return $self->{coverage};
}

sub uncertainty {
    my $self = shift;
    if (@_) { 
	$self->{uncertainty} = shift;
    }
    return $self->{uncertainty};
}

sub mask {
    my $self = shift;
    if (@_) { 
	$self->{mask} = shift;
    }
    return $self->{mask};
}



#sub addAncillary {
#    my $self = shift;
#    if (@_) {
#	push(@{ $self->{ancillary} }, @_ );
#    }
#}

#sub ancillary {
#    my $self = shift;
#    if (@_) {
#	@{ $self->{ancillary} } = @_;
#    }
#    return @{$self->{ancillary}};
#}

sub row {
    my $self = shift;
    if (@_) {
	$self->{row} = shift;
    }
    return $self->{row};
}

sub fields {
    my $self = shift;
    if (@_) {
	@{ $self->{fields} } = @_;
    }
    return @{$self->{fields}};
}


1; 
