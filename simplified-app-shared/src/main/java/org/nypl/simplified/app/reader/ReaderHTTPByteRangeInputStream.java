//@formatter:off
//
//  Copyright (c) 2014 Readium Foundation and/or its licensees. All rights reserved.
//
//  Redistribution and use in source and binary forms, with or without modification,
//  are permitted provided that the following conditions are met:
//
//  1. Redistributions of source code must retain the above copyright notice, this
//  list of conditions and the following disclaimer.
//
//  2. Redistributions in binary form must reproduce the above copyright notice,
//  this list of conditions and the following disclaimer in the documentation and/or
//  other materials provided with the distribution.
//
//  3. Neither the name of the organization nor the names of its contributors may be
//  used to endorse or promote products derived from this software without specific
//  prior written permission.
//
//  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
//  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
//  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
//  IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
//  INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
//  BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
//  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
//  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
//  OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
//  OF THE POSSIBILITY OF SUCH DAMAGE
//
//@formatter:on

package org.nypl.simplified.app.reader;

import com.io7m.jnull.NonNull;
import com.io7m.jnull.NullCheck;
import org.readium.sdk.android.util.ResourceInputStream;

import java.io.IOException;
import java.io.InputStream;

/**
 * <p>A stream type that is capable of reading a byte range, and protects all
 * operations with a {@link ReaderNativeCodeReadLock}.</p>
 *
 * <p>Derived from Readium's {@code ByteRangeInputStream}.</p>
 */

final class ReaderHTTPByteRangeInputStream extends InputStream
{
  private final ResourceInputStream      ris;
  private final ReaderNativeCodeReadLock read_lock;
  private final boolean                  is_range;
  private       long                     requested_offset;
  private       long                     already_read;
  private boolean is_open = true;

  ReaderHTTPByteRangeInputStream(
    final ResourceInputStream in_is,
    final boolean in_range,
    final ReaderNativeCodeReadLock in_lock)
  {
    this.is_range = in_range;
    this.ris = NullCheck.notNull(in_is);
    this.read_lock = NullCheck.notNull(in_lock);
  }

  @Override public void close()
    throws IOException
  {
    this.is_open = false;
    synchronized (this.read_lock) {
      this.ris.close();
    }
  }

  @Override public int read()
    throws IOException
  {
    if (this.is_open) {
      final byte[] buffer = new byte[1];
      if (this.read(buffer) == 1) {
        return buffer[0];
      }
    }
    return -1;
  }

  public int available()
    throws IOException
  {
    final int available;
    synchronized (this.read_lock) {
      available = this.ris.available();
    }
    long remaining = available - this.already_read;
    if (remaining < 0) {
      remaining = 0;
    }
    return (int) remaining;
  }

  @Override public long skip(final long byte_count)
    throws IOException
  {
    if (this.is_range) {
      this.requested_offset = this.already_read + byte_count;
    } else if (byte_count != 0) {
      synchronized (this.read_lock) {
        return this.ris.skip(byte_count);
      }
    }
    return byte_count;
  }

  @Override public synchronized void reset()
    throws IOException
  {
    if (this.is_range) {
      this.requested_offset = 0;
      this.already_read = 0;
    } else {
      synchronized (this.read_lock) {
        this.ris.reset();
      }
    }
  }

  @Override public int read(
    final @NonNull byte[] b,
    final int offset,
    final int len)
    throws IOException
  {
    if (offset != 0) {
      throw new IOException("Offset parameter can only be zero");
    }
    if (len == 0 || !this.is_open) {
      return -1;
    }
    int read;

    synchronized (this.read_lock) {
      if (this.is_range) {
        read = (int) this.ris.getRangeBytesX(
          this.requested_offset + this.already_read, (long) len, b);
      } else {
        read = (int) this.ris.readX((long) len, b);
      }
    }

    this.already_read += read;
    if (read == 0) {
      read = -1;
    }
    return read;
  }
}
