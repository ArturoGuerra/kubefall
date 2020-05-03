package kubefall.connections;

import java.util.HashMap;
import java.util.UUID;

public class Sessions {
    private HashMap<UUID, String> connections;
    
    public Sessions() {
        this.connections = new HashMap<UUID, String>();
    } 

    public String getReconnectServer(UUID playerId) {
        return this.connections.get(playerId);
    }

    public void setReconnectServer(UUID playerId, String serverHost) {
        this.connections.put(playerId, serverHost);
    }
}