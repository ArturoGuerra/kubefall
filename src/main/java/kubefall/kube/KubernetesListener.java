package kubefall.kube;

import java.util.logging.Logger;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.google.common.reflect.TypeToken;
import com.squareup.okhttp.OkHttpClient;

import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.Configuration;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.models.V1Service;
import io.kubernetes.client.models.V1ServicePort;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.Watch;

import java.net.InetSocketAddress;
import java.util.Map;

public class KubernetesListener {
    private ProxyServer proxyServer;
    private kubefall.config.Config config;
    private Logger logger;
    private CoreV1Api api;
    private ApiClient kclient;
    private Watch<V1Service> watch;
    final private String namespace = (System.getenv("KUBE_NAMESPACE") != null) ? System.getenv("KUBE_NAMESPACE") : "minecraft";
    final private String defaultName = (System.getenv("DEFAULT_LOBBY") != null) ? System.getenv("DEFAULT_LOBBY") : "lobby";
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    public KubernetesListener(kubefall.config.Config config, ProxyServer proxyServer, Logger logger) throws IOException, ApiException {
        this.proxyServer = proxyServer;
        this.config = config;
        this.logger = logger;
 
        this.kclient = (System.getenv("KUBEFILE") != null) ? Config.fromConfig(System.getenv("KUBEFILE")) : Config.fromCluster();
        OkHttpClient httpClient = this.kclient.getHttpClient();
        httpClient.setReadTimeout(0, TimeUnit.SECONDS);
        Configuration.setDefaultApiClient(this.kclient);
        this.kclient.setHttpClient(httpClient);
        this.api = new CoreV1Api();
        this.watch = Watch.createWatch(
            this.kclient,
            this.api.listNamespacedServiceCall(this.namespace, true, null, null, null, null, null, null, null, true, null, null),
            new TypeToken<Watch.Response<V1Service>>() {}.getType()
        );
        executorService.execute(new WatchHandler());

    }

    public class WatchHandler implements Runnable {
        @Override
        public void run() {
            try {
                watch.forEach(service -> {
                    switch(service.type) {
                        case "ADDED":
                            addServer(service.type, service.object);
                            break;
                        case "MODIFIED":
                            addServer(service.type, service.object);
                            break;
                        case "REMOVED":
                            removeServer(service.object);
                            break;
                        default:
                            logger.info(String.format("Action: %s%n", service.type));
                            break;
                    }
                });
            } catch (Throwable e) {
                try {
                    Thread.sleep(1000*5);
                } catch (InterruptedException error) {
                    logger.info(error.getMessage());
                }
            }
        }
    }

    private void addServer(String event, V1Service service) {
        Map<String, String> annotations = service.getMetadata().getAnnotations();
        if (annotations != null) {
            if ((annotations.get("io.ar2ro.kubefall/enabled") != null) ? annotations.get("io.ar2ro.kubefall/enabled").contentEquals("true") : false) {
                Integer port = 25565;
                final String externalHost = annotations.get("io.ar2ro.kubefall/host");
                final String name = service.getMetadata().getName();
                final String serviceNamespace = service.getMetadata().getNamespace();
                final String proxyDNS = String.format("%s.%s.svc.cluster.local", name, serviceNamespace);
                final String motd = (annotations.get("io.ar2ro.kubefall/motd") != null ) ? annotations.get("io.ar2ro.kubefall/motd") : config.getMotd();
                final Boolean defaultServer = (annotations.get("io.ar2ro.kubefall/defaultServer") != null) ? annotations.get("io.ar2ro.kubefall/defaultServer").contentEquals("true") : false;
                final Boolean restricted = (annotations.get("io.ar2ro.kubefall/restricted") != null) ? annotations.get("io.ar2ro.kubefall/restricted").contentEquals("true") : false;
                
                for (V1ServicePort servicePort : service.getSpec().getPorts()) {
                    if (servicePort.getName() == "minecraft") {
                        port = servicePort.getPort();
                    }
                }

                final InetSocketAddress address = new InetSocketAddress(proxyDNS, port);
                final ServerInfo server = proxyServer.constructServerInfo(name, address, motd, restricted);

                if (defaultServer) {
                    config.setDefaultServer(server);
                    proxyServer.getServers().put(defaultName, server);
                }

                if (externalHost != null) {
                   config.addForcedHost(externalHost, name);
                }

                proxyServer.getServers().put(name, server);
                logger.info(String.format(
                    "Event: %s Service: %s ExternalHost: %s Default: %b Motd: %s ProxyDNS: %s",
                    event,
                    name,
                    externalHost,
                    defaultServer,
                    motd,
                    address.getHostString()));
            }

        }
    }

    private void removeServer(V1Service service) {
        final String name = service.getMetadata().getName();
        final Map<String, String> annotations = service.getMetadata().getAnnotations();
        String externalHost = "";
        Boolean defaultServer = false;

        if (annotations != null) {
            defaultServer = (annotations.get("io.ar2ro.kubafall/defaultServer") != null) ? annotations.get("io.ar2ro.kubefall/defaultServer").contentEquals("true") : false;
            externalHost = annotations.get("io.ar2ro.kubefall/host");
            if (externalHost != null) config.removeForcedHost(externalHost);
        }

        proxyServer.getServers().remove(name);

        logger.info(String.format(
            "Event: REMOVED Service: %s ExternalHost: %s Default: %b",
            name,
            externalHost != null ? externalHost : "",
            defaultServer
        ));
    }
}