/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.export.health;

import com.farao_community.farao.gridcapa.export.config_properties.SftpConfigurationProperties;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
@Component
@ConditionalOnProperty(prefix = "sftp", name = "active", havingValue = "true")
public class SftpHealthIndicator implements HealthIndicator {

    private final SftpConfigurationProperties sftpConfig;

    public SftpHealthIndicator(SftpConfigurationProperties sftpConfig) {
        this.sftpConfig = sftpConfig;
    }

    @Override
    public Health health() {
        Session jschSession = null;
        ChannelSftp channelSftp = null;

        try {
            jschSession = new JSch().getSession(sftpConfig.getAccessKey(), sftpConfig.getHost(), sftpConfig.getPort());
            jschSession.setPassword(sftpConfig.getSecretKey());
            jschSession.setConfig("StrictHostKeyChecking", "no");
            jschSession.connect();

            channelSftp = (ChannelSftp) jschSession.openChannel("sftp");
            channelSftp.connect();

            SftpATTRS stat = channelSftp.stat(sftpConfig.getRemoteRelativeDestinationDirectory());
            if (stat != null) {
                return Health.up().build();
            }
        } catch (Exception e) {
            return Health.down().build();
        } finally {
            try {
                channelSftp.disconnect();
            } catch (Exception e) {
                // nothing to do
            }

            try {
                jschSession.disconnect();
            } catch (Exception e) {
                // nothing to do
            }
        }
        return Health.down().build();
    }
}
