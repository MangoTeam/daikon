: # Use -*- Perl -*- without knowing its path
  eval 'exec perl -S -wni.bak $0 "$@"'
  if 0;
# Remove variables from .dtrace files (changes files in place).
# Usage:  trace-purge-vars.pl [-v] regexp file ...

# -v flag means to retain rather than discard variables matching the
# regular expression.  (Its name is taken from grep.)

# I'm intentionally not operating by paragraphs, because I want the regexp
# to only apply to the variable lines.

# Problem: this hangs if given a file ending in a partial record.

BEGIN {
  $debug = 0;
  # $debug = 1;

  $discard = 1;
  $regexp = shift(@ARGV);
  if ($regexp eq "-v") {
    $discard = 0;
    $regexp = shift(@ARGV);
  }

  $ppt = undef;
}


if (/^$/) {
  undef($ppt);
  if ($debug) { print STDERR "[blank line]\n"; }
  print;
} elsif (! defined($ppt)) {
  $ppt = $_;
  if ($debug) { print STDERR "ppt: $_"; }
  print;
} else {
  $var = $_;
  if ($var eq "this_invocation_nonce\n") {
    $val = <>;
    print $var;
    print $val;
    undef($var);
    undef($val);
  } else {
    if ($debug) { print STDERR "not nonce: $var"; }
    $val = <>;
    $mod = <>;
    $match = ($var =~ /$regexp/);
    if ($match ? $discard : !$discard) {
      # nothing to do
    } else {
      print $var;
      print $val;
      print $mod;
      undef($var);
      undef($val);
      undef($mod);
    }
  }
}
