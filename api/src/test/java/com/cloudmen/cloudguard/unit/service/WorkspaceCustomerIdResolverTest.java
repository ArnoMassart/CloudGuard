package com.cloudmen.cloudguard.unit.service;

import com.cloudmen.cloudguard.dto.workspace.WorkspaceCustomer;
import com.cloudmen.cloudguard.service.WorkspaceCustomerIdResolver;
import com.google.api.services.admin.directory.model.Customer;
import com.google.api.services.admin.directory.model.CustomerPostalAddress;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorkspaceCustomerIdResolverTest {

    @Test
    void toWorkspaceCustomer_prefersOrganizationNameFromPostalAddressOverDomain() {
        Customer customer = new Customer();
        customer.setId("C-abc123");
        customer.setCustomerDomain("acme.example.com");
        CustomerPostalAddress addr = new CustomerPostalAddress();
        addr.setOrganizationName("Acme Corp");
        customer.setPostalAddress(addr);

        WorkspaceCustomer ws = WorkspaceCustomerIdResolver.toWorkspaceCustomer(customer);

        assertEquals("C-abc123", ws.id());
        assertEquals("Acme Corp", ws.displayName());
    }

    @Test
    void toWorkspaceCustomer_usesCustomerDomainWhenOrganizationNameMissing() {
        Customer customer = new Customer();
        customer.setId("C-abc123");
        customer.setCustomerDomain("acme.example.com");

        WorkspaceCustomer ws = WorkspaceCustomerIdResolver.toWorkspaceCustomer(customer);

        assertEquals("C-abc123", ws.id());
        assertEquals("acme.example.com", ws.displayName());
    }

    @Test
    void toWorkspaceCustomer_ignoresBlankOrganizationNameAndUsesDomain() {
        Customer customer = new Customer();
        customer.setId("C-a");
        customer.setCustomerDomain("only.domain.com");
        CustomerPostalAddress addr = new CustomerPostalAddress();
        addr.setOrganizationName("   ");
        customer.setPostalAddress(addr);

        WorkspaceCustomer ws = WorkspaceCustomerIdResolver.toWorkspaceCustomer(customer);

        assertEquals("only.domain.com", ws.displayName());
    }

    @Test
    void toWorkspaceCustomer_fallsBackWhenDomainMissing() {
        Customer customer = new Customer();
        customer.setId("C-xyz");

        WorkspaceCustomer ws = WorkspaceCustomerIdResolver.toWorkspaceCustomer(customer);

        assertEquals("C-xyz", ws.id());
        assertEquals("Workspace C-xyz", ws.displayName());
    }

    @Test
    void toWorkspaceCustomer_trimsIdAndDomain() {
        Customer customer = new Customer();
        customer.setId("  C-t  ");
        customer.setCustomerDomain("  domain.test  ");

        WorkspaceCustomer ws = WorkspaceCustomerIdResolver.toWorkspaceCustomer(customer);

        assertEquals("C-t", ws.id());
        assertEquals("domain.test", ws.displayName());
    }

    @Test
    void toWorkspaceCustomer_trimsOrganizationName() {
        Customer customer = new Customer();
        customer.setId("C-1");
        CustomerPostalAddress addr = new CustomerPostalAddress();
        addr.setOrganizationName("  Acme Corp  ");
        customer.setPostalAddress(addr);

        WorkspaceCustomer ws = WorkspaceCustomerIdResolver.toWorkspaceCustomer(customer);

        assertEquals("Acme Corp", ws.displayName());
    }
}
