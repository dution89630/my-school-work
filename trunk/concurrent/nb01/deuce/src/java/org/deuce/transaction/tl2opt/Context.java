package org.deuce.transaction.tl2opt;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.deuce.transaction.TransactionException;
import org.deuce.transaction.tl2.field.BooleanWriteFieldAccess;
import org.deuce.transaction.tl2.field.ByteWriteFieldAccess;
import org.deuce.transaction.tl2.field.CharWriteFieldAccess;
import org.deuce.transaction.tl2.field.DoubleWriteFieldAccess;
import org.deuce.transaction.tl2.field.FloatWriteFieldAccess;
import org.deuce.transaction.tl2.field.IntWriteFieldAccess;
import org.deuce.transaction.tl2.field.LongWriteFieldAccess;
import org.deuce.transaction.tl2.field.ObjectWriteFieldAccess;
import org.deuce.transaction.tl2.field.ReadFieldAccess;
import org.deuce.transaction.tl2.field.ShortWriteFieldAccess;
import org.deuce.transaction.tl2.field.WriteFieldAccess;
import org.deuce.transaction.tl2.pool.Pool;
import org.deuce.transaction.tl2.pool.ResourceFactory;
import org.deuce.transform.Exclude;
import org.deuce.trove.TObjectProcedure;

/**
 * TL2 implementation
 *
 * @author	Guy Korland
 * @since	1.0
 */
@Exclude
final public class Context implements org.deuce.transaction.Context{
	
	final static AtomicInteger clock = new AtomicInteger( 0);

	final private ReadSet readSet = new ReadSet();
	final private WriteSet writeSet = new WriteSet();
		
	private ReadFieldAccess currentReadFieldAccess = null;

	//Used by the thread to mark locks it holds.
	final private byte[] locksMarker = new byte[LockTable.LOCKS_SIZE /8 + 1];
	
	//Marked on beforeRead, used for the double lock check
	private int localClock;
	private int lastReadLock;
	private CommitEnum commitEnum;
	
	//Global lock used to allow only one irrevocable transaction solely. 
	final private static ReentrantReadWriteLock irrevocableAccessLock = new ReentrantReadWriteLock();
	private boolean irrevocableState = false;
	
	final private LockProcedure lockProcedure = new LockProcedure(locksMarker);
	
	final private TObjectProcedure<WriteFieldAccess> putProcedure = new TObjectProcedure<WriteFieldAccess>(){

		@Override
		public boolean execute(WriteFieldAccess writeField) {
			writeField.put();
			return true;
		}
		
	};
	
	public Context(){
		this.localClock = clock.get();
	}
	
	public void init(int atomicBlockId, String metainf){
		this.commitEnum = CommitEnum.NotCommited;
		this.readSet.clear(); 
		this.writeSet.clear();
		this.currentReadFieldAccess = readSet.getNextNoIncrement();
		
		this.objectPool.clear();
		this.booleanPool.clear();
		this.bytePool.clear();
		this.charPool.clear();
		this.shortPool.clear();
		this.intPool.clear();
		this.longPool.clear();
		this.floatPool.clear();
		this.doublePool.clear();
		
		//Lock according to the transaction irrevocable state
		if(irrevocableState)
			irrevocableAccessLock.writeLock().lock();
		else
			irrevocableAccessLock.readLock().lock();
		
		this.localClock = clock.get();	
	}
	
	public boolean commit(){
		try{

			if (commitEnum != CommitEnum.NotCommited)
				if (commitEnum == CommitEnum.CommitOK)
					return true;
				else
					return false;

			if (writeSet.isEmpty()) // if the writeSet is empty no need to lock a thing. 
			{
				commitEnum = CommitEnum.CommitOK;
				return true;        
			}

			try
			{
				// pre commit validation phase
				writeSet.forEach(lockProcedure);
				readSet.checkClock(localClock);
			}
			catch( TransactionException exception){
				lockProcedure.unlockAll();
				commitEnum = CommitEnum.CommitFailed;
				return false;
			}

			// commit new values and release locks
			writeSet.forEach(putProcedure);
			lockProcedure.setAndUnlockAll();
			commitEnum = CommitEnum.CommitOK;
			return true;
		}
		finally{
			if(irrevocableState){
				irrevocableState = false;
				irrevocableAccessLock.writeLock().unlock();
			}
			else{
				irrevocableAccessLock.readLock().unlock();
			}
		}
	}	
	
	public void rollback(){
		irrevocableAccessLock.readLock().unlock();
	}

	private WriteFieldAccess onReadAccess0( Object obj, long field, int advice){
		ReadFieldAccess current = currentReadFieldAccess;
		int hash = current.hashCode();

		boolean isReadOnly = (advice & Advisor.CURRENTLY_READ_ONLY) != 0;
		boolean isThreadLocal = (advice & Advisor.THREAD_LOCAL) != 0;
		boolean stableRead = (advice & Advisor.STABLE_READ) != 0;

		// Check the read is still valid
		if (!stableRead && !isThreadLocal)
		{
			LockTable.checkLock(hash, localClock, lastReadLock);
		}

		if (isReadOnly){
			// No need to check in the write set
			return null;
		}
		else{
			// Check if it is already included in the write set
			return writeSet.contains( current);
		}
	}

	private void addWriteAccess0( WriteFieldAccess write){

		// Add to write set
		writeSet.put( write);
	}
	
	public void beforeReadAccess(Object obj, long field, int advice) {
		boolean readonly = (advice & Advisor.READ_ONLY_METHOD) != 0;

		ReadFieldAccess next = null;
		if (readonly)
		{			
			next = currentReadFieldAccess;
		}
		else
		{
			next = readSet.getNext();;
			currentReadFieldAccess = next;
		}
		next.init(obj, field, advice);

		boolean threadLocal = (advice & Advisor.THREAD_LOCAL) != 0;
		if (!threadLocal)
		{
			// Check the read is still valid
			lastReadLock = LockTable.checkLock(next.hashCode(), localClock);
		}
	}
	
	public Object onReadAccess( Object obj, Object value, long field, int advice){
		WriteFieldAccess writeAccess = onReadAccess0(obj, field, advice);
		if( writeAccess == null)
			return value;
		
		return ((ObjectWriteFieldAccess)writeAccess).getValue();  
	}
		
	public boolean onReadAccess(Object obj, boolean value, long field, int advice) {
		WriteFieldAccess writeAccess = onReadAccess0(obj, field, advice);
		if( writeAccess == null)
			return value;
		
		return ((BooleanWriteFieldAccess)writeAccess).getValue();  
	}
	
	public byte onReadAccess(Object obj, byte value, long field, int advice) {
		WriteFieldAccess writeAccess = onReadAccess0(obj, field, advice);
		if( writeAccess == null)
			return value;
		
		return ((ByteWriteFieldAccess)writeAccess).getValue();  
	}
	
	public char onReadAccess(Object obj, char value, long field, int advice) {
		WriteFieldAccess writeAccess = onReadAccess0(obj, field, advice);
		if( writeAccess == null)
			return value;
		
		return ((CharWriteFieldAccess)writeAccess).getValue();  
	}
	
	public short onReadAccess(Object obj, short value, long field, int advice) {
		WriteFieldAccess writeAccess = onReadAccess0(obj, field, advice);
		if( writeAccess == null)
			return value;
		
		return ((ShortWriteFieldAccess)writeAccess).getValue();  

	}
	
	public int onReadAccess(Object obj, int value, long field, int advice) {
		WriteFieldAccess writeAccess = onReadAccess0(obj, field, advice);
		if( writeAccess == null)
			return value;
		
		return ((IntWriteFieldAccess)writeAccess).getValue();  
	}
	
	public long onReadAccess(Object obj, long value, long field, int advice) {
		WriteFieldAccess writeAccess = onReadAccess0(obj, field, advice);
		if( writeAccess == null)
			return value;
		
		return ((LongWriteFieldAccess)writeAccess).getValue();  
	}
	
	public float onReadAccess(Object obj, float value, long field, int advice) {
		WriteFieldAccess writeAccess = onReadAccess0(obj, field, advice);
		if( writeAccess == null)
			return value;
		
		return ((FloatWriteFieldAccess)writeAccess).getValue();  
	}
	
	public double onReadAccess(Object obj, double value, long field, int advice) {
		WriteFieldAccess writeAccess = onReadAccess0(obj, field, advice);
		if( writeAccess == null)
			return value;
		
		return ((DoubleWriteFieldAccess)writeAccess).getValue();  
	}
	
	public void onWriteAccess( Object obj, Object value, long field, int advice){
		ObjectWriteFieldAccess next = objectPool.getNext();
		next.set(value, obj, field, advice);
		addWriteAccess0(next);
	}
	
	public void onWriteAccess(Object obj, boolean value, long field, int advice) {
		
		BooleanWriteFieldAccess next = booleanPool.getNext();
		next.set(value, obj, field, advice);
		addWriteAccess0(next);
	}
	
	public void onWriteAccess(Object obj, byte value, long field, int advice) {
		
		ByteWriteFieldAccess next = bytePool.getNext();
		next.set(value, obj, field, advice);
		addWriteAccess0(next);
	}
	
	public void onWriteAccess(Object obj, char value, long field, int advice) {
		
		CharWriteFieldAccess next = charPool.getNext();
		next.set(value, obj, field, advice);
		addWriteAccess0(next);
	}
	
	public void onWriteAccess(Object obj, short value, long field, int advice) {
		
		ShortWriteFieldAccess next = shortPool.getNext();
		next.set(value, obj, field, advice);
		addWriteAccess0(next);
	}
	
	public void onWriteAccess(Object obj, int value, long field, int advice) {
		
		IntWriteFieldAccess next = intPool.getNext();
		next.set(value, obj, field, advice);
		addWriteAccess0(next);
	}
	
	public void onWriteAccess(Object obj, long value, long field, int advice) {
		
		LongWriteFieldAccess next = longPool.getNext();
		next.set(value, obj, field, advice);
		addWriteAccess0(next);
	}

	public void onWriteAccess(Object obj, float value, long field, int advice) {
		
		FloatWriteFieldAccess next = floatPool.getNext();
		next.set(value, obj, field, advice);
		addWriteAccess0(next);
	}

	
	public void onWriteAccess(Object obj, double value, long field, int advice) {
		
		DoubleWriteFieldAccess next = doublePool.getNext();
		next.set(value, obj, field, advice);
		addWriteAccess0(next);
	}
	
	private Pool<ObjectWriteFieldAccess> objectPool = new Pool<ObjectWriteFieldAccess>( new ResourceFactory<ObjectWriteFieldAccess>(){
		@Override
		public ObjectWriteFieldAccess newInstance() {
			return new ObjectWriteFieldAccess();
		}
	});
	
	private Pool<BooleanWriteFieldAccess> booleanPool = new Pool<BooleanWriteFieldAccess>( new ResourceFactory<BooleanWriteFieldAccess>(){
		@Override
		public BooleanWriteFieldAccess newInstance() {
			return new BooleanWriteFieldAccess();
		}
	});
	
	private Pool<ByteWriteFieldAccess> bytePool = new Pool<ByteWriteFieldAccess>( new ResourceFactory<ByteWriteFieldAccess>(){
		@Override
		public ByteWriteFieldAccess newInstance() {
			return new ByteWriteFieldAccess();
		}
	});
	
	private Pool<CharWriteFieldAccess> charPool = new Pool<CharWriteFieldAccess>( new ResourceFactory<CharWriteFieldAccess>(){
		@Override
		public CharWriteFieldAccess newInstance() {
			return new CharWriteFieldAccess();
		}
	});
	
	private Pool<ShortWriteFieldAccess> shortPool = new Pool<ShortWriteFieldAccess>( new ResourceFactory<ShortWriteFieldAccess>(){
		@Override
		public ShortWriteFieldAccess newInstance() {
			return new ShortWriteFieldAccess();
		}
	});
	
	private Pool<IntWriteFieldAccess> intPool = new Pool<IntWriteFieldAccess>( new ResourceFactory<IntWriteFieldAccess>(){
		@Override
		public IntWriteFieldAccess newInstance() {
			return new IntWriteFieldAccess();
		}
	});
	
	private Pool<LongWriteFieldAccess> longPool = new Pool<LongWriteFieldAccess>( new ResourceFactory<LongWriteFieldAccess>(){
		@Override
		public LongWriteFieldAccess newInstance() {
			return new LongWriteFieldAccess();
		}
	});
	
	private Pool<FloatWriteFieldAccess> floatPool = new Pool<FloatWriteFieldAccess>( new ResourceFactory<FloatWriteFieldAccess>(){
		@Override
		public FloatWriteFieldAccess newInstance() {
			return new FloatWriteFieldAccess();
		}
	});
	
	private Pool<DoubleWriteFieldAccess> doublePool = new Pool<DoubleWriteFieldAccess>( new ResourceFactory<DoubleWriteFieldAccess>(){
		@Override
		public DoubleWriteFieldAccess newInstance() {
			return new DoubleWriteFieldAccess();
		}
	});

	@Override
	public void onIrrevocableAccess() {
		if(irrevocableState) // already in irrevocable state so no need to restart transaction.
			return;
		
		irrevocableState = true;
		throw TransactionException.STATIC_TRANSACTION;
	}

}
