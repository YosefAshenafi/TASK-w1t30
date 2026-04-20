export type Role = 'STUDENT' | 'CORPORATE_MENTOR' | 'FACULTY_MENTOR' | 'ADMIN';
export type UserStatus = 'PENDING' | 'ACTIVE' | 'LOCKED' | 'SUSPENDED' | 'DELETED';
export type Classification = 'PUBLIC' | 'INTERNAL' | 'CONFIDENTIAL' | 'RESTRICTED';

export interface UserProfile {
  id: string;
  username: string;
  displayName: string;
  role: Role;
  status: UserStatus;
  organizationId: string | null;
  allowedIpRanges: string[];
  lastLoginAt: string | null;
  createdAt: string;
}
