/// <reference types="cypress" />

describe("Security preferences (stubbed API)", () => {
  beforeEach(() => {
    cy.stubLoggedInSession();
    cy.stubSecurityPreferencesApis();
  });

  it("shows page title, guidance notice and section accordions", () => {
    cy.visitApp("/security-preferences");
    cy.wait("@authMe");
    cy.wait("@preferencesGet");
    cy.viewport(1280, 900);

    cy.location("pathname").should("eq", "/security-preferences");

    cy.get("app-security-preferences").within(() => {
      cy.contains("Beveiligingsvoorkeuren").should("be.visible");
      cy.contains("Bepaal welke controles meldingen geven").should("be.visible");

      cy.contains("Over deze instellingen").scrollIntoView().should("be.visible");
      cy.contains("Per onderdeel stelt u in").scrollIntoView().should("be.visible");

      cy.contains("h2", "Gebruikers & groepen").scrollIntoView().should("be.visible");
      cy.contains("3 van 3 controles actief").should("be.visible");

      cy.contains("Gebruikers zonder tweefactorauthenticatie (2FA)")
        .scrollIntoView()
        .should("be.visible");

      cy.contains("h2", "Domein & DNS").scrollIntoView().should("be.visible");
      cy.get("select#domain-dns-impSpf").should("be.visible").find("option").contains("Standaard");
    });
  });

  it("disabling a toggle sends PUT and refreshes disabled keys", () => {
    cy.visitApp("/security-preferences");
    cy.wait("@authMe");
    cy.wait("@preferencesGet");
    cy.viewport(1280, 900);

    cy.get("app-security-preferences").within(() => {
      cy.get("#users-groups-2fa").scrollIntoView().click({ force: true });

      cy.wait("@preferencesPut")
        .its("request.body")
        .should("deep.include", {
          section: "users-groups",
          preferenceKey: "2fa",
          enabled: false,
          value: null,
        });

      cy.wait("@preferencesDisabled");

      cy.contains("2 van 3 controles actief").scrollIntoView().should("be.visible");
    });
  });
});
