/*******************************************************************************
 * Copyright (c) 2017, 2020 Eurotech and/or its affiliates and others
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

package org.eclipse.kura.wire.javascript.filter.provider;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;

class WireRecordWrapper implements ProxyObject {

    @HostAccess.Export
    Map<String, TypedValue<?>> properties;

    @HostAccess.Export
    public WireRecordWrapper() {
        this.properties = new HashMap<>();
    }

    public WireRecordWrapper(Map<String, TypedValue<?>> properties) {
        this.properties = properties;
    }

    @Override
    public Object getMember(String key) {
        return this.properties.get(key);
    }

    @Override
    public Object getMemberKeys() {
        return Collections.unmodifiableSet(this.properties.keySet());
    }

    @Override
    public boolean hasMember(String key) {
        return this.properties.containsKey(key);
    }

    @Override
    public void putMember(String key, Value value) {
        if (value == null) {
            this.properties.remove(key);
            return;
        }
        if (!(value.asHostObject() instanceof TypedValue)) {
            throw new IllegalArgumentException("WireRecord properties must be instances of TypedValue");
        }
        this.properties.put(key, TypedValues.newTypedValue(value.asHostObject()));
    }
}
