ROOT = ../..

include $(ROOT)/common/Makefile.common

BINS = $(BINDIR)/lf-ll

.PHONY:	all clean

all:	main

linkedlist.o: 
	$(CC) $(CFLAGS) -c -o $(BUILDIR)/linkedlist.o linkedlist.c

harris.o: linkedlist.h linkedlist.o
	$(CC) $(CFLAGS) -c -o $(BUILDIR)/harris.o harris.c

intset.o: linkedlist.h harris.h
	$(CC) $(CFLAGS) -c -o $(BUILDIR)/intset.o intset.c

test.o: linkedlist.h harris.h intset.h
	$(CC) $(CFLAGS) -c -o $(BUILDIR)/test.o test.c

main: linkedlist.o harris.o intset.o test.o $(TMILB)
	$(CC) $(CFLAGS) $(BUILDIR)/linkedlist.o $(BUILDIR)/harris.o $(BUILDIR)/intset.o $(BUILDIR)/test.o -o $(BINS) $(LDFLAGS)

clean:
	-rm -f $(BINS)
