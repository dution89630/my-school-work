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

#include <unistd.h>
//#include "fraser.h"
#include "newavltree.h"
//#include "thread.h"


#ifdef SEPERATE_MAINTENANCE
#define THROTTLE_TIME 100
#else
#define THROTTLE_NUM  1000
#define THROTTLE_TIME 10000
#define THROTTLE_UPDATE 1000
#define THROTTLE_MAINTENANCE
#endif

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
#if defined(MICROBENCH)
int avl_remove(avl_intset_t *set, val_t key, int transactional, int id);
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

#endif

#endif



//Actual STM methods

#ifdef TINY10B
int avl_search(val_t key, avl_intset_t *set);

int avl_find(val_t key, avl_node_t **place, val_t *k);

int avl_find_parent(val_t key, avl_node_t **place, avl_node_t **parent, val_t *k);

int avl_insert(val_t val, val_t key, avl_intset_t *set);

//int avl_delete(val_t key, avl_intset_t *set, free_list_item **free_list);
#if defined(MICROBENCH)
int avl_delete(val_t key, avl_intset_t *set, int id);
#else
int avl_delete(val_t key, avl_intset_t *set);
#endif

int remove_node(avl_node_t *parent, avl_node_t *place);

int avl_rotate(avl_node_t *parent, short go_left, avl_node_t *node, free_list_item *free_list);

int avl_single_rotate(avl_node_t *parent, short go_left, avl_node_t *node, short left_rotate, short right_rotate, avl_node_t **child_addr, free_list_item *free_list);


int avl_right_rotate(avl_node_t *parent, short go_left, avl_node_t *node, val_t lefth, val_t righth, avl_node_t *left_child, avl_node_t **left_child_addr, short do_rotate, free_list_item *free_list);

int avl_left_rotate(avl_node_t *parent, short go_left, avl_node_t *node, val_t lefth, val_t righth, avl_node_t *right_child, avl_node_t **right_child_addr, short do_rotate, free_list_item *free_list);

//int avl_right_rotate(avl_node_t *parent, short go_left, avl_node_t *node, val_t lefth, val_t righth, avl_node_t *left_child);

//int avl_left_rotate(avl_node_t *parent, short go_left, avl_node_t *node, val_t lefth, val_t righth, avl_node_t *right_child);


int recursive_tree_propagate(avl_intset_t *set, ulong *num, ulong *num_suc, ulong *num_rot, ulong *suc_rot, ulong *num_rem, free_list_item *free_list);


#ifdef SEPERATE_BALANCE2

#ifdef SEPERATE_BALANCE2DEL
int check_remove_list(avl_intset_t *set, ulong *num_rem, free_list_item *free_list);
#endif

int recursive_node_propagate(avl_intset_t *set, balance_node_t *root, balance_node_t *node, balance_node_t *parent, ulong *num, ulong* num_suc, ulong *num_rot, ulong *suc_rot, ulong *num_rem, free_list_item *free_list);

int avl_propagate(balance_node_t *node, short left, short *should_rotate);

balance_node_t* check_expand(balance_node_t *node, short go_left);

#else

int recursive_node_propagate(avl_node_t *root, avl_node_t *node, avl_node_t *parent, ulong *num, ulong* num_suc, ulong *num_rot, ulong *suc_rot, ulong *num_rem, free_list_item *free_list);

int avl_propagate(avl_node_t *node, short left, short *should_rotate);

#endif

#ifdef KEYMAP
val_t avl_get(val_t key, avl_intset_t *set);
int avl_update(val_t v, val_t key, avl_intset_t *set);
#endif

#ifdef SEPERATE_MAINTENANCE
void do_maintenance_thread(avl_intset_t *tree);
#else
void check_maintenance(avl_intset_t *tree);
void do_maintenance(avl_intset_t *tree);
#endif


#endif

//This is ok here?
#ifndef max
	#define max( a, b ) ( ((a) > (b)) ? (a) : (b) )
#endif
