/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.rest.position.api;

import org.osgi.util.position.Position;

public class PositionDTO {

    private final double longitude;
    private final double latitude;
    private final double altitude;

    public PositionDTO(Position position) {
        this.longitude = Math.toDegrees(position.getLongitude().getValue());
        this.latitude = Math.toDegrees(position.getLatitude().getValue());
        this.altitude = position.getAltitude().getValue();
    }

    public double getLongitude() {
        return longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getAltitude() {
        return altitude;
    }    
}
