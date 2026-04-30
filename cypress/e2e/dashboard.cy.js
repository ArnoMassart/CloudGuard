describe('Dashboard page (stubbed session)', ()=>{
    beforeEach(()=>{
        cy.stubLoggedInSession();
    })
    it('successfully shows dashboard overview for a logged-in user', ()=>{
        cy.visitApp('/home');
        cy.viewport(1280, 800);
        cy.location('pathname').should('eq', '/home');
        cy.contains('Security Dashboard Overzicht').should('be.visible');

        //match data with DASHBOARD_OVERVIEW stub
        cy.contains('10').should('be.visible');
        cy.contains('3').should('be.visible');
        cy.contains('80').should('be.visible');

        cy.contains('Algemene Beveiligingsscore').should('be.visible');
    });
});