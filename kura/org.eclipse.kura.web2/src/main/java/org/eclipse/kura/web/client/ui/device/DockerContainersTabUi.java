/*******************************************************************************
 * Copyright (c) 2011, 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.client.ui.device;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.messages.ValidationMessages;
import org.eclipse.kura.web.client.ui.EntryClassUi;
import org.eclipse.kura.web.client.ui.ServicesUi;
import org.eclipse.kura.web.client.ui.Tab;
import org.eclipse.kura.web.client.util.EventService;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.client.util.request.RequestQueue;
import org.eclipse.kura.web.shared.ForwardedEventTopic;
import org.eclipse.kura.web.shared.model.GwtComponentInstanceInfo;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtGroupedNVPair;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtComponentService;
import org.eclipse.kura.web.shared.service.GwtComponentServiceAsync;
import org.eclipse.kura.web.shared.service.GwtDeviceService;
import org.eclipse.kura.web.shared.service.GwtDeviceServiceAsync;
import org.eclipse.kura.web.shared.service.GwtDockerConfigurableGenericManagerService;
import org.eclipse.kura.web.shared.service.GwtDockerConfigurableGenericManagerServiceAsync;
import org.eclipse.kura.web.shared.service.GwtRestrictedComponentServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.PanelHeader;
import org.gwtbootstrap3.client.ui.gwt.CellTable;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.cellview.client.TextHeader;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;

public class DockerContainersTabUi extends Composite implements Tab {

    private static DockerContainersTabUiUiBinder uiBinder = GWT.create(DockerContainersTabUiUiBinder.class);

    interface DockerContainersTabUiUiBinder extends UiBinder<Widget, DockerContainersTabUi> {
    }

    private static final Messages MSGS = GWT.create(Messages.class);
    private static final ValidationMessages validationMessages = GWT.create(ValidationMessages.class);
    private static final String ROW_HEADER_STYLE = "rowHeader";
    private static final String STATUS_TABLE_ROW_STYLE = "status-table-row";

    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
    private final GwtDeviceServiceAsync gwtDeviceService = GWT.create(GwtDeviceService.class);
    private final GwtComponentServiceAsync gwtComponentService = GWT.create(GwtComponentService.class);

    private boolean isRequestRunning = false;

    // private ConfigurableComponentUi containerPanel;
    private GwtRestrictedComponentServiceAsync backend;
    private List<String> factoriesList;

    @UiField
    Panel configurationArea;
    @UiField
    PanelHeader contentPanelHeader;
    @UiField
    Panel mgmtPanel;

    @UiField
    Button containersRefresh;
    @UiField
    Button containersStart;
    @UiField
    Button containersStop;

    @UiField
    CellTable<GwtGroupedNVPair> bundlesGrid = new CellTable<>();
    private final ListDataProvider<GwtGroupedNVPair> bundlesDataProvider = new ListDataProvider<>();
    private final SingleSelectionModel<GwtGroupedNVPair> selectionModel = new SingleSelectionModel<>();

    private final GwtDeviceServiceAsync deviceService = GWT.create(GwtDeviceService.class);
    private final GwtSecurityTokenServiceAsync securityTokenService = GWT.create(GwtSecurityTokenService.class);
    private HashSet allTrackedPids;

    public DockerContainersTabUi() {
        initWidget(uiBinder.createAndBindUi(this));
        loadContainersTable(this.bundlesGrid, this.bundlesDataProvider);

        this.containersRefresh.setText(MSGS.refresh());
        this.containersStart.setText(MSGS.deviceTabContainerStart());
        this.containersStop.setText(MSGS.deviceTabContainerStop());

        this.selectionModel.clear();
        this.bundlesGrid.setSelectionModel(this.selectionModel);
        this.selectionModel.addSelectionChangeHandler(event -> updateButtons());
        this.containersRefresh.addClickHandler(event -> refresh());
        this.containersStart.addClickHandler(event -> startSelectedContainer());
        this.containersStop.addClickHandler(event -> stopSelectedContainer());

        updateButtons();

        EventService.Handler onBundleUpdatedHandler = eventInfo -> {
            if (DockerContainersTabUi.this.isVisible() && DockerContainersTabUi.this.isAttached()) {
                refresh();
            }
        };

        EventService.subscribe(ForwardedEventTopic.DOCKER_RUNNING, onBundleUpdatedHandler);
        EventService.subscribe(ForwardedEventTopic.DOCKER_STARTED, onBundleUpdatedHandler);
        EventService.subscribe(ForwardedEventTopic.DOCKER_STOPPED, onBundleUpdatedHandler);

        // this.containerPanl
        backend = new DockerConfigurableGenericManagerServiceWrapper();
    }

    private void updateButtons() {
        GwtGroupedNVPair selected = this.selectionModel.getSelectedObject();

        this.containersStart.setEnabled(false);
        this.containersStop.setEnabled(false);
        this.mgmtPanel.setVisible(false);
        // TODO: add panel here

        String status;

        if (selected == null || (status = selected.getStatus()) == null) {
            return;
        }

        boolean isActive = "bndActive".equals(status);

        this.containersStart.setEnabled(!isActive);
        this.containersStop.setEnabled(isActive);

        RequestQueue.submit(c -> this.securityTokenService.generateSecurityToken(
                c.callback(token -> this.backend.getConfiguration(token, selected.getId(), c.callback(result -> {

                    this.contentPanelHeader.setText(selected.getName());

                    final ServicesUi servicesUi = new ServicesUi(result);
                    servicesUi.setBackend(new ServicesUi.Backend() {

                        @Override
                        public void updateComponentConfiguration(GwtXSRFToken token, GwtConfigComponent component,
                                AsyncCallback<Void> callback) {
                            DockerContainersTabUi.this.backend.updateConfiguration(token, component, callback);

                        }

                        @Override
                        public void deleteFactoryConfiguration(GwtXSRFToken token, String pid,
                                AsyncCallback<Void> callback) {
                            DockerContainersTabUi.this.backend.deleteFactoryConfiguration(token, pid, callback);
                        }
                    });

                    servicesUi.setDeleteButtonVisible(false);

                    this.mgmtPanel.add(servicesUi);
                    this.configurationArea.setVisible(true);
                })))));
    }

    private void startSelectedContainer() {
        EntryClassUi.showWaitModal();

        this.securityTokenService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onSuccess(GwtXSRFToken token) {
                DockerContainersTabUi.this.deviceService.startContainer(token,
                        DockerContainersTabUi.this.selectionModel.getSelectedObject().getId(),
                        new AsyncCallback<Void>() {

                            @Override
                            public void onFailure(Throwable caught) {
                                EntryClassUi.hideWaitModal();
                                FailureHandler.handle(caught);

                            }

                            @Override
                            public void onSuccess(Void result) {
                                EntryClassUi.hideWaitModal();
                            }
                        });

            }

            @Override
            public void onFailure(Throwable caught) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(caught);
            }
        });
        refresh();
    }

    private void stopSelectedContainer() {
        EntryClassUi.showWaitModal();
        this.securityTokenService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable caught) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(caught);
            }

            @Override
            public void onSuccess(GwtXSRFToken token) {
                DockerContainersTabUi.this.deviceService.stopContainer(token,
                        DockerContainersTabUi.this.selectionModel.getSelectedObject().getId(),
                        new AsyncCallback<Void>() {

                            @Override
                            public void onFailure(Throwable caught) {
                                EntryClassUi.hideWaitModal();
                                FailureHandler.handle(caught);

                            }

                            @Override
                            public void onSuccess(Void result) {
                                EntryClassUi.hideWaitModal();
                                DockerContainersTabUi.this.containersStop.setEnabled(false);
                            }
                        });
            }
        });
        refresh();
    }

    private void loadContainersTable(CellTable<GwtGroupedNVPair> bundlesGrid2,
            ListDataProvider<GwtGroupedNVPair> dataProvider) {

        TextColumn<GwtGroupedNVPair> col1 = new TextColumn<GwtGroupedNVPair>() {

            @Override
            public String getValue(GwtGroupedNVPair object) {
                return object.getId();
            }
        };
        col1.setCellStyleNames(STATUS_TABLE_ROW_STYLE);
        TextHeader id = new TextHeader(MSGS.deviceBndId());
        id.setHeaderStyleNames(ROW_HEADER_STYLE);
        bundlesGrid2.addColumn(col1, id);

        TextColumn<GwtGroupedNVPair> col2 = new TextColumn<GwtGroupedNVPair>() {

            @Override
            public String getValue(GwtGroupedNVPair object) {
                return validationMessages.getString(object.getStatus());
            }
        };
        col2.setCellStyleNames(STATUS_TABLE_ROW_STYLE);
        TextHeader state = new TextHeader(MSGS.deviceBndState());
        state.setHeaderStyleNames(ROW_HEADER_STYLE);
        bundlesGrid2.addColumn(col2, state);

        TextColumn<GwtGroupedNVPair> col3 = new TextColumn<GwtGroupedNVPair>() {

            @Override
            public String getValue(GwtGroupedNVPair object) {
                return object.getName();
            }
        };
        col3.setCellStyleNames(STATUS_TABLE_ROW_STYLE);
        TextHeader name = new TextHeader(MSGS.deviceBndName());
        name.setHeaderStyleNames(ROW_HEADER_STYLE);
        bundlesGrid2.addColumn(col3, name);

        TextColumn<GwtGroupedNVPair> col4 = new TextColumn<GwtGroupedNVPair>() {

            @Override
            public String getValue(GwtGroupedNVPair object) {
                return object.getVersion();
            }
        };
        col4.setCellStyleNames(STATUS_TABLE_ROW_STYLE);
        TextHeader version = new TextHeader(MSGS.deviceBndVersion());
        version.setHeaderStyleNames(ROW_HEADER_STYLE);
        bundlesGrid2.addColumn(col4, version);

        dataProvider.addDataDisplay(bundlesGrid2);
    }

    @Override
    public void setDirty(boolean flag) {
        // Not needed
    }

    @Override
    public boolean isDirty() {
        return true;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void refresh() {
        if (this.isRequestRunning) {
            return;
        }

        this.isRequestRunning = true;
        EntryClassUi.showWaitModal();

        this.bundlesDataProvider.getList().clear();

        this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                DockerContainersTabUi.this.isRequestRunning = false;
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(ex);
            }

            @Override
            public void onSuccess(GwtXSRFToken token) {
                DockerContainersTabUi.this.gwtDeviceService.findContainers(token,
                        new AsyncCallback<List<GwtGroupedNVPair>>() {

                            @Override
                            public void onFailure(Throwable caught) {
                                DockerContainersTabUi.this.isRequestRunning = false;
                                EntryClassUi.hideWaitModal();
                                FailureHandler.handle(caught);
                                DockerContainersTabUi.this.bundlesDataProvider.flush();
                            }

                            @Override
                            public void onSuccess(List<GwtGroupedNVPair> result) {
                                EntryClassUi.hideWaitModal();
                                DockerContainersTabUi.this.isRequestRunning = false;
                                for (GwtGroupedNVPair resultPair : result) {
                                    DockerContainersTabUi.this.bundlesDataProvider.getList().add(resultPair);
                                }
                                int size = DockerContainersTabUi.this.bundlesDataProvider.getList().size();
                                DockerContainersTabUi.this.bundlesGrid.setVisibleRange(0, size);
                                DockerContainersTabUi.this.bundlesDataProvider.flush();
                                DockerContainersTabUi.this.selectionModel.clear();
                                updateButtons();
                            }
                        });
            }

        });

        RequestQueue.submit(
                c -> this.securityTokenService.generateSecurityToken(c.callback(token -> this.gwtComponentService
                        .findTrackedPids(token, c.callback(pids -> this.allTrackedPids = new HashSet<>(pids))))));
    }

    @Override
    public void clear() {
        this.mgmtPanel.clear();
        this.configurationArea.setVisible(false);
        // Not needed
    }

    private static class DockerConfigurableGenericManagerServiceWrapper implements GwtRestrictedComponentServiceAsync {

        private static GwtDockerConfigurableGenericManagerServiceAsync wrapped = GWT
                .create(GwtDockerConfigurableGenericManagerService.class);

        @Override
        public void listFactoryPids(AsyncCallback<Set<String>> callback) {
            wrapped.listFactoryPids(callback);
        }

        @Override
        public void listServiceInstances(AsyncCallback<List<GwtComponentInstanceInfo>> callback) {
            wrapped.listServiceInstances(callback);
        }

        @Override
        public void createFactoryConfiguration(GwtXSRFToken token, String pid, String factoryPid,
                AsyncCallback<Void> callback) {
            wrapped.createFactoryConfiguration(token, pid, factoryPid, callback);
        }

        @Override
        public void getConfiguration(GwtXSRFToken token, String pid, AsyncCallback<GwtConfigComponent> callback) {
            wrapped.getConfiguration(token, pid, callback);
        }

        @Override
        public void updateConfiguration(GwtXSRFToken token, GwtConfigComponent component,
                AsyncCallback<Void> callback) {
            wrapped.updateConfiguration(token, component, callback);
        }

        @Override
        public void deleteFactoryConfiguration(GwtXSRFToken token, String pid, AsyncCallback<Void> callback) {
            wrapped.deleteFactoryConfiguration(token, pid, callback);
        }
    }

}
