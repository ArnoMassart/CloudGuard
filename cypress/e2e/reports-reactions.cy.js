/// <reference types="cypress" />

describe("Notifications & Feedback (reports-reactions, stubbed API)", () => {
  beforeEach(() => {
    cy.stubLoggedInSession();
    cy.stubNotificationsApis();
  });

  it("shows KPIs, critical banner, filter chips and stubbed notifications", () => {
    cy.visitApp("/reports-reactions");
    cy.wait("@authMe");
    cy.wait("@notificationsList");
    cy.viewport(1280, 900);

    cy.location("pathname").should("eq", "/reports-reactions");

    cy.get("app-reports-reactions").within(() => {
      cy.contains("Meldingen & Feedback").should("be.visible");

      cy.contains("span", "Totaal")
        .parent()
        .within(() => {
          cy.contains("span", "3").should("be.visible");
        });

      cy.contains("span", "Kritiek")
        .parent()
        .within(() => {
          cy.contains("span", "1").should("be.visible");
        });

      cy.contains("span", "In behandeling")
        .first()
        .parent()
        .within(() => {
          cy.contains("span", "1").should("be.visible");
        });

      cy.contains("1 kritieke beveiligingswaarschuwing").scrollIntoView().should("be.visible");

      cy.contains("button", "Alle (3)").scrollIntoView().should("be.visible");

      cy.contains("h3", "E2E kritieke melding titel").scrollIntoView().should("be.visible");
      cy.contains("h3", "E2E info in behandeling").scrollIntoView().should("be.visible");
      cy.contains("h3", "E2E opgeloste waarschuwing").scrollIntoView().should("be.visible");
    });
  });

  it("filtered view shows only solved notifications", () => {
    cy.visitApp("/reports-reactions");
    cy.wait("@authMe");
    cy.wait("@notificationsList");
    cy.viewport(1280, 900);

    cy.get("app-reports-reactions").within(() => {
      cy.contains("button", "Opgelost (1)").click();

      cy.contains("h3", "E2E opgeloste waarschuwing").scrollIntoView().should("be.visible");
      cy.contains("E2E kritieke melding titel").should("not.exist");
      cy.contains("E2E info in behandeling").should("not.exist");
    });
  });

  it("submits feedback and marks the notification as in progress", () => {
    cy.visitApp("/reports-reactions");
    cy.wait("@authMe");
    cy.wait("@notificationsList");
    cy.viewport(1280, 900);

    cy.get("app-reports-reactions").within(() => {
      cy.contains("h3", "E2E kritieke melding titel")
        .scrollIntoView()
        .closest("div.overflow-hidden")
        .within(() => {
          cy.contains("button", "Stuur feedback").click();
          cy.get("textarea").type("E2E feedback from Cypress.");
          cy.contains("button", "Verstuur").click();
        });

      cy.wait("@notificationsFeedback")
        .its("request.body")
        .should("deep.include", {
          source: "e2e-google-workspace",
          notificationType: "user-control",
          feedbackText: "E2E feedback from Cypress.",
        });

      cy.contains("h3", "E2E kritieke melding titel")
        .scrollIntoView()
        .closest("div.overflow-hidden")
        .within(() => {
          cy.contains("In behandeling").should("be.visible");
        });
    });
  });

  it("refresh triggers sync then reloads notifications", () => {
    cy.visitApp("/reports-reactions");
    cy.wait("@authMe");
    cy.wait("@notificationsList");
    cy.viewport(1280, 900);

    cy.get("app-reports-reactions").within(() => {
      cy.contains("button", "Vernieuwen").click();
      cy.wait("@notificationsSync");
      cy.wait("@notificationsList");

      cy.contains("h3", "E2E kritieke melding titel").scrollIntoView().should("be.visible");
    });
  });
});
