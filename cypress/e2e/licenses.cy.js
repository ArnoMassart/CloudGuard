/// <reference types="cypress" />

describe("Licenses (stubbed API)", () => {
  beforeEach(() => {
    cy.stubLoggedInSession();
    cy.stubLicensesApis();
  });

  it("shows KPIs, licentietype table, chart block and inactive user row", () => {
    cy.visitApp("/licenses");
    cy.wait("@authMe");
    cy.viewport(1280, 900);

    cy.location("pathname").should("eq", "/licenses");
    cy.contains("Licenties Overzicht").should("be.visible");

    cy.contains("span", "Totaal toegewezen")
      .parent()
      .within(() => {
        cy.contains("span", "120").should("be.visible");
      });

    cy.contains("span", "Ongebruikte Licenties")
      .parent()
      .within(() => {
        cy.contains("span", "5").should("be.visible");
      });

    cy.contains("Licentiegebruik per Type").should("be.visible");
    cy.contains("Licentietype Overzicht").should("be.visible");

    cy.contains("tr", "E2E Google Workspace Enterprise").within(() => {
      cy.contains("td", "80").should("be.visible");
    });
    cy.contains("tr", "E2E Business Starter").within(() => {
      cy.contains("td", "40").should("be.visible");
    });

    cy.contains("Inactieve Gebruikers met Licenties").should("be.visible");
    cy.contains("inactive.e2e@example.com").should("be.visible");
    cy.contains("Enterprise Plus").should("be.visible");
    cy.contains("Uit").should("be.visible");
  });
});
