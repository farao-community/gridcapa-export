package com.farao_community.farao.gridcapa.export;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

@Service
public class FtpClientAdapter {

    private final FtpConfigurationProperties ftpConfigurationProperties;
    private final FTPClient ftp = new FTPClient();

    public FtpClientAdapter(FtpConfigurationProperties ftpConfigurationProperties) {
        this.ftpConfigurationProperties = ftpConfigurationProperties;
    }

    void open() throws IOException {
        ftp.connect(ftpConfigurationProperties.getHost(), ftpConfigurationProperties.getPort());
        int reply = ftp.getReplyCode();
        if (!FTPReply.isPositiveCompletion(reply)) {
            ftp.disconnect();
            throw new IOException("Exception in connecting to FTP Server");
        }
        ftp.login(ftpConfigurationProperties.getAccessKey(), ftpConfigurationProperties.getSecretKey());
    }

    void upload(String fileName, InputStream inputStream) throws IOException {
        ftp.storeFile(makeRemotePath(fileName), inputStream);
    }

    private String makeRemotePath(String fileName) {
        if (ftpConfigurationProperties.getRemoteDestinationDirectory().endsWith("/")) {
            return ftpConfigurationProperties.getRemoteDestinationDirectory() + fileName;
        } else {
            return ftpConfigurationProperties.getRemoteDestinationDirectory() + "/" + fileName;
        }
    }

    void close() throws IOException {
        ftp.disconnect();
    }
}
