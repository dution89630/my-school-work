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

#include <unistd.h>
#include "intset.h"

#define CHECK_FIRST
#define TTIME 500

int avl_contains(avl_intset_t *set, val_t key, int transactional)
{
  int result = 0;
	
#ifdef SEQUENTIAL /* Unprotected */	
  val_t val;
  avl_node_t *next;

  next = set->root;
  
  while(next != NULL) {
    val = next->key;
    
    if(key == val) {
      if(next->deleted) {
	//next = next->left;
	return 0;
      } else {
	return 1;
      }
    } else if(val < key) {
      next = next->right;
    } else {
      next = next->left;
    }
    
  }
  return 0;
  
  
  
#elif defined TINY10B

  result = avl_search(key, set);
#endif
	
  
  return result;
}

inline int avl_seq_add(avl_intset_t *set, val_t key) {
  avl_node_t *new_node, *next, *prev = NULL;
  val_t val = 0;
  
  next = set->root;
  
  while(next != NULL) {
    prev = next;
    val = prev->key;
    
    if(key == val) {
      if(!prev->deleted) {
	return 0;
      } else {
	prev->deleted = 0;
	return 1;
      }
    } else if(key > val) {
      next = prev->right;
    } else {
      next = prev->left;
    }
    
  }
  
  //insert the node
  new_node = avl_new_simple_node(key, key, 0);
  if(key > val) {
    prev->right = new_node;
  } else {
    prev->left = new_node;
  }
  
  return 1;
  
}

int avl_add(avl_intset_t *set, val_t key, int transactional)
{
  int result = 0;

  if (!transactional) {
		
    //result = avl_seq_add(set, key);
    avl_req_seq_add(NULL, set->root, key, key, 0, &result);

	
  } else {

#ifdef SEQUENTIAL
		
    //result = avl_seq_add(set, v);
    avl_req_seq_add(NULL, set->root, key, key, 0, &result);
		
#elif defined TINY10B

    result = avl_insert(key, key, set);

#endif
		
  }
	
  return result;
}


#ifdef TINY10B
int avl_remove(avl_intset_t *set, val_t key, int transactional, free_list_item **free_list)
#else
int avl_remove(avl_intset_t *set, val_t key, int transactional)
#endif
{
  int result = 0;
	
#ifdef SEQUENTIAL
	
  avl_req_seq_delete(NULL, set->root, key, 0, &result);

  /* avl_node_t *new_node, *next, *prev, *parent, *child; */
  /* short parent_left_child, left_child; */
  /* val_t val; */
  
  /* next = set->root; */
  
  /* while(next != NULL) { */
  /*   parent = prev; */
  /*   prev = next; */
  /*   val = prev->key; */
    
  /*   if(v == val) { */
  /*     if(!prev->deleted) { */
  /* 	prev->deleted = 1; */
  /* 	prev->removed = 1; */
  /* 	if(parent != NULL) { */
  /* 	  if(prev->right == NULL || prev->left == NULL) { */
  /* 	    if((child = prev->right) == NULL) { */
  /* 	      child = prev->left; */
  /* 	    } */
  /* 	    //remove the node if it has at least one null child */
  /* 	    if(parent->right == prev) { */
  /* 	      parent->right = child; */
  /* 	    } else { */
  /* 	      parent->left = child; */
  /* 	    } */
  /* 	  } */
  /* 	} */
  /* 	return 1; */
  /*     } */
      
  /*   } else if(v > val) { */
  /*     next = prev->right; */
  /*   } else { */
  /*     next = prev->left; */
  /*   } */
    
  /* } */

  /* return 0; */

#elif defined TINY10B

  result = avl_delete(key, set, free_list);

#endif
	
  return result;
}



int avl_req_seq_delete(avl_node_t *parent, avl_node_t *node, val_t key, int go_left, int *success) {
  avl_node_t *succs;
  int ret;

  if(node != NULL) {
    if(key == node->key) {
      if(node->deleted) {
	*success = 0;
	return 0;
      } else {
	//remove and find successor
	if(node->left == NULL) {
	  if(go_left) {
	    parent->left = node->right;
	  } else {
	    parent->right = node->right;
	  }
	  free(node);
	  *success = 1;
	  return 1;
	} else if(node->right == NULL) {
	  if(go_left) {
	    parent->left = node->left;
	  } else {
	    parent->right = node->left;
	  }
	  free(node);
	  *success = 1;
	  return 1;
	} else {
	  ret = avl_seq_req_find_successor(node, node->right, 0, &succs);
	  succs->right = node->right;
	  //succs->righth = node->localh;
	  succs->left = node->left;
	  //succs->lefth = node->localh;
	  if(go_left) {
	    parent->left = succs;
	  } else {
	    parent->right = succs;
	  }
	  free(node);
	  *success = 1;
	  return avl_seq_propogate(parent, succs, go_left);
	}
      }
    } else if(key > node->key) {
      ret = avl_req_seq_delete(node, node->right, key, 0, success);
    } else {
      ret = avl_req_seq_delete(node, node->left, key, 1, success);
    }
    
    if(ret != 0) {
      return avl_seq_propogate(parent, node, go_left);
    }
    return 0;

  } else {
    //did not find the node
    *success = -1;
    return 0;
  }
  
  return 0;

}


inline int avl_req_seq_add(avl_node_t *parent, avl_node_t *node, val_t val, val_t key, int go_left, int *success) {
  avl_node_t *new_node;
  int ret;
  
  if(node != NULL) {    
    if(key == node->key) {
      if(!node->deleted) {
	//found the node
	*success = -1;
	return 0;
      } else {
	node->deleted = 0;
	node->val = val;
	*success = 1;
	return 0;
      }
    } else if(key > node->key) {
      ret = avl_req_seq_add(node, node->right, val, key, 0, success);
    } else {
      ret = avl_req_seq_add(node, node->left, val, key, 1, success);
    }
    
    if(ret != 0) {
      return avl_seq_propogate(parent, node, go_left);
    }
    return 0;

  } else {
    //insert the node
    new_node = avl_new_simple_node(val, key, 0);
    if(key > parent->key) {
      parent->right = new_node;
    } else {
      parent->left = new_node;
    }
    *success = 1;
  }
  
  return 1;
  
}



int avl_seq_propogate(avl_node_t *parent, avl_node_t *node, int go_left) {
  int new_localh;

  if(node->left != NULL) {
    node->lefth = node->left->localh;
  } else {
    node->lefth = 0;
  }
  if(node->right != NULL) {
    node->righth = node->right->localh;
  } else {
    node->righth = 0;
  }

  new_localh = 1 + max(node->lefth, node->righth);
  if(new_localh != node->localh) {
    node->localh = new_localh;

    //need to do rotations
    if(parent != NULL) {
      avl_seq_check_rotations(parent, node, node->lefth, node->righth, go_left);
    }
    return 1;
  }
  return 0;
}

int avl_seq_check_rotations(avl_node_t *parent, avl_node_t *node, int lefth, int righth, int go_left) {
  int local_bal, lbal, rbal;

  local_bal = lefth - righth;

  if(local_bal > 1) {
    lbal = node->left->lefth - node->left->righth;
    if(lbal >= 0) {
      avl_seq_right_rotate(parent, node, go_left);
    } else {
      avl_seq_left_right_rotate(parent, node, go_left);
    }

  } else if(local_bal < -1) {
    rbal = node->right->lefth - node->right->righth;
    if(rbal < 0) {
      avl_seq_left_rotate(parent, node, go_left);
    } else {
      avl_seq_right_left_rotate(parent, node, go_left);
    }
  }

  return 1;
}


int avl_seq_right_rotate(avl_node_t *parent, avl_node_t *place, int go_left) {
  avl_node_t *u, *v;

  v = place;
  u = place->left;
  if(go_left) {
    parent->left = u;
  } else {
    parent->right = u;
  }

  v->left = u->right;
  u->right = v;

  //now need to do propogations
  u->localh = v->localh - 1;
  v->lefth = u->righth;
  v->localh = 1 + max(v->lefth, v->righth);
  u->righth = v->localh;
  
  return 1;
}


int avl_seq_left_rotate(avl_node_t *parent, avl_node_t *place, int go_left) {
  avl_node_t *u, *v;

  v = place;
  u = place->right;
  if(go_left) {
    parent->left = u;
  } else {
    parent->right = u;
  }

  v->right = u->left;
  u->left = v;

  //now need to do propogations
  u->localh = v->localh - 1;
  v->righth = u->lefth;
  v->localh = 1 + max(v->lefth, v->righth);
  u->lefth = v->localh;
  
  return 1;
}

int avl_seq_left_right_rotate(avl_node_t *parent, avl_node_t *place, int go_left) {
  
  avl_seq_left_rotate(place, place->left, 1);
  avl_seq_right_rotate(parent, place, go_left);
  return 1;
}

int avl_seq_right_left_rotate(avl_node_t *parent, avl_node_t *place, int go_left) {
  
  avl_seq_right_rotate(place, place->right, 0);
  avl_seq_left_rotate(parent, place, go_left);
  return 1;
}

int avl_seq_req_find_successor(avl_node_t *parent, avl_node_t *node, int go_left, avl_node_t** succs) {
  int ret;

  //smallest node in right sub-tree
  if(node->left == NULL) {
    //this is the succs
    if(go_left) {
      parent->left = node->right;
      parent->lefth = node->localh - 1;
    } else {
      parent->right = node->right;
      parent->righth = node->localh - 1;
    }
    *succs = node;
    return 1;
  }
  ret = avl_seq_req_find_successor(node, node->left, 1, succs);
  if(ret == 1) {
    return avl_seq_propogate(parent, node, go_left);
  }
  return ret;
}





#ifdef TINY10B

int avl_search(val_t key, avl_intset_t *set) {
  short done;
  intptr_t rem, del;
  avl_node_t *place;
  
  place = set->root;
  TX_START(NL);
  rem = 1;
  done = 0;
  
  while(rem) {
    avl_find(key, &place);
    
    if(place->key != key) {
      done = 1;
      break;
    }
    rem = (intptr_t)TX_LOAD(&place->removed);
  }
  if(!done) {
    del = (intptr_t)TX_LOAD(&place->deleted);
  }

  TX_END;

  if(done) {
    return 0;
  } else if(del) {
    return 0;
  }
  return 1;

}



int avl_find(val_t key, avl_node_t **place) {
  avl_node_t *next;
  intptr_t rem;
  val_t val;

  next = *place;
  rem = 1;
  while(next != NULL && (val != key || rem)) {
    *place = next;
    rem = (intptr_t)TX_UNIT_LOAD(&(*place)->removed);
    val = (*place)->key;
    if(key > val) {
      next = (avl_node_t*)TX_UNIT_LOAD(&(*place)->right);
    } else {
      next = (avl_node_t*)TX_UNIT_LOAD(&(*place)->left);
    }
  }
  return 0;
}


int avl_find_parent(val_t key, avl_node_t **place, avl_node_t **parent) {
  avl_node_t *next;
  intptr_t rem;
  val_t val;

  next = *place;
  *place = *parent;
  rem = 1;
  while(next != NULL && (val != key || rem)) {
    *parent = *place;
    *place = next;
    rem = (intptr_t)TX_UNIT_LOAD(&(*place)->removed);
    val = (*place)->key;
    if(key > val) {
      next = (avl_node_t*)TX_UNIT_LOAD(&(*place)->right);
    } else {
      next = (avl_node_t*)TX_UNIT_LOAD(&(*place)->left);
    }
  }
  return 0;
}



int avl_insert(val_t v, val_t key, avl_intset_t *set) {
  avl_node_t *place, *next, *new_node;
  intptr_t rem, del;
  short done, go_left = 0;
  int ret;

  place = set->root;
  next = place;
  TX_START(NL);
  ret = 0;
  rem = 1;
  done = 0;
  
  while(rem || next != NULL) {
    avl_find(key, &place);
    rem = (intptr_t)TX_LOAD(&place->removed);
    if(!rem) {
      if(place->key == key) {
	done = 1;
	break;
      } else if(key > place->key){
	next = (avl_node_t*)TX_LOAD(&place->right);
	go_left = 0;
      } else {
	next = (avl_node_t*)TX_LOAD(&place->left);
	go_left = 1;
      }
    }
  }

  if(done) {
    if((del = (intptr_t)TX_LOAD(&place->deleted))) {
      TX_STORE(&place->deleted, 0);
#ifdef MAPKEY
      TX_STORE(&place->val, v);
#endif
      ret = 1;
    } else {
      ret = 0;
    }
  } else {
    new_node = avl_new_simple_node(v, key, 1);
    if(go_left) {
      TX_STORE(&place->left, new_node);
    } else {
      TX_STORE(&place->right, new_node);
    }
    ret = 2;
  }

  TX_END;
  return ret;

}


int avl_delete(val_t key, avl_intset_t *set, free_list_item **free_list) {
  avl_node_t *place, *parent;
  intptr_t rem, del;
  short done;
  int ret;
  free_list_item *free_item;

  place = set->root;
  parent = set->root;
  TX_START(NL);
  ret  = 0;
  done = 0;
  rem = 1;

  while(rem) {
    avl_find_parent(key, &place, &parent);
    //avl_find(v, &place);
    if(place->key != key) {
      done = 1;
      ret = 0;
      break;
    }
    rem = (intptr_t)TX_LOAD(&place->removed);
  }
  /* if(place != NULL) { */
  /*   printf("found %d in delete for %d\n", place->key, v); */
  /* } else { */
  /*   printf("didnt find %d in delete\n", v); */
  /* } */
  if(!done) {
    if((del = (intptr_t)TX_LOAD(&place->deleted))) {
      ret = 0;
    } else {
      TX_STORE(&place->deleted, 1);
      ret = 1;
    }
  }
  TX_END;

  //Do the removal in a new trans(or maybe should do this in maintenance?)
  if(ret == 1) {
    ret = remove_node(parent, place);
    if(ret > 1) {
      if(free_list != NULL) {
	//add it to the garbage collection
	free_item = (free_list_item *)malloc(sizeof(free_list_item));
	free_item->next = NULL;
	free_item->to_free = place;
	(*free_list)->next = free_item;
	//do this in a trans (atomic)?
	*free_list = free_item;
	//TX_START(NL);
	//TX_STORE(&(*free_list), free_item);
	//TX_END;
      }
    }
  }
  return ret;

}

int remove_node(avl_node_t *parent, avl_node_t *place) {
  avl_node_t *right_child, *left_child, *parent_child;
  short go_left;
  val_t v;
  int ret;
  intptr_t rem, del;
  val_t lefth = 0, righth = 0, new_localh;
#ifdef CHECK_FIRST
  val_t parent_localh;
#endif

  v = place->key;
  TX_START(NL);
  ret = 1;
  right_child = NULL;
  left_child = NULL;
  parent_child = NULL;
  rem = (intptr_t)TX_LOAD(&place->removed);
  del = (intptr_t)TX_LOAD(&place->deleted);
  if(!rem && del) {
#ifdef CHECK_FIRST
    parent_localh = (val_t)TX_UNIT_LOAD(&parent->localh);
#endif
    left_child = (avl_node_t *)TX_LOAD(&place->left);
    right_child = (avl_node_t *)TX_LOAD(&place->right);
    
    if(left_child == NULL || right_child == NULL) {
      rem = (intptr_t)TX_LOAD(&parent->removed);
      if(!rem) {
	//make sure parent is correct
	if(v > parent->key) {
	  parent_child = (avl_node_t*)TX_LOAD(&parent->right);
	  go_left = 0;
	} else {
	  parent_child = (avl_node_t*)TX_LOAD(&parent->left);
	  go_left = 1;
	}
	  
	//this section here is only here because something weird with the unit loads it gets stuck if it is below
	if(left_child == NULL && right_child == NULL) {
	  if(go_left) {
	    lefth = 0;
	    righth = (val_t)TX_UNIT_LOAD(&parent->righth);
	  } else {
	    righth = 0;
	    lefth = (val_t)TX_UNIT_LOAD(&parent->lefth);
	  }
	}
	  
	if(parent_child == place) {
	  //remove the node
	  if(go_left) {
	    if(left_child == NULL) {
	      TX_STORE(&parent->left, right_child);
	    } else {
	      TX_STORE(&parent->left, left_child);
	    }
	  } else {
	    if(left_child == NULL) {
	      TX_STORE(&parent->right, right_child);
	    } else {
	      TX_STORE(&parent->right, left_child);
	    }
	  }
	  if(left_child == NULL) {
	    TX_STORE(&place->left, parent);
	  }
	  if(right_child == NULL) {
	    TX_STORE(&place->right, parent);
	    //righth = 0;
	  }
	  
	  if(left_child == NULL && right_child == NULL) {
	    if(go_left) {
	      TX_STORE(&parent->lefth, 0);
	      lefth = 0;
	    } else {
	      TX_STORE(&parent->righth, 0);
	      righth = 0;
	    }
	    
	    //Could also do a read here to check if should change before writing?
	    //Good or bad for performance? Dunno?
	    new_localh = 1 + max(righth, lefth);
#ifdef CHECK_FIRST
	    if(parent_localh != new_localh) {
	      TX_STORE(&parent->localh, new_localh);
	    }
#else
	    TX_STORE(&parent->localh, new_localh);
#endif
	  }
	  
	  TX_STORE(&place->removed, 1);
	  //FREE(place, sizeof(avl_node_t));
	  //Should update parent heights?
	  ret = 2;
	}
      }
    }
  }
  TX_END;
  
  return ret;
}

int avl_rotate(avl_node_t *parent, short go_left, avl_node_t *node, free_list_item *free_list) {
  int ret;
  avl_node_t *child_addr = NULL;
  
  ret = avl_single_rotate(parent, go_left, node, 0, 0, &child_addr, free_list);
  if(ret == 2) {
    //Do a LRR
    //printf("Doin a LRR, %d\n", child_addr->key);
    ret = avl_single_rotate(node, 1, child_addr, 1, 0, &child_addr, free_list);
    if(ret > 0) {
      //printf("Doin the second part, %d\n", child_addr->key);
      avl_single_rotate(parent, go_left, child_addr, 0, 1, NULL, free_list);
    }
  } else if(ret == 3) {
    //Do a RLR
    //printf("Doin a RLR, %d\n", child_addr->key);
    ret = avl_single_rotate(node, 0, child_addr, 0, 1, &child_addr, free_list);
    if(ret > 0) {
      //printf("Doin the second part, %d\n", child_addr->key);
      avl_single_rotate(parent, go_left, child_addr, 1, 0, NULL, free_list);
    }

  }
  return ret;

}


int avl_single_rotate(avl_node_t *parent, short go_left, avl_node_t *node, short left_rotate, short right_rotate, avl_node_t **child_addr, free_list_item* free_list) {
  val_t lefth, righth, bal, child_localh;
  avl_node_t *child, *check;
  intptr_t rem;
  int ret;

  //printf("Rotate val:%d\n", node->key);
  TX_START(NL);
  ret = 0;
  if((rem = (intptr_t)TX_LOAD(&node->removed)) || (rem = (intptr_t)TX_LOAD(&parent->removed))) {
    ret = 0;
  } else {
    if(go_left) {
      check = (avl_node_t*)TX_LOAD(&parent->left);
    } else {
      check = (avl_node_t*)TX_LOAD(&parent->right);
    }
    if(check == node) {

      lefth = TX_LOAD(&node->lefth);
      righth = TX_LOAD(&node->righth);
      bal = lefth - righth;
      
      if(bal >= 2 || right_rotate) {
	child = (avl_node_t *)TX_LOAD(&node->left);
	if(child != NULL) {
	  child_localh = (val_t)TX_LOAD(&child->localh);
	  if(lefth - child_localh == 0) {
	    ret = avl_right_rotate(parent, go_left, node, lefth, righth, child, child_addr, right_rotate, free_list);
	  }
	}
	
      } else if(bal <= -2 || left_rotate) {
	child = (avl_node_t *)TX_LOAD(&node->right);
	if(child != NULL) {
	  child_localh = (val_t)TX_LOAD(&child->localh);
	  if(righth - child_localh == 0) {
	    ret = avl_left_rotate(parent, go_left, node, lefth, righth, child, child_addr, left_rotate, free_list);
	  }
	}
      }
    }
  }
  TX_END;

  return ret;
}

int avl_right_rotate(avl_node_t *parent, short go_left, avl_node_t *node, val_t lefth, val_t righth, avl_node_t *left_child, avl_node_t **left_child_addr, short do_rotate, free_list_item* free_list) {
  val_t left_lefth, left_righth, left_bal, localh;
  avl_node_t *right_child, *left_right_child, *new_node;
  free_list_item *next_list_item;

  //printf("Right rotate %d\n", node->key);
  left_lefth = (val_t)TX_LOAD(&left_child->lefth);
  left_righth = (val_t)TX_LOAD(&left_child->righth);

  if(left_child_addr != NULL) {
    *left_child_addr = left_child;
  }

  left_bal = left_lefth - left_righth;
  if(left_bal < 0 && !do_rotate) {
    //should do a double rotate
    return 2;
  }

  
  left_right_child = (avl_node_t *)TX_LOAD(&left_child->right);
  right_child = (avl_node_t *)TX_LOAD(&node->right);
   
#ifdef KEYMAP
  new_node = avl_new_simple_node((val_t)TX_LOAD(&node->val), node->key, 1);
#else
  new_node = avl_new_simple_node(node->val, node->key, 1);
#endif
  new_node->deleted = (intptr_t)TX_LOAD(&node->deleted);
  new_node->left = left_right_child;
  new_node->right = right_child;
  new_node->lefth = left_righth;
  new_node->righth = righth;
  new_node->localh = 1 + max(left_righth, righth);
  
  localh = (val_t)TX_LOAD(&node->localh);
  if(left_bal >= 0) {
    TX_STORE(&left_child->localh, localh-1);
  } else {
    TX_STORE(&left_child->localh, localh);
  }
  //if(right_child == NULL) {
  TX_STORE(&node->right, new_node);
  //}
  TX_STORE(&node->removed, 1);

  //for garbage cleaning
  next_list_item = (free_list_item *)MALLOC(sizeof(free_list_item));
  next_list_item->next = NULL;
  next_list_item->to_free = node;
  TX_STORE(&free_list->next, next_list_item);

  TX_STORE(&left_child->righth, new_node->localh);
  TX_STORE(&left_child->right, new_node);
  if(go_left) {
    TX_STORE(&parent->left, left_child);
  } else {
    TX_STORE(&parent->right, left_child);
  }
  return 1;

}



int avl_left_rotate(avl_node_t *parent, short go_left, avl_node_t *node, val_t lefth, val_t righth, avl_node_t *right_child, avl_node_t **right_child_addr, short do_rotate, free_list_item* free_list) {
  val_t right_lefth, right_righth, right_bal, localh;
  avl_node_t *left_child, *right_left_child, *new_node;
  free_list_item *next_list_item;

  //printf("Left rotate %d\n", node->key);
  right_lefth = (val_t)TX_LOAD(&right_child->lefth);
  right_righth = (val_t)TX_LOAD(&right_child->righth);

  if(right_child_addr != NULL) {
    *right_child_addr = right_child;
  }

  right_bal = right_lefth - right_righth;
  if(right_bal > 0 && !do_rotate) {
    //should do a double rotate
    return 3;
  }

  right_left_child = (avl_node_t *)TX_LOAD(&right_child->left);
  left_child = (avl_node_t *)TX_LOAD(&node->left);

#ifdef KEYMAP
  new_node = avl_new_simple_node((val_t)TX_LOAD(&node->val), node->key, 1);
#else
  new_node = avl_new_simple_node(node->val, node->key, 1);
#endif
  new_node->deleted = (intptr_t)TX_LOAD(&node->deleted);
  new_node->right = right_left_child;
  new_node->left = left_child;
  new_node->righth = right_lefth;
  new_node->lefth = lefth;
  new_node->localh = 1 + max(right_lefth, lefth);
  
  localh = (val_t)TX_LOAD(&node->localh);
  if(right_bal < 0) {
    TX_STORE(&right_child->localh, localh-1);
  } else {
    TX_STORE(&right_child->localh, localh);
  }
  //if(left_child == NULL) {
  TX_STORE(&node->left, new_node);
  //}
  TX_STORE(&node->removed, 1);

  //for garbage cleaning
  next_list_item = (free_list_item *)MALLOC(sizeof(free_list_item));
  next_list_item->next = NULL;
  next_list_item->to_free = node;
  TX_STORE(&free_list->next, next_list_item);

  TX_STORE(&right_child->lefth, new_node->localh);
  TX_STORE(&right_child->left, new_node);
  if(go_left) {
    TX_STORE(&parent->left, right_child);
  } else {
    TX_STORE(&parent->right, right_child);
  }
  return 1;
}


int avl_propagate(avl_node_t *node, short left, short *should_rotate) {
  avl_node_t *child;
  val_t height, other_height, child_localh = 0, new_localh;
  intptr_t rem;
  int ret = 0;
  short is_reliable = 0;
#ifdef CHECK_FIRST
  val_t localh;
#endif
  
  //printf("starting prop\n");
  TX_START(NL);
  //printf("cont prop\n");
  *should_rotate = 0;
  ret = 0;
  if((rem = (intptr_t)TX_UNIT_LOAD(&node->removed)) == 0) {
    
#ifdef CHECK_FIRST
    //remove this?
    //why work if unit read here? and now down below
    localh = (val_t)TX_UNIT_LOAD(&node->localh);
#endif

    if(left) {
      child = (avl_node_t *)TX_LOAD(&node->left);
    } else {
      child = (avl_node_t *)TX_LOAD(&node->right);
    }
    
    if(child != NULL) {

      if(left) {
	height = (val_t)TX_UNIT_LOAD(&node->lefth);
      } else {
	height = (val_t)TX_UNIT_LOAD(&node->righth);
      }
      
      child_localh = (val_t)TX_UNIT_LOAD(&child->localh);
      if(height - child_localh == 0) {
	is_reliable = 1;
      }

      if(left) {
	other_height = (val_t)TX_UNIT_LOAD(&node->righth);
      } else {
	other_height = (val_t)TX_UNIT_LOAD(&node->lefth);
      }

      //check if should rotate
      if(abs(other_height - child_localh) >= 2) {
	*should_rotate = 1;
      }
      
      if(!is_reliable) { 
	if(left) {
	  TX_STORE(&node->lefth, child_localh);
	} else {
	  TX_STORE(&node->righth, child_localh);
	}
      
	//Could also do a read here to check if should change before writing?
	//Good or bad for performance? Dunno?
	new_localh = 1 + max(child_localh, other_height);
#ifdef CHECK_FIRST
	if(localh != new_localh) {
	  TX_STORE(&node->localh, new_localh);
	  ret = 1;
	}
#else
	TX_STORE(&node->localh, new_localh);
	ret = 1;
#endif
      }
    }
  }
  
  TX_END;
  //printf("Finished prop\n");
  return ret;  
}

int recursive_tree_propagate(avl_intset_t *set, ulong *num, ulong* num_suc, ulong *num_rot, ulong *suc_rot, ulong *num_rem, free_list_item* free_list) {
  free_list_item *next;
  
  //Get to the end of the free list so you can add new elements
  next = free_list;
  while(next->next != NULL) {
    next = next->next;
  }

  recursive_node_propagate(set->root, set->root, NULL, num, num_suc, num_rot, suc_rot, num_rem, next);
  //printf("Finished full prop\n");
  return 1;
}

int recursive_node_propagate(avl_node_t *root, avl_node_t *node, avl_node_t *parent, ulong *num, ulong *num_suc, ulong *num_rot, ulong *suc_rot, ulong *num_rem, free_list_item *free_list) {
  avl_node_t *left, *right;
  intptr_t rem, del;
  int rem_succs;
  short should_rotatel, should_rotater;
  free_list_item *next_list_item;

  usleep(TTIME);

  if(node == NULL) {
    return 1;
  }
  TX_START(NL);
  left = (avl_node_t*)TX_UNIT_LOAD(&node->left);
  right = (avl_node_t*)TX_UNIT_LOAD(&node->right);
  rem = (intptr_t)TX_UNIT_LOAD(&node->removed);
  del = (intptr_t)TX_UNIT_LOAD(&node->deleted);
  TX_END;

  if(!rem) {
    if(left != NULL) {
      recursive_node_propagate(root, left, node, num, num_suc, num_rot, suc_rot, num_rem, free_list);
    }
    if(right != NULL) {
      recursive_node_propagate(root, right, node, num, num_suc, num_rot, suc_rot, num_rem, free_list);

      rem_succs = 0;
      if((left == NULL || right == NULL) && del && parent != NULL) {
	rem_succs = remove_node(parent, node);
	if(rem_succs > 1) {
	  //printf("Removed node in maintenace\n");
	  *num_rem = *num_rem + 1;

	  //add to the list for garbage collection
	  next_list_item = (free_list_item *)malloc(sizeof(free_list_item));
	  next_list_item->next = NULL;
	  next_list_item->to_free = node;
	  free_list->next = next_list_item;
	  free_list = next_list_item;

	  return 1;
	}
      }
    }

    //should do just left or right rotate here?
    TX_START(NL);
    left = (avl_node_t*)TX_UNIT_LOAD(&node->left);
    rem = (intptr_t)TX_UNIT_LOAD(&node->removed);
    TX_END;
    if(left != NULL && !rem && left != root) {
      *num_suc = *num_suc + avl_propagate(left, 1, &should_rotatel);
      *num_suc = *num_suc + avl_propagate(left, 0, &should_rotater);
      if(should_rotatel || should_rotater) {
	//printf("Calling rotate method\n");
	*suc_rot = *suc_rot + avl_rotate(node, 1, left, free_list);
	*num_rot = *num_rot + 1;
      }
      *num = *num + 2;
    }
    
    //should do just left or right rotate here?
    TX_START(NL);
    right = (avl_node_t*)TX_UNIT_LOAD(&node->right);
    rem = (intptr_t)TX_UNIT_LOAD(&node->removed);
    TX_END;
    if(right != NULL && !rem && right != root) {
      *num_suc = *num_suc + avl_propagate(right, 1, &should_rotatel);
      *num_suc = *num_suc + avl_propagate(right, 0, &should_rotater);
      if(should_rotatel || should_rotater) {
	//printf("Calling rotate method\n");
	*suc_rot = *suc_rot + avl_rotate(node, 0, right, free_list);
	*num_rot = *num_rot + 1;
      }
      *num = *num + 2;
    }
    
  }

  return 1;
}





#ifdef KEYMAP

val_t avl_get(val_t key, avl_intset_t *set) {
  short done;
  intptr_t rem, del;
  avl_node_t *place;
  val_t val;
  
  place = set->root;
  TX_START(NL);
  rem = 1;
  done = 0;
  
  while(rem) {
    avl_find(key, &place);
    
    if(place->key != key) {
      done = 1;
      break;
    }
    rem = (intptr_t)TX_LOAD(&place->removed);
  }
  if(!done) {
    del = (intptr_t)TX_LOAD(&place->deleted);
#ifdef KEYMAP
    val = (val_t)TX_LOAD(&place->val);
#else
    val = place->val
#endif
  }

  TX_END;

  if(done) {
    return 0;
  } else if(del) {
    return 0;
  }
  return val;

}


int avl_update(val_t v, val_t key, avl_intset_t *set) {
  avl_node_t *place, *next, *new_node;
  intptr_t rem, del;
  short done, go_left = 0;
  int ret;

  place = set->root;
  next = place;
  TX_START(NL);
  ret = 0;
  rem = 1;
  done = 0;
  
  while(rem || next != NULL) {
    avl_find(key, &place);
    rem = (intptr_t)TX_LOAD(&place->removed);
    if(!rem) {
      if(place->key == key) {
	done = 1;
	break;
      } else if(key > place->key){
	next = (avl_node_t*)TX_LOAD(&place->right);
	go_left = 0;
      } else {
	next = (avl_node_t*)TX_LOAD(&place->left);
	go_left = 1;
      }
    }
  }

  if(done) {
    if((del = (intptr_t)TX_LOAD(&place->deleted))) {
      TX_STORE(&place->deleted, 0);
      TX_STORE(&place->val, v);
      ret = 1;
    } else {
      TX_STORE(&place->val, v);
      ret = 0;
    }
  } else {
    new_node = avl_new_simple_node(v, key, 1);
    if(go_left) {
      TX_STORE(&place->left, new_node);
    } else {
      TX_STORE(&place->right, new_node);
    }
    ret = 2;
  }

  TX_END;
  return ret;

}


/*
=============================================================================
  * TMrbtree_insert
  * -- Returns TRUE on success
  *
=============================================================================
  */
TM_CALLABLE
bool_t
TMrbtree_insert (TM_ARGDECL  rbtree_t* r, void* key, void* val) {
  int ret;

  ret = avl_insert((val_t)val, (val_t)key, (avl_intset_t *)r);

  if(ret > 0) {
    return 1;
  }
  return 0;

}

/*
=============================================================================
  * TMrbtree_delete
  *
=============================================================================
  */
TM_CALLABLE
bool_t
TMrbtree_delete (TM_ARGDECL  rbtree_t* r, void* key) {
  int ret;

  ret = avl_delete((val_t)key, (avl_intset_t *)r, NULL);

  if(ret > 0) {
    return 1;
  }
  return 0;

}

/*
=============================================================================
  * TMrbtree_update
  * -- Return FALSE if had to insert node first
  *
=============================================================================
  */
TM_CALLABLE
bool_t
TMrbtree_update (TM_ARGDECL  rbtree_t* r, void* key, void* val) {
  int ret;

  ret = avl_update((val_t)val, (val_t)key, (avl_intset_t *)r);

  if(ret > 0) {
    return 0;
  }
  return 1;
}

/*
=============================================================================
  * TMrbtree_get
  *
=============================================================================
  */
TM_CALLABLE
void*
TMrbtree_get (TM_ARGDECL  rbtree_t* r, void* key) {

  return (void *)avl_get((val_t)key, (avl_intset_t *)r);
  
}


/*
=============================================================================
  * TMrbtree_contains
  *
=============================================================================
  */
TM_CALLABLE
bool_t
TMrbtree_contains (TM_ARGDECL  rbtree_t* r, void* key) {

  return avl_search((val_t)key, (avl_intset_t*) r);

}

#endif

#endif


#ifdef SEQUENTIAL

#ifdef KEYMAP

val_t avl_seq_get(avl_intset_t *set, val_t key, int transactional)
{
  val_t val;
  avl_node_t *next;

  next = set->root;
  
  while(next != 0) {
    val = next->key;
    
    if(key == val) {
      if(next->deleted) {
	//next = next->left;
	return 0;
      } else {
	return next->val;
      }
    } else if(val < key) {
      next = next->right;
    } else {
      next = next->left;
    }
  }
  return 0;
}


inline int avl_req_seq_update(avl_node_t *parent, avl_node_t *node, val_t val, val_t key, int go_left, int *success) {
  avl_node_t *new_node;
  int ret;
  
  if(node != NULL) {    
    if(key == node->key) {
      if(!node->deleted) {
	//found the node
	node->val = val;
	*success = 1;
	return 0;
      } else {
	node->deleted = 0;
	node->val = val;
	*success = 0;
	return 0;
      }
    } else if(key > node->key) {
      ret = avl_req_seq_add(node, node->right, val, key, 0, success);
    } else {
      ret = avl_req_seq_add(node, node->left, val, key, 1, success);
    }
    
    if(ret != 0) {
      return avl_seq_propogate(parent, node, go_left);
    }
    return 0;

  } else {
    //insert the node
    new_node = avl_new_simple_node(val, key, 0);
    if(key > parent->key) {
      parent->right = new_node;
    } else {
      parent->left = new_node;
    }
    *success = -1;
  }
  
  return 1;
  
}



/* =============================================================================
 * rbtree_insert
 * -- Returns TRUE on success
 * =============================================================================
 */
bool_t
rbtree_insert (rbtree_t* r, void* key, void* val) {
  int ret;
  
  avl_req_seq_add(NULL, ((avl_intset_t *)r)->root, (val_t)val, (val_t)key, 0, &ret);
  
  if(ret > 0) {
    return 1;
  }
  return 0;

}


/* =============================================================================
 * rbtree_delete
 * =============================================================================
 */
bool_t
rbtree_delete (rbtree_t* r, void* key) {
  int ret;

  avl_req_seq_delete(NULL, ((avl_intset_t *)r)->root, (val_t)key, 0, &ret);

  if(ret > 0) {
    return 1;
  }
  return 0;

}


/* =============================================================================
 * rbtree_update
 * -- Return FALSE if had to insert node first
 * =============================================================================
 */
bool_t
rbtree_update (rbtree_t* r, void* key, void* val) {
  int ret;

  avl_req_seq_update(NULL, ((avl_intset_t *)r)->root, (val_t)val, (val_t)key, 0, &ret);

  if(ret > 0) {
    return 1;
  }
  return 0;
}


/* =============================================================================
 * rbtree_get
 * =============================================================================
 */
void*
rbtree_get (rbtree_t* r, void* key) {

  return (void *)avl_seq_get((avl_intset_t *)r, (val_t)key, 0);
  
}


/* =============================================================================
 * rbtree_contains
 * =============================================================================
 */
bool_t
rbtree_contains (rbtree_t* r, void* key) {

  return avl_contains((avl_intset_t*) r, (val_t)key, 0);

}


#endif

#endif
