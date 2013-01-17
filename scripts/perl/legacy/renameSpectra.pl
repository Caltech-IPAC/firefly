#!/usr/bin/perl

# foreach f ( `cat SpitzerLegacySpectraDirs.txt ` )
#   ./renameSpectra.pl $f spectra
# end



use strict;

use File::Copy;
use File::Basename;
use SetInfo;
use IpacTableDefinition;
use IpacTableRow;
use IpacTable;

&usage() if ($#ARGV != 1);

my $metadir = shift;
our $outdir = shift;
mkdir "$outdir" unless -d "$outdir";

my $metafilepath = $metadir . "/" . "tables.tbl";
die "$metafilepath does not exist" if (not -e $metafilepath);

my $setInfo = new SetInfo($metadir);
my $name = basename($metadir);
my $lowername = lc($name);
print "*****************\n";
printf "LEGACY PROGRAM: %s\n", $name;

# get table filepaths from image_meta.tbl
my $metaIpacTable = new IpacTable($metafilepath);
my $pathIdx = $metaIpacTable->definitionIndex("path");
my $tbltypeIdx = $metaIpacTable->definitionIndex("tbltype");
die "can not find path column in $metaIpacTable" if ($pathIdx < 0);
my @metarows = $metaIpacTable->rows();
my @filepaths;
for my $metarow (@metarows) {
    if ($metarow->value($tbltypeIdx) eq "spectra") {
        printf "  filepath: %s\n", $metarow->value($pathIdx);
        push(@filepaths, $metadir . "/" . $metarow->value($pathIdx));
    }
}

for my $filepath (@filepaths) {
    print "*****************\n";
    print "  filepath: $filepath \n";
    my $basename = basename($filepath);
    my $fromname = $filepath; 
    my $toname;
    if ($basename =~ /^$name/) {
        $toname = $outdir . "/" . $basename;
    } elsif ($basename =~ /^$lowername/)  {
        $basename =~ s/^$lowername/$name/;
        $toname = $outdir . "/" . $basename;
    } else {
        $basename = $name . "_" . $basename;
        $toname = $outdir . "/" . $basename;
    }
    #copy($fromname, $toname) or die "File cannot be copied.";
    printf "$fromname -> $toname \n";
    open OUT, ">$toname" or die $!;
    # print out attributes
    my $id = $basename;
    $id =~ s/.tbl$//;
    my $desc = $setInfo->description();
    if ($#filepaths > 0) {
        my $descid = $id;
        $descid =~ s/[^_]+_//;
        $desc = $desc . " - " . $descid;
    }
    print OUT "\\fixlen = T\n";
    printf OUT "\\description = %s (<a href=\"http://irsa.ipac.caltech.edu/data/SPITZER/%s\" target=\"_blank\">more</a>)\n", $desc, $setInfo->name();
    printf OUT "\\identifier = %s\n", $setInfo->identifier() . $id;
    print OUT "\\notes = The data set for ops use\n";
    print OUT "\\datatype = spectra\n";
    print OUT "\\archive = IRSA\n";
    printf OUT "\\set = %s\n", $setInfo->name();
#    printf OUT "\\set = %s\n", uc($id);

#    open FILE, "<", $fromname or die $!;
#    while (<FILE>) {
#        print OUT;
#    }
#    close FILE or die $!;

# update path column values: prepend dir
    my $dirPrefix = $setInfo->name() . "/";
    my $widthAdjust = length($dirPrefix);
    my $ipacTable = new IpacTable($fromname);
    my @ipacTableDefs = $ipacTable->definitions();
    my @idxPath;
    my $n = 0;
# print definitions
    printf OUT "|";
    foreach my $ipacTableDef (@ipacTableDefs) {
        my $name = $ipacTableDef->name();
        if ($name =~ m/_u$/ or $name =~ m/_U$/ or $name =~ m/file_name/) {
            $ipacTableDef->width($ipacTableDef->width()+$widthAdjust);
            push(@idxPath, $n);
        }
        printf OUT "%*s|", $ipacTableDef->width(), $ipacTableDef->name();
        $n++;
    }
    print OUT "\n";
# print types
    printf OUT "|";
    foreach my $ipacTableDef (@ipacTableDefs) {
        printf OUT "%*s|", $ipacTableDef->width(), $ipacTableDef->type();
    }
    print OUT "\n";

    my @ipacTableRows = $ipacTable->rows();
    foreach my $ipacTableRow (@ipacTableRows) {
        my @fields = $ipacTableRow->fields();
# add directory prefix
        foreach my $idx (@idxPath) {
            $fields[$idx] =~ s/^\.\///;
            $fields[$idx] = $dirPrefix . $fields[$idx];
        }
# print fields
        my $n=0;
        printf OUT " ";
        foreach my $ipacTableDef (@ipacTableDefs) {
            printf OUT "%*s ", $ipacTableDef->width(), $fields[$n];
            $n++;
        }
        print OUT "\n";
    }
    close OUT or die $!;
}

sub usage {
    my $scriptName = basename($0);
    print <<"HELP";
USAGE
  $scriptName legacydir outdir
HELP
exit(0);
}

