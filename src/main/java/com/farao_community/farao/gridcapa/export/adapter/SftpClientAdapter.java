/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.export.adapter;

import com.farao_community.farao.gridcapa.export.config_properties.SftpConfigurationProperties;
import com.farao_community.farao.gridcapa.export.exception.ClientAdapterException;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 */

@Component
@ConditionalOnProperty(prefix = "sftp", name = "active", havingValue = "true")
public class SftpClientAdapter implements ClientAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SftpClientAdapter.class);

    private final SftpConfigurationProperties sftpConfigurationProperties;

    public SftpClientAdapter(SftpConfigurationProperties sftpConfigurationProperties) {
        this.sftpConfigurationProperties = sftpConfigurationProperties;
    }

    public void upload(String fileName, InputStream inputStream) throws ClientAdapterException {
        try {
            JSch jsch = new JSch();
            Session jschSession = jsch.getSession(sftpConfigurationProperties.getAccessKey(), sftpConfigurationProperties.getHost(), sftpConfigurationProperties.getPort());
            jschSession.setPassword(sftpConfigurationProperties.getSecretKey());
            jschSession.connect();

            ChannelSftp channelSftp = (ChannelSftp) jschSession.openChannel("sftp");
            LOGGER.info("Attempt to connect to SFTP channel");
            channelSftp.connect();
            LOGGER.info("Connection established");
            LOGGER.info("Attempt to copy {} file to SFTP server", fileName);
            channelSftp.put(inputStream, sftpConfigurationProperties.getRemoteRelativeDestinationDirectory() + fileName);
            LOGGER.info("File {} copied successfully to SFTP server", fileName);

            channelSftp.disconnect();
            jschSession.disconnect();
            LOGGER.info("Connection closed");
        } catch (JSchException | SftpException e) {
            LOGGER.error("Fail during upload");
            throw new ClientAdapterException(e);
        }
    }
}
