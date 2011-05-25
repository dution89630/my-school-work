#!/bin/gawk -f
BEGIN {}
$1 !~ /#/ {
    if ($1 in times) {
	times[$1] = times[$1] / $2;
    } else {
	times[$1] = $2;
    }
}
END {
    print "# transnum speedup"

    for(i in times) sorted[j++]=i+0
    n = asort(sorted);

    for (i = 1; i <= n; i++) {
	print sorted[i], times[sorted[i]];
    }

}
