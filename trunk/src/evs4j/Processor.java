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

package evs4j;

/**
 * Represents the idea of a sending and/or receiving process (or thread)
 * in a configuration. An integer would be sufficient to uniquely identify
 * a sender or receiver but we found it convenient to use an object.
 */
public class Processor implements Comparable {

    private int value;

    public Processor(int value) {
	this.value = value;
    }

    public int getValue() {
	return value;
    }    

    public int compareTo(Object object) {
	//do not catch ClassCastException
	Processor n2 = (Processor) object;
	Processor n1 = this;
	return n1.value - n2.value;
    }

    public boolean equals(Object object) {
	return compareTo(object) == 0;
    }

    public int hashCode() {
	return value;
    }

    public String toString() {
	return String.valueOf(value);
    }

}
