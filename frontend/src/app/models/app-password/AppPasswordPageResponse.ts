import { AppPassword } from "./AppPassword";

export interface AppPasswordPageResponse {
    users: Array<{ id: string; name: string; email: string; role: string; tsv: boolean; passwords: AppPassword[] }>;
    nextPageToken: string | null;
}