package org.java_websocket.util;

import java.awt.Frame;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

/**
 * The {@link DisposedBytesProvider} singleton contains {@link ByteBuffer} instances
 * that have been marked as disposed and allows the immediate reuse of their
 * memory.
 * <p>
 * This prevents the JVM garbage collector from working too hard to free and
 * reallocate {@link ByteBuffer} instances of the same size that are being used
 * multiple times a second.
 * <p>
 * The disposed frame cache removes {@link ByteBuffer} instances that are older
 * than {@value #DISPOSED_CACHE_TTL}ms
 */
public class DisposedBytesProvider {

	private static final long DISPOSED_CACHE_TTL = 1000;

	/**
	 * Gets the current {@link DisposedBytesProvider} instance
	 *
	 * @return {@link DisposedBytesProvider}
	 */
	public static DisposedBytesProvider getInstance() {
		if( instance == null )
			instance = new DisposedBytesProvider();

		return instance;
	}

	private static volatile DisposedBytesProvider instance;

	private HashMap<String,Stack<DisposableByteBuffer>> disposed = new HashMap<String,Stack<DisposableByteBuffer>>();

	public DisposedBytesProvider() {
		( new Thread() {

			@Override
			public void run() {
				while ( true ) {
					update();

					try {
						Thread.sleep( DISPOSED_CACHE_TTL );
					} catch ( Exception e ) {

					}
				}
			}

		} ).start();
	}

	/**
	 * Updates the {@link DisposedBytesProvider}, which removes stale
	 * {@link ByteBuffer} instances from the cache.
	 */
	public synchronized void update() {
		for( String key : disposed.keySet() ) {
			Stack<DisposableByteBuffer> stack = disposed.get( key );

			if( stack == null )
				continue;

			ArrayList<DisposableByteBuffer> toRemove = new ArrayList<DisposableByteBuffer>();
			for( DisposableByteBuffer frame : stack )
				if( System.currentTimeMillis() - frame.getDisposedTime() > DISPOSED_CACHE_TTL )
					toRemove.add( frame );

			stack.removeAll( toRemove );
		}
	}

	/**
	 * Get a piece of memory with the given dimensions, optionally clearing it
	 * first. If no disposed {@link Frame} exists to satisfy the dimensions, a
	 * new piece of memory is allocated.
	 *
	 * @param capacity
	 *            desired size of {@link ByteBuffer}
	 * @param cleanUp
	 *            clear the result
	 * @return {@link ByteBuffer}
	 */
	public synchronized ByteBuffer getDisposedBytes( int capacity, boolean cleanUp ) {
		String hash = getHash( capacity );

		// If we don't have any unused frames of the same size, not much we can
		// do
		if( !disposed.containsKey( hash ) )
			return ByteBuffer.allocate( capacity );

		Stack<DisposableByteBuffer> disposedOfEqualSize = disposed.get( hash );

		// No frames to reuse, have to make a new one
		if( disposedOfEqualSize.isEmpty() )
			return ByteBuffer.allocate( capacity );

		DisposableByteBuffer disposedBytes = disposedOfEqualSize.pop();
		ByteBuffer bytes = disposedBytes.getBytesUnsafe();
		bytes.clear();

		// If the caller doesn't like dirty data, we have to use a O(n)
		// operation to clear it
		if( cleanUp ) {
			for( int i = 0 ; i < capacity ; i++ )
				bytes.put( (byte) 0 );

			bytes.flip();
		}

		return bytes;
	}

	/**
	 * Wrap a {@link ByteBuffer} around a {@link DisposableByteBuffer} and mark it
	 * as disposed, and ready for reuse.
	 *
	 * @param buffer
	 *            {@link ByteBuffer} to dispose
	 */
	public synchronized void disposeBytes( ByteBuffer buffer ) {
		if( buffer.capacity() < 1 )
			return;

		DisposableByteBuffer disposable = new DisposableByteBuffer( buffer );
		disposable.dispose();
	}

	/**
	 * Cache a {@link DisposableByteBuffer} as disposed, and ready for reuse.
	 *
	 * @param frame
	 *            {@link DisposableByteBuffer} to dispose
	 */
	public synchronized void disposeBytes( DisposableByteBuffer frame ) {
		String hash = getHash( frame.getCapacity() );

		Stack<DisposableByteBuffer> disposedFrames = disposed.get( hash );

		if( disposedFrames == null ) {
			disposedFrames = new Stack<DisposableByteBuffer>();
			disposed.put( hash, disposedFrames );
		}

		disposedFrames.push( frame );
	}

	private String getHash( int capacity ) {
		return capacity + "";
	}

}
