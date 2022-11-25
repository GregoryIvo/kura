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
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireReceiver;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.WireSupport;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.osgi.service.wireadmin.Wire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScriptFilter implements WireEmitter, WireReceiver, ConfigurableComponent {

    private static final Logger logger = LoggerFactory.getLogger(ScriptFilter.class);

    private static final String SCRIPT_PROPERTY_KEY = "script";
    private static final String SCRIPT_CONTEXT_DROP_PROPERTY_KEY = "script.context.drop";

    private static final String FILTER_LANGAUGE = "js";

    // private CompiledScript script;
    private String script;
    private Value bindings;
    private Value polyglotBindings;

    private volatile WireHelperService wireHelperService;
    private WireSupport wireSupport;

    private Context scriptContext;

    public void bindWireHelperService(final WireHelperService wireHelperService) {
        if (this.wireHelperService == null) {
            this.wireHelperService = wireHelperService;
        }
    }

    public void unbindWireHelperService(final WireHelperService wireHelperService) {
        if (this.wireHelperService == wireHelperService) {
            this.wireHelperService = null;
        }
    }

    public void activate(final ComponentContext componentContext, final Map<String, Object> properties)
            throws ComponentException {
        logger.info("GREG: Activating Script Filter...");

        this.wireSupport = this.wireHelperService.newWireSupport(this,
                (ServiceReference<WireComponent>) componentContext.getServiceReference());

        killAndCreateNewContext();

        updated(properties);

        logger.info("ActivatingScript Filter... Done");
    }

    public void deactivate() {
        logger.info("Deactivating Script Filter...");

        if (scriptContext != null) {
            scriptContext.close(true);
        }

    }

    public synchronized void updated(final Map<String, Object> properties) {
        logger.info("Updating Script Filter...");

        final String scriptSource = (String) properties.get(SCRIPT_PROPERTY_KEY);

        if (scriptSource == null) {
            logger.warn("Script source is null");
            return;
        }

        this.script = scriptSource; // todo: skip compiling the script for now.
//        this.script = null;
//        
//        try {
//            this.script = ((Compilable) this.scriptEngine).compile(scriptSource);
//        } catch (ScriptException e) {
//            logger.warn("Failed to compile script", e);
//        }

        if (this.bindings == null || (Boolean) properties.getOrDefault(SCRIPT_CONTEXT_DROP_PROPERTY_KEY, false)) {
            killAndCreateNewContext();
        }

        logger.info("Updating Script Filter... Done");
    }

    private void killAndCreateNewContext() {
        if (this.scriptContext != null) {
            this.scriptContext.close();
        }

        // this.scriptContext = Context.create(FILTER_LANGAUGE);
        this.scriptContext = Context.newBuilder(FILTER_LANGAUGE).allowAllAccess(true).allowValueSharing(true).build();

        this.bindings = this.scriptContext.getBindings(FILTER_LANGAUGE);

        this.polyglotBindings = this.scriptContext.getPolyglotBindings();
        createBindings();
    }

    @Override
    public synchronized void onWireReceive(WireEnvelope wireEnvelope) {
        if (this.script == null) {
            logger.warn("Failed to compile script");
            return;
        }

        try {
            final WireEnvelopeWrapper inputEnvelopeWrapper = new WireEnvelopeWrapper(
                    new WireRecordListWrapper(wireEnvelope.getRecords()), wireEnvelope.getEmitterPid());
            final OutputWireRecordListWrapper outputEnvelopeWrapper = new OutputWireRecordListWrapper();

            this.bindings.putMember("input", inputEnvelopeWrapper);
            this.bindings.putMember("output", outputEnvelopeWrapper);

            this.scriptContext.eval(FILTER_LANGAUGE, this.script);

            final List<WireRecord> result = outputEnvelopeWrapper.getRecords();

            if (result != null) {
                this.wireSupport.emit(result);
            }
        } catch (Exception e) {
            logger.warn("Failed to execute script", e);
        }
    }

    private void createBindings() {

        this.bindings.putMember("logger", logger);

        this.bindings.putMember("newWireRecord", (Supplier<WireRecordWrapper>) WireRecordWrapper::new);

        this.bindings.putMember("newBooleanValue", (Function<Boolean, TypedValue<?>>) TypedValues::newBooleanValue);
        this.bindings.putMember("newByteArrayValue", (Function<byte[], TypedValue<?>>) TypedValues::newByteArrayValue);
        this.bindings.putMember("newDoubleValue",
                (Function<Number, TypedValue<?>>) num -> TypedValues.newDoubleValue(num.doubleValue()));
        this.bindings.putMember("newFloatValue",
                (Function<Number, TypedValue<?>>) num -> TypedValues.newFloatValue(num.floatValue()));
        this.bindings.putMember("newIntegerValue",
                (Function<Number, TypedValue<?>>) num -> TypedValues.newIntegerValue(num.intValue()));
        this.bindings.putMember("newLongValue",
                (Function<Number, TypedValue<?>>) num -> TypedValues.newLongValue(num.longValue()));
        this.bindings.putMember("newStringValue",
                (Function<Object, TypedValue<?>>) obj -> TypedValues.newStringValue(obj.toString()));

        this.bindings.putMember("newByteArray", (Function<Integer, byte[]>) size -> new byte[size]);

        for (DataType type : DataType.values()) {
            this.bindings.putMember(type.name(), type);
        }

        this.bindings.removeMember("exit");
        this.bindings.removeMember("quit");

    }

    @Override
    public Object polled(Wire wire) {
        return this.wireSupport.polled(wire);
    }

    @Override
    public void consumersConnected(Wire[] wires) {
        this.wireSupport.consumersConnected(wires);
    }

    @Override
    public void updated(Wire wire, Object value) {
        this.wireSupport.updated(wire, value);
    }

    @Override
    public void producersConnected(Wire[] wires) {
        this.wireSupport.producersConnected(wires);
    }
}
