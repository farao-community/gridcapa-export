package com.farao_community.farao.gridcapa.export;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

@Service
public class FtpClientAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(FtpClientAdapter.class);

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

    void close() throws IOException {
        ftp.disconnect();
    }
}
