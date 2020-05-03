package kubefall.config;

import net.md_5.bungee.api.config.ServerInfo;

import java.util.HashMap;
import java.lang.System;

public class Config {
    private String motd;
    private String[] admins;
    private HashMap<String, String> forcedHosts;
    private ServerInfo defaultServer;

    // Loads Config from environmental variables
    public Config() {
        this.forcedHosts = new HashMap<String, String>();
        this.admins = (System.getenv("admins") != null) ? System.getenv("admins").split(",") : new String[]{};
        this.motd = (System.getenv("motd") != null) ? System.getenv("motd") : "Welcome to northern uwu";
        this.admins = new String[]{};
        this.defaultServer = null;
    }

    public String getMotd() {
        return this.motd;
    }

    public String[] getAdmins() {
        return this.admins;
    }

    public ServerInfo getDefaultServer() {
        return this.defaultServer;
    }

    public void setDefaultServer(ServerInfo server) {
        this.defaultServer = server;
    }

    public void addForcedHost(String host, String server) {
        this.forcedHosts.put(host, server);
    }

    public void removeForcedHost(String host) {
        this.forcedHosts.remove(host);
    }

    public String getForcedHost(String host) {
        return this.forcedHosts.get(host);
    }
}