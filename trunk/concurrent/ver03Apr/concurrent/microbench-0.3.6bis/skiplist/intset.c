/*
 * File:
 *   intset.c
 * Author(s):
 *   Vincent Gramoli <vincent.gramoli@epfl.ch>
 * Description:
 *   Skip list integer set operations 
 *
 * Copyright (c) 2009-2010.
 *
 * intset.c is part of Microbench
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

#include "intset.h"

#define MAXLEVEL    32
#define CONST_K     1
//#define EXTRA_SLOW
//#define LINKED_LIST

int sl_contains(sl_intset_t *set, val_t val, int transactional)
{
	int result = 0;
	
#ifdef SEQUENTIAL /* Unprotected */
	
	int i;
	sl_node_t *node, *next;
	
	node = set->head;
	for (i = node->toplevel-1; i >= 0; i--) {
		next = node->next[i];
		while (next->val < val) {
			node = next;
			next = node->next[i];
		}
	}
	node = node->next[0];
	result = (node->val == val);
		
#elif defined STM

#ifdef TINY10B
	//TX_START(EL);
	result = sl_search(val, set);
	//TX_END;
#else
	
	int i;
	sl_node_t *node, *next;
	val_t v = VAL_MIN;
	
	TX_START(EL);
	node = set->head;
	for (i = node->toplevel-1; i >= 0; i--) {
		next = (sl_node_t *)TX_LOAD(&node->next[i]);
		while ((v = TX_LOAD(&next->val)) < val) {
			node = next;
			next = (sl_node_t *)TX_LOAD(&node->next[i]);
		}
	}
	node = (sl_node_t *)TX_LOAD(&node->next[0]);
	result = (v == val);

	TX_END;



	TX_START(EL);
	TX_END;


#endif
	
#elif defined LOCKFREE /* fraser lock-free */
	
	result = fraser_find(set, val);
	
#endif
	
	return result;
}

inline int sl_seq_add(sl_intset_t *set, val_t val) {
	int i, l, result;
	sl_node_t *node, *next;
	sl_node_t *preds[MAXLEVEL], *succs[MAXLEVEL];
	
	node = set->head;
	for (i = node->toplevel-1; i >= 0; i--) {
		next = node->next[i];
		while (next->val < val) {
			node = next;
			next = node->next[i];
		}
		preds[i] = node;
		succs[i] = node->next[i];
	}
	node = node->next[0];
	if ((result = (node->val != val)) == 1) {
	        l = get_rand_level();
	        //l = 1;
		node = sl_new_simple_node(val, l, 0);
		for (i = 0; i < l; i++) {
			node->next[i] = succs[i];
			preds[i]->next[i] = node;
#ifdef TINY10B
			node->val_arr[i] = val;
#endif
		}
	}
	return result;
}

int sl_add(sl_intset_t *set, val_t val, int transactional)
{
  int result = 0;

  if (!transactional) {
    //printf("Adding %d\n", val);	
		
    result = sl_seq_add(set, val);
	
  } else {

#ifdef SEQUENTIAL
		
	result = sl_seq_add(set, val);
		
#elif defined STM

#ifdef TINY10B
	//TX_START(EL);
	result = sl_insert(val, set, transactional);
	//TX_END;
#else
	
	int i, l;
	sl_node_t *node, *next;
	sl_node_t *preds[MAXLEVEL], *succs[MAXLEVEL];
	val_t v;  
	
	TX_START(EL);
	v = VAL_MIN;
	node = set->head;
	for (i = node->toplevel-1; i >= 0; i--) {
		next = (sl_node_t *)TX_LOAD(&node->next[i]);
		while ((v = TX_LOAD(&next->val)) < val) {
			node = next;
			next = (sl_node_t *)TX_LOAD(&node->next[i]);
		}
		preds[i] = node;
	}
	if ((result = (v != val)) == 1) {
		l = get_rand_level();
		node = sl_new_simple_node(val, l, transactional);
		for (i = 0; i < l; i++) {
			node->next[i] = (sl_node_t *)TX_LOAD(&preds[i]->next[i]);	
			TX_STORE(&preds[i]->next[i], node);
		}
	}
	TX_END;


	TX_START(EL);
	TX_END;

#endif
	
#elif defined LOCKFREE /* fraser lock-free */
	
	result = fraser_insert(set, val);

#endif
		
  }
	
  return result;
}

int sl_remove(sl_intset_t *set, val_t val, int transactional)
{
	int result = 0;
	
#ifdef SEQUENTIAL
	
	int i;
	sl_node_t *node, *next = NULL;
	sl_node_t *preds[MAXLEVEL], *succs[MAXLEVEL];
	
	node = set->head;
	for (i = node->toplevel-1; i >= 0; i--) {
		next = node->next[i];
		while (next->val < val) {
			node = next;
			next = node->next[i];
		}
		preds[i] = node;
		succs[i] = node->next[i];
	}
	if ((result = (next->val == val)) == 1) {
		for (i = 0; i < set->head->toplevel; i++) 
			if (succs[i]->val == val)
				preds[i]->next[i] = succs[i]->next[i];
		sl_delete_node(next); 
	}

#elif defined STM

#ifdef TINY10B
	//TX_START(EL);
	result = sl_delete(val, set);
	//TX_END;
#else
	
	int i;
	sl_node_t *node, *next = NULL;
	sl_node_t *preds[MAXLEVEL], *succs[MAXLEVEL];
	val_t v;  
	
	TX_START(EL);
	v = VAL_MIN;
	node = set->head;
	for (i = node->toplevel-1; i >= 0; i--) {
		next = (sl_node_t *)TX_LOAD(&node->next[i]);
		while ((v = TX_LOAD(&next->val)) < val) {
			node = next;
			next = (sl_node_t *)TX_LOAD(&node->next[i]);
		}
		preds[i] = node;
		succs[i] = next;
	}
	if ((result = (next->val == val))) {
	  for (i = 0; i < set->head->toplevel; i++) {
	    if (succs[i]->val == val) {
	      TX_STORE(&preds[i]->next[i], (sl_node_t *)TX_LOAD(&succs[i]->next[i])); 
	    }
	  }
	  FREE(next, sizeof(sl_node_t) + next->toplevel * sizeof(sl_node_t *));
	}

#ifdef EXTRA_SLOW
	int a;

	TX_STORE(&a, 10);
#endif


	TX_END;


	TX_START(EL);
	TX_END;


#endif
	
#elif defined LOCKFREE
	
	result = fraser_remove(set, val);

#endif
	
	return result;
}

#ifdef TINY10B

/* int sl_search(val_t val, sl_intset_t *set) { */
/*   val_t v; */
/*   sl_node_t *preds[MAXLEVEL], *succs[MAXLEVEL]; */

/*   TX_START(NL); */
/*   sl_find(val, set, preds, succs); */
/*   v = TX_UNIT_LOAD(&succs[0]->val_arr[0]); */
/*   TX_END; */
/*   return (v == val); */
/* } */

int sl_search(val_t val, sl_intset_t *set) {
  int i;
  sl_node_t *node, *next = NULL;
  //sl_node_t *preds[MAXLEVEL], *succs[MAXLEVEL];
  val_t v, v1;
  int found;

  node = set->head;
  TX_START(NL);
  found = 0;
  for (i = node->toplevel-1; i >= 0; i--) {
    next = (sl_node_t *)TX_UNIT_LOAD(&node->next[i]);
    //while ((v = TX_UNIT_LOAD(&next->val_arr[i])) < val) {
    while ((v = TX_UNIT_LOAD(&next->val_arr[0])) < val) {
      //MIGHT WANT TO UNCOMMENT THE LINE BELOW???
      //v = TX_UNIT_LOAD(&next->val_arr[0]);
      node = next;
      next = (sl_node_t *)TX_UNIT_LOAD(&node->next[i]);
    }
    if(v == val) {
      found = 1;
      break;
    }

    //preds[i] = node;
    //succs[i] = next;
  }
  TX_END;

  return found;
}



int sl_find(val_t val, sl_intset_t *set, sl_node_t *preds[], sl_node_t *succs[]) {
  int i;
  sl_node_t *node, *next = NULL;
  //sl_node_t *preds[MAXLEVEL], *succs[MAXLEVEL];
  val_t v;

  node = set->head;
  for (i = node->toplevel-1; i >= 0; i--) {
    next = (sl_node_t *)TX_UNIT_LOAD(&node->next[i]);
    //while ((v = TX_UNIT_LOAD(&next->val_arr[i])) < val) {
    while ((v = TX_UNIT_LOAD(&next->val)) < val) {
      node = next;
      next = (sl_node_t *)TX_UNIT_LOAD(&node->next[i]);
    }
    preds[i] = node;
    succs[i] = next;
  }

  return 0;
}

int sl_find_spot(val_t val, int l, sl_node_t *preds[], sl_node_t *succs[]) {
  sl_node_t *node = NULL, *next = NULL, *temp = NULL;
  //sl_node_t *preds[MAXLEVEL], *succs[MAXLEVEL];
  val_t v, v1;

  next = preds[l];
  while ((v = TX_LOAD(&next->val)) < val || (v1 = TX_LOAD(&next->val_arr[l])) == (val_t)-1) {
  //while (next->val_arr[l] < val) {
    //printf("preds val[l]:%d\n", v);
    node = next;
    next = (sl_node_t *)TX_LOAD(&node->next[l]);
  }
  //temp = (sl_node_t *)TX_LOAD(&next->next[l]);
  //if(temp != NULL) {
  //TX_LOAD(&temp->val_arr[l]);
  //}
  preds[l] = node;
  succs[l] = next;
  return 0;
}


int sl_find_spot_address(sl_node_t *find, val_t val, int l, sl_node_t *preds[], sl_node_t *succs[]) {
  sl_node_t *node = NULL, *next = NULL;
  //sl_node_t *preds[MAXLEVEL], *succs[MAXLEVEL];
  val_t v;

  next = preds[l];
  //Should do transactional lod here? prolly not (no writes)
  while (next != find && (v = TX_LOAD(&next->val_arr[l])) <= val) {
    node = next;
    next = (sl_node_t *)TX_LOAD(&node->next[l]);
  }
  preds[l] = node;
  succs[l] = next;

  return 0;
}

int sl_insert(val_t val, sl_intset_t *set, int transactional) {
  sl_node_t *node;
  val_t v;
  sl_node_t *preds[MAXLEVEL], *succs[MAXLEVEL];
  int l, exec;

  //printf("Inserting %d\n", val);

   TX_START(NL);
   node = NULL;
   l = 0;
   sl_find(val, set, preds, succs);
   TX_END;

   TX_START(NL);
   sl_find_spot(val, 0, preds, succs);

   /* if((v1 = TX_LOAD(&succs[0]->val)) == val && (v = TX_LOAD(&succs[0]->val_arr[0]) == -2)) { */
   /*   TX_STORE(&succs->val, val); */
   /*   TX_STORE(&succs->val_arr[0], val); */
   /* } */
   exec = 0;
   if((v = TX_LOAD(&succs[0]->val_arr[0])) == -2) {
     TX_STORE(&succs[0]->val, val);
     TX_STORE(&succs[0]->val_arr[0], val);
     v = -1;
     exec = 1;
   }
   else if(v != val) {
     l = get_rand_level();
     //l = 2;
     node = sl_new_simple_node(val, l, transactional);
     sl_single_insert(0, node, val, preds, succs);
     //printf("Inserted %d\n", val);
   }
   TX_END;
#ifndef LINKED_LIST
   if(exec == 0) {
     sl_continue_insert(val, l, node, preds, succs);
   }
#endif
   //TX_END;
  
  return (v != val);
}

int sl_continue_insert(val_t val, int l, sl_node_t *new, sl_node_t *preds[], sl_node_t *succs[]) {
  int j;
  val_t v = 0;
  if(new == NULL) {
    return 0;
  }

  //TX_START(NL);
  j = 1;
  while(j < l && v != -1) {
    TX_START(NL);
    v = TX_LOAD(&new->val_arr[0]);
    if(v != -1) {
      sl_find_spot(val, j, preds, succs);
      sl_single_insert(j, new, val, preds, succs);
    }
    TX_END;
    j++;
  }
  //TX_END;

  return 0;
}

int sl_single_insert(int l, sl_node_t *new, val_t val, sl_node_t *preds[], sl_node_t *succs[]) {
  //l = 0;
  TX_STORE(&new->val_arr[l], val);
  TX_STORE(&new->next[l], succs[l]);
  TX_STORE(&preds[l]->next[l], new);
  //preds[l]->next[l] = new;
  //new->val_arr[l] = val;

  return 0;
}

int sl_delete(val_t val, sl_intset_t *set) {
  int deleted;
  sl_node_t *preds[MAXLEVEL], *succs[MAXLEVEL], *node;

  //printf("Deleting %d\n", val);

  TX_START(NL);
  deleted = 0;
  sl_find(val, set, preds, succs);
  TX_END;

  TX_START(NL);
  sl_find_spot(val, 0, preds, succs);
  deleted = sl_single_delete(0, val, preds, succs);
  node = succs[0];
  if(deleted) {
    //FREE(node, sizeof(sl_node_t) + node->toplevel * sizeof(sl_node_t *) + 32*sizeof(val_t));
  }
  TX_END;
  if(deleted == 1) {
    //Should the read of the level be trnasactional? dont think so(never written?)
#ifndef LINKED_LIST
    sl_continue_delete(val, succs[0]->toplevel, preds, succs);
#endif
    //printf("Deleted %d\n", val);
    //FREE(node, sizeof(sl_node_t) + node->toplevel * sizeof(sl_node_t *) + 32*sizeof(val_t));
  }
  //TX_END;
  return deleted;
}

int sl_continue_delete(val_t val, int l, sl_node_t *preds[], sl_node_t *succs[]) {
  int j;
  for(j = 1; j < l; j++) {
    TX_START(NL);
    sl_find_spot_address(succs[0], val, j, preds, succs);
    sl_single_delete(j, val, preds, succs);
    TX_END;
  }
  return 0;
}

int sl_single_delete(int l, val_t val, sl_node_t *preds[], sl_node_t *succs[]) {
  val_t v;

  if((v = TX_LOAD(&succs[l]->val_arr[l])) == val) {
    //fix this    
    if(1) {
      TX_STORE(&succs[l]->val_arr[l], -2);
      return 2;
    }
    TX_STORE(&preds[l]->next[l], (sl_node_t *)TX_LOAD(&succs[l]->next[l]));
    TX_STORE(&succs[l]->val_arr[l], -1);
    TX_STORE(&succs[l]->next[l], preds[l]);
    return 1;
  }
  return 0;
}

#endif
