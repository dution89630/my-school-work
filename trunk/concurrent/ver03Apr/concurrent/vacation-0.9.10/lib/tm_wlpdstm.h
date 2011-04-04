/**
 * Wlpdstm defines used in STAMP applications.
 * 
 * This file is just simplified tm.h from original STAMP distribution.
 * 
 * @author Aleksandar Dragojevic aleksandar.dragojevic@epfl.ch
 */

#ifndef TM_H
#define TM_H 1

#include "stm.h"

#include <stdio.h>

#define MAIN(argc, argv)              int main (int argc, char** argv)
#define MAIN_RETURN(val)              wlpdstm::print_stats();    \
                                      return val
#define GOTO_SIM()                    /* nothing */
#define GOTO_REAL()                   /* nothing */
#define IS_IN_SIM()                   (0)
#define SIM_GET_NUM_CPU(var)          /* nothing */
#define TM_PRINTF                     printf
#define TM_PRINT0                     printf
#define TM_PRINT1                     printf
#define TM_PRINT2                     printf
#define TM_PRINT3                     printf

#define P_MEMORY_STARTUP(numThread)   /* nothing */
#define P_MEMORY_SHUTDOWN()           /* nothing */

#include <string.h>

#define TM_ARGDECL_ALONE                wlpdstm::tx_desc* tx
#define TM_ARGDECL                      wlpdstm::tx_desc* tx,
#define TM_ARG                          tx,
#define TM_ARG_ALONE                    tx
#define TM_CALLABLE                     /* nothing */

#define TM_STARTUP(numThread)     wlpdstm::global_init()
#define TM_SHUTDOWN()             /* nothing */

#define TM_THREAD_ENTER()         wlpdstm::thread_init(); \
                                  wlpdstm::tx_desc *tx = wlpdstm::get_tx_desc()
#define TM_THREAD_EXIT()          /* nothing */

#define P_MALLOC(size)            wlpdstm::s_malloc(size)
#define P_FREE(ptr)               wlpdstm::s_free(ptr)
#define TM_MALLOC(size)           wlpdstm::tx_malloc(tx, size)
#define TM_FREE(ptr)              wlpdstm::tx_free(tx, ptr, sizeof(*ptr))

#define TM_BEGIN()                BEGIN_TRANSACTION_DESC
#define TM_BEGIN_ID(ID)           BEGIN_TRANSACTION_DESC_ID(ID)
#define TM_BEGIN_RO()             STM_BEGIN()
#define TM_END()                  END_TRANSACTION
#define TM_RESTART()              wlpdstm::restart_tx(tx)

#define TM_EARLY_RELEASE(var)       /* nothing */

#define TM_SHARED_READ(var)           wlpdstm::read_word(tx, (wlpdstm::Word *)(&var))
#define TM_SHARED_READ_P(var)         (void *)wlpdstm::read_word(tx, (wlpdstm::Word *)(&var))
#define TM_SHARED_READ_F(var)         wlpdstm::read_float(tx, (&var))

#define TM_SHARED_WRITE(var, val)     wlpdstm::write_word(tx, (wlpdstm::Word *)(&var), (wlpdstm::Word)(val))
#define TM_SHARED_WRITE_P(var, val)   wlpdstm::write_word(tx, (wlpdstm::Word *)(&var), (wlpdstm::Word)(val))
#define TM_SHARED_WRITE_F(var, val)   wlpdstm::write_float(tx, (&var), (val))

#define TM_LOCAL_WRITE(var, val)      ({var = val; var;})
#define TM_LOCAL_WRITE_P(var, val)    ({var = val; var;})
#define TM_LOCAL_WRITE_F(var, val)    ({var = val; var;})

#endif /* TM_H */

