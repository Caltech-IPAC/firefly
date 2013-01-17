#!/usr/bin/perl                                                                                                                                                  
#
# expandXml.pl wise_internal.xml > wise_expanded.xml
#


use strict;
use warnings;

use File::Basename;
use POSIX;


&usage() if ($#ARGV != 0);

my $fileToExpand = shift;

open FILE, "<", $fileToExpand or die $!;                                                                                                                        
our %entities = ();

while (<FILE>) {
    &processLine($_);
}                                                                                                                                                           
close FILE or die $!;
exit 0;

sub processLine {
    my $line = shift;
    if ($line =~ m/ENTITY.*SYSTEM/) {
	my @vals = ($line =~ m/ENTITY\s+(\S+)\s+SYSTEM\s+\"(\S+)\"/);
	$entities{$vals[0]} = $vals[1];
	#printf STDERR "ENTITY %s, %s\n", $vals[0], $vals[1];
    } elsif ($line =~ /xi:include/) {
	local *INCLUDE_FILE;
	my ($includeFile) = ($line =~ m/href=\"(.*)\"/);
	open INCLUDE_FILE, "<", $includeFile or die $!;
	while (<INCLUDE_FILE>) {
	    if (/DOCTYPE/) {
		#skip
	    } elsif (/^\s*\]\>\s*$/) { 
		#skip "]>" after DOCTYPE
	    } else {
		my $iLine = $_;
		&processLine($iLine);
	    }
	}	
	close INCLUDE_FILE or die $!;
    } elsif ((scalar keys(%entities) > 0) && ($line =~ m/^\s+\&/)) {
	my ($entity) = ($line =~ m/^\s+\&(.*)\;/);
	if (grep( /^$entity$/, keys(%entities) )) {
	    my $txtIncludeFile = $entities{$entity};
	    open TXT_INCLUDE_FILE, "<", $txtIncludeFile or die $!;
	    while (<TXT_INCLUDE_FILE>) {
		print;
	    }
	} else {
	    die "Undefined entity $entity";
	}
    } else {
	print $line;
    }              
    return;
}

sub usage {
    my $scriptName = basename($0);
    print <<"HELP";
USAGE
  $scriptName fileToExpand.xml
HELP
exit(0);
}
