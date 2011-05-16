#!/bin/awk -f
BEGIN {}
/Clients/ {core=$3}
/Time/ {times[core]+=$3; counts[core]+=1}
END {
    print "# threadnum duration"
    for(i in times) {
	print i, times[i]/counts[i];
    }
}
