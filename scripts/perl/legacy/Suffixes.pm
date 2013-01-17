#!/usr/bin/perl

package Suffixes;

use strict;

use File::Basename;


sub new {
    my $class = shift;
    my $filepath = shift;
    my $self = {};
    $self->{primary} = undef;
# by default do not remove primary suffix to match primary products with ancillary
    $self->{primarySuffixToRemove} = undef;
    $self->{coverage} = undef;
    $self->{uncertainty} = undef;
    $self->{mask} = undef;

    if ($filepath =~ /C2D\/images_Av.tbl/) {
	$self->{primary} = qr/_Av$/;
	$self->{primarySuffixToRemove} = qr/Av$/;
	$self->{uncertainty} = qr/sigAv$/;
    } elsif ($filepath =~ /C2D\//) {
	$self->{primary} = qr/mosaic$/;
	$self->{coverage} = qr/(cov|exp)$/;
	$self->{uncertainty} = qr/(unc|std)$/;
    } elsif ($filepath =~ /FIDEL\//) {
	$self->{primary} = qr/sci$/;
	$self->{primarySuffixToRemove} = qr/sci$/;
	$self->{coverage} = qr/exp/;
	$self->{uncertainty} = qr/std/;
    } elsif ($filepath =~ /GOODS\//) {
	if ($filepath =~ /GOODS\/ancillary_v1.0.tbl/) {
	    $self->{primary} = qr/drz_img$/;
	    $self->{primarySuffixToRemove} = qr/drz_img$/;
	    $self->{coverage} = qr/wht_img$/;
	} else {
            # http://irsa.ipac.caltech.edu:80/data/GOODS/docs/goods_dr3.html
	    $self->{primary} = qr/sci$/;
	    $self->{primarySuffixToRemove} = qr/sci$/;
	    $self->{coverage} = qr/exp$/;
	    $self->{mask} = qr/flg$/;
            # the weight maps represent the inverse square of the RMS pixel-to-pixel noise (in DN/s) 
            # at the background level of the images
            # should be treated as uncertainty
	    $self->{uncertainty} = qr/wht$/;
	}
    } elsif ($filepath =~ /LVL\//) {
        # http://irsa.ipac.caltech.edu/data/SPITZER/LVL/LVL_DR5_v5.pdf 
	$self->{primary} = qr/(crop_v3-0|image_v3-0|irac[1,2,3,4]|interp)$/;
	$self->{coverage} = qr/(wt|cov)$/;
    } elsif ($filepath =~ /MIPSGAL/) {
        # http://irsa.ipac.caltech.edu/data/SPITZER/MIPSGAL/images/mipsgal_delivery_guide_v3_29aug08.pdf
	$self->{primary} = qr/(\d_024)$/;
	$self->{primarySuffixToRemove} = qr/024$/;
	$self->{coverage} = qr/covg_024$/;
	$self->{uncertainty} = qr/std_024$/;
        $self->{mask} = qr/maskcube_024$/;
    } elsif ($filepath =~ /SAGE-SMC\//) {
        # http://irsa.ipac.caltech.edu/data/SPITZER/SAGE-SMC/docs/sage-smc_delivery_apr11.pdf 
	$self->{primary} = qr/(mosaic|resid|\d)$/;
	$self->{primarySuffixToRemove} = qr/(mosaic|resid)$/;
	$self->{coverage} = qr/wt$/;
    } elsif ($filepath =~ /SAGE\//) {
        # http://irsa.ipac.caltech.edu/data/SPITZER/SAGE/doc/SAGEDataDescription_Delivery2.pdf 
	$self->{primary} = qr/(residual|rotated|\d|cube)$/;
	$self->{primarySuffixToRemove} = qr/(residual|rotated)$/;
	$self->{coverage} = qr/wt$/;
    } elsif ($filepath =~ /SDWFS\//) {
	$self->{primary} = qr/.v32$/;
	$self->{primarySuffixToRemove} = qr/.v32$/;
	$self->{coverage} = qr/.cov$/;
    } elsif ($filepath =~ /SIMPLE\//) {
        # http://irsa.ipac.caltech.edu/data/SPITZER/SIMPLE/doc/00README_images 
	$self->{primary} = qr/\d$/;
	$self->{coverage} = qr/exp$/;
	$self->{uncertainty} = qr/rms$/;
        # "We provide a Flag map, which currently only identifies pixels corrected for muxbleed in channel 1 and channel 2."  
        # One flag file is for all primary products
	$self->{mask} = qr/flag/;
    } elsif ($filepath =~ /SINGS\//) {
        # http://irsa.ipac.caltech.edu/data/SPITZER/SINGS/doc/sings_fifth_delivery_v2.pdf 
        # primary product is something that does not end with "_unc_v3_0", "_unc", or "_wt"
	$self->{primary} = qr/(cont|[^c,t]|[^c]_v3-0)$/;
	$self->{primarySuffixToRemove} = qr/v3-0$/;
	$self->{coverage} = qr/wt$/;
	$self->{uncertainty} = qr/(unc|unc_v3-0)$/;
    } elsif ($filepath =~ /SpUDS\//) {
        # http://irsa.ipac.caltech.edu/data/SPITZER/SpUDS/documentation/readme_irac.txt 
        #IRAC: There are four fits files for each channel - data, coverage,
        #uncertainty and standard deviation fits images. The uncertainty image
        #attempts to estimate the error per pixel that arises due to all steps
        #in the mopex pipeline.
	$self->{primary} = qr/(sci|mosaic)$/;
	$self->{primarySuffixToRemove} = qr/sci$/;
	$self->{coverage} = qr/(cov|exp)$/;
	$self->{uncertainty} = qr/(unc|std|err)$/;
    } elsif ($filepath =~ /SWIRE\//) {
        #http://irsa.ipac.caltech.edu/data/SPITZER/SWIRE/docs/delivery_doc_r2_v2.pdf
	$self->{primary} = qr/(mosaic|mos32|map)$/;
	$self->{primarySuffixToRemove} = qr/mosaic|map$/;
	$self->{coverage} = qr/(cov|mosaic_cov)$/;
	$self->{uncertainty} = qr/(unc|mosaic_unc|mosaic_std|snr)$/;
        $self->{mask} = qr/(mask)$/;
    } elsif ($filepath =~ /Taurus\//) {
	$self->{primary} = qr/(mosaic|\d)$/;
	$self->{coverage} = qr/cov$/;
	$self->{uncertainty} = qr/unc$/;
    } 

    bless($self, $class);
    return $self;
}

sub primary {
    my $self = shift;
    if (@_) { 
	$self->{primary} = shift;
    }
    return $self->{primary};
}

sub primarySuffixToRemove {
    my $self = shift;
    if (@_) { 
	$self->{primarySuffixToRemove} = shift;
    }
    return $self->{primarySuffixToRemove};
}

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

sub ancillary {
    my $self = shift;
    if (@_) {
	@{ $self->{ancillary} } = @_;
    }
    return @{$self->{ancillary}};
}


1; 
