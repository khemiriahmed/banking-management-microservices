export type TransferStatus =
  | 'REQUESTED'
  | 'VALIDATING'
  | 'PROCESSING'
  | 'COMPLETED'
  | 'FAILED';

export type CompensationStatus = 'NONE' | 'PENDING' | 'NOT_REQUIRED' | 'COMPLETED' | 'FAILED';

export interface TransferStatusChange {
  from: TransferStatus | null;
  to: TransferStatus;
  reason: string | null;
  createdAt: string;
}

export interface Transfer {
  id: string;
  ownerUserId: string;
  sourceAccountId: string;
  beneficiaryId: string;
  destinationReference: string;
  amount: number;
  currency: string;
  reason: string | null;
  status: TransferStatus;
  failureReason: string | null;
  compensationStatus: CompensationStatus;
  createdAt: string;
  updatedAt: string;
  history: TransferStatusChange[];
}

export interface TransferPage {
  content: Transfer[];
  page: number;
  size: number;
  totalElements: number;
  last: boolean;
}

export interface CreateTransferRequest {
  sourceAccountId: string;
  beneficiaryId: string;
  amount: number;
  currency: string;
  reason?: string;
}
