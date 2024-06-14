package com.farao_community.farao.gridcapa.export.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

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
