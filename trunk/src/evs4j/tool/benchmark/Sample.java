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

package evs4j.tool.benchmark;

/**
 * Used by MonitorConnection.
 */
public class Sample {

    private double[] data;

    private int pos;

    private int size;

    public Sample(int size) {
	this.size = size;
	data = new double[size];
	pos = 0;
    }
    
    public static final int DEFAULT_SIZE = 100;

    public Sample() {
	this(DEFAULT_SIZE);	
    }

    public void add(double value) {
	data[pos++] = value;
	if (pos == size) {
	    pos = 0;
	}
    }

    public double getAverage() {
	double sum = 0;
	for (int i = 0; i < size; i++) {
	    sum = sum + data[i];
	}
	return sum / (double) size;
    }

}
