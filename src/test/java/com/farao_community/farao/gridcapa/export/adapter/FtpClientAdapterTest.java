/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.export.adapter;

import com.farao_community.farao.gridcapa.export.exception.ClientAdapterException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.DirectoryEntry;
import org.mockftpserver.fake.filesystem.FileSystem;
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * @author Mohamed Benrejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */

@SpringBootTest
class FtpClientAdapterTest {

    private FakeFtpServer fakeFtpServer;

    @Autowired
    private FtpClientAdapter ftpClientAdapter;

    @BeforeEach
    public void setup() throws ClientAdapterException {
        fakeFtpServer = new FakeFtpServer();
        fakeFtpServer.addUserAccount(new UserAccount("user", "password", "/data"));

        FileSystem fileSystem = new UnixFakeFileSystem();
        fileSystem.add(new DirectoryEntry("/data"));
        fakeFtpServer.setFileSystem(fileSystem);
        fakeFtpServer.setServerControlPort(3030);
        fakeFtpServer.start();
    }

    @AfterEach
    public void teardown() {
        fakeFtpServer.stop();
    }

    @Test
    void checkFileTransferredToRemoteDestination() throws ClientAdapterException {
        ftpClientAdapter.upload("test.txt", new ByteArrayInputStream("test content".getBytes(StandardCharsets.UTF_8)));
        Assertions.assertTrue(fakeFtpServer.getFileSystem().exists("/data/test.txt"));
    }
}