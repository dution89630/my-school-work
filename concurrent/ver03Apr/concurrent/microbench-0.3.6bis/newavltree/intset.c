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

#define CHECK_FIRST

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


#if defined(MICROBENCH)
int avl_remove(avl_intset_t *set, val_t key, int transactional, int id)
#else
int avl_remove(avl_intset_t *set, val_t key, int transactional)
#endif
{
  int result = 0;
	
#ifdef SEQUENTIAL
	
  avl_req_seq_delete(NULL, set->root, key, 0, &result);

  /* avl_node_t *new_node, *next, *prev, *parent, *child; */
  /* int parent_left_child, left_child; */
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

#if defined(MICROBENCH)
  result = avl_delete(key, set, id);
#else
  result = avl_delete(key, set);
#endif

#endif

	
  return result;
}



int avl_req_seq_delete(avl_node_t *parent, avl_node_t *node, val_t key, int go_left, int *success) {
  avl_node_t *succs;
  int ret;

  //printf("seq del %d\n", key);

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
#ifdef SEPERATE_BALANCE2
	    if(node->bnode == NULL) {
	      avl_new_balance_node(node, 0);
	    }
	    if(parent->bnode == NULL) {
	      avl_new_balance_node(parent, 0);
	    }
	    parent->bnode->left = node->bnode->right;
	    if(node->bnode->right != NULL) {
	      parent->bnode->left->parent = parent->bnode;
	    }
#endif
	  } else {
	    parent->right = node->right;
#ifdef SEPERATE_BALANCE2
	    if(parent->bnode == NULL) {
	      avl_new_balance_node(parent, 0);
	    }
	    if(node->bnode != NULL) {
	      parent->bnode->right = node->bnode->right;
	      if(node->bnode->right != NULL) {
		parent->bnode->right->parent = parent->bnode;
	      }
	    } else {
	      parent->bnode->right = NULL;
	    }

#endif
	  }
#ifdef SEPERATE_BALANCE
	  if(node->bnode != NULL) {
	    free(node->bnode);
	  }
#endif
	  free(node);
	  *success = 1;
	  return 1;
	} else if(node->right == NULL) {
	  if(go_left) {
	    parent->left = node->left;
#ifdef SEPERATE_BALANCE2
	    if(parent->bnode == NULL) {
	      avl_new_balance_node(parent, 0);
	    }
	    if(node->bnode != NULL) {
	      parent->bnode->left = node->bnode->left;
	      if(node->bnode->left != NULL) {
		parent->bnode->left->parent = parent->bnode;
	      }
	    } else {
	      parent->bnode->left = NULL;
	    }
#endif
	  } else {
	    parent->right = node->left;
#ifdef SEPERATE_BALANCE2
	    if(node->bnode != NULL) {
	      parent->bnode->right = node->bnode->left;
	      if(node->bnode->left != NULL) {
		parent->bnode->right->parent = parent->bnode;
	      }
	    } else {
	      parent->bnode->right = NULL;
	    }
#endif
	  }
#ifdef SEPERATE_BALANCE
	  if(node->bnode != NULL) {
	    free(node->bnode);
	  }
#endif
	  free(node);
	  *success = 1;
	  return 1;
	} else {
	  ret = avl_seq_req_find_successor(node, node->right, 0, &succs);
	  succs->right = node->right;
#ifdef SEPERATE_BALANCE2
	  succs->bnode->right = node->bnode->right;
	  if(node->bnode->right != NULL) {
	    succs->bnode->right->parent = succs->bnode;
	  }
#endif
	  //succs->righth = node->localh;
	  succs->left = node->left;
#ifdef SEPERATE_BALANCE2
	  succs->bnode->left = node->bnode->left;
	  if(node->bnode->left != NULL) {
	    succs->bnode->left->parent = succs->bnode;
	  }
#endif
	  //succs->lefth = node->localh;
	  if(go_left) {
	    parent->left = succs;
#ifdef SEPERATE_BALANCE2
	    parent->bnode->left = succs->bnode;
	    succs->bnode->parent = parent->bnode;
#endif
	  } else {
	    parent->right = succs;
#ifdef SEPERATE_BALANCE2
	    parent->bnode->right = succs->bnode;
	    succs->bnode->parent = parent->bnode;
#endif
	  }
#ifdef SEPERATE_BALANCE
	  if(node->bnode != NULL) {
	    free(node->bnode);
	  }
#endif
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
#ifdef SEPERATE_BALANCE2
  balance_node_t *bnode;
#endif
  
  //printf("seq add %d\n", key);

  if(node != NULL) {    
    if(key == node->key) {
      if(!node->deleted) {
	//found the node
	//printf("seq cannot insert key %d\n", key);
	*success = -1;
	return 0;
      } else {
	node->deleted = 0;
	node->val = val;
	//printf("seq insert (del) key %d\n", key);
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
#ifdef SEPERATE_BALANCE2
    bnode = avl_new_balance_node(new_node, 0);
    bnode->parent = parent->bnode;
#endif
    if(key > parent->key) {
      parent->right = new_node;
#ifdef SEPERATE_BALANCE2
      parent->bnode->right = bnode;
#endif
    } else {
      parent->left = new_node;
#ifdef SEPERATE_BALANCE2
      parent->bnode->left = bnode;
#endif
    }
    //printf("seq insert key %d\n", key);
    *success = 1;
  }
  
  return 1;
  
}



int avl_seq_propogate(avl_node_t *parent, avl_node_t *thenode, int go_left) {
  int new_localh;
#ifdef SEPERATE_BALANCE
  balance_node_t *node;
  node = thenode->bnode;
#else
  avl_node_t *node;
  node = thenode;
#endif

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
#ifdef SEPERATE_BALANCE
      avl_seq_check_rotations(parent, thenode, node->lefth, node->righth, go_left);
#else
      avl_seq_check_rotations(parent, node, node->lefth, node->righth, go_left);
#endif
    }
    return 1;
  }
  return 0;
}

int avl_seq_check_rotations(avl_node_t *parent, avl_node_t *node, int lefth, int righth, int go_left) {
  int local_bal, lbal, rbal;

  local_bal = lefth - righth;

  if(local_bal > 1) {
#ifdef SEPERATE_BALANCE
    lbal = node->bnode->left->lefth - node->bnode->left->righth;
#else
    lbal = node->left->lefth - node->left->righth;
#endif
    if(lbal >= 0) {
      avl_seq_right_rotate(parent, node, go_left);
    } else {
      avl_seq_left_right_rotate(parent, node, go_left);
    }

  } else if(local_bal < -1) {
#ifdef SEPERATE_BALANCE
    rbal = node->bnode->right->lefth - node->bnode->right->righth;
#else
    rbal = node->right->lefth - node->right->righth;
#endif
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
#ifdef SEPERATE_BALANCE
  balance_node_t *ub, *vb, *pb;
  pb = parent->bnode;
#endif

  v = place;
  u = place->left;
#ifdef SEPERATE_BALANCE
  ub = u->bnode;
  vb = v->bnode;
#endif
  if(go_left) {
    parent->left = u;
#ifdef SEPERATE_BALANCE2
    pb->left = ub;
    ub->parent = pb;
#endif
  } else {
    parent->right = u;
#ifdef SEPERATE_BALANCE2
    pb->right = ub;
    ub->parent = pb;
#endif
  }

  v->left = u->right;
  u->right = v;

#ifdef SEPERATE_BALANCE2
  vb->left = ub->right;
  ub->right = vb;
#endif
#ifdef SEPERATE_BALANCE
  //now need to do propogations
  ub->localh = vb->localh - 1;
  vb->lefth = ub->righth;
  vb->localh = 1 + max(vb->lefth, vb->righth);
  ub->righth = vb->localh;
#else
  //now need to do propogations
  u->localh = v->localh - 1;
  v->lefth = u->righth;
  v->localh = 1 + max(v->lefth, v->righth);
  u->righth = v->localh;
#endif
  return 1;
}


int avl_seq_left_rotate(avl_node_t *parent, avl_node_t *place, int go_left) {
  avl_node_t *u, *v;
#ifdef SEPERATE_BALANCE
  balance_node_t *ub, *vb, *pb;
  pb = parent->bnode;
#endif

  v = place;
  u = place->right;
#ifdef SEPERATE_BALANCE
  ub = u->bnode;
  vb = v->bnode;
#endif
  if(go_left) {
    parent->left = u;
#ifdef SEPERATE_BALANCE2
    pb->left = ub;
    ub->parent = pb;
#endif
  } else {
    parent->right = u;
#ifdef SEPERATE_BALANCE2
    pb->right = ub;
    ub->parent = pb;
#endif
  }

  v->right = u->left;
  u->left = v;

#ifdef SEPERATE_BALANCE2
  vb->right = ub->left;
  ub->left = vb;
#endif
#ifdef SEPERATE_BALANCE
  //now need to do propogations
  ub->localh = vb->localh - 1;
  vb->righth = ub->lefth;
  vb->localh = 1 + max(vb->lefth, vb->righth);
  ub->lefth = vb->localh;
#else
  //now need to do propogations
  u->localh = v->localh - 1;
  v->righth = u->lefth;
  v->localh = 1 + max(v->lefth, v->righth);
  u->lefth = v->localh;
#endif
  
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
#ifdef SEPERATE_BALANCE2
      if(node->bnode == NULL) {
	avl_new_balance_node(node, 0);
      }
      if(parent->bnode == NULL) {
	avl_new_balance_node(parent, 0);
      }
      parent->bnode->left = node->bnode->right;
      if(node->bnode->right != NULL) {
	parent->bnode->left->parent = parent->bnode;
      }
#endif
#ifdef SEPERATE_BALANCE
      parent->bnode->lefth = node->bnode->localh - 1;
#else 
      parent->lefth = node->localh - 1;
#endif
    } else {
      parent->right = node->right;
#ifdef SEPERATE_BALANCE2
      if(node->bnode == NULL) {
	avl_new_balance_node(node, 0);
      }
      if(parent->bnode == NULL) {
	avl_new_balance_node(parent, 0);
      }
      parent->bnode->right = node->bnode->right;
      if(node->bnode->right != NULL) {
	parent->bnode->right->parent = parent->bnode;
      }
#endif
#ifdef SEPERATE_BALANCE
      parent->bnode->righth = node->bnode->localh - 1;
#else
      parent->righth = node->localh - 1;
#endif
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

int avl_search(val_t key, const avl_intset_t *set) {
  int done;
  intptr_t rem, del;
  avl_node_t *place, *next;
  val_t k;
  
  place = set->root;
  TX_START(NL);
  //place = set->root;
  rem = 1;
  done = 0;
  
  while(rem) {
    avl_find(key, &place, &k);

    if(k != key) {

      /* //do this load for when you have nested trans */
      /* if(place->key > ) { */
      /* 	next = TX_LOAD(place->right); */
      /* } else { */
      /* 	next = TX_LOAD(place->left); */
      /* } */

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



int avl_find(val_t key, avl_node_t **place, val_t *val) {
  avl_node_t *next, *placet;
  intptr_t rem;
  val_t valt;
#ifdef CHANGE_KEY
  val_t new_val;
#endif

  placet = *place;
  next = placet;
  valt = *val;

  rem = 1;
  while(next != NULL && (valt != key || rem)) {
    placet = next;
    rem = (intptr_t)TX_UNIT_LOAD(&(placet)->removed);
#ifdef CHANGE_KEY
    valt = (val_t)TX_UNIT_LOAD(&(placet)->key);
    //*val = (*place)->key;
    while(1) {
      
      if(key > valt) {
	next = (avl_node_t*)TX_UNIT_LOAD(&(placet)->right);
      } else {
	next = (avl_node_t*)TX_UNIT_LOAD(&(placet)->left);
      }
      new_val = (val_t)TX_UNIT_LOAD(&(placet)->key);
      //new_val = (*place)->key;

      if(new_val == key) {
	if(key == (val_t)TX_LOAD(&(placet)->key)) {
	  valt = key;
	  break;
	}
      }
      if(new_val == valt) {
	break;
      }
      valt = new_val;
    }
#else
    valt = (placet)->key;
#endif

    if(key != valt || rem) {
#ifndef CHANGE_KEY
      if(key > valt) {
	next = (avl_node_t*)TX_UNIT_LOAD(&(placet)->right);
      } else {
	next = (avl_node_t*)TX_UNIT_LOAD(&(placet)->left);
      }
#endif
      
      //Do this for nested transactions
      if(next == NULL ) {
#ifdef CHANGE_KEY
	valt = (val_t)TX_LOAD(&(placet)->key);
#endif
	if(key > valt) {
	  next = (avl_node_t*)TX_LOAD(&(placet)->right);
	} else {
	  next = (avl_node_t*)TX_LOAD(&(placet)->left);
	}
      }
    }
  }
  *val = valt;
  *place = placet;
  return 0;
}


int avl_find_parent(val_t key, avl_node_t **place, avl_node_t **parent, val_t *val) {
  avl_node_t *next, *placet, *parentt;
  intptr_t rem;
  val_t valt;
#ifdef CHANGE_KEY
  val_t new_val;
#endif

  
  next = *place;
  placet = *parent;
  valt = *val;

  rem = 1;
  while(next != NULL && (valt != key || rem)) {
    parentt = placet;
    placet = next;
    rem = (intptr_t)TX_UNIT_LOAD(&(placet)->removed);

#ifdef CHANGE_KEY
    valt = (val_t)TX_UNIT_LOAD(&(placet)->key);
    //valt = (placet)->key;
    while(1) {
      
      if(key > valt) {
	next = (avl_node_t*)TX_UNIT_LOAD(&(placet)->right);
      } else {
	next = (avl_node_t*)TX_UNIT_LOAD(&(placet)->left);
      }
      new_val = (val_t)TX_UNIT_LOAD(&(placet)->key);
      //new_val = (placet)->key;

      if(new_val == key) {
	if(key == (val_t)TX_LOAD(&(placet)->key)) {
	  valt = key;
	  break;
	}
      }
      if(new_val == valt) {
	break;
      }
      valt = new_val;
    }
#else
    valt = (placet)->key;
#endif

    if(key != valt || rem) {
#ifndef CHANGE_KEY
      if(key > valt) {
	next = (avl_node_t*)TX_UNIT_LOAD(&(placet)->right);
      } else {
	next = (avl_node_t*)TX_UNIT_LOAD(&(placet)->left);
      }
#endif

    //Do this for nested transactions
      if(next == NULL) {
#ifdef CHANGE_KEY
	valt = (val_t)TX_LOAD(&(placet)->key);
#endif
	if(key > valt) {
	  next = (avl_node_t*)TX_LOAD(&(placet)->right);
	} else {
	  next = (avl_node_t*)TX_LOAD(&(placet)->left);
	}
      }
    }
  }
  *val = valt;
  *place = placet;
  *parent = parentt;
  return 0;
}



int avl_insert(val_t v, val_t key, const avl_intset_t *set) {
  avl_node_t *place, *next, *new_node;
  intptr_t rem, del;
  int done, go_left = 0;
  int ret;
  val_t k;

  //printf("inserting %d %d\n", key, v);  

  place = set->root;
  next = place;
  TX_START(NL);
  //place = set->root;
  //next = place;
  ret = 0;
  rem = 1;
  done = 0;
  
  while(rem || next != NULL) {
    avl_find(key, &place, &k);
    rem = (intptr_t)TX_LOAD(&place->removed);
    if(!rem) {
      if(k == key) {
	done = 1;
	break;
      } else if(key > k){
	next = (avl_node_t*)TX_LOAD(&place->right);
	go_left = 0;
      } else {
	next = (avl_node_t*)TX_LOAD(&place->left);
	go_left = 1;
      }
    }
  }

  if(done) {
    //printf("Found the node %d, %d %d\n", key, place->key, place->val);
    if((del = (intptr_t)TX_LOAD(&place->deleted))) {
      TX_STORE(&place->deleted, 0);
#ifdef MAPKEY
      TX_STORE(&place->val, v);
#endif
      TX_STORE(&place->val, v);
      ret = 1;
      //printf("undeleting\n");
    } else {
      ret = 0;
    }
  } else {

#ifdef CHANGE_KEY
    if((del = (intptr_t)TX_LOAD(&place->deleted))) {
      TX_STORE(&place->deleted, 0);
      TX_STORE(&place->key, key);
      TX_STORE(&place->val, v);
      ret = 1;
    } else {
#endif
    
      new_node = avl_new_simple_node(v, key, 1);
      if(go_left) {
	TX_STORE(&place->left, new_node);
	//printf("inserted node %d %d %d at left %d\n", new_node, new_node->key, new_node->val, place->key);
	//printf("val is %d\n", (val_t)TX_UNIT_LOAD(&place->left));
      } else {
	TX_STORE(&place->right, new_node);
	//printf("inserted node %d %d %d at right %d\n", new_node, new_node->key, new_node->val, place->key);
	//printf("val is %d\n", (val_t)TX_UNIT_LOAD(&place->right));
      }
      ret = 2;
#ifdef CHANGE_KEY
    }
#endif
  }

  TX_END;


  //printf("ret = %d\n", ret);
  //printf("should be new node %d %d %d\n", place, place->key, place->right);
  //printf("should be new node %d %d %d\n", place, place->key, place->left);
  return ret;

}

#if defined(MICROBENCH)
int avl_delete(val_t key, const avl_intset_t *set, int id) {
#else
int avl_delete(val_t key, const avl_intset_t *set) {
#endif
  avl_node_t *place, *parent;
  intptr_t rem, del;
  int done;
  int ret;
  val_t k;
  //#ifndef SEPERATE_BALANCE2
#if !defined(SEPERATE_BALANCE2) || defined(SEPERATE_BALANCE2NLDEL)
#ifdef REMOVE_LATER
  remove_list_item_t *next_remove;
#else
  free_list_item *free_item;
  free_list_item *free_list;
#endif
#endif
#ifndef MICROBENCH
  long id;
#endif

  //printf("deleting %d\n", key);  

  place = set->root;
  parent = set->root;
  TX_START(NL);
  //place = set->root;
  //parent = set->root;
  ret  = 0;
  done = 0;
  rem = 1;

  while(rem) {
    avl_find_parent(key, &place, &parent, &k);
    //avl_find(v, &place);
    if(k != key) {
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

#if defined(SEPERATE_BALANCE2DEL) || defined(REMOVE_LATER)
  if(ret == 1 && set->active_remove && ((avl_node_t *)TX_UNIT_LOAD(&place->left) == NULL || (avl_node_t *)TX_UNIT_LOAD(&place->right) == NULL)) {

#ifndef MICROBENCH
    id = thred_getId();
#endif

#ifdef REMOVE_LATER
    next_remove = (remove_list_item_t*)MALLOC(sizeof(remove_list_item_t));
    next_remove->item = place;
    next_remove->parent = parent;
    next_remove->next = (remove_list_item_t*)TX_UNIT_LOAD(&set->to_remove_later[id]);
    TX_STORE(&set->to_remove_later[id], next_remove);
#else

    /* delete = (free_list_item *)MALLOC(sizeof(free_list_item)); */
    /* delete->to_free = place; */
    /* if((old = (free_list_item *)TX_UNIT_LOAD(&set->to_remove[id])) != NULL) { */
    /*   TX_STORE(&old->next, delete); */
    /* } */
    /* TX_STORE(&set->to_remove[id], delete); */
    TX_STORE(&set->to_remove[id], place);
    TX_STORE(&set->to_remove_parent[id], parent);

#endif

  }

#endif
  TX_END;

  //Do the removal in a new trans(or maybe should do this in maintenance?)
#if !defined(SEPERATE_BALANCE2) || defined(SEPERATE_BALANCE2NLDEL)
#ifndef REMOVE_LATER
  if(ret == 1 && set->active_remove) {
    ret = remove_node(parent, place);
    if(ret > 1) {
#ifdef SEPERATE_MAINTENANCE
      TX_START(NL);
#endif
      //free_list = set->t_free_list[id];
      free_list = (free_list_item *)TX_UNIT_LOAD(&set->t_free_list[id]);
      //add it to the garbage collection
      free_item = (free_list_item *)MALLOC(sizeof(free_list_item));
      free_item->next = NULL;
      free_item->to_free = place;
#ifdef SEPERATE_MAINTENANCE
      free_list->next = free_item;
      //free_list = free_item;
      
	//set->t_free_list[id] = free_item;
      TX_STORE(&set->t_free_list[id], free_item);
      TX_END;
#else
      while(free_list->next != NULL) {
	free_list = free_list->next;
      }
      //printf("adding to free list %d %d\n", place->key, id);
      //(*free_list)->next = free_item;
      //free_list->next = free_item;
      
      //SHould only do this if in a transaction
      
      TX_STORE(&free_list->next, free_item);
      
      //do this in a trans (atomic)?
      //*free_list = free_item;
      //TX_START(NL);
      //TX_STORE(&(*free_list), free_item);
      //TX_END;
      
#endif
    
    }
  }
#endif
#endif
  

  return ret;
}
 

#ifdef REMOVE_LATER
#ifdef MICROBENCH
 int finish_removal(avl_intset_t *set, int id) {
#else
 int finish_removal(avl_intset_t *set) {
#endif
   remove_list_item_t *next;
   avl_node_t *place, *parent;
   free_list_item *free_item;
   free_list_item *free_list;
   int ret;
#ifndef MICROBENCH
   long id;
   id = thred_getId();
#endif

   next = set->to_remove_later[id];
   set->to_remove_later[id] = NULL;

   while(next != NULL) {
     parent = next->parent;
     place = next->item;

     ret = remove_node(parent, place);
     if(ret > 1) {
#ifdef SEPERATE_MAINTENANCE
       TX_START(NL);
#endif
       //free_list = set->t_free_list[id];
       free_list = (free_list_item *)TX_UNIT_LOAD(&set->t_free_list[id]);
       //add it to the garbage collection
       free_item = (free_list_item *)MALLOC(sizeof(free_list_item));
       free_item->next = NULL;
       free_item->to_free = place;
#ifdef SEPERATE_MAINTENANCE
       free_list->next = free_item;
       //free_list = free_item;
       
       //set->t_free_list[id] = free_item;
       TX_STORE(&set->t_free_list[id], free_item);
       TX_END;
#else
       while(free_list->next != NULL) {
	 free_list = free_list->next;
       }
       //printf("adding to free list %d %d\n", place->key, id);
       //(*free_list)->next = free_item;
       //free_list->next = free_item;
       
       //SHould only do this if in a transaction
      
       TX_STORE(&free_list->next, free_item);
       
       //do this in a trans (atomic)?
       //*free_list = free_item;
       //TX_START(NL);
       //TX_STORE(&(*free_list), free_item);
       //TX_END;
       
#endif
       
     }
     next = next->next;
     free(next);
   }
   return 0;
 }
#endif

int remove_node(avl_node_t *parent, avl_node_t *place) {
  avl_node_t *right_child, *left_child, *parent_child;
  int go_left;
  val_t v;
  int ret;
  intptr_t rem, del;
  val_t lefth = 0, righth = 0, new_localh;
#ifdef CHECK_FIRST
  val_t parent_localh;
#endif
#ifdef SEPERATE_BALANCE
  balance_node_t *bnode;
  bnode = parent->bnode;
#ifdef SEPERATE_BALANCE2
  balance_node_t *child_bnode;
#endif
#endif

  TX_START(NL);
#ifdef CHANGE_KEY
  v = (val_t)TX_UNIT_LOAD(&place->key);
#else
  v = place->key;
#endif
  ret = 1;
  right_child = NULL;
  left_child = NULL;
  parent_child = NULL;
  rem = (intptr_t)TX_LOAD(&place->removed);
  del = (intptr_t)TX_LOAD(&place->deleted);
  if(!rem && del) {
#ifdef CHECK_FIRST
#ifdef SEPERATE_BALANCE
    if(bnode != NULL) {
      parent_localh = (val_t)TX_UNIT_LOAD(&bnode->localh);
    } else {
      parent_localh = 1;
    }
#else
    parent_localh = (val_t)TX_UNIT_LOAD(&parent->localh);
#endif
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
#ifdef SEPERATE_BALANCE
	    if(bnode != NULL) {
	      righth = (val_t)TX_UNIT_LOAD(&bnode->righth);
	    } else {
	      righth = 0;
	    }
#else
	    righth = (val_t)TX_UNIT_LOAD(&parent->righth);
#endif
	  } else {
	    righth = 0;
#ifdef SEPERATE_BALANCE
	    if(bnode != NULL) {
	      lefth = (val_t)TX_UNIT_LOAD(&bnode->lefth);
	    } else {
	      lefth = 0;
	    }
#else
	    lefth = (val_t)TX_UNIT_LOAD(&parent->lefth);
#endif
	  }
	}
	  
	if(parent_child == place) {
	  //remove the node
	  if(go_left) {
	    if(left_child == NULL) {
	      TX_STORE(&parent->left, right_child);
#ifdef SEPERATE_BALANCE2
	      /* if(right_child != NULL) { */
	      /* 	child_bnode = right_child->bnode; */
	      /* 	if(child_bnode == NULL) { */
	      /* 	  child_bnode = avl_new_balance_node(right_child, 1); */
	      /* 	} */
	      /* 	TX_STORE(&bnode->left, child_bnode); */
	      /* 	TX_STORE(&child_bnode->parent, bnode); */
	      /* } else { */
	      /* 	TX_STORE(&bnode->left, NULL); */
	      /* } */
#endif
	    } else {
	      TX_STORE(&parent->left, left_child);
#ifdef SEPERATE_BALANCE2
	      /* if(left_child != NULL) { */
	      /* 	child_bnode = left_child->bnode; */
	      /* 	if(child_bnode == NULL) { */
	      /* 	  child_bnode = avl_new_balance_node(left_child, 1); */
	      /* 	} */
	      /* 	TX_STORE(&bnode->left, child_bnode); */
	      /* 	TX_STORE(&child_bnode->parent, bnode); */
	      /* } else { */
	      /* 	TX_STORE(&bnode->left, NULL); */
	      /* } */
#endif
	    }
	  } else {
	    if(left_child == NULL) {
	      TX_STORE(&parent->right, right_child);
#ifdef SEPERATE_BALANCE2
	      /* if(right_child != NULL) { */
	      /* 	child_bnode = right_child->bnode; */
	      /* 	if(child_bnode == NULL) { */
	      /* 	  child_bnode = avl_new_balance_node(right_child, 1); */
	      /* 	} */
	      /* 	TX_STORE(&bnode->right, child_bnode); */
	      /* 	TX_STORE(&child_bnode->parent, bnode); */
	      /* } else { */
	      /* 	TX_STORE(&bnode->right, NULL); */
	      /* } */
#endif
	    } else {
	      TX_STORE(&parent->right, left_child);
#ifdef SEPERATE_BALANCE2
	      /* if(left_child != NULL) { */
	      /* 	child_bnode = left_child->bnode; */
	      /* 	if(child_bnode == NULL) { */
	      /* 	  child_bnode = avl_new_balance_node(left_child, 1); */
	      /* 	} */
	      /* 	TX_STORE(&bnode->right, child_bnode); */
	      /* 	TX_STORE(&child_bnode->parent, bnode); */
	      /* } else { */
	      /* 	TX_STORE(&bnode->right, NULL); */
	      /* } */
#endif
	    }
	  }
	  //MAYBE THIS ISNT NECESSARY
	  //if(left_child == NULL) {
	    TX_STORE(&place->left, parent);
	    //}
	    //if(right_child == NULL) {
	    TX_STORE(&place->right, parent);
	    //righth = 0;
	    //}
	  
	  if(left_child == NULL && right_child == NULL) {
	    if(go_left) {
#ifdef SEPERATE_BALANCE
	      if(bnode != NULL) {
		TX_STORE(&bnode->lefth, 0);
	      }
#else
	      TX_STORE(&parent->lefth, 0);
#endif
	      lefth = 0;
	    } else {
#ifdef SEPERATE_BALANCE
	      if(bnode != NULL) {
		TX_STORE(&bnode->righth, 0);
	      }
#else
	      TX_STORE(&parent->righth, 0);
#endif
	      righth = 0;
	    }
	    
	    //Could also do a read here to check if should change before writing?
	    //Good or bad for performance? Dunno?
	    new_localh = 1 + max(righth, lefth);
#ifdef CHECK_FIRST
	    if(parent_localh != new_localh) {
#ifdef SEPERATE_BALANCE
	      if(bnode != NULL) {
		TX_STORE(&bnode->localh, new_localh);
	      }
#else
	      TX_STORE(&parent->localh, new_localh);
#endif
	    }
#else
#ifdef SEPERATE_BALANCE
	    if(bnode != NULL) {
	      TX_STORE(&bnode->localh, new_localh);
	    }
#else
	    TX_STORE(&parent->localh, new_localh);
#endif
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

int avl_rotate(avl_node_t *parent, int go_left, avl_node_t *node, free_list_item *free_list) {
  int ret;
  avl_node_t *child_addr = NULL;
  
  //printf("here");
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


int avl_single_rotate(avl_node_t *parent, int go_left, avl_node_t *node, int left_rotate, int right_rotate, avl_node_t **child_addr, free_list_item* free_list) {
  val_t lefth, righth, bal, child_localh;
  avl_node_t *child, *check;
  intptr_t rem;
  int ret;
#ifdef SEPERATE_BALANCE
  balance_node_t *bnode;
#endif

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
#ifdef SEPERATE_BALANCE
      //do update to values here
      bnode = node->bnode;
      lefth = TX_UNIT_LOAD(&bnode->lefth);
      righth = TX_UNIT_LOAD(&bnode->righth);
#else
      lefth = TX_LOAD(&node->lefth);
      righth = TX_LOAD(&node->righth);
#endif
      bal = lefth - righth;
      
      if(bal >= 2 || right_rotate) {
	child = (avl_node_t *)TX_LOAD(&node->left);
	if(child != NULL) {
#ifdef SEPERATE_BALANCE	  
	  if(child->bnode == NULL) {
	    avl_new_balance_node(child, 0);
	    child_localh = 1;
	  } else {
	    child_localh = (val_t)TX_UNIT_LOAD(&child->bnode->localh);
	  }
#else
	  child_localh = (val_t)TX_LOAD(&child->localh);
#endif
	  if(lefth - child_localh == 0) {
	    ret = avl_right_rotate(parent, go_left, node, lefth, righth, child, child_addr, right_rotate, free_list);
	  }
	}
	
      } else if(bal <= -2 || left_rotate) {
	child = (avl_node_t *)TX_LOAD(&node->right);
	if(child != NULL) {
#ifdef SEPERATE_BALANCE
	  if(child->bnode == NULL) {
	    avl_new_balance_node(child, 0);
	    child_localh = 1;
	  } else {
	    child_localh = (val_t)TX_UNIT_LOAD(&child->bnode->localh);
	  }
#else
	  child_localh = (val_t)TX_LOAD(&child->localh);
#endif
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

int avl_right_rotate(avl_node_t *parent, int go_left, avl_node_t *node, val_t lefth, val_t righth, avl_node_t *left_child, avl_node_t **left_child_addr, int do_rotate, free_list_item* free_list) {
  val_t left_lefth, left_righth, left_bal, localh;
  avl_node_t *right_child, *left_right_child, *new_node;
  free_list_item *next_list_item;
#ifdef SEPERATE_BALANCE
  balance_node_t *bnode, *lchild_bnode;
#ifdef SEPERATE_BALANCE2
  balance_node_t *other_bnode;
#endif
#endif
  //printf("Right rotate %d\n", node->key);
#ifdef SEPERATE_BALANCE
  lchild_bnode = left_child->bnode;
  if(lchild_bnode == NULL) {
    left_lefth = 0;
    left_righth = 0;
  } else {
    left_lefth = (val_t)TX_UNIT_LOAD(&lchild_bnode->lefth);
    left_righth = (val_t)TX_UNIT_LOAD(&lchild_bnode->righth);
  }
#else
  left_lefth = (val_t)TX_LOAD(&left_child->lefth);
  left_righth = (val_t)TX_LOAD(&left_child->righth);
#endif

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
#ifdef CHANGE_KEY
  new_node = avl_new_simple_node((val_t)TX_LOAD(&node->val), (val_t)TX_LOAD(&node->key), 1);
#else
  new_node = avl_new_simple_node((val_t)TX_LOAD(&node->val), node->key, 1);
#endif
#else
#ifdef CHANGE_KEY
  new_node = avl_new_simple_node(node->val, (val_t)TX_LOAD(&node->key), 1);
#else
  new_node = avl_new_simple_node(node->val, node->key, 1);
#endif
#endif
#ifdef SEPERATE_BALANCE2
  bnode = avl_new_balance_node(new_node, 1);
  if(left_right_child != NULL) {
    bnode->left = left_right_child->bnode;
  }
  if(right_child != NULL) {
    bnode->right = right_child->bnode;
  }
#endif
  new_node->deleted = (intptr_t)TX_LOAD(&node->deleted);
  new_node->left = left_right_child;
  new_node->right = right_child;


#ifdef SEPERATE_BALANCE
#ifndef SEPERATE_BALANCE2
  bnode = new_node->bnode;
#endif
  bnode->lefth = left_righth;
  bnode->righth = righth;
  bnode->localh = 1 + max(left_righth, righth);
#else
  new_node->lefth = left_righth;
  new_node->righth = righth;
  new_node->localh = 1 + max(left_righth, righth);
#endif
#ifdef SEPERATE_BALANCE
  localh = (val_t)TX_UNIT_LOAD(&node->bnode->localh);
  if(left_bal >= 0) {
    TX_STORE(&lchild_bnode->localh, localh-1);
  } else {
    TX_STORE(&lchild_bnode->localh, localh);
  }
#else
  localh = (val_t)TX_LOAD(&node->localh);
  if(left_bal >= 0) {
    TX_STORE(&left_child->localh, localh-1);
  } else {
    TX_STORE(&left_child->localh, localh);
  }
#endif
  //if(right_child == NULL) {
  TX_STORE(&node->right, new_node);
  //}
  TX_STORE(&node->removed, 1);

  //for garbage cleaning
  next_list_item = (free_list_item *)MALLOC(sizeof(free_list_item));
  next_list_item->next = NULL;
  next_list_item->to_free = node;
  TX_STORE(&free_list->next, next_list_item);

#ifdef SEPERATE_BALANCE
  TX_STORE(&lchild_bnode->righth, bnode->localh);
#else
  TX_STORE(&left_child->righth, new_node->localh);
#endif


  TX_STORE(&left_child->right, new_node);
#ifdef SEPERATE_BALANCE2
  other_bnode = left_child->bnode;
  TX_STORE(&other_bnode->right, bnode);
  TX_STORE(&bnode->parent, other_bnode);
#endif


  if(go_left) {
    TX_STORE(&parent->left, left_child);
#ifdef SEPERATE_BALANCE2
    bnode = parent->bnode;
    TX_STORE(&bnode->left, other_bnode);
    TX_STORE(&other_bnode->parent, bnode);
#endif
  } else {
    TX_STORE(&parent->right, left_child);
#ifdef SEPERATE_BALANCE2
    bnode = parent->bnode;
    TX_STORE(&bnode->right, other_bnode);
    TX_STORE(&other_bnode->parent, bnode);
#endif
  }
  return 1;
}



int avl_left_rotate(avl_node_t *parent, int go_left, avl_node_t *node, val_t lefth, val_t righth, avl_node_t *right_child, avl_node_t **right_child_addr, int do_rotate, free_list_item* free_list) {
  val_t right_lefth, right_righth, right_bal, localh;
  avl_node_t *left_child, *right_left_child, *new_node;
  free_list_item *next_list_item;
#ifdef SEPERATE_BALANCE
  balance_node_t *bnode, *rchild_bnode;
#endif
#ifdef SEPERATE_BALANCE2
  balance_node_t *other_bnode;
#endif

  //printf("Left rotate %d\n", node->key);
#ifdef SEPERATE_BALANCE
  rchild_bnode = right_child->bnode;
  if(rchild_bnode == NULL) {
    right_lefth = 0;
    right_righth = 0;
  } else {
    right_lefth = (val_t)TX_UNIT_LOAD(&rchild_bnode->lefth);
    right_righth = (val_t)TX_UNIT_LOAD(&rchild_bnode->righth);
  }
#else
  right_lefth = (val_t)TX_LOAD(&right_child->lefth);
  right_righth = (val_t)TX_LOAD(&right_child->righth);
#endif

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
#ifdef CHANGE_KEY
  new_node = avl_new_simple_node((val_t)TX_LOAD(&node->val), (val_t)TX_LOAD(&node->key), 1);
#else
  new_node = avl_new_simple_node((val_t)TX_LOAD(&node->val), node->key, 1);
#endif
#else
#ifdef CHANGE_KEY
  new_node = avl_new_simple_node(node->val, (val_t)TX_LOAD(&node->key), 1);
#else
  new_node = avl_new_simple_node(node->val, node->key, 1);
#endif
#endif

#ifdef SEPERATE_BALANCE2
  bnode = avl_new_balance_node(new_node, 1);
  if(right_left_child != NULL) {
    bnode->right = right_left_child->bnode;
  }
  if(bnode->left != NULL) {
    bnode->left = left_child->bnode;
  }
#endif
  new_node->deleted = (intptr_t)TX_LOAD(&node->deleted);
  new_node->right = right_left_child;
  new_node->left = left_child;


#ifdef SEPERATE_BALANCE
#ifndef SEPERATE_BALANCE2
  bnode = new_node->bnode;
#endif
  bnode->righth = right_lefth;
  bnode->lefth = lefth;
  bnode->localh = 1 + max(right_lefth, lefth);
#else
  new_node->righth = right_lefth;
  new_node->lefth = lefth;
  new_node->localh = 1 + max(right_lefth, lefth);
#endif  

#ifdef SEPERATE_BALANCE
  localh = (val_t)TX_UNIT_LOAD(&node->bnode->localh);
  if(right_bal < 0) {
    TX_STORE(&rchild_bnode->localh, localh-1);
  } else {
    TX_STORE(&rchild_bnode->localh, localh);
  }
#else
  localh = (val_t)TX_LOAD(&node->localh);
  if(right_bal < 0) {
    TX_STORE(&right_child->localh, localh-1);
  } else {
    TX_STORE(&right_child->localh, localh);
  }
#endif
  //if(left_child == NULL) {
  TX_STORE(&node->left, new_node);
  //}
  TX_STORE(&node->removed, 1);

  //for garbage cleaning
  next_list_item = (free_list_item *)MALLOC(sizeof(free_list_item));
  next_list_item->next = NULL;
  next_list_item->to_free = node;
  TX_STORE(&free_list->next, next_list_item);

#ifdef SEPERATE_BALANCE
  TX_STORE(&rchild_bnode->lefth, bnode->localh);
#else
  TX_STORE(&right_child->lefth, new_node->localh);
#endif


  TX_STORE(&right_child->left, new_node);
#ifdef SEPERATE_BALANCE2
  other_bnode = right_child->bnode;
  TX_STORE(&other_bnode->left, bnode);
  TX_STORE(&bnode->parent, other_bnode);
#endif


  if(go_left) {
    TX_STORE(&parent->left, right_child);
#ifdef SEPERATE_BALANCE2
    bnode = parent->bnode;
    TX_STORE(&bnode->left, other_bnode);
    TX_STORE(&other_bnode->parent, bnode);
#endif
  } else {
    TX_STORE(&parent->right, right_child);
#ifdef SEPERATE_BALANCE2
    bnode = parent->bnode;
    TX_STORE(&bnode->right, other_bnode);
    TX_STORE(&other_bnode->parent, bnode);
#endif
  }
  return 1;
}


#ifndef SEPERATE_BALANCE2

int avl_propagate(avl_node_t *node, int left, int *should_rotate) {
  avl_node_t *child;
  val_t height, other_height, child_localh = 0, new_localh;
  intptr_t rem;
  int ret = 0;
  int is_reliable = 0;
#ifdef CHECK_FIRST
  val_t localh;
#endif
#ifdef SEPERATE_BALANCE1
  balance_node_t *bnode, *child_bnode;
  bnode = node->bnode;
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
#ifdef SEPERATE_BALANCE1
    localh = (val_t)TX_UNIT_LOAD(&bnode->localh);
#else
    localh = (val_t)TX_UNIT_LOAD(&node->localh);
#endif
#endif

    if(left) {
      child = (avl_node_t *)TX_LOAD(&node->left);
    } else {
      child = (avl_node_t *)TX_LOAD(&node->right);
    }
    
    if(child != NULL) {
#ifdef SEPERATE_BALANCE1
      child_bnode = child->bnode;
#endif
      if(left) {
#ifdef SEPERATE_BALANCE1
	height = (val_t)TX_UNIT_LOAD(&bnode->lefth);
#else
	height = (val_t)TX_UNIT_LOAD(&node->lefth);
#endif
      } else {
#ifdef SEPERATE_BALANCE1
	height = (val_t)TX_UNIT_LOAD(&bnode->righth);
#else
	height = (val_t)TX_UNIT_LOAD(&node->righth);
#endif
      }
      
#ifdef SEPERATE_BALANCE1
      child_localh = (val_t)TX_UNIT_LOAD(&child_bnode->localh);
#else
      child_localh = (val_t)TX_UNIT_LOAD(&child->localh);
#endif
      if(height - child_localh == 0) {
	is_reliable = 1;
      }

      if(left) {
#ifdef SEPERATE_BALANCE1
	other_height = (val_t)TX_UNIT_LOAD(&bnode->righth);
#else
	other_height = (val_t)TX_UNIT_LOAD(&node->righth);
#endif
      } else {
#ifdef SEPERATE_BALANCE1
	other_height = (val_t)TX_UNIT_LOAD(&bnode->lefth);
#else
	other_height = (val_t)TX_UNIT_LOAD(&node->lefth);
#endif
      }

      //check if should rotate
      if(abs(other_height - child_localh) >= 2) {
	*should_rotate = 1;
      }
      
      if(!is_reliable) { 
	if(left) {
#ifdef SEPERATE_BALANCE1
	  TX_STORE(&bnode->lefth, child_localh);
#else
	  TX_STORE(&node->lefth, child_localh);
#endif
	} else {
#ifdef SEPERATE_BALANCE1
	  TX_STORE(&bnode->righth, child_localh);
#else
	  TX_STORE(&node->righth, child_localh);
#endif
	}
      
	//Could also do a read here to check if should change before writing?
	//Good or bad for performance? Dunno?
	new_localh = 1 + max(child_localh, other_height);
#ifdef CHECK_FIRST
	if(localh != new_localh) {
#ifdef SEPERATE_BALANCE1
	  TX_STORE(&bnode->localh, new_localh);
#else
	  TX_STORE(&node->localh, new_localh);
#endif
	  ret = 1;
	}
#else
#ifdef SEPERATE_BALANCE1
	TX_STORE(&bnode->localh, new_localh);
#else
	TX_STORE(&node->localh, new_localh);
#endif
	ret = 1;
#endif
      }
    }
  }
  
  TX_END;
  //printf("Finished prop\n");
  return ret;  
}

#else /* ndef SEPERATE_BALANCE2 */


#ifdef SEPERATE_BALANCE2DEL

 int check_remove_list(avl_intset_t *set, ulong *num_rem, free_list_item *free_list) {
   int i;
   avl_node_t *next, *parent;
   int rem_succs;
   free_list_item *next_list_item;
   balance_node_t *bnode;
   
   for(i = 0; i < set->nb_threads; i++) {
     TX_START(NL);
     parent = (avl_node_t*)TX_UNIT_LOAD(&set->to_remove_parent[i]);
     next = (avl_node_t*)TX_UNIT_LOAD(&set->to_remove[i]);
     TX_END;       
     if(next != NULL && parent != NULL && next != set->to_remove_seen[i]) {
       if(parent->bnode != NULL && next->bnode != NULL) {
	 rem_succs = remove_node(parent, next);
	 //rem_succs = 0;
	 if(rem_succs > 1) {
	   printf("Removed, %d\n", next->key);
	   bnode = next->bnode;
	   if(bnode != NULL) {
	     bnode->left = NULL;
	     bnode->right = NULL;
	     bnode->removed = 1;
	   }
	   *num_rem = *num_rem + 1;
	 
	   //add to the list for garbage collection
	   next_list_item = (free_list_item *)malloc(sizeof(free_list_item));
	   next_list_item->next = NULL;
	   next_list_item->to_free = next;
	   free_list->next = next_list_item;
	   free_list = next_list_item;	 
	 }  
	 set->to_remove_seen[i] = next;
       }
     }
   }
   return 1;
 }

#endif


int avl_propagate(balance_node_t *node, int left, int *should_rotate) {

  balance_node_t *child;
  val_t height, other_height, child_localh = 0, new_localh;
  //intptr_t rem;
  int ret = 0;
  int is_reliable = 0;
#ifdef CHECK_FIRST
  val_t localh;
#endif

  *should_rotate = 0;
  ret = 0;

#ifdef CHECK_FIRST
  //remove this?
  //why work if unit read here? and now down below
  localh = node->localh;
#endif
  
  if(left) {
    child = node->left;
  } else {
    child = node->right;
  }
  
  if(child != NULL) {
    if(left) {
      height = node->lefth;
    } else {
      height = node->righth;
    }
    
    child_localh = child->localh;
    
    if(height - child_localh == 0) {
      is_reliable = 1;
    }
    
    if(left) {
      other_height = node->righth;
    } else {
      other_height = node->lefth;
    }

    //check if should rotate
    if(abs(other_height - child_localh) >= 2) {
      *should_rotate = 1;
    }
    
    if(!is_reliable) { 
      if(left) {
	node->lefth = child_localh;
      } else {
	node->righth = child_localh;
      }
      
      //Could also do a read here to check if should change before writing?
      //Good or bad for performance? Dunno?
      new_localh = 1 + max(child_localh, other_height);
#ifdef CHECK_FIRST
      if(localh != new_localh) {
	node->localh = new_localh;
	ret = 1;
      }
#else
      node->localh = new_localh;
      ret = 1;
#endif
    }
  }
  
  return ret;  

}

#endif /* def SEPERATE_BALANCE2 */

 int recursive_tree_propagate(avl_intset_t *set, free_list_item* free_list) {
  free_list_item *next;
  
  set->current_deleted_count = 0;
  set->current_tree_size = 0;

  //Get to the end of the free list so you can add new elements
  next = free_list;
  while(next->next != NULL) {
    next = next->next;
  }

#ifdef SEPERATE_BALANCE2
  recursive_node_propagate(set, set->root->bnode, NULL, next);
#else
  recursive_node_propagate(set, set->root, NULL, next);
#endif
  //printf("Finished full prop\n");
  //printf("deleted count: %lu\n", *deleted_count);
  set->deleted_count = set->current_deleted_count;
  set->tree_size = set->current_tree_size;

  if(set->deleted_count > set->tree_size * ACTIVE_REM_CONSTANT) {
    if(!set->active_remove) {
      set->active_remove = 1;
      //printf("active remove\n");
    }
  } else {
    if(set->active_remove) {
      set->active_remove = 0;
      //printf("non active remove\n");
    }
  }

  return 1;
}

#ifdef SEPERATE_BALANCE2

balance_node_t* check_expand(balance_node_t *node, int go_left) {
  avl_node_t *anode;
  balance_node_t *bnode;

  anode = node->anode;

  if(node->removed) {
    return NULL;
  }

  TX_START(NL);
  if(go_left) {
    anode = (avl_node_t*)TX_UNIT_LOAD(&anode->left);
  } else {
    anode = (avl_node_t*)TX_UNIT_LOAD(&anode->right);
  }
#ifdef SEPERATE_BALANCE2NLDEL
  if(anode != NULL) {
    if(TX_UNIT_LOAD(&anode->removed)) {
      anode = NULL;
    }
  }
#endif
  TX_END;

  if(anode != NULL) {
    //printf("expanding %d\n", anode->key);

    bnode = avl_new_balance_node(anode, 0);
    bnode->parent = node;
    if(go_left) {
      node->left = bnode;
    } else {
      node->right = bnode;
    }
    return bnode;
  }

  return NULL;
}


 int recursive_node_propagate(avl_intset_t *set, balance_node_t *node, balance_node_t *parent, free_list_item *free_list) {
   balance_node_t *left, *right, *root;
   int rem_succs;
   intptr_t rem;
   int should_rotatel, should_rotater;
   free_list_item *next_list_item;
   avl_node_t *anode;

  if(node == NULL) {
    return 1;
  }


#ifdef SEPERATE_BALANCE2DEL
  check_remove_list(set, set->nb_removed, free_list);
#endif


  if(node->removed) {
    return 0;
  }

  anode = node->anode;

  TX_START(NL);
  rem = (intptr_t)TX_UNIT_LOAD(&anode->removed);
  TX_END;

  if(rem) {
    node->removed = 1;
    if(parent->left == node) {
      parent->left = NULL;
      parent->lefth = 0;
    } else {
      parent->right = NULL;
      parent->righth = 0;
    }
    return 0;
  }

  //should do this in a transaction?
  if(anode->deleted) {
    (set->current_deleted_count)++;
  } else {
    set->current_tree_size++;
  }
  if(set->current_deleted_count > set->current_tree_size * ACTIVE_REM_CONSTANT) {
    if(!set->active_remove) {
      set->active_remove = 1;
      //printf("active remove\n");
    }
  } else {
    if(set->active_remove) {
      set->active_remove = 0;
      //printf("non active remove\n");
    }
  }

  left = node->left;
  right = node->right;

  /* if(left != NULL) { */
  /*   recursive_node_propagate(set, root, left, node, num, num_suc, num_rot, suc_rot, num_rem, deleted_count, old_deleted_count, free_list); */
  /* } else { */
  /*   if((left = check_expand(node, 1)) != NULL) { */
  /*     recursive_node_propagate(set, root, left, node, num, num_suc, num_rot, suc_rot, num_rem, deleted_count, old_deleted_count, free_list); */
  /*   } */
  /* } */
  /* if(right != NULL) { */
  /*   recursive_node_propagate(set, root, right, node, num, num_suc, num_rot, suc_rot, num_rem, deleted_count, old_deleted_count, free_list); */
  /* } else { */
  /*   if((right = check_expand(node, 0)) != NULL) { */
  /*     recursive_node_propagate(set, root, right, node, num, num_suc, num_rot, suc_rot, num_rem, deleted_count, old_deleted_count, free_list); */
  /*   } */
  /* } */

  if(left == NULL) {
    left = check_expand(node, 1);
  }

  if(right == NULL) {
    right = check_expand(node, 0);
  }

  rem_succs = 0;
  if((left == NULL || right == NULL) && parent != NULL && !parent->removed) {
    rem_succs = remove_node(parent->anode, node->anode);
    if(rem_succs > 1) {

      node->removed = 1;
      if(parent->left == node) {
	parent->left = NULL;
	parent->lefth = 0;
      } else {
	parent->right = NULL;
	parent->righth = 0;
      }
      
      //printf("RemovedB! %d\n", node->anode->key);
      set->nb_removed++;
      
      //add to the list for garbage collection
      next_list_item = (free_list_item *)malloc(sizeof(free_list_item));
      next_list_item->next = NULL;
      next_list_item->to_free = node->anode;
      free_list->next = next_list_item;
      free_list = next_list_item;

      return 1;
    }
  }

  if(left != NULL) {
    recursive_node_propagate(set, left, node, free_list);
  }
  if(right != NULL) {
    recursive_node_propagate(set, right, node, free_list);
  }

  
#ifdef SEPERATE_MAINTENANCE
  usleep((THROTTLE_TIME)/(set->deleted_count + (set->current_deleted_count * set->current_deleted_count) + 1));
  //printf("sleeping %lu, %lu, %lu\n", old_deleted_count, *deleted_count, ((THROTTLE_TIME)/(old_deleted_count + (*deleted_count * *deleted_count) + 1)));
#endif

  root = set->root->bnode;

  //printf("prop %d\n", node->anode->key);
  //should do just left or right rotate here?
  left = node->left;
  
  if(left != NULL && left != root) {
    set->nb_suc_propogated += avl_propagate(left, 1, &should_rotatel);
    set->nb_suc_propogated += avl_propagate(left, 0, &should_rotater);
    if(should_rotatel || should_rotater) {
      //printf("Calling rotate method\n");

      /* if(left->right != NULL) { */
      /* 	anode = left->right->anode; */
      /* 	TX_START(NL); */
      /* 	rem = (intptr_t)TX_UNIT_LOAD(&anode->removed); */
      /* 	TX_END;       */
      /* 	if(rem) { */
      /* 	  left->right->removed = 1; */
      /* 	  left->right = NULL; */
      /* 	  left->righth = 0; */
      /* 	} */
      /* } */
      /* if(left->left != NULL) { */
      /* 	anode = left->left->anode; */
      /* 	TX_START(NL); */
      /* 	rem = (intptr_t)TX_UNIT_LOAD(&anode->removed); */
      /* 	TX_END;       */
      /* 	if(rem) { */
      /* 	  left->left->removed = 1; */
      /* 	  left->left = NULL; */
      /* 	  left->lefth = 0; */
      /* 	} */
      /* } */

      set->nb_suc_rotated += avl_rotate(node->anode, 1, left->anode, free_list);
      set->nb_rotated++;
    }
    set->nb_propogated += 2;
  }
  
  //should do just left or right rotate here?
  right = node->right;
  if(right != NULL && right != root) {
    set->nb_suc_propogated += avl_propagate(right, 1, &should_rotatel);
    set->nb_suc_propogated+= avl_propagate(right, 0, &should_rotater);
    if(should_rotatel || should_rotater) {
      //printf("Calling rotate method\n");

      /* if(right->right != NULL) { */
      /* 	anode = right->right->anode; */
      /* 	TX_START(NL); */
      /* 	rem = (intptr_t)TX_UNIT_LOAD(&anode->removed); */
      /* 	TX_END;       */
      /* 	if(rem) { */
      /* 	  right->right->removed = 1; */
      /* 	  right->right = NULL; */
      /* 	  right->righth = 0; */
      /* 	} */
      /* } */
      /* if(right->left != NULL) { */
      /* 	anode = right->left->anode; */
      /* 	TX_START(NL); */
      /* 	rem = (intptr_t)TX_UNIT_LOAD(&anode->removed); */
      /* 	TX_END;       */
      /* 	if(rem) { */
      /* 	  right->left->removed = 1; */
      /* 	  right->left = NULL; */
      /* 	  right->lefth = 0; */
      /* 	} */
      /* } */

      set->nb_suc_rotated += avl_rotate(node->anode, 0, right->anode, free_list);
      set->nb_rotated++;
    }
    set->nb_propogated += 2;
  }

  return 1;
}


#else

 int recursive_node_propagate(avl_intset_t *set, avl_node_t *node, avl_node_t *parent, free_list_item *free_list) {
   avl_node_t *left, *right, *root;
  intptr_t rem, del;
  int rem_succs;
  int should_rotatel, should_rotater;
  free_list_item *next_list_item;

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

    if(del) {
      (set->current_deleted_count)++;
    }

    rem_succs = 0;
    if((left == NULL || right == NULL) && del && parent != NULL) {
      rem_succs = remove_node(parent, node);
      if(rem_succs > 1) {
	//printf("Removed node in maintenace\n");
	set->nb_removed++;
	
	//add to the list for garbage collection
	next_list_item = (free_list_item *)malloc(sizeof(free_list_item));
	next_list_item->next = NULL;
	next_list_item->to_free = node;
	free_list->next = next_list_item;
	free_list = next_list_item;
	
	return 1;
      }
    }
    
#ifdef SEPERATE_MAINTENANCE
    usleep((THROTTLE_TIME)/(set->deleted_count + 1));
    //printf("sleeping %lu, %lu, %lu\n", old_deleted_count, *deleted_count, ((THROTTLE_TIME)/(old_deleted_count + (*deleted_count * *deleted_count) + 1)));
#endif

    
    if(left != NULL) {
      recursive_node_propagate(set, left, node, free_list);
    }
    if(right != NULL) {
      recursive_node_propagate(set, right, node, free_list);
    }

    root = set->root;

    //should do just left or right rotate here?
    TX_START(NL);
    left = (avl_node_t*)TX_UNIT_LOAD(&node->left);
    rem = (intptr_t)TX_UNIT_LOAD(&node->removed);
    TX_END;
    if(left != NULL && !rem && left != root) {
      set->nb_suc_propogated += avl_propagate(left, 1, &should_rotatel);
      set->nb_suc_propogated += avl_propagate(left, 0, &should_rotater);
      if(should_rotatel || should_rotater) {
	//printf("Calling rotate method\n");
	set->nb_suc_rotated += avl_rotate(node, 1, left, free_list);
	set->nb_rotated++;
      }
      set->nb_propogated += 2;
    }
    
    //should do just left or right rotate here?
    TX_START(NL);
    right = (avl_node_t*)TX_UNIT_LOAD(&node->right);
    rem = (intptr_t)TX_UNIT_LOAD(&node->removed);
    TX_END;
    if(right != NULL && !rem && right != root) {
      set->nb_suc_propogated += avl_propagate(right, 1, &should_rotatel);
      set->nb_suc_propogated += avl_propagate(right, 0, &should_rotater);
      if(should_rotatel || should_rotater) {
	//printf("Calling rotate method\n");
	set->nb_suc_rotated += avl_rotate(node, 0, right, free_list);
	set->nb_rotated++;
      }
      set->nb_suc_propogated += 2;
    }
    
  }

  return 1;
}

#endif

#ifdef KEYMAP

val_t avl_get(val_t key, const avl_intset_t *set) {
  int done;
  intptr_t rem, del;
  avl_node_t *place;
  val_t val;
  val_t k;
  
  //printf("getting %d\n", key);  

  //place = set->root;
  TX_START(NL);
  place = set->root;
  rem = 1;
  done = 0;
  
  while(rem) {
    avl_find(key, &place, &k);
    
    if(k != key) {
      done = 1;
      break;
    }
    rem = (intptr_t)TX_LOAD(&place->removed);
  }
  if(!done) {
    del = (intptr_t)TX_LOAD(&place->deleted);
    val = (val_t)TX_LOAD(&place->val);
  }

  TX_END;


  if(done) {
    //printf("got nothing\n");
    return 0;
  } else if(del) {
    //printf("got nothing\n");
    return 0;
  }
  //printf("got %d\n", val);  

  return val;

}


int avl_update(val_t v, val_t key, const avl_intset_t *set) {
  avl_node_t *place, *next, *new_node;
  intptr_t rem, del;
  int done, go_left = 0;
  int ret;
  val_t k;

  //printf("updating %d\n", key);  

  //place = set->root;
  //next = place;
  TX_START(NL);
  place = set->root;
  next = place;
  ret = 0;
  rem = 1;
  done = 0;
  
  while(rem || next != NULL) {
    avl_find(key, &place, &k);
    rem = (intptr_t)TX_LOAD(&place->removed);
    if(!rem) {
      if(k == key) {
	done = 1;
	break;
      } else if(key > k){
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


#ifdef SEQUENTIAL

val_t avl_seq_get(avl_intset_t *set, val_t key, int transactional)
{
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
  
  //printf("seq update %d\n", key);

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
      ret = avl_req_seq_update(node, node->right, val, key, 0, success);
    } else {
      ret = avl_req_seq_update(node, node->left, val, key, 1, success);
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


#endif

#endif

/* #ifdef TINY10BNO */

/* void test_maintenance(avl_intset_t *tree) { */
/*   int done; */
/*   int i; */
/*   ulong *tmp; */
/*   free_list_item *next, *tmp_item;//, **t_list_items; */
/*   //avl_intset_t *tree; */
/*   //manager_t *mgrPtr; */
/*   long id; */
/*   ulong nb_propagated, nb_suc_propagated, nb_rotated, nb_suc_rotated, nb_removed; */
/* #ifdef THROTTLE_MAINTENANCE */
/*   //ulong nb_modified, last_modified = 0; */
/* #endif */
/*   ulong *t_nb_trans; */
/*   ulong *t_nb_trans_old; */

/*   id = thread_getId(); */
/*   mgrPtr = (manager_t *)data; */

/*   if(id == mgrPtr->nb_clients) { */
/*     tree = (avl_intset_t*)mgrPtr->carTablePtr; */
/*     //printf("car table start\n"); */
/*   } else if(id == mgrPtr->nb_clients+1) { */
/*     tree = (avl_intset_t*)mgrPtr->roomTablePtr; */
/*     //printf("room table start\n"); */
/*   } else if(id == mgrPtr->nb_clients+2) { */
/*     tree = (avl_intset_t*)mgrPtr->flightTablePtr; */
/*     //printf("flight table start\n"); */
/*   } else if(id == mgrPtr->nb_clients+3) { */
/*     tree = (avl_intset_t*)mgrPtr->customerTablePtr; */
/*     //printf("customer table start\n"); */
/*   } else {  */
/*     printf("BAD table start\n");    */
/*   } */

/*   t_nb_trans = (ulong *)malloc(mgrPtr->nb_clients * sizeof(ulong)); */
/*   t_nb_trans_old = (ulong *)malloc(mgrPtr->nb_clients * sizeof(ulong)); */
  
/*   /\* Create transaction *\/ */
/*   TM_THREAD_ENTER(); */
/*   /\* Wait on barrier *\/ */
/*   //barrier_cross(d->barrier); */
	
/*   /\* Is the first op an update? *\/ */
/*   //unext = (rand_range_re(&d->seed, 100) - 1 < d->update); */
  
/*   while(!thread_getDone()) { */

/*     //printf("m"); */
/*       //do maintenance, but only when there have been enough modifications */

/*       //need to do garbage collection here */
/*       done = 0; */
/*       for(i = 0; i < mgrPtr->nb_clients; i++) { */
/* 	t_nb_trans[i] = mgrPtr->nb_committed[i]; */
/* 	if(t_nb_trans[i] == t_nb_trans_old[i]) { */
/* 	  done = 1; */
/* 	  //printf("Done at i:%d, %d, %d\n", i, d->t_nb_trans[i], d->t_nb_trans_old[i]); */
/* 	  break; */
/* 	} */
/*       } */
/*       if(!done) { */
/* 	tmp = (t_nb_trans_old); */
/* 	(t_nb_trans_old) = (t_nb_trans); */
/* 	(t_nb_trans) = tmp; */
/* 	next = tree->free_list->next; */
/* 	//printf("Starting free\n"); */
/* 	while(next != NULL) { */
/* 	  //printf("FREEING\n"); */
/* 	  free(next->to_free); */
/* 	  tmp_item = next; */
/* 	  next = next->next; */
/* 	  free(tmp_item); */
/* 	} */
/* 	tree->free_list->next = NULL; */

/* 	/\* //now do the frees for the ones removed in the non-maint threads *\/ */
/* 	/\* for(i = 0; i < mgrPtr->nb_clients; i++) { *\/ */
/* 	/\*   while(t_free_list[i] != t_list_items[i]) { *\/ */
/* 	/\*     if(t_free_list[i]->to_free != NULL) { *\/ */
/* 	/\*       //printf("freeing %d\n", d->t_free_list[i]->to_free->key); *\/ */
/* 	/\*       free(tree->t_free_list[i]->to_free); *\/ */
/* 	/\*     } *\/ */
/* 	/\*     tmp_item = tree->t_free_list[i]; *\/ */
/* 	/\*     tree->t_free_list[i] = tree->t_free_list[i]->next; *\/ */
/* 	/\*     free(tmp_item); *\/ */
/* 	/\*   } *\/ */
/* 	/\*   //update the t_list *\/ */
/* 	/\*   t_list_items[i] = tree->t_data[i].free_list; *\/ */
/* 	/\*   //t_list_items[i] = (free_list_item *)TX_UNIT_LOAD(d->t_data[i].free_list); *\/ */
/* 	/\* } *\/ */
/*       } */
      

/*       recursive_tree_propagate(tree, &nb_propagated, &nb_suc_propagated, &nb_rotated, &nb_suc_rotated, &nb_removed, tree->free_list); */



/* #ifdef THROTTLE_MAINTENANCE */
/* /\*       nb_modified = 0; *\/ */
/* /\*       while(nb_modified < (THROTTLE_NUM + last_modified)) { *\/ */
/* /\* 	nb_modified = 0; *\/ */
/* /\* 	for(i = 0; i < d->nb_threads; i++) { *\/ */
/* /\* 	  nb_modified += d->t_data[i].nb_modifications; *\/ */
/* /\* 	} *\/ */
/* /\* 	usleep(THROTTLE_TIME); *\/ */

/* /\* #ifdef ICC *\/ */
/* /\* 	if(stop != 0) break; *\/ */
/* /\* #else *\/ */
/* /\* 	if(AO_load_full(&stop) != 0) break; *\/ */
/* /\* #endif /\\* ICC *\\/ *\/ */
	
/* /\*       } *\/ */
/* /\*       last_modified = nb_modified; *\/ */

/* usleep(THROTTLE_TIME); */

/* #endif */

      
/*   } */
  
/*   /\* Free transaction *\/ */
/*   //printf("Exiting maint\n"); */
/*   TM_THREAD_EXIT(); */
  
/* } */


/* #endif */







#ifdef SEPERATE_MAINTENANCE


void do_maintenance_thread(avl_intset_t *tree) {

  int done;
  int i;
  ulong *tmp;
  free_list_item *next, *tmp_item;//, **t_list_items;
  long nb_threads;
  ulong *nb_committed;
  //ulong nb_propagated, nb_suc_propagated, nb_rotated, nb_suc_rotated, nb_removed, deleted_count;
  ulong *t_nb_trans;
  ulong *t_nb_trans_old;

  nb_threads = tree->nb_threads;
  t_nb_trans = tree->t_nbtrans;
  t_nb_trans_old = tree->t_nbtrans_old;
  nb_committed = tree->nb_committed;
  
  //do maintenance, but only when there have been enough modifications
  //not doing this here, but maybe should?
  

  //check if you can free removed nodes
  done = 0;
  for(i = 0; i < nb_threads; i++) {
    t_nb_trans[i] = nb_committed[i];
    if(t_nb_trans[i] == t_nb_trans_old[i]) {
      done = 1;
      //printf("Done at i:%d, new %d, old %d\n", i, t_nb_trans[i], t_nb_trans_old[i]);
      break;
    } else {
      //printf("Not Done at i:%d, new %d, old %d\n", i, t_nb_trans[i], t_nb_trans_old[i]);
    }
  }
  if(!done) {
    //free removed nodes
    tmp = (t_nb_trans_old);
    (tree->t_nbtrans_old) = (t_nb_trans);
    (tree->t_nbtrans) = tmp;
    next = tree->free_list->next;
    //printf("Starting free\n");
    while(next != NULL) {

      //printf("FREEING\n");
      //free(next->to_free);
      tmp_item = next;
      next = next->next;
      
      //free(tmp_item);
    }
    tree->free_list->next = NULL;
    
#ifndef SEPERATE_BALANCE2
    //now do the frees for the ones removed in the non-maint threads
    for(i = 0; i < tree->nb_threads; i++) {
      while(tree->maint_list_start[i] != tree->maint_list_end[i]) {
	if(tree->maint_list_start[i]->to_free != NULL) {
	  //printf("freeing %d\n", tree->maint_list_start[i]->to_free->key);
	  //free(tree->maint_list_start[i]->to_free);
	}
	tmp_item = tree->maint_list_start[i];
	tree->maint_list_start[i] = tree->maint_list_start[i]->next;
	//free(tmp_item);
      }
      //update the t_list
      //tree->maint_list_end[i] = tree->t_free_list[i];
      TX_START(NL);
      tree->maint_list_end[i] = (free_list_item*)TX_UNIT_LOAD(&tree->t_free_list[i]);
      TX_END;
    }
#endif
  }
  
  recursive_tree_propagate(tree, tree->free_list);
  
}

  
#else

void do_maintenance(avl_intset_t *tree) {
  int done;
  int i;
  ulong *tmp;
  free_list_item *next, *tmp_item;//, **t_list_items;
  long nb_threads;
  ulong *nb_committed;
  //avl_intset_t *tree;
  //manager_t *mgrPtr;
  long id;
  //ulong nb_propagated, nb_suc_propagated, nb_rotated, nb_suc_rotated, nb_removed, deleted_count;
#ifdef THROTTLE_MAINTENANCE
  //ulong nb_modified, last_modified = 0;
#endif
  ulong *t_nb_trans;
  ulong *t_nb_trans_old;


  nb_threads = tree->nb_threads;
  t_nb_trans = tree->t_nbtrans;
  t_nb_trans_old = tree->t_nbtrans_old;
  nb_committed = tree->nb_committed;

  //TODO FIX THIS SHOULD NOT BE 0
  id = 0;
  //id = thread_getId();
  
  //need to do garbage collection here
  done = 0;
  for(i = 0; i < nb_threads; i++) {
    t_nb_trans[i] = nb_committed[i];
    if(t_nb_trans[i] == t_nb_trans_old[i]) {
      done = 1;
      //printf("Done at i:%d, %d\n", i, thread_getId());
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
   
#ifndef SEPERATE_BALANCE2 
    //free the ones from your own main thread
    next = tree->t_free_list[id]->next;
    //printf("id %d\n", id);
    while(next != NULL) {
      if(next->to_free != NULL) {
	//printf("freeing %d\n", next->to_free->key);
	free(next->to_free);
      }
      tmp_item = next;
      next = next->next;
      free(tmp_item);
    }
    tree->t_free_list[id]->next = NULL;
    //printf("iddone %d\n", id);
#endif

  }
  
  
  recursive_tree_propagate(tree, tree->free_list);
  
  
}


void check_maintenance(avl_intset_t *tree) {
  long id;
  long nextHelp;

  //TODO FIX THIS SHOULD NOT BE 0
  id = 0;
  //id = thread_getId();
  nextHelp = tree->next_maintenance;
  
  //printf("next_maint %d %d\n", tree->next_maintenance, tree->nb_threads);

  if(nextHelp == id) {
    if(tree->nb_committed[id] >= THROTTLE_UPDATE + tree->nb_committed_old[id]) {
      //printf("yes doing maint %d\n", id);
      do_maintenance(tree);
      tree->nb_committed_old[id] = tree->nb_committed[id];
      //printf("done doing maint %d\n", id);
    } else {
      //printf("not doing maint %d\n", id);
    }
    tree->next_maintenance = (nextHelp + 1) % tree->nb_threads;
  }
  
}


#endif





#endif

