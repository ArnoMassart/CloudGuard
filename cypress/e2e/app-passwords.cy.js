/// <reference types="cypress" />

describe("App passwords (stubbed API)", () => {
  beforeEach(() => {
    cy.stubLoggedInSession();
    cy.stubAppPasswordsApis();
  });

  it("shows KPIs, user row, app-password count and disabled Volgende pagination", () => {
    cy.visitApp("/app-passwords");
    cy.wait("@authMe");
    cy.viewport(1280, 800);

    cy.location("pathname").should("eq", "/app-passwords");
    cy.contains("App Wachtwoorden").should("be.visible");
    cy.contains("Vernieuw Data").should("be.visible");
    cy.contains("Gebruikers met app-wachtwoorden").should("be.visible");

    cy.contains("span", "Toegestaan")
      .parent()
      .within(() => {
        cy.contains("span", "Ja").should("be.visible");
      });

    cy.contains("span", "Totaal app-wachtwoorden")
      .parent()
      .within(() => {
        cy.contains("span", "7").should("be.visible");
      });

    cy.contains("E2E Worker").should("be.visible");
    cy.contains("e2e.worker@test.local").should("be.visible");
    cy.get("tbody").within(() => {
      cy.contains("Regular").should("be.visible");
      cy.contains("Enabled").should("be.visible");
    });

    cy.contains("Volgende").should("be.disabled");
  });

  it("expands a user row to show app password details", () => {
    cy.visitApp("/app-passwords");
    cy.wait("@authMe");
    cy.viewport(1280, 800);

    cy.contains("tr", "E2E Worker").click();

    cy.get("app-app-passwords").within(() => {
      cy.contains("Legacy Mail Client").should("be.visible");
      cy.contains("Aangemaakt").should("be.visible");
      cy.contains("Laatst gebruikt").should("be.visible");
      cy.contains("Nooit").should("be.visible");
    });
  });
});
