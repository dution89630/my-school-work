/* =============================================================================
 *
 * vacation.c
 *
 * =============================================================================
 *
 * Copyright (C) Stanford University, 2006.  All Rights Reserved.
 * Author: Chi Cao Minh
 *
 * =============================================================================
 *
 * For the license of bayes/sort.h and bayes/sort.c, please see the header
 * of the files.
 * 
 * ------------------------------------------------------------------------
 * 
 * For the license of kmeans, please see kmeans/LICENSE.kmeans
 * 
 * ------------------------------------------------------------------------
 * 
 * For the license of ssca2, please see ssca2/COPYRIGHT
 * 
 * ------------------------------------------------------------------------
 * 
 * For the license of lib/mt19937ar.c and lib/mt19937ar.h, please see the
 * header of the files.
 * 
 * ------------------------------------------------------------------------
 * 
 * For the license of lib/rbtree.h and lib/rbtree.c, please see
 * lib/LEGALNOTICE.rbtree and lib/LICENSE.rbtree
 * 
 * ------------------------------------------------------------------------
 * 
 * Unless otherwise noted, the following license applies to STAMP files:
 * 
 * Copyright (c) 2007, Stanford University
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 * 
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in
 *       the documentation and/or other materials provided with the
 *       distribution.
 * 
 *     * Neither the name of Stanford University nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY STANFORD UNIVERSITY ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL STANFORD UNIVERSITY BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 *
 * =============================================================================
 */


#include <assert.h>
#include <stdlib.h>
#include <stdio.h>
#include <getopt.h>
#include "client.h"
#include "customer.h"
#include "list.h"
#include "manager.h"
#include "map.h"
#include "memory.h"
#include "operation.h"
#include "random.h"
#include "reservation.h"
#include "thread.h"
#include "timer.h"
#include "tm.h"
#include "types.h"
#include "utility.h"

enum param_types {
    PARAM_CLIENTS      = (unsigned char)'c',
    PARAM_NUMBER       = (unsigned char)'n',
    PARAM_QUERIES      = (unsigned char)'q',
    PARAM_RELATIONS    = (unsigned char)'r',
    PARAM_TRANSACTIONS = (unsigned char)'t',
    PARAM_USER         = (unsigned char)'u',
};

#define PARAM_DEFAULT_CLIENTS      (1)
#define PARAM_DEFAULT_NUMBER       (10)
#define PARAM_DEFAULT_QUERIES      (90)
#define PARAM_DEFAULT_RELATIONS    (1 << 16)
#define PARAM_DEFAULT_TRANSACTIONS (1 << 26)
#define PARAM_DEFAULT_USER         (80)


double global_params[256]; /* 256 = ascii limit */


/* =============================================================================
 * displayUsage
 * =============================================================================
 */
static void
displayUsage (const char* appName)
{
    printf("Usage: %s [options]\n", appName);
    puts("\nOptions:                                             (defaults)\n");
    printf("    c <UINT>   Number of [c]lients                   (%i)\n",
           PARAM_DEFAULT_CLIENTS);
    printf("    n <UINT>   [n]umber of user queries/transaction  (%i)\n",
           PARAM_DEFAULT_NUMBER);
    printf("    q <UINT>   Percentage of relations [q]ueried     (%i)\n",
           PARAM_DEFAULT_QUERIES);
    printf("    r <UINT>   Number of possible [r]elations        (%i)\n",
           PARAM_DEFAULT_RELATIONS);
    printf("    t <UINT>   Number of [t]ransactions              (%i)\n",
           PARAM_DEFAULT_TRANSACTIONS);
    printf("    u <UINT>   Percentage of [u]ser transactions     (%i)\n",
           PARAM_DEFAULT_USER);
    exit(1);
}


/* =============================================================================
 * setDefaultParams
 * =============================================================================
 */
static void
setDefaultParams ()
{
    global_params[PARAM_CLIENTS]      = PARAM_DEFAULT_CLIENTS;
    global_params[PARAM_NUMBER]       = PARAM_DEFAULT_NUMBER;
    global_params[PARAM_QUERIES]      = PARAM_DEFAULT_QUERIES;
    global_params[PARAM_RELATIONS]    = PARAM_DEFAULT_RELATIONS;
    global_params[PARAM_TRANSACTIONS] = PARAM_DEFAULT_TRANSACTIONS;
    global_params[PARAM_USER]         = PARAM_DEFAULT_USER;
}


/* =============================================================================
 * parseArgs
 * =============================================================================
 */
static void
parseArgs (long argc, char* const argv[])
{
    long i;
    long opt;

    opterr = 0;

    setDefaultParams();

    while ((opt = getopt(argc, argv, "c:n:q:r:t:u:")) != -1) {
        switch (opt) {
            case 'c':
            case 'n':
            case 'q':
            case 'r':
            case 't':
            case 'u':
                global_params[(unsigned char)opt] = atol(optarg);
                break;
            case '?':
            default:
                opterr++;
                break;
        }
    }

    for (i = optind; i < argc; i++) {
        fprintf(stderr, "Non-option argument: %s\n", argv[i]);
        opterr++;
    }

    if (opterr) {
        displayUsage(argv[0]);
    }
}


/* =============================================================================
 * addCustomer
 * -- Wrapper function
 * =============================================================================
 */
static bool_t
addCustomer (manager_t* managerPtr, long id, long num, long price)
{
    return manager_addCustomer_seq(managerPtr, id);
}


/* =============================================================================
 * initializeManager
 * =============================================================================
 */
static manager_t*
#ifdef TINY10B
initializeManager (long nb_threads)
#else
initializeManager ()
#endif
{
    manager_t* managerPtr;
    long i;
    long numRelation;
    random_t* randomPtr;
    long* ids;
    bool_t (*manager_add[])(manager_t*, long, long, long) = {
        &manager_addCar_seq,
        &manager_addFlight_seq,
        &manager_addRoom_seq,
        &addCustomer
    };
    long t;
    long numTable = sizeof(manager_add) / sizeof(manager_add[0]);

    printf("Initializing manager... ");
    fflush(stdout);

    randomPtr = random_alloc();
    assert(randomPtr != NULL);
#ifdef TINY10B
    managerPtr = manager_alloc(nb_threads);
#else
    managerPtr = manager_alloc();
#endif
    assert(managerPtr != NULL);

    numRelation = (long)global_params[PARAM_RELATIONS];
    ids = (long*)malloc(numRelation * sizeof(long));
    for (i = 0; i < numRelation; i++) {
        ids[i] = i + 1;
    }

    for (t = 0; t < numTable; t++) {

        /* Shuffle ids */
        for (i = 0; i < numRelation; i++) {
            long x = random_generate(randomPtr) % numRelation;
            long y = random_generate(randomPtr) % numRelation;
            long tmp = ids[x];
            ids[x] = ids[y];
            ids[y] = tmp;
        }

        /* Populate table */
        for (i = 0; i < numRelation; i++) {
            bool_t status;
            long id = ids[i];
            long num = ((random_generate(randomPtr) % 5) + 1) * 100;
            long price = ((random_generate(randomPtr) % 5) * 10) + 50;
            status = manager_add[t](managerPtr, id, num, price);
            assert(status);
        }

    } /* for t */

    puts("done.");
    fflush(stdout);

    random_free(randomPtr);
    free(ids);

    return managerPtr;
}


/* =============================================================================
 * initializeClients
 * =============================================================================
 */
static client_t**
initializeClients (manager_t* managerPtr)
{
    random_t* randomPtr;
    client_t** clients;
    long i;
    long numClient = (long)global_params[PARAM_CLIENTS];
    long numTransaction = (long)global_params[PARAM_TRANSACTIONS];
    long numTransactionPerClient;
    long numQueryPerTransaction = (long)global_params[PARAM_NUMBER];
    long numRelation = (long)global_params[PARAM_RELATIONS];
    long percentQuery = (long)global_params[PARAM_QUERIES];
    long queryRange;
    long percentUser = (long)global_params[PARAM_USER];

    printf("Initializing clients... ");
    fflush(stdout);

    randomPtr = random_alloc();
    assert(randomPtr != NULL);

    clients = (client_t**)malloc(numClient * sizeof(client_t*));
    assert(clients != NULL);
    numTransactionPerClient = (long)((double)numTransaction / (double)numClient + 0.5);
    queryRange = (long)((double)percentQuery / 100.0 * (double)numRelation + 0.5);

    for (i = 0; i < numClient; i++) {
        clients[i] = client_alloc(i,
                                  managerPtr,
                                  numTransactionPerClient,
                                  numQueryPerTransaction,
                                  queryRange,
                                  percentUser);
        assert(clients[i]  != NULL);
    }

    puts("done.");
    printf("    Transactions        = %li\n", numTransaction);
    printf("    Clients             = %li\n", numClient);
    printf("    Transactions/client = %li\n", numTransactionPerClient);
    printf("    Queries/transaction = %li\n", numQueryPerTransaction);
    printf("    Relations           = %li\n", numRelation);
    printf("    Query percent       = %li\n", percentQuery);
    printf("    Query range         = %li\n", queryRange);
    printf("    Percent user        = %li\n", percentUser);
    fflush(stdout);

    random_free(randomPtr);

    return clients;
}


/* =============================================================================
 * checkTables
 * -- some simple checks (not comprehensive)
 * -- dependent on tasks generated for clients in initializeClients()
 * =============================================================================
 */
void
checkTables (manager_t* managerPtr)
{
    long i;
    long numRelation = (long)global_params[PARAM_RELATIONS];
    MAP_T* customerTablePtr = managerPtr->customerTablePtr;
    MAP_T* tables[] = {
        managerPtr->carTablePtr,
        managerPtr->flightTablePtr,
        managerPtr->roomTablePtr,
    };
    long numTable = sizeof(tables) / sizeof(tables[0]);
    bool_t (*manager_add[])(manager_t*, long, long, long) = {
        &manager_addCar_seq,
        &manager_addFlight_seq,
        &manager_addRoom_seq
    };
    long t;

    printf("Checking tables... ");
    fflush(stdout);

    /* Check for unique customer IDs */
    long percentQuery = (long)global_params[PARAM_QUERIES];
    long queryRange = (long)((double)percentQuery / 100.0 * (double)numRelation + 0.5);
    long maxCustomerId = queryRange + 1;
    for (i = 1; i <= maxCustomerId; i++) {
        if (MAP_FIND(customerTablePtr, i)) {
            if (MAP_REMOVE(customerTablePtr, i)) {
                assert(!MAP_FIND(customerTablePtr, i));
            }
        }
    }

    /* Check reservation tables for consistency and unique ids */
    for (t = 0; t < numTable; t++) {
        MAP_T* tablePtr = tables[t];
        for (i = 1; i <= numRelation; i++) {
            if (MAP_FIND(tablePtr, i)) {
                assert(manager_add[t](managerPtr, i, 0, 0)); /* validate entry */
                if (MAP_REMOVE(tablePtr, i)) {
                    assert(!MAP_REMOVE(tablePtr, i));
                }
            }
        }
    }

    puts("done.");
    fflush(stdout);
}


/* =============================================================================
 * freeClients
 * =============================================================================
 */
static void
freeClients (client_t** clients)
{
    long i;
    long numClient = (long)global_params[PARAM_CLIENTS];

    for (i = 0; i < numClient; i++) {
        client_t* clientPtr = clients[i];
        client_free(clientPtr);
    }
}

#ifdef MAP_USE_TFAVLTREE
#ifdef SEPERATE_MAINTENANCE

void run_maintenance(void *data) {
  avl_intset_t *tree;
  manager_t *mgrPtr;
  long id;
  
  id = thread_getId();
  mgrPtr = (manager_t *)data;
  
  if(id == mgrPtr->nb_clients) {
    tree = (avl_intset_t*)mgrPtr->carTablePtr;
    //printf("car table start\n");
  } else if(id == mgrPtr->nb_clients+1) {
    tree = (avl_intset_t*)mgrPtr->roomTablePtr;
    //printf("room table start\n");
  } else if(id == mgrPtr->nb_clients+2) {
    tree = (avl_intset_t*)mgrPtr->flightTablePtr;
    //printf("flight table start\n");
  } else if(id == mgrPtr->nb_clients+3) {
    tree = (avl_intset_t*)mgrPtr->customerTablePtr;
    //printf("customer table start\n");
  } else { 
    printf("BAD table start\n");   
    return;
  }

  do_maintenance_thread(tree);

}

#endif
#endif


#ifdef TINY10BNO

void test_maintenance(void *data) {
  short done;
  int i;
  ulong *tmp;
  free_list_item *next, *tmp_item;//, **t_list_items;
  avl_intset_t *tree;
  manager_t *mgrPtr;
  long id;
  ulong nb_propagated, nb_suc_propagated, nb_rotated, nb_suc_rotated, nb_removed;
#ifdef THROTTLE_MAINTENANCE
  //ulong nb_modified, last_modified = 0;
#endif
  ulong *t_nb_trans;
  ulong *t_nb_trans_old;

  id = thread_getId();
  mgrPtr = (manager_t *)data;

  if(id == mgrPtr->nb_clients) {
    tree = (avl_intset_t*)mgrPtr->carTablePtr;
    //printf("car table start\n");
  } else if(id == mgrPtr->nb_clients+1) {
    tree = (avl_intset_t*)mgrPtr->roomTablePtr;
    //printf("room table start\n");
  } else if(id == mgrPtr->nb_clients+2) {
    tree = (avl_intset_t*)mgrPtr->flightTablePtr;
    //printf("flight table start\n");
  } else if(id == mgrPtr->nb_clients+3) {
    tree = (avl_intset_t*)mgrPtr->customerTablePtr;
    //printf("customer table start\n");
  } else { 
    printf("BAD table start\n");   
  }

  t_nb_trans = (ulong *)malloc(mgrPtr->nb_clients * sizeof(ulong));
  t_nb_trans_old = (ulong *)malloc(mgrPtr->nb_clients * sizeof(ulong));
  
  /* Create transaction */
  TM_THREAD_ENTER();
  /* Wait on barrier */
  //barrier_cross(d->barrier);
	
  /* Is the first op an update? */
  //unext = (rand_range_re(&d->seed, 100) - 1 < d->update);
  
  while(!thread_getDone()) {

    //printf("m");
      //do maintenance, but only when there have been enough modifications

      //need to do garbage collection here
      done = 0;
      for(i = 0; i < mgrPtr->nb_clients; i++) {
	t_nb_trans[i] = mgrPtr->nb_committed[i];
	if(t_nb_trans[i] == t_nb_trans_old[i]) {
	  done = 1;
	  //printf("Done at i:%d, %d, %d\n", i, d->t_nb_trans[i], d->t_nb_trans_old[i]);
	  break;
	}
      }
      if(!done) {
	tmp = (t_nb_trans_old);
	(t_nb_trans_old) = (t_nb_trans);
	(t_nb_trans) = tmp;
	next = tree->free_list->next;
	//printf("Starting free\n");
	while(next != NULL) {
	  //printf("FREEING\n");
	  free(next->to_free);
	  tmp_item = next;
	  next = next->next;
	  free(tmp_item);
	}
	tree->free_list->next = NULL;

	/* //now do the frees for the ones removed in the non-maint threads */
	/* for(i = 0; i < mgrPtr->nb_clients; i++) { */
	/*   while(t_free_list[i] != t_list_items[i]) { */
	/*     if(t_free_list[i]->to_free != NULL) { */
	/*       //printf("freeing %d\n", d->t_free_list[i]->to_free->key); */
	/*       free(tree->t_free_list[i]->to_free); */
	/*     } */
	/*     tmp_item = tree->t_free_list[i]; */
	/*     tree->t_free_list[i] = tree->t_free_list[i]->next; */
	/*     free(tmp_item); */
	/*   } */
	/*   //update the t_list */
	/*   t_list_items[i] = tree->t_data[i].free_list; */
	/*   //t_list_items[i] = (free_list_item *)TX_UNIT_LOAD(d->t_data[i].free_list); */
	/* } */
      }
      

      recursive_tree_propagate(tree, &nb_propagated, &nb_suc_propagated, &nb_rotated, &nb_suc_rotated, &nb_removed, tree->free_list);



#ifdef THROTTLE_MAINTENANCE
/*       nb_modified = 0; */
/*       while(nb_modified < (THROTTLE_NUM + last_modified)) { */
/* 	nb_modified = 0; */
/* 	for(i = 0; i < d->nb_threads; i++) { */
/* 	  nb_modified += d->t_data[i].nb_modifications; */
/* 	} */
/* 	usleep(THROTTLE_TIME); */

/* #ifdef ICC */
/* 	if(stop != 0) break; */
/* #else */
/* 	if(AO_load_full(&stop) != 0) break; */
/* #endif /\* ICC *\/ */
	
/*       } */
/*       last_modified = nb_modified; */

usleep(THROTTLE_TIME);

#endif

      
  }
  
  /* Free transaction */
  //printf("Exiting maint\n");
  TM_THREAD_EXIT();
  
}

#endif




#ifdef MAP_USE_TFAVLTREE
#ifdef SEPERATE_MAINTENANCE
typedef struct thread_data {
  manager_t* managerPtr;
  client_t** clients;
  long nbClients;
} thread_data_t;


void
do_run (void* argPtr)
{
  long id;
  id = thread_getId();
  if(id < ((thread_data_t *)argPtr)->nbClients) {
    client_run(((thread_data_t *)argPtr)->clients);
    thread_setDone();
  }
  else {
    run_maintenance(((thread_data_t *)argPtr)->managerPtr);
    //printf("here 2, %d\n", id);
  }
}
#endif
#endif

/* =============================================================================
 * main
 * =============================================================================
 */
MAIN(argc, argv)
{
    manager_t* managerPtr;
    client_t** clients;
    TIMER_T start;
    TIMER_T stop;
#ifdef MAP_USE_TFAVLTREE
#ifdef SEPERATE_MAINTENANCE
    thread_data_t tdata;
#endif
#endif
    GOTO_REAL();

    /* Initialization */
    parseArgs(argc, (char** const)argv);
    SIM_GET_NUM_CPU(global_params[PARAM_CLIENTS]);
    long numThread = global_params[PARAM_CLIENTS];
#ifdef TINY10B
    managerPtr = initializeManager(numThread);
    //printf("Number of cli %d\n", numThread);
#else
    managerPtr = initializeManager();
#endif
    assert(managerPtr != NULL);
    clients = initializeClients(managerPtr);
    assert(clients != NULL);
#ifdef MAP_USE_TFAVLTREE
    //#ifdef TINY10B
    managerPtr->nb_clients = numThread;
#endif

#ifdef MAP_USE_TFAVLTREE
#ifdef SEPERATE_MAINTENANCE
    TM_STARTUP(numThread + 4);
    P_MEMORY_STARTUP(numThread + 4);
    thread_startup(numThread + 4);
    tdata.managerPtr = managerPtr;
    tdata.clients = clients;
    tdata.nbClients = numThread;
#else
    TM_STARTUP(numThread);
    P_MEMORY_STARTUP(numThread);
    thread_startup(numThread);
#endif
#else
    TM_STARTUP(numThread);
    P_MEMORY_STARTUP(numThread);
    thread_startup(numThread);
#endif

    /* Run transactions */
    printf("Running clients... ");
    fflush(stdout);
    TIMER_READ(start);
    GOTO_SIM();
#ifdef OTM
#pragma omp parallel
    {
        client_run(clients);
    }
#else
#ifdef MAP_USE_TFAVLTREE
#ifdef SEPERATE_MAINTENANCE
    //do_run((void*)clients, managerPtr, numThread);
    thread_start(do_run, (void*)&tdata);
#else
    thread_start(client_run, (void*)clients);
#endif
#else
    thread_start(client_run, (void*)clients);
#endif
#endif
    GOTO_REAL();
    TIMER_READ(stop);
    puts("done.");
    printf("Time = %0.6lf\n",
           TIMER_DIFF_SECONDS(start, stop));
    fflush(stdout);
    checkTables(managerPtr);

    /* Clean up */
    printf("Deallocating memory... ");
    fflush(stdout);
    //printf("here11\n");
    freeClients(clients);
    //printf("here12\n");
    /*
     * TODO: The contents of the manager's table need to be deallocated.
     */
    manager_free(managerPtr);
    //xprintf("here13\n");
    puts("done.");
    fflush(stdout);

    TM_SHUTDOWN();
    P_MEMORY_SHUTDOWN();

    GOTO_SIM();

    thread_shutdown();

    MAIN_RETURN(0);
}


/* =============================================================================
 *
 * End of vacation.c
 *
 * =============================================================================
 */

