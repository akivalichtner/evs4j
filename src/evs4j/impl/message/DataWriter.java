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

public abstract class DataWriter {

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

    protected void writeFloat(float f) {
	writeInt(Float.floatToIntBits(f));
    }

    protected void writeLong(long v) {
	int i = this.offset;
	data[i++] = (byte) ((v >>> 56) & 0xFF);
	data[i++] = (byte) ((v >>> 48) & 0xFF);
	data[i++] = (byte) ((v >>> 40) & 0xFF);
	data[i++] = (byte) ((v >>> 32) & 0xFF);	
	data[i++] = (byte) ((v >>> 24) & 0xFF);
	data[i++] = (byte) ((v >>> 16) & 0xFF);
	data[i++] = (byte) ((v >>> 8)  & 0xFF);
	data[i++] = (byte) ((v >>> 0)  & 0xFF);	
	this.offset = i;
    }

    protected void writeInt(int v) {
	int i = this.offset;
	data[i++] = (byte) ((v >>> 24) & 0xFF);
	data[i++] = (byte) ((v >>> 16) & 0xFF);
	data[i++] = (byte) ((v >>> 8)  & 0xFF);
	data[i++] = (byte) ((v >>> 0)  & 0xFF);	
	this.offset = i;
    }

    protected void writeShort(short s) {
	int i = this.offset;
	data[i++] = (byte) ((s >>> 8) & 0xFF);
	data[i++] = (byte) ((s >>> 0) & 0xFF);
	this.offset = i;
    }

    protected void writeByte(byte b) {
	data[this.offset++] = b;	
    }

    protected void writeBoolean(boolean b) {
	data[this.offset++] = (byte) (b ? 1 : 0);
    }

}



