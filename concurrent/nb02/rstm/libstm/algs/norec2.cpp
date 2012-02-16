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
 *  NOrec Implementation
 *
 *    This STM was published by Dalessandro et al. at PPoPP 2010.  The
 *    algorithm uses a single sequence lock, along with value-based validation,
 *    for concurrency control.  This variant offers semantics at least as
 *    strong as Asymmetric Lock Atomicity (ALA).
 */

#include "../cm.hpp"
#include "algs.hpp"
#include "RedoRAWUtils.hpp"

// Don't just import everything from stm. This helps us find bugs.
using stm::TxThread;
using stm::WriteSet;
using stm::timestamp;
using stm::WriteSetEntry;
using stm::ValueList;
using stm::ValueListEntry;
using stm::start_writeset;

namespace {

  const uintptr_t VALIDATION_FAILED = 1;
  NOINLINE uintptr_t validate(TxThread*);
  bool irrevoc(STM_IRREVOC_SIG(,));
  void onSwitchTo();


  template <class CM>
  struct NOrec2_Generic
  {
      static TM_FASTCALL bool begin(TxThread*);
      static TM_FASTCALL void commit(STM_COMMIT_SIG(,));
      static TM_FASTCALL void commit_ro(STM_COMMIT_SIG(,));
      static TM_FASTCALL void commit_rw(STM_COMMIT_SIG(,));
      static TM_FASTCALL void* read_ro(STM_READ_SIG(,,));
      static TM_FASTCALL void* read_rw(STM_READ_SIG(,,));
      static TM_FASTCALL void write_ro(STM_WRITE_SIG(,,,));
      static TM_FASTCALL void write_rw(STM_WRITE_SIG(,,,));
      static stm::scope_t* rollback(STM_ROLLBACK_SIG(,,,));
      static void initialize(int id, const char* name);
  };

  uintptr_t
  validate(TxThread* tx)
  {
      while (true) {
          // read the lock until it is even
	WriteSet* s = (WriteSet*)(timestamp.val);
          if (!(s->done))
              continue;

          // check the read set
          CFENCE;
          // don't branch in the loop---consider it backoff if we fail
          // validation early
          bool valid = true;
          foreach (ValueList, i, tx->vlist)
              valid &= i->isValid();

          if (!valid)
              return VALIDATION_FAILED;

          // restart if timestamp changed during read set iteration
          CFENCE;
          if (timestamp.val == (uintptr_t)s)
	    return (uintptr_t)s;
      }
  }

  bool
  irrevoc(STM_IRREVOC_SIG(tx,upper_stack_bound))
  {
    while (!bcasptr(&timestamp.val, tx->start_time, tx->current_writes))
          if ((tx->start_time = validate(tx)) == VALIDATION_FAILED)
              return false;

      // redo writes
      tx->writes.writeback(STM_WHEN_PROTECT_STACK(upper_stack_bound));

      // Release the sequence lock, then clean up
      CFENCE;
      //bcasptr(&timestamp.val, timestamp.val, tx->start_time + 2);
      //timestamp.val = tx->start_time + 2;
      tx->vlist.reset();
      tx->writes.reset();
      return true;
  }

  void
  onSwitchTo() {
      // We just need to be sure that the timestamp is not odd, or else we will
      // block.  For safety, increment the timestamp to make it even, in the event
      // that it is odd.
      printf("in onSwitchTo\n");
      timestamp.val = (uintptr_t)&start_writeset;
      // if (timestamp.val & 1)
      //     ++timestamp.val;
  }


  template <typename CM>
  void
  NOrec2_Generic<CM>::initialize(int id, const char* name)
  {
      // set the name
      stm::stms[id].name = name;

      // set the pointers
      stm::stms[id].begin     = NOrec2_Generic<CM>::begin;
      stm::stms[id].commit    = NOrec2_Generic<CM>::commit_ro;
      stm::stms[id].read      = NOrec2_Generic<CM>::read_ro;
      stm::stms[id].write     = NOrec2_Generic<CM>::write_ro;
      stm::stms[id].irrevoc   = irrevoc;
      stm::stms[id].switcher  = onSwitchTo;
      stm::stms[id].privatization_safe = true;
      stm::stms[id].rollback  = NOrec2_Generic<CM>::rollback;

      //new to set the start pointer
      timestamp.val = (uintptr_t)&start_writeset;
  }

  template <class CM>
  bool
  NOrec2_Generic<CM>::begin(TxThread* tx)
  {
      // Originally, NOrec2 required us to wait until the timestamp is odd
      // before we start.  However, we can round down if odd, in which case
      // we don't need control flow here.

      // Sample the sequence lock, if it is even decrement by 1
      tx->start_time = timestamp.val;// & ~(1L);

      // notify the allocator
      tx->allocator.onTxBegin();

      tx->current_writes = tx->n2list.get(tx->n2listloc);
      
      if(!tx->current_writes->done) printf("Looping and not done\n");
      tx->current_writes->done = false;
      tx->current_writes->reset();
      // notify CM
      CM::onBegin(tx);

      return false;
  }

  template <class CM>
  void
  NOrec2_Generic<CM>::commit(STM_COMMIT_SIG(tx,upper_stack_bound))
  {
      // From a valid state, the transaction increments the seqlock.  Then it
      // does writeback and increments the seqlock again

      // read-only is trivially successful at last read
      //if (!tx->norec2_desc->writes.size()) {
      if (!tx->current_writes->size()) {
          CM::onCommit(tx);
          tx->vlist.reset();
          OnReadOnlyCommit(tx);
          return;
      }

      // get the lock and validate (use RingSTM obstruction-free technique)
      while (!bcasptr(&timestamp.val, tx->start_time, tx->current_writes))
	if ((tx->start_time = validate(tx)) == VALIDATION_FAILED) {
	  tx->tmabort(tx);
	}

      tx->current_writes->writeback(STM_WHEN_PROTECT_STACK(upper_stack_bound));
      tx->current_writes->done = 1;
      tx->n2listloc = (tx->n2listloc + 1) % tx->n2list.size();
      //tx->norec2_desc->writes.writeback(STM_WHEN_PROTECT_STACK(upper_stack_bound));

      // Release the sequence lock, then clean up
      CFENCE;
      //bcasptr(&timestamp.val, timestamp.val, tx->start_time + 2);
      //timestamp.val = tx->start_time + 2;
      CM::onCommit(tx);
      tx->vlist.reset();
      //tx->writes.reset();
      OnReadWriteCommit(tx);
  }

  template <class CM>
  void
  NOrec2_Generic<CM>::commit_ro(STM_COMMIT_SIG(tx,))
  {
      // Since all reads were consistent, and no writes were done, the read-only
      // NOrec2 transaction just resets itself and is done.
      CM::onCommit(tx);
      tx->vlist.reset();
      OnReadOnlyCommit(tx);
  }

  template <class CM>
  void
  NOrec2_Generic<CM>::commit_rw(STM_COMMIT_SIG(tx,upper_stack_bound))
  {
      // From a valid state, the transaction increments the seqlock.  Then it does
      // writeback and increments the seqlock again

      // get the lock and validate (use RingSTM obstruction-free technique)
    while (!bcasptr(&timestamp.val, tx->start_time, tx->current_writes))
	if ((tx->start_time = validate(tx)) == VALIDATION_FAILED) {
	  tx->tmabort(tx);
	}

      tx->current_writes->writeback(STM_WHEN_PROTECT_STACK(upper_stack_bound));
      tx->current_writes->done = 1;
      tx->n2listloc = (tx->n2listloc + 1) % tx->n2list.size();
      //tx->norec2_desc->writes.writeback(STM_WHEN_PROTECT_STACK(upper_stack_bound));

      // Release the sequence lock, then clean up
      CFENCE;
      //bcasptr(&timestamp.val, timestamp.val, tx->start_time + 2);
      //timestamp.val = tx->start_time + 2;

      // notify CM
      CM::onCommit(tx);

      tx->vlist.reset();
      //tx->writes.reset();

      // This switches the thread back to RO mode.
      OnReadWriteCommit(tx, read_ro, write_ro, commit_ro);
  }

  template <class CM>
  void*
  NOrec2_Generic<CM>::read_ro(STM_READ_SIG(tx,addr,mask))
  {
      // A read is valid iff it occurs during a period where the seqlock does
      // not change and is even.  This code also polls for new changes that
      // might necessitate a validation.

      // read the location to a temp
      void* tmp = *addr;
      CFENCE;

      // if the timestamp has changed since the last read, we must validate and
      // restart this read
      while (tx->start_time != timestamp.val) {
          if ((tx->start_time = validate(tx)) == VALIDATION_FAILED)
              tx->tmabort(tx);
          tmp = *addr;
          CFENCE;
      }

      // log the address and value
      tx->vlist.insert(STM_VALUE_LIST_ENTRY(addr, tmp, mask));
      return tmp;
  }

  template <class CM>
  void*
  NOrec2_Generic<CM>::read_rw(STM_READ_SIG(tx,addr,mask))
  {
      // check the log for a RAW hazard, we expect to miss
      WriteSetEntry log(STM_WRITE_SET_ENTRY(addr, NULL, mask));
      //bool found = tx->norec2_desc->writes.find(log);
      bool found = tx->current_writes->find(log);
      REDO_RAW_CHECK(found, log, mask);

      // Use the code from the read-only read barrier. This is complicated by
      // the fact that, when we are byte logging, we may have successfully read
      // some bytes from the write log (if we read them all then we wouldn't
      // make it here). In this case, we need to log the mask for the rest of the
      // bytes that we "actually" need, which is computed as bytes in mask but
      // not in log.mask. This is only correct because we know that a failed
      // find also reset the log.mask to 0 (that's part of the find interface).
      void* val = read_ro(tx, addr STM_MASK(mask & ~log.mask));
      REDO_RAW_CLEANUP(val, found, log, mask);
      return val;
  }

  template <class CM>
  void
  NOrec2_Generic<CM>::write_ro(STM_WRITE_SIG(tx,addr,val,mask))
  {
      // buffer the write, and switch to a writing context
      tx->current_writes->insert(WriteSetEntry(STM_WRITE_SET_ENTRY(addr, val, mask)));

      //tx->norec2_desc->writes.insert(WriteSetEntry(STM_WRITE_SET_ENTRY(addr, val, mask)));
      OnFirstWrite(tx, read_rw, write_rw, commit_rw);
  }

  template <class CM>
  void
  NOrec2_Generic<CM>::write_rw(STM_WRITE_SIG(tx,addr,val,mask))
  {
      // just buffer the write
      tx->current_writes->insert(WriteSetEntry(STM_WRITE_SET_ENTRY(addr, val, mask)));
      //tx->norec2_desc->writes.insert(WriteSetEntry(STM_WRITE_SET_ENTRY(addr, val, mask)));
  }

  template <class CM>
  stm::scope_t*
  NOrec2_Generic<CM>::rollback(STM_ROLLBACK_SIG(tx, upper_stack_bound, except, len))
  {
    
      stm::PreRollback(tx);

      // notify CM
      CM::onAbort(tx);

      // Perform writes to the exception object if there were any... taking the
      // branch overhead without concern because we're not worried about
      // rollback overheads.
      STM_ROLLBACK(tx->writes, upper_stack_bound, except, len);

      tx->vlist.reset();
      tx->current_writes->reset();
      return stm::PostRollback(tx, read_ro, write_ro, commit_ro);
  }
} // (anonymous namespace)

// Register NOrec2 initializer functions. Do this as declaratively as
// possible. Remember that they need to be in the stm:: namespace.
#define FOREACH_NOREC2(MACRO)                    \
    MACRO(NOrec2, HyperAggressiveCM)             \
    MACRO(NOrec2Hour, HourglassCM)               \
    MACRO(NOrec2Backoff, BackoffCM)              \
    MACRO(NOrec2HB, HourglassBackoffCM)

#define INIT_NOREC2(ID, CM)                      \
    template <>                                 \
    void initTM<ID>() {                         \
        NOrec2_Generic<CM>::initialize(ID, #ID);     \
    }

namespace stm {
  FOREACH_NOREC2(INIT_NOREC2)
}

#undef FOREACH_NOREC2
#undef INIT_NOREC2
