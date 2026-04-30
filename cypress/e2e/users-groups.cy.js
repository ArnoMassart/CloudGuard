/// <reference types="cypress" />

describe("Users and groups (stubbed API)", () => {
  beforeEach(() => {
    cy.stubLoggedInSession();
    cy.stubUsersGroupsApis();
  });

  it("loads the users tab with KPIs and a user row", () => {
    cy.visit("/users-groups", {
      onBeforeLoad(win) {
        win.sessionStorage.setItem("has_seen_splash", "true");
        win.sessionStorage.removeItem("user-group-section");
      },
    });
    cy.viewport(1280, 800);
    cy.location("pathname").should("eq", "/users-groups");

    cy.contains("Gebruikers & Groepen").should("be.visible");
    cy.get("app-users-section").within(() => {
      cy.contains("42").should("exist");
      cy.contains("E2E Person").should("be.visible");
      cy.contains("person@example.com").should("be.visible");
    });
  });

  it("shows the groups tab when selected", () => {
    cy.visit("/users-groups", {
      onBeforeLoad(win) {
        win.sessionStorage.setItem("has_seen_splash", "true");
        win.sessionStorage.removeItem("user-group-section");
      },
    });
    cy.viewport(1280, 800);

    cy.get("app-users-groups").contains("button", "Groepen").click();
    cy.get("app-groups-section").within(() => {
      cy.contains("E2E Test Group").should("be.visible");
      cy.contains("15").should("exist");
    });
  });
});
