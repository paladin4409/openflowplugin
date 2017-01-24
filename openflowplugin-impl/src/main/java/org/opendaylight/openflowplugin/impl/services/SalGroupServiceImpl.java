/**
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.openflowplugin.impl.services;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.Future;
import javax.annotation.Nullable;
import org.opendaylight.openflowplugin.api.openflow.device.DeviceContext;
import org.opendaylight.openflowplugin.api.openflow.device.RequestContextStack;
import org.opendaylight.openflowplugin.api.openflow.rpc.ItemLifeCycleSource;
import org.opendaylight.openflowplugin.api.openflow.rpc.listener.ItemLifecycleListener;
import org.opendaylight.openflowplugin.impl.util.ErrorUtil;
import org.opendaylight.openflowplugin.openflow.md.core.sal.convertor.ConvertorExecutor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.AddGroupInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.AddGroupOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.RemoveGroupInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.RemoveGroupOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.SalGroupService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.UpdateGroupInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.UpdateGroupOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.GroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.GroupKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SalGroupServiceImpl implements SalGroupService, ItemLifeCycleSource {
    private static final Logger LOG = LoggerFactory.getLogger(SalGroupServiceImpl.class);
    private final GroupService<AddGroupInput, AddGroupOutput> addGroup;
    private final GroupService<Group, UpdateGroupOutput> updateGroup;
    private final GroupService<RemoveGroupInput, RemoveGroupOutput> removeGroup;
    private final GroupMessageService<AddGroupOutput> addGroupMessage;
    private final GroupMessageService<UpdateGroupOutput> updateGroupMessage;
    private final GroupMessageService<RemoveGroupOutput> removeGroupMessage;

    private final DeviceContext deviceContext;
    private ItemLifecycleListener itemLifecycleListener;

    public SalGroupServiceImpl(final RequestContextStack requestContextStack, final DeviceContext deviceContext, final ConvertorExecutor convertorExecutor) {
        this.deviceContext = deviceContext;
        addGroup = new GroupService<>(requestContextStack, deviceContext, AddGroupOutput.class, convertorExecutor);
        updateGroup = new GroupService<>(requestContextStack, deviceContext, UpdateGroupOutput.class, convertorExecutor);
        removeGroup = new GroupService<>(requestContextStack, deviceContext, RemoveGroupOutput.class, convertorExecutor);

        addGroupMessage = new GroupMessageService<>(requestContextStack, deviceContext, AddGroupOutput.class);
        updateGroupMessage = new GroupMessageService<>(requestContextStack, deviceContext, UpdateGroupOutput.class);
        removeGroupMessage = new GroupMessageService<>(requestContextStack, deviceContext, RemoveGroupOutput.class);
    }

    @Override
    public void setItemLifecycleListener(@Nullable ItemLifecycleListener itemLifecycleListener) {
        this.itemLifecycleListener = itemLifecycleListener;
    }

    @Override
    public Future<RpcResult<AddGroupOutput>> addGroup(final AddGroupInput input) {
        final ListenableFuture<RpcResult<AddGroupOutput>> resultFuture = addGroupMessage.isSupported()
            ? addGroupMessage.handleServiceCall(input)
            : addGroup.handleServiceCall(input);

        Futures.addCallback(resultFuture, new FutureCallback<RpcResult<AddGroupOutput>>() {
            @Override
            public void onSuccess(RpcResult<AddGroupOutput> result) {
                if (result.isSuccessful()) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Group add with id={} finished without error", input.getGroupId().getValue());
                    }
                    deviceContext.getDeviceGroupRegistry().store(input.getGroupId());
                    addIfNecessaryToDS(input.getGroupId(), input);
                } else {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Group add with id={} failed, errors={}", input.getGroupId().getValue(),
                                ErrorUtil.errorsToString(result.getErrors()));
                    }
                }
            }

            @Override
            public void onFailure(Throwable t) {
                LOG.warn("Service call for adding group={} failed, reason: {}", input.getGroupId().getValue(), t);
            }
        });
        return resultFuture;
    }


    @Override
    public Future<RpcResult<UpdateGroupOutput>> updateGroup(final UpdateGroupInput input) {
        final ListenableFuture<RpcResult<UpdateGroupOutput>> resultFuture = updateGroupMessage.isSupported()
            ? updateGroupMessage.handleServiceCall(input.getUpdatedGroup())
            : updateGroup.handleServiceCall(input.getUpdatedGroup());

        Futures.addCallback(resultFuture, new FutureCallback<RpcResult<UpdateGroupOutput>>() {
            @Override
            public void onSuccess(@Nullable RpcResult<UpdateGroupOutput> result) {
                if (result.isSuccessful()) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Group update with original id={} finished without error",
                                input.getOriginalGroup().getGroupId().getValue());
                    }
                    removeIfNecessaryFromDS(input.getOriginalGroup().getGroupId());
                    addIfNecessaryToDS(input.getUpdatedGroup().getGroupId(), input.getUpdatedGroup());
                } else {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Group update with original id={} failed, errors={}",
                                input.getOriginalGroup().getGroupId(), ErrorUtil.errorsToString(result.getErrors()));
                    }
                }
            }

            @Override
            public void onFailure(Throwable t) {
                LOG.warn("Service call for updating group={} failed, reason: {}",
                        input.getOriginalGroup().getGroupId(), t);
            }
        });
        return resultFuture;
    }

    @Override
    public Future<RpcResult<RemoveGroupOutput>> removeGroup(final RemoveGroupInput input) {
        final ListenableFuture<RpcResult<RemoveGroupOutput>> resultFuture = removeGroupMessage.isSupported()
            ? removeGroupMessage.handleServiceCall(input)
            : removeGroup.handleServiceCall(input);

        Futures.addCallback(resultFuture, new FutureCallback<RpcResult<RemoveGroupOutput>>() {
            @Override
            public void onSuccess(@Nullable RpcResult<RemoveGroupOutput> result) {
                if (result.isSuccessful()) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Group remove with id={} finished without error", input.getGroupId().getValue());
                    }
                    removeGroup.getDeviceRegistry().getDeviceGroupRegistry().markToBeremoved(input.getGroupId());
                    removeIfNecessaryFromDS(input.getGroupId());
                } else {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Group remove with id={} failed, errors={}", input.getGroupId().getValue(),
                                ErrorUtil.errorsToString(result.getErrors()));
                    }
                }
            }

            @Override
            public void onFailure(Throwable t) {
                LOG.warn("Service call for removing group={} failed, reason: {}",
                        input.getGroupId().getValue(), t);
            }
        });
        return resultFuture;
    }

    private void removeIfNecessaryFromDS(final GroupId groupId) {
        if (itemLifecycleListener != null) {
            KeyedInstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group, GroupKey> groupPath
                    = createGroupPath(groupId,
                    deviceContext.getDeviceInfo().getNodeInstanceIdentifier());
            itemLifecycleListener.onRemoved(groupPath);
        }
    }

    private void addIfNecessaryToDS(final GroupId groupId, final Group data) {
        if (itemLifecycleListener != null) {
            KeyedInstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group, GroupKey> groupPath
                    = createGroupPath(groupId,
                    deviceContext.getDeviceInfo().getNodeInstanceIdentifier());
            itemLifecycleListener.onAdded(groupPath, new GroupBuilder(data).build());
        }
    }

    private static KeyedInstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group, GroupKey>
    createGroupPath(final GroupId groupId, final KeyedInstanceIdentifier<Node, NodeKey> nodePath) {
        return nodePath.augmentation(FlowCapableNode.class).
                child(org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group.class, new GroupKey(groupId));
    }
}
