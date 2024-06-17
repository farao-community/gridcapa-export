/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.export;

import com.farao_community.farao.gridcapa.export.config.UnzipExportFileConfiguration;
import com.farao_community.farao.gridcapa.export.config_properties.FtpConfigurationProperties;
import com.farao_community.farao.gridcapa.export.config_properties.SftpConfigurationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * @author Mohamed Benrejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 */
@SuppressWarnings("hideutilityclassconstructor")
@SpringBootApplication
@EnableConfigurationProperties({FtpConfigurationProperties.class, SftpConfigurationProperties.class, UnzipExportFileConfiguration.class})
public class GridcapaExportApplication {
    public static void main(String[] args) {
        SpringApplication.run(GridcapaExportApplication.class, args);
    }
}
