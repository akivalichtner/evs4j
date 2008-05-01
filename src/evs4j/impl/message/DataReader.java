/**
 *
 *  Copyright 2000-2006 Guglielmo Lichtner (lichtner_at_bway_dot_net)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package evs4j.impl.message;

public abstract class DataReader {

    protected byte[] data;

    public byte[] getData() {
	return data;
    }
    
    protected int offset;
    
    public int getOffset() {
	return offset;
    }
    
    public void reset(byte[] data, int offset) {
	this.data = data;
	this.offset = offset;
    }
    
    protected float readFloat() {
	return Float.intBitsToFloat(readInt());
    }

    protected long readLong() {
	int i = this.offset;
	long v = 0;
	v |= (((long) data[i++]) << 56) & 0xFF00000000000000L;
	v |= (((long) data[i++]) << 48) & 0xFF000000000000L;
	v |= (((long) data[i++]) << 40) & 0xFF0000000000L;
	v |= (((long) data[i++]) << 32) & 0xFF00000000L;
	v |= (((long) data[i++]) << 24) & 0xFF000000L;
	v |= (((long) data[i++]) << 16) & 0xFF0000L;
	v |= (((long) data[i++]) << 8)  & 0xFF00L;
	v |= (((long) data[i++]) << 0)  & 0xFFL;
	this.offset = i;
	return v;
    }

    protected int readInt() {
	int i = this.offset;
	int v = 0;
	v |= (data[i++] << 24) & 0xFF000000;
	v |= (data[i++] << 16) & 0xFF0000;
	v |= (data[i++] << 8)  & 0xFF00;
	v |= (data[i++] << 0)  & 0xFF;
	this.offset = i;
	return v;
    }

    protected short readShort() {
	int i = this.offset;
	short s = 0;
	s |= (data[i++] << 8) & 0xFF00;
	s |= (data[i++] << 0) & 0xFF;
	this.offset = i;
	return s;
    }

    protected byte readByte() {
	return data[this.offset++];
    }

    protected boolean readBoolean() {
	return (data[this.offset++] > 0);
    }
    
}

