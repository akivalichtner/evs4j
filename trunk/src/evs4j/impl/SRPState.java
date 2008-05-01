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

import evs4j.impl.message.RegularTokenMessage;
import evs4j.impl.message.CommitTokenMessage;
import evs4j.impl.message.RegularMessage;
import evs4j.impl.message.JoinMessage;

public interface SRPState {

    public abstract void regularTokenReceived(RegularTokenMessage m);
    
    public abstract void commitTokenReceived(CommitTokenMessage m);

    public abstract void regularMessageReceived(RegularMessage m);
    
    public abstract void foreignMessageReceived(RegularMessage m);
    
    public abstract void joinMessageReceived(JoinMessage m);

    public abstract void tokenLossTimeoutExpired();

    public abstract void tokenDroppedTimeoutExpired();
    
    public abstract void consensusTimeoutExpired();

    public abstract void joinTimeoutExpired();

}
