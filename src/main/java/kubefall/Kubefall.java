package kubefall;

import java.io.IOException;
import java.util.logging.Logger;

import io.kubernetes.client.ApiException;
import kubefall.kube.KubernetesListener;
import kubefall.config.Config;
import kubefall.connections.*;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;


public class Kubefall extends Plugin {
    @Override
    public void onEnable() {
        Logger logger = getLogger();
        ProxyServer proxyServer = getProxy();
        Config config = loadConfig();
        Sessions sessions = new Sessions();
        
        connectionManager(config, proxyServer, sessions, logger);
        kubernetesListener(config, proxyServer, logger);
    }

    private void connectionManager(Config config, ProxyServer proxyServer, Sessions sessions, Logger logger) {
        ConnectionManager connectionManager = new ConnectionManager(config, proxyServer, sessions, logger);
        proxyServer.setReconnectHandler(connectionManager);
    }


    private void kubernetesListener(Config config, ProxyServer proxyserver, Logger logger)  {
        try {
           new KubernetesListener(config, proxyserver, logger);
        } catch (IOException | ApiException e) {
            logger.info(e.getMessage());
        }
    }

    private Config loadConfig() {
        return new Config();
    }
}
