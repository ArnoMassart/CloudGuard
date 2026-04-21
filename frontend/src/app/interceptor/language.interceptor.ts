import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { TranslocoService } from '@jsverse/transloco';

export const languageInterceptor: HttpInterceptorFn = (req, next) => {
  // Do not intercept Transloco dictionary requests.
  if (req.url.includes('/assets/i18n/')) {
    return next(req);
  }

  const translocoService = inject(TranslocoService);
  const activeLang =
    translocoService.getActiveLang() ||
    localStorage.getItem('currentLang') ||
    'nl';

  const languageReq = req.clone({
    setHeaders: {
      'Accept-Language': activeLang,
    },
  });

  return next(languageReq);
};
