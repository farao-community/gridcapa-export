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
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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

    public void upload(String fileName, boolean unzip, InputStream inputStream) throws ClientAdapterException {
        int performedRetries = 0;
        final int maxRetryCount = ftpConfigurationProperties.getRetryCount();
        final int retrySleep = ftpConfigurationProperties.getRetrySleep();
        boolean successfulFtpSend = false;
        while (performedRetries <= maxRetryCount && !successfulFtpSend) {
            try {
                TimeUnit.SECONDS.sleep((long) performedRetries * retrySleep);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            performedRetries++;
            successfulFtpSend = performSingleUploadAttempt(fileName, unzip, inputStream);
        }
        if (!successfulFtpSend) {
            throw new ClientAdapterException(String.format("Upload of file %s failed after %d retries", fileName, maxRetryCount));
        }
    }

    private boolean performSingleUploadAttempt(String fileName, boolean unzip, InputStream inputStream) {
        boolean successFlag;
        try {
            FTPClient ftp = new FTPClient(); // NOSONAR
            LOGGER.info("Attempt to connect to FTP server");
            ftp.connect(ftpConfigurationProperties.getHost(), ftpConfigurationProperties.getPort());

            int reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftp.disconnect();
                return false;
            }
            ftp.login(ftpConfigurationProperties.getAccessKey(), ftpConfigurationProperties.getSecretKey());
            LOGGER.info("Connection established");

            // if ftp working dir is /home/farao/upload and you want to upload files under /home/farao/upload/outputs, then the remote relative destination dir should be 'outputs', FTPClient will append it itself
            ftp.changeWorkingDirectory(ftpConfigurationProperties.getRemoteRelativeDestinationDirectory());
            ftp.enterLocalPassiveMode();
            ftp.setFileType(FTP.BINARY_FILE_TYPE);  // required because ASCII is the default file type, otherwise zip will be corrupted
            if (unzip) {
                LOGGER.info("Attempt to unzip {} to FTP server", fileName);
                successFlag = unzipAndStoreFiles(ftp, fileName, inputStream);
            } else {
                logAttemptStoreFile(fileName);
                successFlag = ftp.storeFile(fileName, inputStream);
                logSuccess(successFlag, fileName);
            }
            ftp.disconnect();
            LOGGER.info("Connection closed");
            return successFlag;
        } catch (IOException e) {
            LOGGER.error("Fail during upload", e);
            return false;
        }
    }

    private boolean unzipAndStoreFiles(FTPClient ftp, String fileName, InputStream inputStream) throws IOException {
        final String directory = fileName.replace(".zip", "");
        ftp.makeDirectory(directory);
        ftp.changeWorkingDirectory(directory);
        boolean successFlag = true;
        try (ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
            ZipEntry zipEntry = zipInputStream.getNextEntry();
            while (zipEntry != null) {
                final String zippedFileName = zipEntry.getName();
                logAttemptStoreFile(zippedFileName);
                final boolean oneFileSuccessFlag = ftp.storeFile(zippedFileName, zipInputStream);
                logSuccess(oneFileSuccessFlag, zippedFileName);
                successFlag = oneFileSuccessFlag && successFlag;
                zipEntry = zipInputStream.getNextEntry();
            }
        }
        return successFlag;
    }

    private static void logAttemptStoreFile(String fileName) {
        LOGGER.info("Attempt to copy {} file to FTP server", fileName);
    }

    private static void logSuccess(boolean isSuccessful, String fileName) {
        if (isSuccessful) {
            LOGGER.info("File {} copied successfully to FTP server", fileName);
        } else {
            LOGGER.error("File {} couldn't be copied to FTP server", fileName);
        }
    }
}
