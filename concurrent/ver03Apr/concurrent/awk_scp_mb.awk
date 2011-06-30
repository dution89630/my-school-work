#!/bin/gawk -f
BEGIN {}
/Nb threads/ {core=$4}
/#txs/ {times[core]+=$3; counts[core]+=1}
END {
    print "# threadnum txs"

    for(i in times) sorted[j++]=i+0
    n = asort(sorted);

    for (i = 1; i <= n; i++) {
	print sorted[i], times[sorted[i]]/counts[sorted[i]];
    }

}
