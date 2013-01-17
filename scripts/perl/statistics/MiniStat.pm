#!/usr/bin/perl -w

package MiniStat;

sub new {
    my $class = shift;
    my $self = {
	_totalnum => 0,
	_totalsize => 0,
    };
    bless $self, $class;
    return $self;
}

sub add {
    my ( $self, $num, $size) = @_;
    $self->{_totalnum} += $num;
    $self->{_totalsize} += $size;
}

sub getTotalNum {
    my ($self) = @_; 
    return $self->{_totalnum};
}

sub getTotalSize {
    my ($self) = @_;
    return $self->{_totalsize};
}

sub getAverageSize {
    my ($self) = @_;
    return ($self->{_totalnum} < 1) ? 0 : $self->{_totalsize}/$self->{_totalnum};
}

sub printhdr {
    printf "%20.20s    %10s %10s %10s\n\n", "Type",
	    "Total Files",
            "Total Size",
            "Avg Size";
}

sub printit {
    my ( $self, $desc) = @_;
    printf "%20.20s : %10d %10.3f %10.3f\n", $desc,
    $self->{_totalnum},
    $self->{_totalsize},
    ($self->{_totalnum} < 1) ? 0 : $self->{_totalsize}/1.0/$self->{_totalnum};
}

1;
