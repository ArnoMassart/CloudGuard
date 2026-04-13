package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.utility.GoogleApiFactory;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.admin.directory.Directory;
import com.google.api.services.admin.directory.DirectoryScopes;
import com.google.api.services.admin.directory.model.Customer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Resolves the Google Workspace customer id for the tenant the delegating admin belongs to,
 * using the Admin SDK Directory API ({@code customers.get(my_customer)}) with domain-wide delegation.
 * <p>
 * Each Workspace customer must authorize the CloudGuard service account with scope
 * {@link DirectoryScopes#ADMIN_DIRECTORY_CUSTOMER_READONLY}.
 */
@Service
public class WorkspaceCustomerIdResolver {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceCustomerIdResolver.class);
    private static final String MY_CUSTOMER = "my_customer";
    private static final AtomicBoolean LOGGED_RESOLVER_DISABLED = new AtomicBoolean();

    private final GoogleApiFactory googleApiFactory;

    @Value("${google.workspace.resolve-customer-id-on-login:true}")
    private boolean resolveOnLogin;

    public WorkspaceCustomerIdResolver(GoogleApiFactory googleApiFactory) {
        this.googleApiFactory = googleApiFactory;
    }

    /**
     * @param delegatingAdminEmail the Google admin user to impersonate (typically the user signing in)
     * @return customer id (e.g. {@code Cxxxxx}) or empty if disabled, missing email, or API failure
     */
    public Optional<String> resolveForDelegatingUser(String delegatingAdminEmail) {
        if (!resolveOnLogin) {
            if (LOGGED_RESOLVER_DISABLED.compareAndSet(false, true)) {
                log.info(
                        "Workspace customer id resolution is off (google.workspace.resolve-customer-id-on-login=false); "
                                + "org linking will use JWT claim only if present.");
            }
            return Optional.empty();
        }
        if (delegatingAdminEmail == null || delegatingAdminEmail.isBlank()) {
            return Optional.empty();
        }
        try {
            Directory directory = googleApiFactory.getDirectoryService(
                    Set.of(DirectoryScopes.ADMIN_DIRECTORY_CUSTOMER_READONLY),
                    delegatingAdminEmail.trim());
            Customer customer = directory.customers().get(MY_CUSTOMER).execute();
            if (customer == null || customer.getId() == null || customer.getId().isBlank()) {
                log.warn(
                        "Directory customers.get({}) returned no id for delegating user {}",
                        MY_CUSTOMER,
                        delegatingAdminEmail);
                return Optional.empty();
            }
            return Optional.of(customer.getId().trim());
        } catch (GoogleJsonResponseException e) {
            log.warn(
                    "Directory customers.get failed for {}: HTTP {} — {} (add scope {} in domain-wide delegation if missing)",
                    delegatingAdminEmail,
                    e.getStatusCode(),
                    e.getDetails() != null ? e.getDetails().getMessage() : e.getMessage(),
                    DirectoryScopes.ADMIN_DIRECTORY_CUSTOMER_READONLY);
            return Optional.empty();
        } catch (Exception e) {
            log.warn(
                    "Could not resolve Google Workspace customer id (delegating user {}): {}",
                    delegatingAdminEmail,
                    e.getMessage());
            return Optional.empty();
        }
    }
}
