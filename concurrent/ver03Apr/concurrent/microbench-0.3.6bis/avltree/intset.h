/*
 * File:
 *   intset.h
 * Author(s):
 *   Vincent Gramoli <vincent.gramoli@epfl.ch>
 * Description:
 *   Skip list integer set operations 
 *
 * Copyright (c) 2009-2010.
 *
 * intset.h is part of Microbench
 * 
 * Microbench is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, version 2
 * of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

//#include "fraser.h"
#include "avltree.h"

//temporary stuff
/* #define KEYMAP */
/* #define TM_CALLABLE */
/* #define TM_ARGDECL */
/* #define bool_t int */

/* typedef struct rbtree { */

/* } rbtree_t; */


//Wrappers?
int avl_contains(avl_intset_t *set, val_t key, int transactional);
int avl_add(avl_intset_t *set, val_t key, int transactional);
#ifdef TINY10B
int avl_remove(avl_intset_t *set, val_t key, int transactional, free_list_item **free_list);
#else
int avl_remove(avl_intset_t *set, val_t key, int transactional);
#endif


//seqential calls
int avl_req_seq_delete(avl_node_t *parent, avl_node_t *node, val_t key, int go_left, int *success);

inline int avl_req_seq_add(avl_node_t *parent, avl_node_t *node, val_t val, val_t key, int go_left, int *success);

int avl_seq_propogate(avl_node_t *parent, avl_node_t *node, int go_left);

int avl_seq_check_rotations(avl_node_t *parent, avl_node_t *node, int lefth, int righth, int go_left);

int avl_seq_right_rotate(avl_node_t *parent, avl_node_t *place, int left_child);

int avl_seq_left_rotate(avl_node_t *parent, avl_node_t *place, int go_left);

int avl_seq_left_right_rotate(avl_node_t *parent, avl_node_t *place, int go_left);

int avl_seq_right_left_rotate(avl_node_t *parent, avl_node_t *place, int go_left);

int avl_seq_req_find_successor(avl_node_t *parent, avl_node_t *node, int go_left, avl_node_t** succs);


#ifdef SEQUENTIAL

#ifdef KEYMAP

val_t avl_seq_get(avl_intset_t *set, val_t key, int transactional);

inline int avl_req_seq_update(avl_node_t *parent, avl_node_t *node, val_t val, val_t key, int go_left, int *success);


/* =============================================================================
 * rbtree_insert
 * -- Returns TRUE on success
 * =============================================================================
 */
bool_t
rbtree_insert (rbtree_t* r, void* key, void* val);

/* =============================================================================
 * rbtree_delete
 * =============================================================================
 */
bool_t
rbtree_delete (rbtree_t* r, void* key);



/* =============================================================================
 * rbtree_update
 * -- Return FALSE if had to insert node first
 * =============================================================================
 */
bool_t
rbtree_update (rbtree_t* r, void* key, void* val);


/* =============================================================================
 * rbtree_get
 * =============================================================================
 */
void*
rbtree_get (rbtree_t* r, void* key);



/* =============================================================================
 * rbtree_contains
 * =============================================================================
 */
bool_t
rbtree_contains (rbtree_t* r, void* key);


#endif

#endif



//Actual STM methods

#ifdef TINY10B
int avl_search(val_t key, avl_intset_t *set);

int avl_find(val_t key, avl_node_t **place);

int avl_insert(val_t val, val_t key, avl_intset_t *set);

int avl_delete(val_t key, avl_intset_t *set, free_list_item **free_list);

int remove_node(avl_node_t *parent, avl_node_t *place);

int avl_rotate(avl_node_t *parent, short go_left, avl_node_t *node, free_list_item *free_list);

int avl_single_rotate(avl_node_t *parent, short go_left, avl_node_t *node, short left_rotate, short right_rotate, avl_node_t **child_addr, free_list_item *free_list);


int avl_right_rotate(avl_node_t *parent, short go_left, avl_node_t *node, val_t lefth, val_t righth, avl_node_t *left_child, avl_node_t **left_child_addr, short do_rotate, free_list_item *free_list);

int avl_left_rotate(avl_node_t *parent, short go_left, avl_node_t *node, val_t lefth, val_t righth, avl_node_t *right_child, avl_node_t **right_child_addr, short do_rotate, free_list_item *free_list);

//int avl_right_rotate(avl_node_t *parent, short go_left, avl_node_t *node, val_t lefth, val_t righth, avl_node_t *left_child);

//int avl_left_rotate(avl_node_t *parent, short go_left, avl_node_t *node, val_t lefth, val_t righth, avl_node_t *right_child);

int avl_propagate(avl_node_t *node, short left, short *should_rotate);

int recursive_tree_propagate(avl_intset_t *set, ulong *num, ulong *num_suc, ulong *num_rot, ulong *suc_rot, ulong *num_rem, free_list_item *free_list);

int recursive_node_propagate(avl_node_t *root, avl_node_t *node, avl_node_t *parent, ulong *num, ulong* num_suc, ulong *num_rot, ulong *suc_rot, ulong *num_rem, free_list_item *free_list);



#ifdef KEYMAP

int avl_update(val_t v, val_t key, avl_intset_t *set);

val_t avl_get(val_t key, avl_intset_t *set);


/*
=============================================================================
  * TMrbtree_insert
  * -- Returns TRUE on success
  *
=============================================================================
  */
TM_CALLABLE
bool_t
TMrbtree_insert (TM_ARGDECL  rbtree_t* r, void* key, void* val);

/*
=============================================================================
  * TMrbtree_delete
  *
=============================================================================
  */
TM_CALLABLE
bool_t
TMrbtree_delete (TM_ARGDECL  rbtree_t* r, void* key);

/*
=============================================================================
  * TMrbtree_update
  * -- Return FALSE if had to insert node first
  *
=============================================================================
  */
TM_CALLABLE
bool_t
TMrbtree_update (TM_ARGDECL  rbtree_t* r, void* key, void* val);

/*
=============================================================================
  * TMrbtree_get
  *
=============================================================================
  */
TM_CALLABLE
void*
TMrbtree_get (TM_ARGDECL  rbtree_t* r, void* key);


/*
=============================================================================
  * TMrbtree_contains
  *
=============================================================================
  */
TM_CALLABLE
bool_t
TMrbtree_contains (TM_ARGDECL  rbtree_t* r, void* key);

#endif


#endif

//This is ok here?
#ifndef max
	#define max( a, b ) ( ((a) > (b)) ? (a) : (b) )
#endif
