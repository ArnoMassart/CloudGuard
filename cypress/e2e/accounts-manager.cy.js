/// <reference types="cypress" />

describe("Accounts manager (stubbed API, Cloudmen staff)", () => {
  const visitAccountsManager = () => {
    cy.visit("/accounts-manager", {
      onBeforeLoad(win) {
        win.sessionStorage.setItem("has_seen_splash", "true");
        win.sessionStorage.removeItem("account-section");
      },
    });
  };

  beforeEach(() => {
    cy.stubLoggedInSession();
    cy.stubCloudmenStaffAccess();
    cy.stubAccountsManagerApis();
  });

  it("shows users tab with request list, all users table and org filter", () => {
    visitAccountsManager();
    cy.wait("@authMe");
    cy.wait("@cloudmenStaff");
    cy.wait(["@accountsUsersAll", "@accountsUsersNoRoles", "@accountsOrgsAll"]);
    cy.viewport(1280, 900);

    cy.location("pathname").should("eq", "/accounts-manager");

    cy.get("app-accounts-manager").within(() => {
      cy.contains("Accountbeheer").should("be.visible");
      cy.contains("Beheer hier als beheerder van CLOUDMEN").should("be.visible");

      cy.contains("button", "Gebruikers").should("be.visible");
      cy.contains("button", "Organisaties").should("be.visible");

      cy.contains("Aanvragen").scrollIntoView().should("be.visible");
      cy.contains("pending.roles@example.com").scrollIntoView().should("be.visible");
      cy.contains("Rol Toewijzen").scrollIntoView().should("be.visible");

      cy.contains("Alle gebruikers").scrollIntoView().should("be.visible");
      cy.contains("acct.e2e@example.com").scrollIntoView().should("be.visible");
      cy.contains("Super Admin").scrollIntoView().should("be.visible");
      cy.contains("CLOUDMEN admin").scrollIntoView().should("be.visible");

      cy.contains("span", "Filter:").scrollIntoView().should("be.visible");
      cy.contains("option", "Alle organisaties").should("exist");
      cy.contains("option", "E2E Organization").should("exist");
    });
  });

  it("organizations tab loads paged org list", () => {
    visitAccountsManager();
    cy.wait("@authMe");
    cy.wait("@cloudmenStaff");
    cy.wait(["@accountsUsersAll", "@accountsUsersNoRoles", "@accountsOrgsAll"]);
    cy.viewport(1280, 900);

    cy.get("app-accounts-manager").within(() => {
      cy.contains("button", "Organisaties").click();
    });

    cy.wait("@accountsOrgsPaged");

    cy.get("app-accounts-manager").within(() => {
      cy.contains("E2E Organization").scrollIntoView().should("be.visible");
      cy.contains("cust-e2e-1").scrollIntoView().should("be.visible");
      cy.contains("org.admin@e2e.example.com").scrollIntoView().should("be.visible");
    });
  });
});
