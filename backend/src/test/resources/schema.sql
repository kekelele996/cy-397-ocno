CREATE TABLE contract_templates (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  type VARCHAR(32),
  title VARCHAR(120),
  content CLOB,
  variables CLOB
);

CREATE TABLE contracts (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT,
  template_id BIGINT,
  title VARCHAR(120),
  content CLOB,
  status VARCHAR(32),
  signed_at TIMESTAMP,
  signers CLOB
);

CREATE TABLE contract_signing_rounds (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  contract_id BIGINT NOT NULL,
  round_number INT NOT NULL,
  status VARCHAR(32) NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  completed_at TIMESTAMP,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (contract_id, round_number)
);

CREATE TABLE contract_signer_invitations (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  round_id BIGINT NOT NULL,
  contract_id BIGINT NOT NULL,
  signer_name VARCHAR(120) NOT NULL,
  signer_email VARCHAR(120),
  identity_hash VARCHAR(64) NOT NULL,
  identity_last_four VARCHAR(4) NOT NULL,
  invitation_token VARCHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL,
  invited_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  first_signed_at TIMESTAMP,
  signature_value VARCHAR(255),
  UNIQUE (round_id, identity_hash),
  UNIQUE (invitation_token)
);
