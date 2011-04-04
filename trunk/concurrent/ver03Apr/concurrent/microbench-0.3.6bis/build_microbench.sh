#!/bin/sh

#version/computer specific stuff
CON_VER=ver03Apr
#hp laptop
DIR=/home/tcrain
#asus laptop
#DIR=/home/tc


# list of stms to test
#TARGETS="estm wlpdstm tinystm sequential"
TARGETS="estm tiny10B sequential"
# list of lock to test
LOCKS="lock spinlock"
#LOCKS="spinlock"
# othe list for lockfree
LFS="lockfree"
# current microbench version
MBENCH_VERSION=0.3.6
# where to store binaries
EXPERIMENT_BIN_DIR=${DIR}/my-school-work/concurrent/${CON_VER}/concurrent/microbench-0.3.6bis/bin
# make or gmake
MAKE="make"

for TARGET in $TARGETS
do
       printf "\n********************************\n*  Building $app with $TARGET...\n********************************\n\n"
       ${MAKE} clean
       ${MAKE} ${TARGET} 
       mv linkedlist/lf-ll ${EXPERIMENT_BIN_DIR}/ll-${MBENCH_VERSION}-${TARGET}
       mv skiplist/lf-sl ${EXPERIMENT_BIN_DIR}/sl-${MBENCH_VERSION}-${TARGET}
       mv avltree/lf-at ${EXPERIMENT_BIN_DIR}/at-${MBENCH_VERSION}-${TARGET}
       mv newavltree/newlf-at ${EXPERIMENT_BIN_DIR}/newat-${MBENCH_VERSION}-${TARGET}
       mv hashtable/lf-ht ${EXPERIMENT_BIN_DIR}/ht-${MBENCH_VERSION}-${TARGET}
       mv rbtree/lf-rt ${EXPERIMENT_BIN_DIR}/rt-${MBENCH_VERSION}-${TARGET}
       mv deque/lf-dq ${EXPERIMENT_BIN_DIR}/dq-${MBENCH_VERSION}-${TARGET}
done
for LOCK in $LOCKS
do 
       printf "\n********************************\n*  Building $app with $LOCK...\n********************************\n\n"
       ${MAKE} clean
       ${MAKE} ${LOCK} 
       mv linkedlist-lock/lb-ll ${EXPERIMENT_BIN_DIR}/ll-${MBENCH_VERSION}-${LOCK}
       mv skiplist-lock/lb-sl ${EXPERIMENT_BIN_DIR}/sl-${MBENCH_VERSION}-${LOCK}
       mv hashtable-lock/lb-ht ${EXPERIMENT_BIN_DIR}/ht-${MBENCH_VERSION}-${LOCK}
done
for LF in $LFS
do 
       printf "\n********************************\n*  Building $app with $LF...\n********************************\n\n"
       ${MAKE} clean
       ${MAKE} ${LF} 
       mv linkedlist/lf-ll ${EXPERIMENT_BIN_DIR}/ll-${MBENCH_VERSION}-${LF}
       mv skiplist/lf-sl ${EXPERIMENT_BIN_DIR}/sl-${MBENCH_VERSION}-${LF}
       mv hashtable/lf-ht ${EXPERIMENT_BIN_DIR}/ht-${MBENCH_VERSION}-${LF}
done

${MAKE} clean

