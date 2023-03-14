/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.export.adapter;

import java.io.InputStream;

/**
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 */

public interface ClientAdapter {

    void open() throws Exception;

    void upload(String fileName, InputStream inputStream) throws Exception;

    void close() throws Exception;
}
