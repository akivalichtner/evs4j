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

package evs4j.impl;

import evs4j.Alert;

/**
 * Used to send performance-related token information up to the application.
 */
public class SRPTokenAlert implements Alert {

    private int lowMessageId;
    private int window;
    private int missed;

    public int getLowMessageId() { return lowMessageId; }
    public int getWindow() { return window; }
    public int getMissed() { return missed; }

    public SRPTokenAlert(int lowMessageId, 
			 int window, 
			 int missed) {
	this.lowMessageId = lowMessageId;
	this.window = window;
	this.missed = missed;
    }

}
