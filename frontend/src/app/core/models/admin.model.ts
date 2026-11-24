export interface AuditEventItem { id: string; eventId: string; eventType: string; actorId: string | null; action: string; targetId: string | null; occurredAt: string; metadata: string | null; }
export interface AuditPage { content: AuditEventItem[]; page: number; size: number; totalElements: number; last: boolean; }
export interface UserProfile { authUserId: string; email: string; firstName?: string; lastName?: string; phone?: string; }
export interface UserPage { content: UserProfile[]; page: number; size: number; totalElements: number; totalPages: number; last: boolean; }
