/// <reference types="cypress" />

describe("Devices (stubbed API)", () => {
  beforeEach(() => {
    cy.stubLoggedInSession();
    cy.stubDevicesApis();
  });

  it("shows KPIs and a stubbed device row", () => {
    cy.visitApp("/devices");
    cy.wait("@authMe");
    cy.viewport(1280, 800);

    cy.location("pathname").should("eq", "/devices");
    cy.contains("Apparaten Controle").should("be.visible");
    cy.contains("Vernieuw Data").should("be.visible");
    cy.contains("Alle apparaten").should("be.visible");

    cy.contains("span", "Totaal apparaten")
      .parent()
      .within(() => {
        cy.contains("span", "42").should("be.visible");
      });

    cy.contains("Non-compliant").should("be.visible");
    cy.contains("E2E Person").should("be.visible");
    cy.contains("e2e.person@example.com").should("be.visible");
    cy.contains("E2E Pixel 8").should("be.visible");
    cy.contains("Android 15").should("be.visible");

    cy.get("app-devices tbody").within(() => {
      cy.root().contains("85%").should("be.visible");
      cy.root().contains("Approved").should("be.visible");
    });
  });

  it("expands a device row to show security factor cards", () => {
    cy.visitApp("/devices");
    cy.wait("@authMe");
    cy.viewport(1280, 800);

    cy.contains("tr", "E2E Pixel 8").click();

    cy.get("app-devices").within(() => {
      cy.contains("Vergrendelscherm").should("be.visible");
      cy.contains("Encryptie").should("be.visible");
      cy.contains("OS Versie").should("be.visible");
      cy.contains("Integriteit").should("be.visible");
    });
  });

  it("shows no devices message when filtering to status Blocked", () => {
    cy.visitApp("/devices");
    cy.wait("@authMe");
    cy.viewport(1280, 800);

    cy.get("app-devices").find("select").first().select("Blocked");

    cy.contains("Geen apparaten gevonden").should("be.visible");
  });
});
