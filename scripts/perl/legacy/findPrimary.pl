#!/usr/bin/perl


use strict;

use File::Basename;
use POSIX;

use Primary;
use Ancillary;
use Suffixes;
use SetInfo;
use IpacTableDefinition;
use IpacTableRow;
use IpacTable;

&usage() if ($#ARGV != 1);

my $metadir = shift;
our $outdir = shift;
mkdir "$outdir" unless -d "$outdir";

my $metafilepath = $metadir . "/" . "image_meta.tbl";
die "$metafilepath does not exist" if (not -e $metafilepath);

my $setInfo = new SetInfo($metadir);
print "*****************\n";
printf "LEGACY PROGRAM: %s\n", $setInfo->name();

# get table filepaths from image_meta.tbl
my $metaIpacTable = new IpacTable($metafilepath);
my $pathIdx = $metaIpacTable->definitionIndex("path");
die "can not find path column in $metaIpacTable" if ($pathIdx < 0);
my @metarows = $metaIpacTable->rows();
my @filepaths;
for my $metarow (@metarows) {
    printf "  filepath: %s\n", $metarow->value($pathIdx);
    push(@filepaths, $metadir . "/" . $metarow->value($pathIdx));
}

my $coverageWidth = 8; # "coverage"
my $uncertaintyWidth = 11; # "uncertainty"
my $maskWidth = 4; # "mask"
my $sourceWidth = 9; # "tblsource";

my @combinedPrimaryFiles;
my @combinedIpacTableDefs;


for my $filepath (@filepaths) {
    print "*****************\n";
    print "  filepath: $filepath \n";
    
    my $suffixes = new Suffixes($filepath);
    my $allPrimary = &isAllPrimaryFile($filepath);
    die "unrecognized legacy filepath pattern: $filepath" 
        if ((not $allPrimary) and (not defined $suffixes->primary()));
    my $primaryRE = $suffixes->primary();
    printf "  primaryRE: %s\n", $primaryRE;
    my $primaryRemovableSuffixRE = $suffixes->primarySuffixToRemove();
    my $coverageRE = $suffixes->coverage();
    printf "  coverageRE: %s\n", $coverageRE;
    my $uncertaintyRE = $suffixes->uncertainty();
    printf "  uncertaintyRE: %s\n", $uncertaintyRE;
    my $maskRE = $suffixes->mask();
    printf "  maskRE: %s\n", $maskRE;

    my @primaryFiles = ();
    my @ancillaryFiles = ();

    my $source = $setInfo->name() . "/" . basename($filepath);
    $sourceWidth = &getMax(length($source), $sourceWidth);

##########################################################
# parse IPAC table, extract definitions and fields from it
##########################################################
    my $ipacTable = new IpacTable($filepath);
    my $fnameIdx = $ipacTable->definitionIndex("fname");
    die "fname is not found in $filepath" if ($fnameIdx < 0);
    my @ipacTableDefs = $ipacTable->definitions();
    my @ipacTableRows = $ipacTable->rows();
    my $row = 0;
    foreach my $ipacTableRow (@ipacTableRows) {
        my @fields = $ipacTableRow->fields();
        if ($allPrimary) {
            my $primary = new Primary($source, $fields[$fnameIdx], $row, "");
            $primary->fields(@fields);
            push(@primaryFiles, $primary);
        } else {
###	    $base = basename($fields[$fnameIdx]);
            my $base = $fields[$fnameIdx]; # use full name (needed in SINGS/images.tbl)
            $base =~ s/\.gz$//; #remove zip extension
            $base =~ s/\.fits$//; #remove extension
            if ($base =~ m/$primaryRE/) {
###		printf "Adding primary: %s\n", $base; 
                if (defined $primaryRemovableSuffixRE) {
                    $base =~ s/$primaryRemovableSuffixRE//;
                }
                my $primary = new Primary($source, $fields[$fnameIdx], $row, $base);
                $primary->fields(@fields);
                push(@primaryFiles, $primary);
            } else {
###		printf "Adding ancillary: %s\n", $base; 
                push(@ancillaryFiles, new Ancillary($fields[$fnameIdx], $row, $base));
            }
        }
        
        $row++;
    }
    
###################################
# match primary to ancillary files
###################################
    if (!$allPrimary) {
        my $abase;
        my $pbase;
        foreach my $aPrimary (@primaryFiles) {
            $pbase = $aPrimary->basename();
            foreach my $anAncillary (@ancillaryFiles) {
                $abase = $anAncillary->basename();
                if (index($abase, $pbase) == 0) {
                    my $amatch = substr($abase, length($pbase));
#		printf "    amatch %s\n", $amatch;
##################################	    
# will need to be fullname later
##################################
                    if (defined $coverageRE and $amatch =~ m/^.?$coverageRE/) {
                        my $coverageFile = $setInfo->name() . "/" . $anAncillary->fullname();
                        if (defined $aPrimary->coverage()) {
                            printf "WARNING: adding extra coverage %s to %s for primary %s\n", 
                            $abase, $aPrimary->coverage(), $pbase;
                            $coverageFile = $aPrimary->coverage() . ";" . $coverageFile;
                        }
                        $aPrimary->coverage($coverageFile);
                        $anAncillary->matched(1);
                        $coverageWidth = &getMax($coverageWidth, length($aPrimary->coverage()));
                    } elsif (defined $uncertaintyRE and $amatch =~ m/^.?$uncertaintyRE/) {
                        my $uncertaintyFile = $setInfo->name() . "/" . $anAncillary->fullname();
                        if (defined $aPrimary->uncertainty()) {
                            printf "WARNING: adding extra uncertainty %s to %s for primary %s\n", 
                            $abase, $aPrimary->uncertainty(), $pbase;
                            $uncertaintyFile = $aPrimary->uncertainty() . ";" . $uncertaintyFile;
                        }
                        $aPrimary->uncertainty($uncertaintyFile);
                        $anAncillary->matched(1);
                        $uncertaintyWidth = &getMax($uncertaintyWidth, length($aPrimary->uncertainty()));
                    } elsif (defined $maskRE and $amatch =~ m/^.?$maskRE/) {
                        my $maskFile = $setInfo->name() . "/" . $anAncillary->fullname();
                        if (defined $aPrimary->mask()) {
                            printf "WARNING: adding extra mask %s to %s for primary %s\n", 
                            $abase, $aPrimary->mask(), $pbase;
                            $maskFile = $aPrimary->mask() . ";" . $maskFile;
                        }
                        $aPrimary->mask($maskFile);
                        $anAncillary->matched(1);
                        $maskWidth = &getMax($maskWidth, length($aPrimary->mask()));
                    } else {
#		    printf "Partial match: |%s| to |%s|\n", $abase, $pbase;
                    }
                }	
            }
        }
    }
    foreach my $anAncillary (@ancillaryFiles) {
        if (not $anAncillary->matched()) {
            #special case SIMPLE: SIMPLE_ECDFS_IRAC_flag should be mask file for each primary
            if ($anAncillary->basename() =~ /SIMPLE_ECDFS_IRAC_flag$/) {
                my $maskFile = $anAncillary->fullname();
                foreach my $aPrimary (@primaryFiles) {
                    $aPrimary->mask($setInfo->name() . "/" . $maskFile);
                }
                $maskWidth = &getMax($maskWidth, length($maskFile));
            } elsif ($setInfo->name() eq "S4G") {
                # special handling for S4G final mask
                # this is trying to handle final mask files
                my $pbase;
                my $matched = 0;
                my $abase = $anAncillary->basename();
                $abase =~ s/\/P2\//\/P1\//; #final masks are produced by the second pipeline
                if ($abase =~ m/1.final_mask$/) {
                    $abase =~ s/1.final_mask$/phot.1.final_mask/;
                } elsif ($abase =~ m/2.final_mask$/) {
                    $abase =~ s/2.final_mask$/phot.2.final_mask/;
                }
                foreach my $aPrimary (@primaryFiles) {
                    $pbase = $aPrimary->basename();
                    
                    if (index(lc($abase), lc($pbase)) == 0) {
                        my $file = $anAncillary->fullname();
                        $aPrimary->mask($setInfo->name() . "/" . $file);
                        $maskWidth = &getMax($maskWidth, length($aPrimary->mask()));
                        $matched = 1;
                        last;
                    }
                }
                if (not $matched) {
                    printf "WARNING: %s can not be classified\n", $anAncillary->basename();
                }

            } elsif ($setInfo->name() eq "Abell1763") {
#how do you know which is which? - read documentation                 
#WARNING: images/palomar/WIRC_Jweight can not be classified
#WARNING: images/palomar/rmsH can not be classified
#WARNING: images/palomar/rmsJ can not be classified
#WARNING: images/palomar/rms_r can not be classified
#WARNING: images/palomar/rmsK can not be classified
                my $pbase;
                my $matched = 0;
                my $isWeight = 0;
                my $abase = $anAncillary->basename();
                if ($abase =~ m/rmsH$/) {
                    $abase =~ s/rmsH$/WIRC_H_rms/;
                } elsif ($abase =~ m/rmsJ$/) {
                    $abase =~ s/rmsJ$/WIRCJ_rms/;
                } elsif ($abase =~ m/rmsK$/) {
                    $abase =~ s/rmsK$/WIRC_K_rms/;
                } elsif ($abase=~ m/rms_r$/){
                    $abase =~ s/rms_r$/A1763_r/;
                } elsif ($abase=~ m/WIRC_Jweight$/) {
                    $abase =~ s/WIRC_Jweight$/WIRCJweight/;
                    $isWeight = 1;
                }
                foreach my $aPrimary (@primaryFiles) {
                    $pbase = $aPrimary->basename();
                    
                    if (index($abase, $pbase) == 0) {
                        my $file = $anAncillary->fullname();
                        if ($isWeight) {
                            $aPrimary->uncertainty($setInfo->name() . "/" . $file);
                            $uncertaintyWidth = &getMax($uncertaintyWidth, length($aPrimary->uncertainty()));
                        } else {
                            $aPrimary->coverage($setInfo->name() . "/" . $file);
                            $coverageWidth = &getMax($coverageWidth, length($aPrimary->coverage()));

                        }
                        $matched = 1;
                        last;
                    }
                }
                if (not $matched) {
                    printf "WARNING: %s can not be classified\n", $anAncillary->basename();
                }
            }
        }
    }



    my $defRef = &mergeDefinitions(\@combinedIpacTableDefs, \@ipacTableDefs);
    if (not defined $defRef) {
        print "WARNING: Unable to merge $filepath table results with others.\n";

        my $outInfo = {
            FILENAME => $setInfo->name() . "_" . basename($filepath),
            SOURCE_WIDTH => $sourceWidth,
            COVERAGE_WIDTH => $coverageWidth,
            UNCERTAINTY_WIDTH => $uncertaintyWidth,
            MASK_WIDTH => $maskWidth,
            IPAC_TABLE_DEFS => [@ipacTableDefs],
            PRIMARY_FILES => [@primaryFiles],
            USE_ID_IN_DESC => 1,
        };

        &printOutTable($outInfo, $setInfo);

    } else {
        push(@combinedPrimaryFiles, @primaryFiles);
        @combinedIpacTableDefs = @$defRef;
    }
}

my $outInfo = {
    FILENAME => $setInfo->name() . "_images.tbl",
    SOURCE_WIDTH => $sourceWidth,
    COVERAGE_WIDTH => $coverageWidth,
    UNCERTAINTY_WIDTH => $uncertaintyWidth,
    MASK_WIDTH => $maskWidth,
    IPAC_TABLE_DEFS => [@combinedIpacTableDefs],
    PRIMARY_FILES => [@combinedPrimaryFiles],
    USE_ID_IN_DESC => 0,
};

&printOutTable($outInfo, $setInfo);



sub printOutTable {
    $outInfo = shift;
    $setInfo = shift;

#   adjustmentto add legacy dir to relative fname
    my $widthAdjust = length($setInfo->name())+1;

    my $outfilepath = $outdir . "/" . $outInfo->{FILENAME};

    my $sourceWidth = $outInfo->{SOURCE_WIDTH};
    my $coverageWidth = $outInfo->{COVERAGE_WIDTH};
    my $uncertaintyWidth = $outInfo->{UNCERTAINTY_WIDTH};
    my $maskWidth = $outInfo->{MASK_WIDTH};
    my @ipacTableDefs = @{ $outInfo->{IPAC_TABLE_DEFS} };
    my @primaryFiles = @{ $outInfo->{PRIMARY_FILES} };

    open OUT, ">$outfilepath" or die $!;
    
# indexes if the path fields that need dir name prefixed and width adjusted
    my @idxPath;

#### Removed
# special handling for SWIRE: table already has coverage, uncertainty, mask columns, named as coverage_u, noise_u, flags_u
####
#    my $isSwire = ($setInfo->name() eq "SWIRE") ? 1 : 0;
#    if ($isSwire) {
#        my $n = 0;
#        # rename coverage, uncertainty, mask columns into standard names
#        foreach my $ipacTableDef (@ipacTableDefs) {
#            my $name = $ipacTableDef->name();
#            if ($name =~ m/_u$/) {
#                push(@idxPath, $n);
#                $ipacTableDef->width($ipacTableDef->width()+$widthAdjust);
#                if ($name eq "coverage_u") {
#                    $ipacTableDef->name("coverage");
#                } elsif ($name eq "noise_u") {
#                    $ipacTableDef->name("uncertainty");
#                } elsif ($name eq "flags_u") {
#                    $ipacTableDef->name("mask");
#                }
#            } elsif ($name eq "fname") {
#                $ipacTableDef->width($ipacTableDef->width()+$widthAdjust);
#                push(@idxPath, $n);
#            }
#            $n++;
#        }
#    } else {
# record indexes of path fields
        my $n = 0;
        foreach my $ipacTableDef (@ipacTableDefs) {
            my $name = $ipacTableDef->name();
            if ($name eq "fname" or $name =~ /_u$/ or $name =~ /_U/) {
                $ipacTableDef->width($ipacTableDef->width()+$widthAdjust);
                push(@idxPath, $n);
            }
            $n++;
        }
#    }
# update path fields
    foreach my $aPrimary (@primaryFiles) {
        my @flds = $aPrimary->fields();
        foreach my $idx (@idxPath) {
            if (not $flds[$idx] eq "none") {
                $flds[$idx] = $setInfo->name() . "/" . $flds[$idx];
            }
        }
        $aPrimary->fields(@flds);
    }


# print out attributes
    my $id = $outInfo->{FILENAME};
    $id =~ s/.tbl$//;
    my $desc = $setInfo->description();
    if ($outInfo->{USE_ID_IN_DESC}) {
        my $descid = $id;
        $descid =~ s/[^_]+_//;
        $desc = $desc . " - " . $descid;
    }
    print OUT "\\fixlen = T\n";
    printf OUT "\\description = %s (<a href=\"http://irsa.ipac.caltech.edu/data/SPITZER/%s\" target=\"_blank\">more</a>)\n", $desc, $setInfo->name();
    printf OUT "\\identifier = %s\n", $setInfo->identifier() . $id;
    print OUT "\\notes = The data set for ops use\n";
    print OUT "\\datatype = images\n";
    print OUT "\\archive = IRSA\n";
    printf OUT "\\set = %s\n", $setInfo->name();
#    printf OUT "\\set = %s\n", uc($id); 
   
# print out definition line 
    printf OUT "|%*s|", $sourceWidth, "tblsource";
    foreach my $ipacTableDef (@ipacTableDefs) {
        printf OUT "%*s|", $ipacTableDef->width(), $ipacTableDef->name();
    }
#    if ($isSwire) {
#        print OUT "\n";
#    } else {
        printf OUT "%*s|%*s|%*s|\n", $coverageWidth, "coverage", $uncertaintyWidth, "uncertainty", $maskWidth, "mask";
#    }

# print out types line
    printf OUT "|%*s|", $sourceWidth, "char";
    foreach my $ipacTableDef (@ipacTableDefs) {
        printf OUT "%*s|", $ipacTableDef->width(), $ipacTableDef->type();
    }
#    if ($isSwire) {
#        print OUT "\n";
#    } else {
        printf OUT "%*s|%*s|%*s|\n", $coverageWidth, "char", $uncertaintyWidth, "char", $maskWidth, "char";
#    }

#print out fields
    foreach my $aPrimary (@primaryFiles) {
        printf OUT " %*s ", $sourceWidth, $aPrimary->source();
        my @flds = $aPrimary->fields();
        my $n = 0;
        foreach my $ipacTableDef (@ipacTableDefs) {
            printf OUT "%*s ", $ipacTableDef->width(), $flds[$n];
            $n++;
        }
#        if ($isSwire) {
#            print OUT "\n";
#        } else {
            my $coverage = $aPrimary->coverage();
            if (not defined $coverage) { $coverage = "none";}
            my $uncertainty = $aPrimary->uncertainty();
            if (not defined $uncertainty) { $uncertainty = "none"; }
            my $mask = $aPrimary->mask();
            if (not defined $mask) { $mask = "none"; }
            printf OUT "%*s %*s %*s \n", $coverageWidth, $coverage, $uncertaintyWidth, $uncertainty, $maskWidth, $mask;
#        }

#    printf "(%d)  %s -- ", $aPrimary->row(), $aPrimary->fullname();
#    if (defined $aPrimary->coverage()) {
#	printf "cov:%s ", $aPrimary->coverage();	
#    } 
#    if (defined $aPrimary->uncertainty()) {
#	printf "unc:%s ", $aPrimary->uncertainty();	
#    }
#    if (defined $aPrimary->mask()) {
#	printf "mask:%s ", $aPrimary->mask();	
#    }
#    print "\n";
    }

}





sub isAllPrimaryFile {

my @allPrimaryFilenames = (
	qr/C2D\/images_bolocam.tbl/,
	qr/FEPS\//,
	qr/FLS_ELAISN1_R\//,
	qr/FLS_MAIN_R\//,
	qr/FLS_VLA\//,
	qr/GLIMPSE\//,
	qr/GOALS\//,
	qr/LVL\/halpha.tbl/,
	qr/LVL\/galex.tbl/,
	qr/MIPS_LG\//,
	qr/SAGE\/spec_cubes.tbl/,
	qr/SAGE\/irac_mos.tbl/,
);

    my $filename = shift;
    foreach my $re (@allPrimaryFilenames) {
	if ($filename =~ /$re/) { 
	    printf "NOTE: %s contains only primary filenames\n", $filename; 
	    return 1; 
	}
    }
    return 0;
}    


sub getMax {
    my $val1 = $_[0];
    my $val2 = $_[1];
    if ($val1>$val2) {
        return $val1;
    } else {
        return $val2;
    }
}

sub mergeDefinitions {
    my @defs1 = @{$_[0]}; # dereference first array reference
    my @defs2 = @{$_[1]}; # dereference second array reference
    my @combinedDefs;

    if ($#defs1 < 1) {
        push(@combinedDefs, @defs2);
    } else {
        for (my $i=0; $i<=$#defs1; $i++) {
            my $def1 = $defs1[$i];
            my $def2 = $defs2[$i];
            if (not $def1->name() eq $def2->name()) {
                return undef;
            }
            my $width1 = $def1->width();
            my $width2 = $def2->width();
            my $width = &getMax($width1, $width2);
            my $def = new IpacTableDefinition($def1->index(), $def1->name(), 0, $width);
            $def->type($def1->type());
            push(@combinedDefs, $def);
        }
    }
    return \@combinedDefs;
}

sub usage {
    my $scriptName = basename($0);
    print <<"HELP";
USAGE
  $scriptName legacydir outdir
HELP
exit(0);
}

