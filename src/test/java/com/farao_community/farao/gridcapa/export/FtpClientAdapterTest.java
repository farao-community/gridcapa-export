package com.farao_community.farao.gridcapa.export;

import org.junit.jupiter.api.*;
import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.DirectoryEntry;
import org.mockftpserver.fake.filesystem.FileSystem;
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@SpringBootTest
class FtpClientAdapterTest {

    private FakeFtpServer fakeFtpServer;

    @Autowired
    private FtpClientAdapter ftpClientAdapter;

    @BeforeEach
    public void setup() throws IOException {
        fakeFtpServer = new FakeFtpServer();
        fakeFtpServer.addUserAccount(new UserAccount("user", "password", "/data"));

        FileSystem fileSystem = new UnixFakeFileSystem();
        fileSystem.add(new DirectoryEntry("/data"));
        fakeFtpServer.setFileSystem(fileSystem);
        fakeFtpServer.setServerControlPort(3030);
        fakeFtpServer.start();
        ftpClientAdapter.open();
    }

    @AfterEach
    public void teardown() throws IOException {
        ftpClientAdapter.close();
        fakeFtpServer.stop();
    }

    @Test
    void checkFileTransferredToRemoteDestination()
        throws IOException {
        ftpClientAdapter.upload("test.txt", new ByteArrayInputStream("test content".getBytes(StandardCharsets.UTF_8)));
        Assertions.assertTrue(fakeFtpServer.getFileSystem().exists("/data/test.txt"));
    }
}
