/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.export.adapter;

import com.farao_community.farao.gridcapa.export.configProperties.FtpConfigurationProperties;
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
 */

@Component
@ConditionalOnProperty(prefix = "ftp", name = "active", havingValue = "true")
public class FtpClientAdapter implements ClientAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(FtpClientAdapter.class);

    private final FtpConfigurationProperties ftpConfigurationProperties;
    private final FTPClient ftp = new FTPClient(); // NOSONAR

    public FtpClientAdapter(FtpConfigurationProperties ftpConfigurationProperties) {
        this.ftpConfigurationProperties = ftpConfigurationProperties;
    }

    public void open() throws IOException {
        ftp.connect(ftpConfigurationProperties.getHost(), ftpConfigurationProperties.getPort());
        int reply = ftp.getReplyCode();
        if (!FTPReply.isPositiveCompletion(reply)) {
            ftp.disconnect();
            throw new IOException("Exception in connecting to FTP Server");
        }
        ftp.login(ftpConfigurationProperties.getAccessKey(), ftpConfigurationProperties.getSecretKey());
    }

    public void upload(String fileName, InputStream inputStream) throws IOException {
        // if ftp working dir is /home/farao/upload and you want to upload files under /home/farao/upload/outputs, then the remote relative destination dir should be 'outputs', FTPClient will append it itself
        ftp.changeWorkingDirectory(ftpConfigurationProperties.getRemoteRelativeDestinationDirectory());
        ftp.enterLocalPassiveMode();
        ftp.setFileType(FTP.BINARY_FILE_TYPE);  // required because ASCII is the default file type, otherwise zip will be corrupted
        boolean successFlag = ftp.storeFile(fileName, inputStream);
        if (successFlag) {
            LOGGER.info("File {} copied successfully to FTP server", fileName);
        } else {
            LOGGER.error("File {} couldn't be copied successfully to FTP server", fileName);
        }
    }

    public void close() throws IOException {
        ftp.disconnect();
    }
}
