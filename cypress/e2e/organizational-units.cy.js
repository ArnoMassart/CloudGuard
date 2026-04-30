/// <reference types="cypress" />

describe("Organizational units (stubbed API)", () => {
  beforeEach(() => {
    cy.stubLoggedInSession();
    cy.stubOrganizationalUnitsApis();
  });
-
  it("shows structure, root OU tree row, beleidsregels en stubbed policies", () => {
    cy.visit("/organizational-units", {
      onBeforeLoad(win) {
        win.sessionStorage.setItem("has_seen_splash", "true");
      },
    });
    cy.viewport(1280, 800);

    cy.location("pathname").should("eq", "/organizational-units");
    cy.contains("Organisatie-eenheden").should("be.visible");
    cy.contains("Structuur").should("be.visible");
    cy.contains("Vernieuw Data").should("be.visible");

    cy.contains("button", "E2E Root OU").should("be.visible");
    cy.contains("100").should("exist");

    cy.contains("h3", "Beleidsregels").should("be.visible");
    cy.contains("E2E Mobile Policy").should("be.visible");
    cy.contains("Stubbed policy row for Cypress").should("be.visible");
  });

  it("updates the detail header when a child organisational unit is selected", () => {
    cy.visit("/organizational-units", {
      onBeforeLoad(win) {
        win.sessionStorage.setItem("has_seen_splash", "true");
      },
    });
    cy.viewport(1280, 800);

    cy.contains("button", "E2E Child OU").should("be.visible").click();

    cy.get("app-organizational-units").contains("h2", "E2E Child OU").should("be.visible");
  });
});
