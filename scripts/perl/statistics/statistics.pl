#!/usr/bin/perl -w

BEGIN {
# search the directory where script is located for modules
use File::Basename;
push @INC, dirname(__FILE__);}

use strict;

use Time::Local;
use POSIX qw(strftime);

use Getopt::Std;

use Stat;
use MiniStat;


my $verbose = 2; 
my $ipMaxCnt = 20;

our $date_format_pattern_noyear = qr/(\d\d)\/(\d\d) (\d\d):(\d\d):(\d\d)/;
our $date_format_pattern_notime = qr/(\d\d\d\d)\/(\d\d)\/(\d\d)/;
our $date_format_pattern = qr/(\d\d\d\d)\/(\d\d)\/(\d\d) (\d\d):(\d\d):(\d\d)/;

our $search_type;
our $download_type;
our $rec;
our $total_rec;

# process options
my %options = ();
getopts("hv:t:i:c:", \%options);                                                                                                                                                
&usage if defined $options{h};

$verbose = $options{v} if defined $options{v};
$ipMaxCnt = $options{c} if defined $options{c};

my ($time_period, $current_time); 

# report ending time 
if (defined $options{t}) {
    $current_time = &dt_parse($options{t});
#   my ($mm, $dd, $yy) = split('/', $options{t});
#   $current_time = timelocal(00, 30, 23, $dd, $mm-1, $yy-1900);
} else {
    $current_time = timelocal(localtime());
}

# time interval covered by the report
if (defined $options{i} and $options{i} > 1) {
    $time_period = $options{i};
} else {
    # seven days
    $time_period = 7*86400;
}

if ($#ARGV < 0) {
    &usage;
}


my $download_queued_stat = new Stat();
my $download_immediate_stat = new Stat();
my $download_size0_stat = new Stat();
my $download_file_stat = new MiniStat();
my $download_by_wavelength_total = new MiniStat();
my $download_by_datatype_total = new MiniStat();
my (%browsers, %searches, %searches_nodata, %searches_by_originator, %downloadByWavelength, %downloadByDataType, %downloadFileByExt, %searchHosts, %downloadHosts);

my $nodata_present = 0;
my $time;
for my $f ( 0 .. $#ARGV ) {
    my $file = $ARGV[$f];
    open(LOG, "< $file") || die "Can read $file: $!\n";

    while (<LOG>) {
	chomp;
	my $time = &dt_parse($_);
	my $time_passed = $current_time - $time;
	if ($time_passed >= 0 and $time_passed < $time_period) {
	} else {
	    next;
	}

        if (/\s+search\s+/) {
	    if (/SEARCH-LOG-COLUMNS-DESC/) {
            } elsif (/BROWSER/) {
                my ($browser, $version, $os) = m/BROWSER: (\w+)\s+Version:\s+([\d,\.,-]+)\s+-\s+(.+)$/;
		my $browser_entry = $os . " " . $browser . " " . $version;
		if (not defined($browsers{$browser_entry})) {
		    $browsers{$browser_entry} = 0;
		}
		$browsers{$browser_entry} += 1;
	    } else {
		s/127.0.0.1, 127.0.0.1/127\.0\.0\.1/;
		my @fields = split(/\s+/);
		my ($type, $ip, $rows, $size, $time) = ($fields[3], $fields[4], $fields[5], $fields[6], $fields[8]); 
		#m/\s+search\s+(\w+)\s+/;
		if ($rows < 1) {
		    $nodata_present = 1;
		    if (not defined($searches_nodata{$type})) {
			$searches_nodata{$type} = new Stat();;
		    }
		    ($searches_nodata{$type})->addLine($time, $size) ;
		} else {
		    if (not defined($searches{$type})) {
			$searches{$type} = new Stat();;
		    }
		    ($searches{$type})->addLine($time, $size) ;
		}
		# some searches might have originator set, ex. Originator=HTTP_ByFixedTarget for SHA VO searches 
		my ($originator) = ($fields[10] =~ m/Originator=([^,]+)[,\)]/);
		if (defined ($originator)) {
		    if (not defined($searches_by_originator{$originator})) {
			$searches_by_originator{$originator} = new Stat();;
		    }
		    ($searches_by_originator{$originator})->addLine($time, $size) ;
		}
		if (not defined $searchHosts{$ip}) {
		    $searchHosts{$ip} = 0;
		}
		$searchHosts{$ip} +=1;
	    }
        } elsif (/\s+download\s+/) {
	    #size zipped (uncompressed size of all files) and time it took to zip
	    if (/\s+File\:/) {
		my ($bytes) = m/bytes: (\d+)/;
		$download_file_stat->add(1, $bytes/1024.0/1024.0);
		if (/\.[^\.]*, size/) {
		    my ($ext) = m/\.([^\.]*), size/;
		    if (not defined($downloadFileByExt{$ext})) {
			$downloadFileByExt{$ext} = new MiniStat();
		    }
		    $downloadFileByExt{$ext}->add(1, $bytes/1024.0/1024.0);
		}
	    } elsif (/\s+immediate/) {
		#print $_;
		my ($zsize, $wtime_h, $wtime_m, $wtime_s, $ip) = m/ (\d+) -- zTime.+wTime: (\d\d):(\d\d):(\d\d).+host: (.*)$/;
		my $wtime = $wtime_h*60*60+$wtime_m*60+$wtime_s;
		$download_immediate_stat->addLine($wtime, $zsize/1024/1024);
		if (not defined $downloadHosts{$ip}) {
                    $downloadHosts{$ip} = 0;
                }
		$downloadHosts{$ip} +=1;
	    } elsif (/\s+queued/) {
		#print $_;
		my ($zsize, $wtime_h, $wtime_m, $wtime_s, $ip) = m/ (\d+) -- zTime.+wTime: (\d\d):(\d\d):(\d\d).+host: (.*)$/;
		my $wtime = $wtime_h*60*60+$wtime_m*60+$wtime_s;
		if ($zsize > 0) {
		    $download_queued_stat->addLine($wtime, $zsize/1024/1024);
		} else {
		    $download_size0_stat->addLine($wtime, 0);
		}
		if (not defined $downloadHosts{$ip}) {
                    $downloadHosts{$ip} = 0;
                }
		$downloadHosts{$ip} +=1;
	    } elsif (/\s+wavelength\s+/) {
		my @fields = split(/--/);
		if ($#fields >= 1) {
		    for my $f ( 1 .. $#fields) {
			my ($type, $files, $size) = ($fields[$f] =~ m/\s(.+)\s\:\s(\d+)\s(\d+)/);
			if (not defined($downloadByWavelength{$type})) {
			    $downloadByWavelength{$type} = new MiniStat();
			}
			$downloadByWavelength{$type}->add($files, $size/1024.0);
			$download_by_wavelength_total->add($files, $size/1024.0);
		    }
		}
	    } elsif (/\s+datatype\s+/) {
		my @fields = split(/--/);
                if ($#fields >= 1) {
                    for my $f ( 1 .. $#fields) {
                        my ($type, $files, $size) = ($fields[$f] =~ m/\s(.+)\s\:\s(\d+)\s(\d+)/);
                        if (not defined($downloadByDataType{$type})) {
                            $downloadByDataType{$type} = new MiniStat();
                        }
			$downloadByDataType{$type}->add($files, $size/1024.0);
			$download_by_datatype_total->add($files, $size/1024.0);
                    }
                }
	    }
        }
    }
}

if ($verbose > 1) {
    printf "\nArchive Usage Statistics: %s - %s\n\n", &dt_fmt_hdr($current_time - $time_period), &dt_fmt_hdr($current_time);
}

# SUMMARY
{
    my @excludeSearchTypes = ("searchHistory", "tags", "GatorDD","catScan","irsaCatalogMasterTable","WiseQueryArtifact");
    my $totalSearches = 0;
    foreach $search_type (keys(%searches)) {
	if (not (grep {$_ eq $search_type} @excludeSearchTypes)) {
	    $totalSearches += $searches{$search_type}->getTotalNum();
	} 
    }
    foreach $search_type (keys(%searches_nodata)) {
	if (not (grep {$_ eq $search_type} @excludeSearchTypes)) {
	    $totalSearches += $searches_nodata{$search_type}->getTotalNum();
	} 
    }
    my $packagedUncompressedMB = $download_immediate_stat->getTotalSize()+$download_queued_stat->getTotalSize();
    
    if ($verbose>0) {
	printf "\n";
	printf "%-21s\t%15s\t%10s\t%13s\t%14s\t%10s\n", "Report Period", "Search Requests", "Unique IPs", "Downloaded GB", "Packaged GB", "Unique IPs";
	printf "%-21s\t%15s\t%10s\t%13s\t%14s\t%10s\n", "", "", "(search)", "(compressed)", "(uncompressed)", "(download)";
	printf "%-21s\t%15s\t%10s\t%13s\t%14s\t%10s\n", "---------------------", "---------------", "----------", "-------------", "--------------", "----------";
    }
    printf "%s-%s\t%15d\t%10d\t%13.1f\t%14.1f\t%10d\n", &dt_fmt_brief($current_time - $time_period), &dt_fmt_brief($current_time), $totalSearches, (scalar keys %searchHosts), $download_file_stat->getTotalSize()/1024.0, $packagedUncompressedMB/1024.0, (scalar keys %downloadHosts);
    if ($verbose < 2) {
	exit 0;
    }
}

printf "\n\nBROWSERS:\n\n";
my ($browser_entry, $numBrowserRecs);
$numBrowserRecs = 0;
foreach $browser_entry (keys(%browsers)) {
    $numBrowserRecs += $browsers{$browser_entry};
}
if ($numBrowserRecs > 0) {
    foreach $browser_entry (sort(keys(%browsers))) {
	printf "    %35.35s: %10d (%.1f%%)\n", $browser_entry, $browsers{$browser_entry}, ($browsers{$browser_entry}*100.0/$numBrowserRecs);
    } 
}

printf "\n\nSUCCESSFUL SEARCHES: \n";
$~ = 'TOP';
write(STDOUT);
foreach $search_type (sort(keys(%searches))) {
    $rec = $searches{$search_type};
    $~ = 'REC';
    write(STDOUT);
}

if ($nodata_present) {
    printf "\nNO DATA RETURNED: \n";
    $~ = 'TOP_NODATA';
    write(STDOUT);
    foreach $search_type (sort(keys(%searches_nodata))) {
	$rec = $searches_nodata{$search_type};
	$~ = 'REC_NODATA';
	write(STDOUT);
    }
}

if (scalar(keys(%searches_by_originator)) > 0) {
    printf "\nSEARCHES WITH KNOWN ORIGINATOR: \n(subset of the searches above)\n";
    $~ = 'TOP';
    write(STDOUT);
    foreach $search_type (sort(keys(%searches_by_originator))) {
	$rec = $searches_by_originator{$search_type};
	$~ = 'REC';
	write(STDOUT);
    }
}

printf "\n\nDOWNLOADS: \n\n";
my $total_size = $download_immediate_stat->getTotalSize();
printf "    Immediately packaged requests: %d     (%.0f MB = %.1f GB)\n", $download_immediate_stat->getTotalNum(), $total_size, $total_size/1024;
if ($total_size > 0) {
    printf "    Average requested uncompressed size: %.1f MB (min %.3f MB; max %.1f MB)\n", $download_immediate_stat->getAverageSize(), $download_immediate_stat->getMinSize(), $download_immediate_stat->getMaxSize();
    printf "    Average packaging time: %.1f sec (min %.0f sec; max %.0f sec)\n", $download_immediate_stat->getAverageTime(), $download_immediate_stat->getMinTime(), $download_immediate_stat->getMaxTime();
}
printf "\n";
$total_size = $download_queued_stat->getTotalSize();
printf "    Queued packaging requests: %d     (%.0f MB = %.1f GB)\n", $download_queued_stat->getTotalNum(), $total_size, $total_size/1024;
if ($total_size > 0) {
    printf "    Average requested uncompressed size: %.1f MB (min %.1f MB; max %.1f MB)\n", $download_queued_stat->getAverageSize(), $download_queued_stat->getMinSize(), $download_queued_stat->getMaxSize();
    printf "    Average packaging time: %.0f sec (min %.0f sec; max %.0f sec)\n", $download_queued_stat->getAverageTime(), $download_queued_stat->getMinTime(), $download_queued_stat->getMaxTime();
}
printf "\n";
my $total_num = $download_size0_stat->getTotalNum();
printf "    Aborted or failed packaging requests: %d\n", $total_num; 
if ($total_size > 0) {
    printf "    Average time: %.0f sec (min %.0f sec; max %.0f sec)\n", $download_size0_stat->getAverageTime(), $download_size0_stat->getMinTime(), $download_size0_stat->getMaxTime();
}
printf "\n";
$total_size = $download_file_stat->getTotalSize();
$total_num = $download_file_stat->getTotalNum();
printf "    File download requests: %d     (%.0f MB = %.1f GB)\n", $total_num, $total_size, $total_size/1024;
foreach my $ext (sort(keys(%downloadFileByExt))) {
    printf STDOUT "      %s -- %d files or %.0f%%, %.0f MB\n", $ext, $downloadFileByExt{$ext}->getTotalNum(), (100.0*$downloadFileByExt{$ext}->getTotalNum()/($total_num*1.0)), $downloadFileByExt{$ext}->getTotalSize();
}
printf("\n\n");

my ($wavelength, $datatype);

if ($download_by_datatype_total->getTotalNum() > 0) {
    printf "\nPACKAGING REQUESTS BY DATA TYPE: \n";
    $~ = 'TOP_DOWNLOAD';
    write(STDOUT);
    $total_rec = $download_by_datatype_total;
    foreach $datatype (sort(keys(%downloadByDataType))) {
	$download_type = $datatype;
	$rec = $downloadByDataType{$datatype};
	$~ = 'REC_DOWNLOAD';
	write(STDOUT);
    }
    printf("\n");
    $download_type = "TOTAL";
    $rec = $total_rec;
    write(STDOUT);
}

if ($download_by_wavelength_total->getTotalNum() > 0) {
    printf "\nPACKAGING REQUESTS BY WAVELENGTH \n(raw, bcd, pbcd primary products only): \n";
    $~ = 'TOP_DOWNLOAD';
    write(STDOUT);
    $total_rec = $download_by_wavelength_total;
    foreach $wavelength (sort(keys(%downloadByWavelength))) {
	$download_type = $wavelength;
	$rec = $downloadByWavelength{$wavelength};
	$~ = 'REC_DOWNLOAD';
	write(STDOUT);
    }
    printf("\n");
    $download_type = "TOTAL";
    $rec = $total_rec;
    write(STDOUT);
    
    printf("\n\n");
}

# HOSTS - printed in summary
#printf "UNIQUE IPs (search): %d\n", (scalar keys %searchHosts); 
#printf "UNIQUE IPs (download): %d\n", (scalar keys %downloadHosts);

if ($verbose > 2) {
    my $modulesAvailable = 1;
    eval " use LWP::Simple";
     if ( $@ ) {
	$modulesAvailable = 0;
    }
    eval "use XML::XPath";
    if ( $@ ) {
	$modulesAvailable = 0;
    }
    my ($info, $org);
  SK: {
      printf "First %d most active IPs on search:\n", $ipMaxCnt; 
      my $cnt = 0;
      foreach my $sk (sort {(sprintf "%08d", $searchHosts{$b}) cmp (sprintf "%08d", $searchHosts{$a}) } keys %searchHosts) {
	  $cnt++;
	  if ($cnt > $ipMaxCnt) { last SK; }
	  
	  if ($modulesAvailable) {
	      $info = get(sprintf "http://whois.arin.net/rest/nets;q=%s?showDetails=true&showARIN=false", $sk);
	      if (defined $info) {
		  my $xPath = XML::XPath->new(xml => $info);
		  $org = $xPath->find('//net[last()]/orgRef/@name');
		  $org = $org->string_value;
	      } else {
		  $org = "";
	      }
	      printf "%8d - %16s (%s)\n", $searchHosts{$sk}, $sk, $org;
	  } else {
	      printf "%8d - %16s\n", $searchHosts{$sk}, $sk;
	  }
      }
    }
  DK: {
      printf "First %d most active IPs on download:\n", $ipMaxCnt; 
      my $cnt = 0;
      foreach my $dk (sort {(sprintf "%08d", $downloadHosts{$b}) cmp (sprintf "%08d", $downloadHosts{$a}) } keys %downloadHosts) {
	  $cnt++;
	  if ($cnt > $ipMaxCnt) { last DK; }
	  if ($modulesAvailable) {
	      $info = get(sprintf "http://whois.arin.net/rest/nets;q=%s?showDetails=true&showARIN=false", $dk);
	      if (defined $info) {
		  my $xPath = XML::XPath->new(xml => $info);
		  $org = $xPath->find('//net[last()]/orgRef/@name');
		  $org = $org->string_value;
	      } else {
		  $org = "";
	      }
	      printf "%8d - %16s (%s)\n", $downloadHosts{$dk}, $dk, $org;
	  } else {
	      printf "%8d - %16s\n", $downloadHosts{$dk}, $dk;
	  }
      }
    }
}



exit 0;


# routine to parse time
# takes time string from log file 
# returns local time in seconds
sub dt_parse {
    my ($time_string) = @_;
     
    #printf "TIME_STR: |%s|\n", $time_string;
    my ($year, $mon, $mday, $hour, $min, $sec);       
    if ($time_string =~ m/^$date_format_pattern_noyear/) {
	($mon, $mday, $hour, $min, $sec) = ($time_string =~ /^$date_format_pattern_noyear/);
	$year = 2011;
    } elsif ($time_string =~ m/^$date_format_pattern/) {
	($year, $mon, $mday, $hour, $min, $sec) = ($time_string =~ /^$date_format_pattern/);
    } elsif ($time_string =~ m/^$date_format_pattern_notime/) {
	($year, $mon, $mday) = ($time_string =~ /^$date_format_pattern_notime/);
	$hour=23;
	$min=30;
	$sec=0;
    } else {
	die ("Unsupported timestamp pattern");
    }

    # month from 0 to 11
    $mon -= 1;

    # year since 1900
    $year -= 1900;
    
    # hour from 0 to 23
    
    # time in seconds
    return timelocal($sec, $min, $hour, $mday, $mon, $year);
}


# routine to format time in summary header
sub dt_fmt_hdr {
    my ($epoch) = @_;
    if ($epoch eq '') {
        return "";
    } else {
        return strftime("%B %d, %Y (%H:%M:%S)", localtime($epoch));        
    }
}

sub dt_fmt_brief {
    my ($epoch) = @_;
    if ($epoch eq '') {
        return "";
    } else {
        return strftime("%Y/%m/%d", localtime($epoch));        
    }
}


# prints usage and exits
sub usage {
    print STDERR << "HELP";
SYNOPSIS
    $0 [-hv] [-t end_date] [-i report_period_in_sec] LOG_FILES...;

DESCRIPTION
    The script goes through the archive statistics log files
    and extracts usage statistics. The report is printed to 
    standard output. 

    -h             -- prints this help

    -v             -- verbose 
                      0 brief (one line) report
		      1 brief (one line) report with header
		      2 default complete report
                      3 prints the details for each IP address
                      used to access archive.

    -c number      -- number of IP addresses to show (max 100)		      

    -t yyyy/mm/dd  -- set the end date (23:30 PM) for 
                      the report; default is current time.
		      Use quotes to specify time
		      ex. -t 'yyyy/mm/dd hh:mm:ss'

    -i numsec      -- tells how far back to look. Use 604800 for 
                      weekly (7 days) report. Default - seven days.

    LOG_FILES      -- specifies sha statistics log files. 
                      These files are generated by sha servers.


HELP
    exit;
}

# The format of column headings.                                                                                                                                  

format TOP =

Search Type          Total Searches    Median Time        min          max       Average Size          min         max
                                             (sec)       (sec)        (sec)              (KB)         (KB)        (KB)
-----------------    --------------   --------------------------------------    ---------------------------------------
.

# The format for each record  

format REC =
@<<<<<<<<<<<<<<<<<<<<   @#######      @######.###  @######.###  @######.###       @########.#  @########.#  @########.# 
$search_type, $rec->getTotalNum(), $rec->getMedianTime(), $rec->getMinTime(), $rec->getMaxTime(), $rec->getAverageSize(), $rec->getMinSize(), $rec->getMaxSize()
.

format TOP_NODATA =

Search Type          Total Searches    Median Time        min          max  
                                             (sec)       (sec)        (sec) 
-----------------    --------------   --------------------------------------
.

# The format for each record  (search - no data)

format REC_NODATA =
@<<<<<<<<<<<<<<<<<<<<   @#######      @######.###  @######.###  @######.### 
$search_type, $rec->getTotalNum(), $rec->getMedianTime(), $rec->getMinTime(), $rec->getMaxTime()
.

# The format for download by type

format TOP_DOWNLOAD =

TYPE                    Total Files                Total Size                 Average Size      
                              (num.)     %               (KB)      %                 (KB) 
-----------------       --------------------     ----------------------      -------------
.

# The format for download by type record


format REC_DOWNLOAD =
@<<<<<<<<<<<<<<<<<<<<   @#########   @###%       @##########.#  @###%         @#########.#  
$download_type, $rec->getTotalNum(), (($total_rec->getTotalNum() > 0) ? $rec->getTotalNum()*100.0/$total_rec->getTotalNum() : 0), $rec->getTotalSize(), (($total_rec->getTotalSize() > 0) ? $rec->getTotalSize()*100.0/$total_rec->getTotalSize() : 0), $rec->getAverageSize()
.
