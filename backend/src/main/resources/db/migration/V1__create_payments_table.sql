CREATE TABLE payments (
                          id UUID PRIMARY KEY,
                          amount_sats BIGINT NOT NULL,
                          bitcoin_address VARCHAR(100) NOT NULL,
                          status VARCHAR(20) NOT NULL,
                          created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                          expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
                          paid_at TIMESTAMP WITH TIME ZONE,

                          CONSTRAINT chk_payments_amount_positive
                              CHECK (amount_sats > 0)
);