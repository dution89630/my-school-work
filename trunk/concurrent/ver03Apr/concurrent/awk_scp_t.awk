#!/bin/gawk -f
BEGIN {}
/Transactions / {trans=$3}
/Time/ {times[trans]+=$3; counts[trans]+=1}
END {
    print "# transnum duration"

    for(i in times) sorted[j++]=i+0
    n = asort(sorted);

    for (i = 1; i <= n; i++) {
	print sorted[i], times[sorted[i]]/counts[sorted[i]];
    }

}
