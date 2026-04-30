/// <reference types="cypress" />

describe("Shared drives (stubbed API)", () => {
  beforeEach(() => {
    cy.stubLoggedInSession();
    cy.stubSharedDrivesApis();
  });

  it("shows KPIs, drive cards, and risk labels from stubbed data", () => {
    cy.visitApp("/shared-drives");
    cy.wait("@authMe");
    cy.viewport(1280, 800);

    cy.location("pathname").should("eq", "/shared-drives");
    cy.contains("Gedeelde Drives Controle").should("be.visible");
    cy.contains("Vernieuw Data").should("be.visible");
    cy.contains("Alle drives").should("be.visible");

    cy.contains("span", "Totaal drives")
      .parent()
      .within(() => {
        cy.contains("span", "7").should("be.visible");
      });

    cy.contains("article", "E2E Marketing Drive").within(() => {
      cy.contains("Laag risico").should("be.visible");
      cy.contains("TOTAAL LEDEN")
        .parent()
        .within(() => {
          cy.get("span.font-medium.text-slate-600").should("have.text", "8");
        });
      cy.get("span")
        .filter((_, el) => el.textContent?.trim() === "Ja")
        .should("have.length", 2);
    });

    cy.contains("article", "E2E Finance Drive").within(() => {
      cy.contains("Hoog risico").should("be.visible");
      cy.get("span")
        .filter((_, el) => el.textContent?.trim() === "Nee")
        .should("have.length", 2);
    });
  });

  it("shows empty state when search query returns no drives", () => {
    cy.visitApp("/shared-drives");
    cy.wait("@authMe");
    cy.viewport(1280, 800);

    cy.contains("E2E Marketing Drive").should("be.visible");

    cy.get("app-shared-drives app-search-bar input").clear().type("__E2E_EMPTY__");
    cy.wait(400);

    cy.contains("Geen drives gevonden").should("be.visible");
  });
});
