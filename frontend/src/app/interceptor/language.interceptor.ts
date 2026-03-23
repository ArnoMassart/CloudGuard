import { HttpInterceptorFn } from '@angular/common/http';

export const languageInterceptor: HttpInterceptorFn = (req, next) => {
  // 1. Voorkom dat we Transloco's eigen bestands-requests onderscheppen
  if (req.url.includes('/assets/i18n/')) {
    return next(req);
  }

  // 2. Haal de taal direct en veilig uit localStorage (voorkomt Circular Dependency)
  const activeLang = localStorage.getItem('currentLang') || 'nl';

  // 3. Kloon het request en voeg de header toe
  const languageReq = req.clone({
    setHeaders: {
      'Accept-Language': activeLang,
    },
  });

  // 4. Stuur het door naar de backend
  return next(languageReq);
};
