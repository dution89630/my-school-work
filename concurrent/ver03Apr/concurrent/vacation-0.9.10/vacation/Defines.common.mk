# ==============================================================================
#
# Defines.common.mk
#
# ==============================================================================


CFLAGS += -DLIST_NO_DUPLICATES

#STM
#CFLAGS += -DTINY10B -DTLS -DMAP_USE_RBTREE
CFLAGS += -DTINY10B -DTLS -DMAP_USE_TFAVLTREE -DSEPERATE_MAINTENANCE
##sequential
#CFLAGS += -DSEQAVL -DTINY10B -DTLS -DMAP_USE_TFAVLTREE

PROG := vacation

# SRCS += \
# 	client.c \
# 	customer.c \
# 	manager.c \
# 	reservation.c \
# 	vacation.c \
# 	$(LIB)/list.c \
# 	$(LIB)/pair.c \
# 	$(LIB)/mt19937ar.c \
# 	$(LIB)/random.c \
# 	$(LIB)/rbtree.c \
# 	$(LIB)/thread.c \

SRCS += \
	client.c \
	customer.c \
	manager.c \
	reservation.c \
	vacation.c \
	$(LIB)/list.c \
	$(LIB)/pair.c \
	$(LIB)/mt19937ar.c \
	$(LIB)/random.c \
	$(LIB)/newavltree.c \
	$(LIB)/intset.c \
	$(LIB)/thread.c \

OBJS := ${SRCS:.c=.o}


# ==============================================================================
#
# End of Defines.common.mk
#
# ==============================================================================
