export interface TransactionEntry {
  accountId: string | null;
  destinationReference: string | null;
  side: 'DEBIT' | 'CREDIT';
  amount: number;
  currency: string;
}

export interface TransactionRecord {
  id: string;
  externalReference: string;
  transferId: string;
  type: 'TRANSFER' | 'REVERSAL';
  amount: number;
  currency: string;
  occurredAt: string;
  status: 'CONFIRMED';
  entries: TransactionEntry[];
}

export interface TransactionPage {
  content: TransactionRecord[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
