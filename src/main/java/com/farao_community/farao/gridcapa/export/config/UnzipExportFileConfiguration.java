/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.export.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @author Marc Schwitzguébel {@literal <marc.schwitzguebel at rte-france.com>}
 */
@Configuration
@ConfigurationProperties(prefix = "export")
public class UnzipExportFileConfiguration {

    private List<String> unzipFiles;

    public List<String> getUnzipFiles() {
        return unzipFiles;
    }

    public void setUnzipFiles(List<String> unzipFiles) {
        this.unzipFiles = unzipFiles;
    }

}
