export interface Beneficiary { id: string; ownerUserId: string; displayName: string; destinationAccountReference: string; status: string; createdAt: string; }
export interface CreateBeneficiaryRequest { displayName: string; destinationAccountReference: string; }
export interface UpdateBeneficiaryRequest { displayName?: string; destinationAccountReference?: string; status?: string; }
