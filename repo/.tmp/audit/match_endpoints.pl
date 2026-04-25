use strict;
use warnings;
my (%true, %mock);
open my $tfh, '<', 'repo/.tmp/audit/true_raw.tsv' or die $!;
while (my $line = <$tfh>) { chomp $line; my ($m,$p,$f)=split(/\t/,$line,3); push @{ $true{$m} }, [$p,$f]; }
close $tfh;
open my $mfh, '<', 'repo/.tmp/audit/mock_raw.tsv' or die $!;
while (my $line = <$mfh>) { chomp $line; my ($m,$p,$f)=split(/\t/,$line,3); push @{ $mock{$m} }, [$p,$f]; }
close $mfh;
open my $efh, '<', 'repo/.tmp/audit/endpoints_with_file.tsv' or die $!;
while (my $line = <$efh>) {
  chomp $line;
  my ($m,$ep,$src)=split(/\t/,$line,3);
  my $re = quotemeta($ep);
  $re =~ s{\\\{var\\\}}{[^\\/?]+}g;
  $re = qr/^$re$/;
  my ($tmatch,$tmfile)=(0,'');
  for my $r (@{ $true{$m} || [] }) { my ($p,$f)=@$r; if ($p =~ $re) { $tmatch=1; $tmfile=$f; last; } }
  my ($mmatch,$mmfile)=(0,'');
  for my $r (@{ $mock{$m} || [] }) { my ($p,$f)=@$r; if ($p =~ $re) { $mmatch=1; $mmfile=$f; last; } }
  my $covered = ($tmatch || $mmatch) ? 'yes' : 'no';
  my $type = $tmatch ? 'true no-mock HTTP' : ($mmatch ? 'HTTP with mocking' : 'unit-only / indirect');
  my $tf = $tmatch ? $tmfile : ($mmatch ? $mmfile : '-');
  print "$m $ep\t$covered\t$type\t$tf\t$src\n";
}
close $efh;
