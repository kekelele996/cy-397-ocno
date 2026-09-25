CREATE TABLE IF NOT EXISTS contract_templates (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  type VARCHAR(32),
  title VARCHAR(120),
  content TEXT,
  variables JSON
);

CREATE TABLE IF NOT EXISTS contracts (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT,
  template_id BIGINT,
  title VARCHAR(120),
  content MEDIUMTEXT,
  status VARCHAR(32),
  signed_at DATETIME,
  signers JSON NOT NULL DEFAULT (JSON_ARRAY())
);

CREATE TABLE IF NOT EXISTS contract_signing_rounds (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  contract_id BIGINT NOT NULL,
  round_number INT NOT NULL,
  status VARCHAR(32) NOT NULL,
  expires_at DATETIME NOT NULL,
  completed_at DATETIME,
  created_at DATETIME NOT NULL,
  UNIQUE KEY uk_contract_round (contract_id, round_number),
  KEY idx_round_status_expires_at (status, expires_at)
);

CREATE TABLE IF NOT EXISTS contract_signer_invitations (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  round_id BIGINT NOT NULL,
  contract_id BIGINT NOT NULL,
  signer_name VARCHAR(120) NOT NULL,
  signer_email VARCHAR(120),
  identity_hash VARCHAR(64) NOT NULL,
  identity_last_four VARCHAR(4) NOT NULL,
  invitation_token VARCHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL,
  invited_at DATETIME NOT NULL,
  first_signed_at DATETIME,
  signature_value VARCHAR(255),
  UNIQUE KEY uk_round_identity_hash (round_id, identity_hash),
  UNIQUE KEY uk_invitation_token (invitation_token),
  KEY idx_contract_status (contract_id, status),
  KEY idx_round_status (round_id, status)
);

CREATE TABLE IF NOT EXISTS legal_tickets (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT,
  type VARCHAR(32),
  description TEXT,
  status VARCHAR(32),
  attachments JSON
);

CREATE TABLE IF NOT EXISTS legal_faq (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  category VARCHAR(60),
  question VARCHAR(200),
  answer TEXT
);

INSERT INTO legal_faq(category, question, answer)
VALUES ('合同纠纷','合同逾期未签署怎么办','可先发出书面催告并保存沟通证据。')
ON DUPLICATE KEY UPDATE question=question;
