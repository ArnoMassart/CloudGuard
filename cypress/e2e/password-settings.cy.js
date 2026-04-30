/// <reference types="cypress" />

describe("Password settings (stubbed API)", () => {
  beforeEach(() => {
    cy.stubLoggedInSession();
    cy.stubPasswordSettingsApis();
  });

  it("shows KPIs, admins section, and OU policy preview without warnings", () => {
    cy.visitApp("/password-settings");
    cy.wait("@authMe");
    cy.viewport(1280, 800);

    cy.location("pathname").should("eq", "/password-settings");
    cy.contains("Wachtwoordinstellingen").should("be.visible");
    cy.contains("Vernieuw Data").should("be.visible");

    cy.contains("span", "Aantal Organisatie-eenheden")
      .parent()
      .within(() => {
        cy.contains("span", "1").should("be.visible");
      });

    cy.contains("Admins zonder Security Keys").should("be.visible");
    cy.contains("Alle admins hebben security keys ingeschreven").should("be.visible");

    cy.contains("Wachtwoordbeleid per Organisatie-Eenheid").should("be.visible");
    cy.contains("E2E Root").should("be.visible");
    cy.contains("Goed geconfigureerd").should("be.visible");
  });

  it("expands an OU row to show policy and 2SV details", () => {
    cy.visitApp("/password-settings");
    cy.wait("@authMe");
    cy.viewport(1280, 800);

    cy.contains("tr", "E2E Root").click();

    cy.get("app-password-settings").within(() => {
      cy.contains("Minimale lengte").scrollIntoView().should("be.visible");
      cy.contains("14 tekens").should("be.visible");
      cy.contains("Sterke wachtwoorden").should("be.visible");
      cy.contains("2-Step Verification").should("be.visible");
      cy.contains("Verplicht").should("be.visible");
    });
  });
});
