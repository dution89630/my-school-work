ROOT = ..

include $(ROOT)/common/Makefile.common

BINS = lf-rt lf-rtmove

.PHONY:	all clean

all:	main main_move

rbtree.o: interface.h
	$(CC) $(CFLAGS) -c -o rbtree.o rbtree.c

intset.o: rbtree.h
	$(CC) $(CFLAGS) -c -o intset.o intset.c

test.o: rbtree.o intset.h
	$(CC) $(CFLAGS) -c -o test.o test.c

main: intset.o test.o $(TMILB)
	$(CC) $(CFLAGS) intset.o test.o -o lf-rt $(LDFLAGS)

move_test.o: rbtree.o intset.h
	$(CC) $(CFLAGS) -c -o move_test.o move_test.c

main_move: intset.o move_test.o $(TMILB)
	$(CC) $(CFLAGS) intset.o move_test.o -o lf-rtmove $(LDFLAGS)

clean:
	-rm -f $(BINS) *.o
