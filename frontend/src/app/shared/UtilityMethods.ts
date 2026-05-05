import { Router } from '@angular/router';
import { RoleLabels, User } from '../models/users/User';

export class UtilityMethods {
  static openAdminPage(link: string) {
    window.open(link);
  }

  static goToPage(route: string, router: Router) {
    router.navigate([route]);
  }
}
