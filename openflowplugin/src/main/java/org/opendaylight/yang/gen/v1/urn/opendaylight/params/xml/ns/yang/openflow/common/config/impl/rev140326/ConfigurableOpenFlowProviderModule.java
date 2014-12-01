/**
* Generated file

* Generated from: yang module name: openflow-provider-impl  yang module local name: openflow-provider-impl
* Generated by: org.opendaylight.controller.config.yangjmxgenerator.plugin.JMXGenerator
* Generated at: Wed Apr 02 16:59:36 PDT 2014
*
* Do not modify this file unless it is present under src/main directory
*/
package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.openflow.common.config.impl.rev140326;

import org.opendaylight.openflowplugin.openflow.md.core.sal.OpenflowPluginProvider;

import com.google.common.base.Objects;

/**
*
*/
public final class ConfigurableOpenFlowProviderModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.openflow.common.config.impl.rev140326.AbstractConfigurableOpenFlowProviderModule {

    private OpenflowPluginProvider pluginProvider;

    /**
     * @param identifier
     * @param dependencyResolver
     */
    public ConfigurableOpenFlowProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    /**
     * @param identifier
     * @param dependencyResolver
     * @param oldModule
     * @param oldInstance
     */
    public ConfigurableOpenFlowProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
            ConfigurableOpenFlowProviderModule oldModule, java.lang.AutoCloseable oldInstance) {

        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    protected void customValidation() {
        // Add custom validation for module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        pluginProvider =  new OpenflowPluginProvider();
        pluginProvider.setBroker(getBindingAwareBrokerDependency());
        pluginProvider.setSwitchConnectionProviders(getOpenflowSwitchConnectionProviderDependency());
        pluginProvider.setRole(getRole());
        pluginProvider.initialization();
        return pluginProvider;
    }

    @Override
    public boolean canReuseInstance(
            AbstractConfigurableOpenFlowProviderModule oldModule) {
        // we can reuse if only the role field changed
        boolean noChangeExceptRole = true;
        noChangeExceptRole &= getBindingAwareBrokerDependency().equals(oldModule.getBindingAwareBrokerDependency());
        noChangeExceptRole &= getOpenflowSwitchConnectionProviderDependency().equals(oldModule.getOpenflowSwitchConnectionProviderDependency());
        return noChangeExceptRole;
    }

    @Override
    public AutoCloseable reuseInstance(AutoCloseable oldInstance) {
        OpenflowPluginProvider recycled = (OpenflowPluginProvider) super.reuseInstance(oldInstance);
        // change role if different
        recycled.fireRoleChange(Objects.firstNonNull(getRole(), getRole()));

        return recycled;
    }
}
