/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.export.adapter;

import com.farao_community.farao.gridcapa.export.config_properties.FtpConfigurationProperties;
import com.farao_community.farao.gridcapa.export.exception.ClientAdapterException;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 */

@Component
@ConditionalOnProperty(prefix = "ftp", name = "active", havingValue = "true")
public class FtpClientAdapter implements ClientAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(FtpClientAdapter.class);

    private final FtpConfigurationProperties ftpConfigurationProperties;

    public FtpClientAdapter(FtpConfigurationProperties ftpConfigurationProperties) {
        this.ftpConfigurationProperties = ftpConfigurationProperties;
    }

    public void upload(String fileName, InputStream inputStream) throws ClientAdapterException {
        int performedRetries = 0;
        final int maxRetryCount = ftpConfigurationProperties.getRetryCount();
        boolean successfulFtpSend = false;
        while (performedRetries < maxRetryCount && !successfulFtpSend) {
            performedRetries++;
            successfulFtpSend = performSingleUploadAttempt(fileName, inputStream);
        }
        if (!successfulFtpSend) {
            throw new ClientAdapterException(String.format("Upload failed after %s retires", maxRetryCount));
        }
    }

    private boolean performSingleUploadAttempt(String fileName, InputStream inputStream) {
        boolean successFlag = false;
        try {
            FTPClient ftp = new FTPClient(); // NOSONAR
            LOGGER.info("Attempt to connect to FTP server");
            ftp.connect(ftpConfigurationProperties.getHost(), ftpConfigurationProperties.getPort());

            int reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftp.disconnect();
                throw new IOException("Exception in connecting to FTP Server");
            }
            ftp.login(ftpConfigurationProperties.getAccessKey(), ftpConfigurationProperties.getSecretKey());
            LOGGER.info("Connection established");

            // if ftp working dir is /home/farao/upload and you want to upload files under /home/farao/upload/outputs, then the remote relative destination dir should be 'outputs', FTPClient will append it itself
            ftp.changeWorkingDirectory(ftpConfigurationProperties.getRemoteRelativeDestinationDirectory());
            ftp.enterLocalPassiveMode();
            ftp.setFileType(FTP.BINARY_FILE_TYPE);  // required because ASCII is the default file type, otherwise zip will be corrupted
            LOGGER.info("Attempt to copy {} file to FTP server", fileName);
            successFlag = ftp.storeFile(fileName, inputStream);
            if (successFlag) {
                LOGGER.info("File {} copied successfully to FTP server", fileName);
            } else {
                LOGGER.error("File {} couldn't be copied successfully to FTP server", fileName);
            }

            ftp.disconnect();
            LOGGER.info("Connection closed");
            return successFlag;
        } catch (IOException e) {
            LOGGER.error("Fail during upload", e);
            return successFlag;
        }
    }
}
