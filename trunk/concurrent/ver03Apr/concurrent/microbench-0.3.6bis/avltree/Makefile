ROOT = ..

include $(ROOT)/common/Makefile.common

BINS = lf-at

.PHONY:	all clean

all:	main

avltree.o:
	$(CC) $(CFLAGS) -c -o avltree.o avltree.c

intset.o: avltree.h
	$(CC) $(CFLAGS) -c -o intset.o intset.c

test.o: avltree.h intset.h
	$(CC) $(CFLAGS) -c -o test.o test.c

main: avltree.o intset.o test.o 
	$(CC) $(CFLAGS) avltree.o intset.o test.o -o lf-at $(LDFLAGS)

clean:
	-rm -f $(BINS) *.o
