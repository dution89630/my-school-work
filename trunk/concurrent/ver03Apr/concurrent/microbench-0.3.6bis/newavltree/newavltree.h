#include <assert.h>
#include <getopt.h>
#include <limits.h>
#include <pthread.h>
#include <signal.h>
#include <stdlib.h>
#include <stdio.h>
#include <sys/time.h>
#include <time.h>
#include <stdint.h>

#include <atomic_ops.h>

//#ifndef RBTREE_H
//#define RBTREE_H 1

#include "tm.h"

#define SEPERATE_MAINTENANCE
//#define CHANGE_KEY
//#define SEPERATE_BALANCE
//#define SEPERATE_BALANCE1
//#define SEPERATE_BALANCE2
//#define SEPERATE_BALANCE2DEL
//#define MICROBENCH


#define DEFAULT_DURATION                10000
#define DEFAULT_INITIAL                 256
#define DEFAULT_NB_THREADS              1
#define DEFAULT_NB_MAINTENANCE_THREADS  1
#define DEFAULT_RANGE                   0x7FFFFFFF
#define DEFAULT_SEED                    0
#define DEFAULT_UPDATE                  20
#define DEFAULT_ELASTICITY              4
#define DEFAULT_ALTERNATE               0
#define DEFAULT_EFFECTIVE               1

//#define KEYMAP
//#define SEQUENTIAL


#define XSTR(s)                         STR(s)
#define STR(s)                          #s

#define ATOMIC_CAS_MB(a, e, v)          (AO_compare_and_swap_full((volatile AO_t *)(a), (AO_t)(e), (AO_t)(v)))
#define ATOMIC_FETCH_AND_INC_FULL(a)    (AO_fetch_and_add1_full((volatile AO_t *)(a)))

extern volatile AO_t stop;
extern unsigned int global_seed;
#ifdef TLS
extern __thread unsigned int *rng_seed;
#else /* ! TLS */
extern pthread_key_t rng_seed_key;
#endif /* ! TLS */
extern unsigned int levelmax;

#define TRANSACTIONAL                   d->unit_tx

#define VAL_MIN                         INT_MIN
#define VAL_MAX                         INT_MAX

typedef intptr_t val_t;
//#define val_t void*
typedef intptr_t level_t;


typedef struct avl_node {
  val_t key, val;
  intptr_t deleted;
  intptr_t removed;
  struct avl_node *left;
  struct avl_node *right;
#ifndef SEPERATE_BALANCE
  val_t lefth, righth, localh;
#else
  struct balance_node *bnode;
#endif
} avl_node_t;

#ifdef SEPERATE_BALANCE
typedef struct balance_node {
  val_t lefth, righth, localh;
  intptr_t removed;
  struct balance_node *left;
  struct balance_node *right;
  struct balance_node *parent;
  avl_node_t *anode;
} balance_node_t;
#endif

typedef struct free_list_item_t {
  struct free_list_item_t *next;
  avl_node_t *to_free;
} free_list_item;



typedef struct avl_intset {
  avl_node_t *root;
  //manager_t *managerPtr;
#ifdef SEPERATE_MAINTENANCE
  free_list_item **maint_list_start;
  free_list_item **maint_list_end;
#endif
  free_list_item **t_free_list;
  free_list_item *free_list;

#ifdef SEPERATE_BALANCE2DEL
  avl_node_t **to_remove;
  avl_node_t **to_remove_parent;
  avl_node_t **to_remove_seen;
#endif

  long nb_threads;
  ulong *t_nbtrans;
  ulong *t_nbtrans_old;
  ulong *nb_committed;
  ulong *nb_committed_old;
  ulong next_maintenance;
} avl_intset_t;



int floor_log_2(unsigned int n);

avl_node_t *avl_new_simple_node(val_t val, val_t key, int transactional);
#ifdef SEPERATE_BALANCE2
balance_node_t *avl_new_balance_node(avl_node_t *node, int transactional);
#endif
void avl_delete_node(avl_node_t *node);
void avl_delete_node_free(avl_node_t *node, int transactional);

avl_intset_t *avl_set_new();
avl_intset_t *avl_set_new_alloc(int transactional, long nb_threads);

void avl_set_delete(avl_intset_t *set);
void avl_set_delete_node(avl_node_t *node);

int avl_set_size(avl_intset_t *set);
int avl_tree_size(avl_intset_t *set);
void avl_set_size_node(avl_node_t *node, int* size, int tree);

























/* /\* */
/* ============================================================================= */
/*   * TMrbtree_insert */
/*   * -- Returns TRUE on success */
/*   * */
/* ============================================================================= */
/*   *\/ */
/* TM_CALLABLE */
/* bool_t */
/* TMrbtree_insert (TM_ARGDECL  rbtree_t* r, void* key, void* val); */

/* /\* */
/* ============================================================================= */
/*   * TMrbtree_delete */
/*   * */
/* ============================================================================= */
/*   *\/ */
/* TM_CALLABLE */
/* bool_t */
/* TMrbtree_delete (TM_ARGDECL  rbtree_t* r, void* key); */

/* /\* */
/* ============================================================================= */
/*   * TMrbtree_update */
/*   * -- Return FALSE if had to insert node first */
/*   * */
/* ============================================================================= */
/*   *\/ */
/* TM_CALLABLE */
/* bool_t */
/* TMrbtree_update (TM_ARGDECL  rbtree_t* r, void* key, void* val); */

/* /\* */
/* ============================================================================= */
/*   * TMrbtree_get */
/*   * */
/* ============================================================================= */
/*   *\/ */
/* TM_CALLABLE */
/* void* */
/* TMrbtree_get (TM_ARGDECL  rbtree_t* r, void* key); */


/* /\* */
/* ============================================================================= */
/*   * TMrbtree_contains */
/*   * */
/* ============================================================================= */
/*   *\/ */
/* TM_CALLABLE */
/* bool_t */
/* TMrbtree_contains (TM_ARGDECL  rbtree_t* r, void* key); */

/* //This is ok here? */
/* #ifndef max */
/* 	#define max( a, b ) ( ((a) > (b)) ? (a) : (b) ) */
/* #endif */

/* typedef struct avl_node { */
/*   val_t key, val, localh; */
/*   val_t lefth, righth; */
/*   intptr_t deleted; */
/*   intptr_t removed; */
/*   struct avl_node *left; */
/*   struct avl_node *right; */
/* } avl_node_t; */

/* typedef struct avl_intset { */
/*   avl_node_t *root; */
/* } avl_intset_t; */

/* typedef struct free_list_item_t { */
/*   struct free_list_item_t *next; */
/*   avl_node_t *to_free; */
/* } free_list_item; */

/* int floor_log_2(unsigned int n); */

/* avl_node_t *avl_new_simple_node(val_t val, val_t key, int transactional); */
/* void avl_delete_node(avl_node_t *n); */

/* avl_intset_t *avl_set_new(); */

/* void avl_set_delete(avl_intset_t *set); */
/* void avl_set_delete_node(avl_node_t *node); */

/* int avl_set_size(avl_intset_t *set); */
/* int avl_tree_size(avl_intset_t *set); */
/* void avl_set_size_node(avl_node_t *node, int* size, int tree); */

/* #ifdef TINY10B */
/*  int avl_contains(avl_intset_t *set, val_t val, int transactional); */
/*  int avl_add(avl_intset_t *set, val_t val, int transactional); */
/*  int avl_remove(avl_intset_t *set, val_t val, int transactional, free_list_item **free_list); */
/* #else */
/*  int avl_remove(avl_intset_t *set, val_t val, int transactional); */
/* #endif */

/* // Actual STM methods */

/*  int avl_search(val_t key, avl_intset_t *set); */
/*  int avl_find(val_t key, avl_node_t **place); */
/*  int avl_insert(val_t val, val_t key, avl_intset_t *set); */
/*  int avl_delete(val_t key, avl_intset_t *set, free_list_item **free_list); */
/*  int remove_node(avl_node_t *parent, avl_node_t *place); */
/*  int avl_rotate(avl_node_t *parent, short go_left, avl_node_t *node, free_list_item *free_list); */
/*  int avl_single_rotate(avl_node_t *parent, short go_left, avl_node_t *node, short left_rotate, short right_rotate, avl_node_t **child_addr, free_list_item *free_list); */
/*  int avl_right_rotate(avl_node_t *parent, short go_left, avl_node_t *node, val_t lefth, val_t righth, avl_node_t *left_child, avl_node_t **left_child_addr, short do_rotate, free_list_item *free_list); */
/*  int avl_left_rotate(avl_node_t *parent, short go_left, avl_node_t *node, val_t lefth, val_t righth, avl_node_t *right_child, avl_node_t **right_child_addr, short do_rotate, free_list_item *free_list); */
/*  //int avl_right_rotate(avl_node_t *parent, short go_left, avl_node_t *node, val_t lefth, val_t righth, avl_node_t *left_child); */
/*  //int avl_left_rotate(avl_node_t *parent, short go_left, avl_node_t *node, val_t lefth, val_t righth, avl_node_t *right_child); */
/*  int avl_propagate(avl_node_t *node, short left, short *should_rotate); */
/*  int recursive_tree_propagate(avl_intset_t *set, ulong *num, ulong *num_suc, ulong *num_rot, ulong *suc_rot, ulong *num_rem, free_list_item *free_list); */
/*  int recursive_node_propagate(avl_node_t *root, avl_node_t *node, avl_node_t *parent, ulong *num, ulong* num_suc, ulong *num_rot, ulong *suc_rot, ulong *num_rem, free_list_item *free_list); */

/* /\*============================================================================= */
/*   * TMrbtree_insert */
/*   * -- Returns TRUE on success */
/*   =============================================================================*\/ */
/* TM_CALLABLE */
/* bool_t */
/* TMrbtree_insert (TM_ARGDECL  rbtree_t* r, void* key, void* val); */

/* /\*============================================================================= */
/*   * TMrbtree_delete */
/*   =============================================================================*\/ */
/* TM_CALLABLE */
/* bool_t */
/* TMrbtree_delete (TM_ARGDECL  rbtree_t* r, void* key); */

/* /\*============================================================================= */
/*   * TMrbtree_update */
/*   * -- Return FALSE if had to insert node first */
/*   =============================================================================*\/ */
/* TM_CALLABLE */
/* bool_t */
/* TMrbtree_update (TM_ARGDECL  rbtree_t* r, void* key, void* val); */

/* /\*============================================================================= */
/*   * TMrbtree_get */
/*   =============================================================================*\/ */
/* TM_CALLABLE */
/* void* */
/* TMrbtree_get (TM_ARGDECL  rbtree_t* r, void* key); */

/* /\*============================================================================= */
/*   * TMrbtree_contains */
/*   =============================================================================*\/ */
/* TM_CALLABLE */
/* bool_t */
/* TMrbtree_contains (TM_ARGDECL  rbtree_t* r, void* key); */

/* //#endif /\* RBTREE_H *\/ */

/* //This is ok here? */
/* #ifndef max */
/* 	#define max( a, b ) ( ((a) > (b)) ? (a) : (b) ) */
/* #endif */
