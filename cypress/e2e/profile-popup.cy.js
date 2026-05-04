/// <reference types="cypress" />

describe("Profile popup (stubbed session)", () => {
  beforeEach(() => {
    cy.stubLoggedInSession();
  });

  it("opens from the navbar and shows stubbed user details (nl)", () => {
    cy.visitApp("/home");
    cy.wait("@authMe");
    cy.viewport(1280, 900);

    cy.get('[data-testid="nav-profile-name"]').should("contain", "E2E User").click();

    cy.get("app-profile").within(() => {
      cy.contains("h3", "Profiel").should("be.visible");
      cy.contains("Bekijk uw profielinformatie").should("be.visible");
      cy.contains("E2E User").should("be.visible");
      cy.contains("e2e-user@test.local").should("be.visible");
      cy.contains("Test Org").should("be.visible");
      cy.contains("Super Admin").should("be.visible");
      cy.contains("a", "Mijn Google Account beheren").should("be.visible");
      cy.contains("button", "Uitloggen").should("be.visible");
    });
  });

  it("closes when the header close button is used", () => {
    cy.visitApp("/home");
    cy.wait("@authMe");
    cy.viewport(1280, 900);

    cy.get('[data-testid="nav-profile-name"]').click();
    cy.get("app-profile").should("be.visible");

    cy.get("app-profile").find('button[aria-label="Sluiten"]').click();
    cy.get("app-profile").should("not.exist");
  });

  it("closes when clicking the dimmed overlay outside the card", () => {
    cy.visitApp("/home");
    cy.wait("@authMe");
    cy.viewport(1280, 900);

    cy.get('[data-testid="nav-profile-name"]').click();
    cy.get(".profile-overlay").should("be.visible");

    cy.get(".profile-overlay").click("topLeft");
    cy.get("app-profile").should("not.exist");
  });
});
