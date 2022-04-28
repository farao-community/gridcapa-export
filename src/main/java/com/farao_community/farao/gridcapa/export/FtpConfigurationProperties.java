package com.farao_community.farao.gridcapa.export;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConstructorBinding
@ConfigurationProperties("ftp")
public class FtpConfigurationProperties {
    private String host;
    private int port;
    private String accessKey;
    private String secretKey;
    private String remoteRelativeDestinationDirectory;

    public FtpConfigurationProperties(String host, int port, String accessKey, String secretKey, String remoteRelativeDestinationDirectory) {
        this.host = host;
        this.port = port;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.remoteRelativeDestinationDirectory = remoteRelativeDestinationDirectory;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public String getRemoteRelativeDestinationDirectory() {
        return remoteRelativeDestinationDirectory;
    }
}
