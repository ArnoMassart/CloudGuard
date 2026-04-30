/// <reference types = "cypress" />

describe('Login page', () => {
  it('successfully loads with branding and Google CTA', () => {
    cy.visitApp('/login');
    cy.contains('h1', 'CloudGuard').should('be.visible');
    cy.get('[data-testid="google-login-button"]').scrollIntoView().should('be.visible').and('be.enabled');
  })
});

describe('Login session (stubbed API)', () => {
  it('reaches /home when session APIs report a logged-in user', () => {
    cy.stubLoggedInSession();
    cy.visitApp('/home');
    cy.location('pathname').should('eq', '/home');
    cy.viewport(1280, 800);
    cy.get('app-navbar .navbar-scroll').scrollTo('bottom', { ensureScrollable: false });
    cy.get('[data-testid="nav-profile-name"]').should('be.visible');
  });

  it('sends guests from /home to /login with returnUrl', () =>{
    cy.stubGuestSession();
    cy.visitApp('/home');
    cy.location('pathname').should('eq', '/login');
    cy.location('search').should('include', 'returnUrl');
  });
});