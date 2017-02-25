package org.java_websocket.util;

import java.nio.ByteBuffer;

/**
 * A {@link DisposableByteBuffer} will provide a {@link ByteBuffer} of a given capacity
 * with a chance of reusing memory, if there already exists a disposed {@link ByteBuffer}
 * in the {@link DisposedBytesProvider} of the same capacity.
 */
public class DisposableByteBuffer {

	private ByteBuffer buffer;
	private long disposedTime;

	/**
	 * Allocate or retrieve a disposed {@link ByteBuffer} of the given capacity.
	 * The resulting bytes may or may not be zeroed.
	 * 
	 * @param capacity
	 * @see DisposableByteBuffer#DisposableByteBuffer(int, boolean) DisposableByteBuffer(capacity, zeroed)
	 */
	public DisposableByteBuffer( int capacity ) {
		this( capacity, false );
	}

	/**
	 * Allocate or retrieve a disposed {@link ByteBuffer} of the given capacity.
	 * If {@code zeroed} is true, the resulting bytes are guaranteed to be set to zero.
	 * 
	 * @param capacity
	 * @param zeroed
	 */
	public DisposableByteBuffer( int capacity , boolean zeroed ) {
		buffer = DisposedBytesProvider.getInstance().getDisposedBytes( capacity, zeroed );
	}

	/**
	 * Wrap an existing {@link ByteBuffer} around a {@link DisposableByteBuffer} instance.
	 * 
	 * @param buffer
	 */
	public DisposableByteBuffer( ByteBuffer buffer ) {
		this.buffer = buffer;
	}

	/**
	 * Get the underlying {@link ByteBuffer}.
	 * 
	 * @return {@link ByteBuffer}
	 * @throws IllegalStateException
	 *             if this instance has been disposed
	 */
	public ByteBuffer getBytes() {
		if( checkDisposed() )
			throw new IllegalStateException( "Operating with a disposed byte buffer. Unsafe!" );

		return getBytesUnsafe();
	}

	/**
	 * Get the underlying {@link ByteBuffer} without checking if
	 * this {@link DisposableByteBuffer} has been disposed.
	 * 
	 * @return {@link ByteBuffer}
	 */
	public ByteBuffer getBytesUnsafe() {
		return buffer;
	}

	public int getCapacity() {
		return buffer.capacity();
	}

	public long getDisposedTime() {
		return disposedTime;
	}

	/**
	 * Mark this {@link DisposableByteBuffer} as disposed and ready for reuse.
	 * The underlying {@link ByteBuffer} will no longer be accessible through this
	 * {@link DisposableByteBuffer} instance.
	 */
	public void dispose() {
		disposedTime = System.currentTimeMillis();
		DisposedBytesProvider.getInstance().disposeBytes( this );
	}

	/**
	 * @return {@code true} if this {@link DisposableByteBuffer} has been disposed
	 */
	public boolean checkDisposed() {
		return disposedTime > 0;
	}
}
