export interface NotificationItem { id: string; eventType: string; title: string; message: string; read: boolean; occurredAt: string; createdAt: string; }
export interface NotificationPage { content: NotificationItem[]; page: number; size: number; totalElements: number; last: boolean; }
