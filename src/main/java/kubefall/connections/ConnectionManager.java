package kubefall.connections;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.ReconnectHandler;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.config.ServerInfo;
import kubefall.config.Config;
import java.util.logging.Logger;


public class ConnectionManager implements ReconnectHandler {
    private Sessions sessions;
    private Config config;
    private Logger logger;
    private ProxyServer proxyServer;

    public ConnectionManager(Config config, ProxyServer proxyServer, Sessions sessions, Logger logger) {
        this.sessions = sessions;
        this.config = config;
        this.proxyServer = proxyServer;
        this.logger = logger;
    }

    @Override
    public ServerInfo getServer(ProxiedPlayer proxiedPlayer) {
        ServerInfo serverInfo = null;

        if (proxiedPlayer.getPendingConnection().getVirtualHost().getHostName() != null) {
            serverInfo = this.proxyServer.getServerInfo(this.config.getForcedHost(proxiedPlayer.getPendingConnection().getVirtualHost().getHostName()));
        }

        if (serverInfo == null && this.sessions.getReconnectServer(proxiedPlayer.getUniqueId()) != null) {
            serverInfo = this.proxyServer.getServerInfo(this.sessions.getReconnectServer(proxiedPlayer.getUniqueId()));
        }

        if (serverInfo != null) {
            serverInfo = this.config.getDefaultServer();
        }

        return serverInfo;
    }

    @Override
    public void setServer(ProxiedPlayer proxiedPlayer) {
        this.sessions.setReconnectServer(
            proxiedPlayer.getUniqueId(),
            (proxiedPlayer.getReconnectServer() != null)
            ? proxiedPlayer.getReconnectServer().getName()
            : proxiedPlayer.getServer().getInfo().getName() 
        );
    }

    @Override
    public void save() {
        this.logger.info("Saving lol nvm we don't do that here");
    }

    @Override
    public void close() {
        this.logger.info("Closing nvm we don't do that here lol");
    }
}