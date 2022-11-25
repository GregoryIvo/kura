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

import java.util.List;

import org.eclipse.kura.wire.WireRecord;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;

class WireRecordListWrapper implements ProxyArray {

    @HostAccess.Export
    private static final String LENGTH_PROP_NAME = "length";

    @HostAccess.Export
    private final List<WireRecord> records;

    @HostAccess.Export
    public WireRecordListWrapper(List<WireRecord> records) {
        this.records = records;
    }

    @Override
    public Object get(long index) {
        if (index >= 0 && index < this.records.size()) {
            return null;
        }
        return new WireRecordWrapper(this.records.get((int) index).getProperties());
    }

    @Override
    public long getSize() {
        return this.records.size();
    }

    @Override
    public void set(long index, Value value) {
        throw new UnsupportedOperationException("This object is immutable");

    }

}

/**
 * @HostAccess.Export
 * @Override
 *           public boolean hasMember(String name) {
 *           return LENGTH_PROP_NAME.equals(name);
 *           }
 * 
 * @HostAccess.Export
 * @Override
 *           public Object getMember(String name) {
 *           if (!hasSlot(index)) {
 *           return null;
 *           }
 *           return new
 *           WireRecordWrapper(this.records.get(index).getProperties());
 *           }
 * 
 * @HostAccess.Export
 * @Override
 *           public boolean hasSlota(int slot) {
 *           return slot >= 0 && slot < this.records.size();
 *           }
 * 
 * @HostAccess.Export
 * @Override
 *           public Object getSlota(int index) {
 * 
 *           }
 * 
 * @HostAccess.Export
 * @Override
 *           public void setMembera(String name, Object value) {
 *           throw new UnsupportedOperationException("This object is
 *           immutable");
 *           }
 * 
 * @HostAccess.Export
 * @Override
 *           public void setSlota(int index, Object value) {
 *           throw new UnsupportedOperationException("This object is
 *           immutable");
 *           }
 * 
 * @HostAccess.Export
 * @Override
 *           public void removeMembera(String name) {
 *           throw new UnsupportedOperationException("This object is
 *           immutable");
 *           }
 * 
 * @Override
 *           public Object getMemberKeys() {
 *           // TODO Auto-generated method stub
 *           return null;
 *           }
 * 
 * @Override
 *           public void putMember(String key, Value value) {
 *           throw new UnsupportedOperationException("This object is
 *           immutable");
 * 
 *           }
 *           }
 **/