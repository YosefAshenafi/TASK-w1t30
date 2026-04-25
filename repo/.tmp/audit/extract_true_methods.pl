use strict;
use warnings;

sub norm_expr {
  my ($e)=@_;
  $e =~ s/[\n\r\t ]+//g;
  my @parts=split(/\+/,$e);
  my $o="";
  for my $p (@parts){
    if($p =~ /^"(.*)"$/){$o.=$1}
    elsif($p ne ""){$o.="{var}"}
  }
  $o =~ s/\{var\}\{var\}+/{var}/g;
  $o =~ s/\?.*$//;
  return $o;
}

open my $fh, '<', 'repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java' or die $!;
my ($in,$depth,$name,$buf) = (0,0,'','');
while (my $line = <$fh>) {
  if (!$in && $line =~ /\bvoid\s+([A-Za-z0-9_]+)\s*\(/) {
    $in = 1;
    $name = $1;
    $buf = $line;
    my $opens = ($line =~ tr/{/{/);
    my $closes = ($line =~ tr/}/}/);
    $depth = $opens - $closes;
    next;
  }

  if ($in) {
    $buf .= $line;
    my $opens = ($line =~ tr/{/{/);
    my $closes = ($line =~ tr/}/}/);
    $depth += $opens - $closes;
    if ($depth <= 0) {
      while($buf =~ /rest\.(getForEntity|postForEntity|exchange)\s*\((.*?)\)\s*;/sg){
        my ($kind,$args)=($1,$2);
        my $method="";
        if($kind eq 'getForEntity'){$method='GET'}
        elsif($kind eq 'postForEntity'){$method='POST'}
        else { if($args =~ /HttpMethod\.([A-Z]+)/){$method=$1} }
        next if $method eq '';
        if($args =~ /url\((.*?)\)/s){
          my $u=norm_expr($1);
          next if $u eq '';
          print "$method\t$u\t$name\n";
        }
      }
      ($in,$depth,$name,$buf) = (0,0,'','');
    }
  }
}
close $fh;
