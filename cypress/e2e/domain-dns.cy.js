/// <reference types="cypress" />

describe("Domain & DNS (stubbed API)", () => {
  beforeEach(() => {
    cy.stubLoggedInSession();
    cy.stubDomainDnsApis();
  });

  it("shows domain KPIs, domain table and security checks for stubbed DNS", () => {
    cy.visitApp("/domain-dns");
    cy.wait("@authMe");
    cy.viewport(1400, 900);

    cy.location("pathname").should("eq", "/domain-dns");
    cy.contains("Domein & DNS").should("be.visible");
    cy.contains("Vernieuw Data").should("be.visible");

    cy.contains("span", "Totaal domeinen")
      .parent()
      .within(() => {
        cy.contains("span", "1").should("be.visible");
      });

    cy.contains("span", "Geldige DNS records")
      .parent()
      .within(() => {
        cy.contains("span", "2").should("be.visible");
      });

    cy.contains("e2e-example.com").should("be.visible");
    cy.contains("Primary Domain").should("be.visible");
    cy.contains("Geverifieerd").should("be.visible");

    cy.get("app-domain-dns").within(() => {
      cy.contains("Beveiligingscontroles", { timeout: 20000 }).should("be.visible");
      cy.contains("SPF record is correct geconfigureerd").should("be.visible");
      cy.contains("MX records verwijzen correct naar Google").should("be.visible");
      cy.contains("Geldig").should("exist");
    });
  });

  it("expands a DNS record row to show description and full value", () => {
    cy.visitApp("/domain-dns");
    cy.wait("@authMe");
    cy.viewport(1400, 900);
    cy.contains("DNS Records", { timeout: 20000 }).should("be.visible");
    cy.contains("tr", "SPF").first().click();

    cy.get("app-domain-dns").within(() => {
      cy.contains(
        "SPF record authoriseert Google om emails te verzenden namens uw domein"
      ).should("be.visible");
      cy.contains("v=spf1 include:_spf.google.com ~all").should("be.visible");
    });
  });
});
