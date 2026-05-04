/// <reference types="cypress" />

describe("App access (stubbed API)", () => {
  beforeEach(() => {
    cy.stubLoggedInSession();
    cy.stubAppAccessApis();
  });

  it("shows KPIs and a stubbed connected app card", () => {
    cy.visitApp("/app-access");
    cy.wait("@authMe");
    cy.viewport(1280, 800);

    cy.location("pathname").should("eq", "/app-access");
    cy.contains("App Toegang").should("be.visible");
    cy.contains("Vernieuw Data").should("be.visible");
    cy.contains("Verbonden Apps").should("be.visible");

    cy.contains("span", "Gekoppelde 3rd-party apps")
      .parent()
      .within(() => {
        cy.contains("span", "12").should("be.visible");
      });

    cy.contains("Hoog risico").should("be.visible");
    cy.contains("E2E Connected App").should("be.visible");
    cy.contains("MARKETPLACE").should("be.visible");
    cy.get("app-app-access article").first().within(() => {
      cy.contains("12%").should("be.visible");
      cy.contains("3 permissies").should("be.visible");
    });
  });

  it("expands an app to show data-access rows", () => {
    cy.visitApp("/app-access");
    cy.wait("@authMe");
    cy.viewport(1280, 800);

    cy.contains("article", "E2E Connected App").click();

    cy.get("app-app-access").within(() => {
      cy.contains("Data toegang").should("be.visible");
      cy.contains("Gmail API").should("be.visible");
      cy.contains("E-mailadres inzien").should("be.visible");
    });
  });

  it("shows empty state when search returns no apps", () => {
    cy.visitApp("/app-access");
    cy.wait("@authMe");
    cy.viewport(1280, 800);

    cy.contains("E2E Connected App").should("be.visible");

    cy.get("app-app-access app-search-bar input").clear().type("__E2E_EMPTY__");
    cy.wait(400);

    cy.contains("Geen apps gevonden").should("be.visible");
  });
});
