use strict; use warnings;
my (%true_calls,%mock_calls);
open my $th, '<', 'repo/.tmp/audit/true_methods.tsv' or die $!;
while(<$th>){ chomp; my($m,$p,$fn)=split(/\t/,$_,3); push @{ $true_calls{$m} }, [$p,$fn]; }
close $th;
open my $mh, '<', 'repo/.tmp/audit/mock_raw.tsv' or die $!;
while(<$mh>){ chomp; my($m,$p,$f)=split(/\t/,$_,3); push @{ $mock_calls{$m} }, [$p,$f]; }
close $mh;
open my $eh, '<', 'repo/.tmp/audit/endpoints_with_file.tsv' or die $!;
while(<$eh>){
  chomp; my($m,$ep,$src)=split(/\t/,$_,3);
  my $re = quotemeta($ep); $re =~ s{\\\{var\\\}}{[^\\/?]+}g; my $qr=qr/^$re$/;
  my ($t,$fn)=(0,'');
  for my $r (@{ $true_calls{$m} || [] }){ my($p,$f)=@$r; if($p =~ $qr){$t=1;$fn=$f;last;} }
  my ($h,$hf)=(0,'');
  for my $r (@{ $mock_calls{$m} || [] }){ my($p,$f)=@$r; if($p =~ $qr){$h=1;$hf=$f;last;} }
  my $covered = ($t||$h)?'yes':'no';
  my $type = $t ? 'true no-mock HTTP' : ($h ? 'HTTP with mocking' : 'unit-only / indirect');
  my $testfile = $t ? 'repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java' : ($h ? $hf : '-');
  my $evidence = $t ? $fn : ($h ? 'mockMvc.perform(..)' : '-');
  print "$m $ep\t$covered\t$type\t$testfile\t$evidence\t$src\n";
}
close $eh;
