import { AppPassword } from "./AppPassword";

export interface UserAppPasswords {
    id: string;
    name: string;
    email: string;
    role: string;
    twoFactorEnabled: boolean;
    appPasswords: AppPassword[];
}