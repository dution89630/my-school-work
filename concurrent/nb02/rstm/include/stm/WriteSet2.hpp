/**
 *  Copyright (C) 2011
 *  University of Rochester Department of Computer Science
 *    and
 *  Lehigh University Department of Computer Science and Engineering
 *
 * License: Modified BSD
 *          Please see the file LICENSE.RSTM for licensing information
 */

/**
 *  The RSTM backends that use redo logs all rely on this datastructure,
 *  which provides O(1) clear, insert, and lookup by maintaining a hashed
 *  index into a vector.
 */

#ifndef WRITESET2_HPP__
#define WRITESET2_HPP__

#include <stm/config.h>
#include "stm/MiniVector.hpp"
#include "stm/WriteSet.hpp"

#ifdef STM_CC_SUN
#include <string.h>
#include <stdlib.h>
#else
#include <cstdlib>
#include <cstring>
#endif
#include <cassert>

namespace stm
{

  /**
   *  The write set is an indexed array of WriteSetEntry elements.  As with
   *  MiniVector, we make sure that certain expensive but rare functions are
   *  never inlined.
   */
  class WriteSet2
  {
      /***  data type for the index */
      struct index_t
      {
          volatile size_t version;
          volatile void*  address;
          volatile size_t index;

          index_t() : version(0), address(NULL), index(0) { }
      };

      volatile index_t* index;                             // hash entries
      volatile size_t   shift;                             // for the hash function
      volatile size_t   ilength;                           // max size of hash
      volatile size_t   version;                           // version for fast clearing

      volatile WriteSetEntry* list;                        // the array of actual data
      volatile size_t   capacity;                          // max array size
      volatile size_t   lsize;                             // elements in the array


      /**
       *  hash function is straight from CLRS (that's where the magic
       *  constant comes from).
       */
      size_t hash(void* const key) const
      {
          static const unsigned long long s = 2654435769ull;
          const unsigned long long r = ((unsigned long long)key) * s;
          return (size_t)((r & 0xFFFFFFFF) >> shift);
      }

      /**
       *  This doubles the size of the index. This *does not* do anything as
       *  far as actually doing memory allocation. Callers should delete[]
       *  the index table, increment the table size, and then reallocate it.
       */
      size_t doubleIndexLength();

      /**
       *  Supporting functions for resizing.  Note that these are never
       *  inlined.
       */
      void rebuild();
      void resize();
      void reset_internal();

    public:


    //For lock-free
    volatile WriteSet2* next;
    volatile bool done;
    volatile uint32_t writer;
    volatile uint32_t time;


      WriteSet2();
      WriteSet2(const size_t initial_capacity);
      ~WriteSet2();

      void init();


      /**
       *  Search function.  The log is an in/out parameter, and the bool
       *  tells if the search succeeded. When we are byte-logging, the log's
       *  mask is updated to reflect the bytes in the returned value that are
       *  valid. In the case that we don't find anything, the mask is set to 0.
       */
      bool find(WriteSetEntry& log) const
      {
	//if(index == NULL) this.init();
          size_t h = hash(log.addr);

          while (index[h].version == version) {
              if (index[h].address != log.addr) {
                  // continue probing
                  h = (h + 1) % ilength;
                  continue;
              }
#if defined(STM_WS_WORDLOG)
              log.val = list[index[h].index].val;
              return true;
#elif defined(STM_WS_BYTELOG)
              // Need to intersect the mask to see if we really have a match. We
              // may have a full intersection, in which case we can return the
              // logged value. We can have no intersection, in which case we can
              // return false. We can also have an awkward intersection, where
              // we've written part of what we're trying to read. In that case,
              // the "correct" thing to do is to read the word from memory, log
              // it, and merge the returned value with the partially logged
              // bytes.
              WriteSetEntry& entry = list[index[h].index];
              if (__builtin_expect((log.mask & entry.mask) == 0, false)) {
                  log.mask = 0;
                  return false;
              }

              // The update to the mask transmits the information the caller
              // needs to know in order to distinguish between a complete and a
              // partial intersection.
              log.val = entry.val;
              log.mask = entry.mask;
              return true;
#else
#error "Preprocessor configuration error."
#endif
          }

#if defined(STM_WS_BYTELOG)
          log.mask = 0x0;
#endif
          return false;
      }


      /**
       *  Encapsulate writeback in this routine, so that we can avoid making
       *  modifications to lots of STMs when we need to change writeback for a
       *  particular compiler.
       */
#if !defined(STM_PROTECT_STACK)
      TM_INLINE void writeback()
      {
#else
      TM_INLINE void writeback(void** upper_stack_bound)
      {
#endif
          for (iterator i = begin(), e = end(); i != e; ++i)
          {
#ifdef STM_PROTECT_STACK
              // See if this falls into the protected stack region, and avoid
              // the writeback if that is the case. The filter call will update
              // a byte-log's mask if there is an awkward intersection.
              //
              void* top_of_stack;
              if (i->filter(&top_of_stack, upper_stack_bound))
                  continue;
#endif
              i->writeback();
          }
      }

#if !defined(STM_PROTECT_STACK)
      TM_INLINE void writebackatomic()
      {
#else
      TM_INLINE void writebackatomic(void** upper_stack_bound)
      {
#endif
          for (iterator i = begin(), e = end(); i != e; ++i)
          {
#ifdef STM_PROTECT_STACK
              // See if this falls into the protected stack region, and avoid
              // the writeback if that is the case. The filter call will update
              // a byte-log's mask if there is an awkward intersection.
              //
              void* top_of_stack;
              if (i->filter(&top_of_stack, upper_stack_bound))
                  continue;
#endif
              i->writebackatomic();
          }
      }



      /*** size() lets us know if the transaction is read-only */
      size_t size() const { return lsize; }

      /**
       *  We use the version number to reset in O(1) time in the common case
       */
      void reset()
      {
          lsize    = 0;
          version += 1;

          // check overflow
          if (version != 0)
              return;
          reset_internal();
      }

      /*** Iterator interface: iterate over the list, not the index */
      typedef WriteSetEntry* iterator;
      iterator begin() const { return (WriteSetEntry*)list; }
      iterator end()   const { return (WriteSetEntry*)(list + lsize); }
  };





  // class NOrec2ListEntry {
  //   volatile bool done;
  //   volatile bool taken;
  //   WriteSet writes;

  //   NOrec2ListEntry() : done(false), taken(false), writes(64) { }

  // };


  // struct NOrec2List : public MiniVector<WriteSet2*> {
  //     NOrec2List(const unsigned long cap) : MiniVector<WriteSet2*>(cap) {
  //     }
  // };


}

#endif // WRITESET_HPP__
