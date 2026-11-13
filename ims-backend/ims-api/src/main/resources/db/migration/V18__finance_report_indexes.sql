-- Phase 4.4: report query indexes for aging and collections.

CREATE INDEX idx_invoices_due_status ON invoices (institute_id, due_date, status);
CREATE INDEX idx_payments_paid_status ON payments (institute_id, paid_at, status);
CREATE INDEX idx_alloc_payment_invoice ON payment_allocations (institute_id, payment_id, invoice_id);
