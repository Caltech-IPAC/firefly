#!/usr/bin/perl

package SetInfo;

use strict;


sub new {
    my $class = shift;
    my $self = {};
    my $filepath = shift;
    my $name;
    if ($filepath =~ /[\/]?([^\/]+)\/[^\/]+tbl$/) {
        $name = $1;
    } elsif ($filepath =~ /[\/]?([^\/]+)$/) {
        $name = $1;
    }
# info to be included in IPAC Table header 
    $self->{name} = undef; 
    $self->{identifier} = undef;
    $self->{description} = undef;
    if (defined $name) {
        $self->{name} = $name; 
        $self->{identifier} = "ivo://irsa.ipac/spitzer.legacy.";
        if ($name eq "C2D") {
            $self->{description} = "C2D: From Molecular Cores to Planet-Forming Disks";            
        } elsif ($name eq "FEPS") {
            $self->{description} = "FEPS: The Formation and Evolution of Planetary Systems";            
        } elsif ($name eq "GLIMPSE") {
            $self->{description} = "GLIMPSE: Galactic Legacy Infrared Midplane Survey Extraordinaire";            
        } elsif ($name eq "MIPSGAL") {
            $self->{description} = "MIPSGAL: A 24 and 70 Micron Survey of the Inner Galactic Disk with MIPS";
        } elsif ($name eq "Taurus") {
            $self->{description} = "Taurus 2: Finishing the Spitzer Map of the Taurus Molecular Clouds";
        } elsif ($name eq "SASS") {
            $self->{description} = "SASS: Spitzer Archive of Stellar Spectra";
        } elsif ($name eq "5MUSES") {
            $self->{description} = "5MUSES: 5 mJy Unbiased Spitzer Extragalactic Survey";
        } elsif ($name eq "FIDEL") {
            $self->{description} = "FIDEL: Far-Infrared Deep Extragalactic Legacy Survey";            
        } elsif ($name eq "GOALS") {
            $self->{description} = "GOALS: Great Observatory All-sky LIRG Survey";            
        } elsif ($name eq "GOODS") {
            $self->{description} = "GOODS: Great Observatories Origins Deep Survey - Spitzer and Ancillary Data";
        } elsif ($name eq "LVL") {
            $self->{description} = "LVL: Spitzer Local Volume Legacy Survey";
        } elsif ($name eq "SAGE-SMC") {
            $self->{description} = "SAGE: Small Magellanic Cloud and Magellanic Bridge";            
        } elsif ($name eq "SAGE") {
            $self->{description} = "SAGE: Surveying the Agents of a Galaxy's Evolution";            
        } elsif ($name eq "SDWFS") {
            $self->{description} = "SDWFS: Spitzer Deep, Wide-Field Survey";            
        } elsif ($name eq "SIMPLE") {
            $self->{description} = "SIMPLE: Spitzer IRAC/MUSYC Public Legacy Survey in the Extended Chandra Deep Field South";
        } elsif ($name eq "SINGS") {
            $self->{description} = "SINGS: The Spitzer Infrared Nearby Galaxies Survey";
        } elsif ($name eq "SPITZER") {
            $self->{description} = "SPITZER";
        } elsif ($name eq "SpUDS") {
            $self->{description} = "SpUDS: Spitzer Public Legacy Survey of the UKIDSS Ultra Deep Survey";            
        } elsif ($name eq "SSGSS") {
            $self->{description} = "SSGSS: The Spitzer SDSS Galaxy Spectroscopic Survey";
        } elsif ($name eq "SWIRE") {
            $self->{description} = "SWIRE: Spitzer Wide-area InfraRed Extragalactic Survey";
        } elsif ($name eq "FLS_VLA") {
            $self->{description} = "Spitzer FLS: Ancillary VLA Data";            
        } elsif ($name eq "FLS_MAIN_R") {
            $self->{description} = "Spitzer FLS: NOAO Extragalactic R-band Data";            
        } elsif ($name eq "FLS_ELAISN1_R") {
            $self->{description} = "Spitzer FLS: NOAO ELAIS N1 R-band Data";            
        } 
    }
    bless($self, $class);
    return $self;
}

sub name {
    my $self = shift;
    if (@_) { 
	$self->{name} = shift;
    }
    return $self->{name};
}

sub identifier {
    my $self = shift;
    if (@_) { 
	$self->{identifier} = shift;
    }
    return $self->{identifier};
}


sub description {
    my $self = shift;
    if (@_) {
	$self->{description} = shift;
    }
    return $self->{description};
}


1; 
