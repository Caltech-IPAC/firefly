#!/usr/bin/perl -w

package Stat;

sub new {
    my $class = shift;
    my $self = {
	_totalnum => 0,
	_totaltime => 0,
	_totalsize => 0,
	_mintime => -1,
	_maxtime => 0,
	_minsize => -1,
	_maxsize => 0,
	_times => [],
	_sizes => []
    };
    bless $self, $class;
    return $self;
}

sub addLine {
    my ( $self, $time, $size) = @_;
    $self->{_totalnum} += 1;
    $self->{_totaltime} += $time;
    $self->{_totalsize} += $size;
    
    my $mintime = $self->{_mintime};
    if ($mintime < 0 || $time < $mintime ) {
	$self->{_mintime} = $time;
    }
    if ($time > $self->{_maxtime}) {
	$self->{_maxtime} = $time;
    }

    my $minsize = $self->{_minsize};
    if ($minsize < 0 || $size < $minsize ) {
	$self->{_minsize} = $size;
    }
    if ($size > $self->{_maxsize}) {
	$self->{_maxsize} = $size;
    } 

    push((@{$self->{_times}}), $time);
    push((@{$self->{_sizes}}), $size);
}

sub getTotalNum {
    my ($self) = @_; 
    return $self->{_totalnum};
}

sub getTotalSize {
    my ($self) = @_;
    return $self->{_totalsize};
}

sub getAverageTime {
    my ($self) = @_;
    return ($self->{_totalnum} < 1) ? 0 : $self->{_totaltime}/$self->{_totalnum};
}

sub getMedianTime {
    my ($self) = @_;
    return &median($self->{_times});
}


sub getAverageSize {
    my ($self) = @_;
    return ($self->{_totalnum} < 1) ? 0 : $self->{_totalsize}/$self->{_totalnum};
}

sub getMedianSize {
    my ($self) = @_;
    return &median($self->{_sizes});
}


sub getMinTime {
    my ($self) = @_;
    return $self->{_mintime};
}

sub getMaxTime {
    my ($self) = @_;
    return $self->{_maxtime};
}

sub getMinSize {
    my ($self) = @_;
    return $self->{_minsize};
}

sub getMaxSize {
    my ($self) = @_;
    return $self->{_maxsize};
}

sub median {
    my ($arrayRef) = @_;
    my $numRecs = scalar @{$arrayRef};
    if ($numRecs < 1) { return 0; }
    my @sortedArray = sort { $a <=> $b } @$arrayRef;
    if ($numRecs % 2) {
	return $sortedArray[($numRecs-1)/2];
    } else {
	return ($sortedArray[$numRecs/2] + $sortedArray[$numRecs/2-1])/2;
    }
}

sub printhdr {
    printf "%20.20s    %10s %10s %10s %10s %8s %8s %8s %8s\n\n", "Search Type",
	    "Requests",
	    "Avg Time",
	    "min  ",
	    "max  ",
	    "Avg Size",
	    "min  ",
	    "max  ";
}

sub printit {
    my ( $self, $desc) = @_;
    printf "%20.20s : %10d %10.3f %10.3f %10.3f %8.0f %8.0f %8.0f\n", $desc,
    $self->{_totalnum},
    ($self->{_totalnum} < 1) ? 0 : $self->{_totaltime}/1.0/$self->{_totalnum},
    $self->{_mintime},
    $self->{_maxtime},
    ($self->{_totalnum} < 1) ? 0 : $self->{_totalsize}/1.0/$self->{_totalnum},
    $self->{_minsize},
    $self->{_maxsize};
}

1;
