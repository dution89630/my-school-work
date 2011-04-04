# ==============================================================================
#
# Defines.common.mk
#
# ==============================================================================

#computer settings
#asus laptop
#COMPUTER := /home/tc
#hp laptop
COMPUTER := /home/tcrain
CONVER := ver03Apr
CONDIR := $(COMPUTER)/concurrent/$(CONVER)/concurrent/

CC       := gcc
CFLAGS   += -Wall -pthread
#CFLAGS   += -O3
CFLAGS   += -g
CFLAGS   += -I$(LIB) -I$(LIBAO_INC)
CPP      := g++
CPPFLAGS += $(CFLAGS)
LD       := g++
LIBS     += -lpthread

# Remove these files when doing clean
OUTPUT +=

LIB := ../lib
LIBAO_INC := $(CONDIR)/libatomic/include

#STM := /home/gramoli/svn/private/dev/tmfriendly/tinySTM-1.0.0
STM := $(CONDIR)/tinySTM-1.0.0

LOSTM := ../../OpenTM/lostm


# ==============================================================================
#
# End of Defines.common.mk
#
# ==============================================================================
