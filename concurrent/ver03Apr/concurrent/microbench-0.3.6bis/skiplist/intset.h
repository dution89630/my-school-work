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

#include "fraser.h"

int sl_contains(sl_intset_t *set, val_t val, int transactional);
int sl_add(sl_intset_t *set, val_t val, int transactional);
int sl_remove(sl_intset_t *set, val_t val, int transactional);

#ifdef TINY10B
int sl_search(val_t val, sl_intset_t *set);

int sl_find(val_t val, sl_intset_t *set, sl_node_t *preds[], sl_node_t *succs[]);
int sl_find_spot(val_t val, int l, sl_node_t *preds[], sl_node_t *succs[]);
int sl_find_spot_address(sl_node_t *find, val_t val, int l, sl_node_t *preds[], sl_node_t *succs[]);

int sl_insert(val_t val, sl_intset_t *set, int transactional);

int sl_continue_insert(val_t val, int l, sl_node_t *new, sl_node_t *preds[], sl_node_t *succs[]);
int sl_single_insert(int l, sl_node_t *new, val_t val, sl_node_t *preds[], sl_node_t *succs[]);

int sl_delete(val_t val, sl_intset_t *set);

int sl_continue_delete(val_t val, int l, sl_node_t *preds[], sl_node_t *succs[]);
int sl_single_delete(int l, val_t val, sl_node_t *preds[], sl_node_t *succs[]);
#endif
