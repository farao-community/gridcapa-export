/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.export.health;

import com.farao_community.farao.gridcapa.export.config_properties.FtpConfigurationProperties;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
@Component
@ConditionalOnProperty(prefix = "ftp", name = "active", havingValue = "true")
public class FtpHealthIndicator implements HealthIndicator {

    private final FtpConfigurationProperties ftpConfig;

    public FtpHealthIndicator(FtpConfigurationProperties ftpConfig) {
        this.ftpConfig = ftpConfig;
    }

    @Override
    public Health health() {
        FTPClient ftp = new FTPClient();
        try {
            ftp.connect(ftpConfig.getHost(), ftpConfig.getPort());

            if (FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
                ftp.login(ftpConfig.getAccessKey(), ftpConfig.getSecretKey());
                ftp.enterLocalPassiveMode();
                boolean directoryExists = ftp.changeWorkingDirectory(ftpConfig.getRemoteRelativeDestinationDirectory());

                if (directoryExists) {
                    return Health.up().build();
                }
            }
        } catch (IOException e) {
            return Health.down().build();
        } finally {
            try {
                ftp.disconnect();
            } catch (IOException e) {
                // nothing to do
            }
        }
        return Health.down().build();
    }
}
