/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *
 * This file is part of STEP
 *
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package step.agents.provisioning.local;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

/**
 * Reserves the ports of the local agents which do not pick one themselves.
 * <p>
 * The Java agent binds a port chosen by the operating system when none is configured, whereas the Node.js and .NET
 * agents fall back to a fixed one (3000 and 8098), which two concurrent executions would fight over. Their port is
 * therefore reserved here and written into their configuration, together with the URL they register with.
 */
class LocalPorts {

    private LocalPorts() {
    }

    /**
     * @return a port which was free on the loopback interface. The socket is closed before returning, so this is a
     * hint rather than a reservation: the agent binds it moments later, and nothing else on this machine is expected
     * to take it in between.
     */
    static int findFreeLoopbackPort() throws LocalAgentException {
        try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getByName(AgentConfWriter.LOOPBACK_HOST))) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new LocalAgentException("Unable to reserve a port for a local agent", e);
        }
    }
}
