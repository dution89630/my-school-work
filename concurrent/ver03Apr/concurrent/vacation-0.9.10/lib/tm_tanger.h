/**
 *  @author Aleksandar Dragojevic aleksandar.dragojevic@epfl.ch
 */

#ifndef TM_H
#define TM_H 1

#include <stdlib.h>

//#include "tanger-stm-std-math.h"
//#include "tanger-stm-std-stdlib.h"
//#include "tanger-stm-std-string.h"
#include "tanger-stm.h"
#include "tanger-stm-std-string.h"
#include "tanger-stm-std-stdlib.h"

#include <stdio.h>

#define MAIN(argc, argv)              int main (int argc, char** argv)
#define MAIN_RETURN(val)              return val
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

#define TM_ARGDECL_ALONE                /* nothing */
#define TM_ARGDECL                      /* nothing */
#define TM_ARG                          /* nothing */
#define TM_ARG_ALONE                    /* nothing */
#define TM_CALLABLE                     /* nothing */

#define TM_STARTUP(numThread)     tanger_init()
#define TM_SHUTDOWN()             tanger_shutdown()

#define TM_THREAD_ENTER()         tanger_thread_init()
#define TM_THREAD_EXIT()          tanger_thread_shutdown()

#define P_MALLOC(size)            malloc(size)
#define P_FREE(ptr)               free(ptr)
#define TM_MALLOC(size)           malloc(size)
#define TM_FREE(ptr)              free(ptr)

#define TM_BEGIN()                tanger_begin()
#define TM_BEGIN_ID(ID)           tanger_begin()
#define TM_BEGIN_RO()             tanger_begin()
#define TM_END()                  tanger_commit()
#define TM_RESTART()              tanger_restart()

#define TM_EARLY_RELEASE(var)       /* nothing */

#define TM_SHARED_READ(var)           (var)
#define TM_SHARED_READ_P(var)         (var)
#define TM_SHARED_READ_F(var)         (var)

#define TM_SHARED_WRITE(var, val)     ((var) = (val))
#define TM_SHARED_WRITE_P(var, val)   ((var) = (val))
#define TM_SHARED_WRITE_F(var, val)   ((var) = (val))

#define TM_LOCAL_WRITE(var, val)      ((var) = (val))
#define TM_LOCAL_WRITE_P(var, val)    ((var) = (val))
#define TM_LOCAL_WRITE_F(var, val)    ((var) = (val))

#ifdef __cplusplus
extern "C" {
#endif	

static double tanger_wrapperpure_log(double) __attribute__ ((weakref("log")));
static double tanger_wrapperpure_sqrt(double) __attribute__ ((weakref("sqrt")));
static double tanger_wrapperpure_acos(double) __attribute__ ((weakref("acos")));
static double tanger_wrapperpure_fabs(double) __attribute__ ((weakref("fabs")));
static int tanger_wrapperpure_tolower(int) __attribute__ ((weakref("tolower")));

#ifdef __cplusplus
}
#endif
		
#endif /* TM_H */

