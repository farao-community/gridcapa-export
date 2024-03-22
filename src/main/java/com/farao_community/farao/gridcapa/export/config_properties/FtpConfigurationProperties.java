/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.export.config_properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Mohamed Benrejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */

@ConfigurationProperties("ftp")
public class FtpConfigurationProperties {
    private String host;
    private int port;
    private String accessKey;
    private String secretKey;
    private String remoteRelativeDestinationDirectory;

    private int retryCount;
    private int retrySleep;

    public FtpConfigurationProperties(String host, int port, String accessKey, String secretKey, String remoteRelativeDestinationDirectory, int retryCount, int retrySleep) {
        this.host = host;
        this.port = port;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.remoteRelativeDestinationDirectory = remoteRelativeDestinationDirectory;
        this.retryCount = retryCount;
        this.retrySleep = retrySleep;
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

    public int getRetryCount() {
        return retryCount;
    }

    public int getRetrySleep() {
        return retrySleep;
    }
}
