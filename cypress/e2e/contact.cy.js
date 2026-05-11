describe('Contact page', () => {
  beforeEach(() => {
    cy.stubLoggedInSession();
    cy.intercept('POST', /\/api\/contact\/send(?:\/)?(?=\?|$)/, {
      statusCode: 200,
      body: '',
    }).as('contactSend');
  });

  it('loads and submits the contact form against the API', () => {
    cy.visitApp('/contact');
    cy.viewport(1280, 800);
    cy.location('pathname').should('eq', '/contact');

    cy.get('#subject').clear().type('E2E onderwerp');
    cy.get('#message').clear().type('Dit is een testbericht met voldoende tekens voor validatie.');
    cy.contains('button[type="submit"]', 'Bericht verzenden').click();

    cy.wait('@contactSend').then((interception) => {
      const raw = interception.request.body;
      const parsed = typeof raw === 'string' ? JSON.parse(raw) : raw;
      expect(parsed).to.include({
        topic: 'support',
        subject: 'E2E onderwerp',
      });
      expect(parsed.message).to.be.a('string').and.have.length.of.at.least(10);
    });

    cy.contains('Bericht verzonden!').should('be.visible');
  });
});
